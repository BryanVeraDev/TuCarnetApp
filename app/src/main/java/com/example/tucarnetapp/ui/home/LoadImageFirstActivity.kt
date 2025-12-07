package com.example.tucarnetapp.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tucarnetapp.R
import com.example.tucarnetapp.data.repository.LivenessRepository
import com.example.tucarnetapp.data.repository.StudentRepository
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.utils.SnackRouter
import com.example.tucarnetapp.utils.showSnack
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class LoadImageFirstActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnGoHome: MaterialButton
    private lateinit var btnConfirmPhoto: MaterialButton
    private lateinit var loadingOverlay: View


    private val repository = LivenessRepository()
    private val studentRepository = StudentRepository()

    private var imageUri: Uri? = null
    private var tempCameraUri: Uri? = null

    private var attemptsLeft = 3

    private var TAG = "CompareFaces"


    // ---------------- UCROP ----------------

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let {
                    imageUri = it
                    imgProfile.setImageURI(it)
                    enableConfirmButton()
                }
            }
        }

    // --------------- CHOOSER ----------------

    private val chooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: tempCameraUri
                uri?.let { startCrop(it) }
            }
        }

    // ---------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SnackRouter.deliver(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_load_image_first)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        initViews()
        setupListeners()
        showAttemptsInfoDialog()
    }

    // ---------------- INIT ----------------

    private fun initViews() {
        imgProfile = findViewById(R.id.imgProfile)
        btnEdit = findViewById(R.id.btnEdit)
        btnGoHome = findViewById(R.id.btnGoHome)
        btnConfirmPhoto = findViewById(R.id.btnConfirmPhoto)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        btnConfirmPhoto.isEnabled = false
        btnConfirmPhoto.alpha = 0.5f
    }

    private fun setupListeners() {

        btnEdit.setOnClickListener { openChooser() }

        btnGoHome.setOnClickListener {
            startActivity(Intent(this, HomeScreenActivity::class.java))
            finish()
        }

        btnConfirmPhoto.setOnClickListener {

            if (imageUri == null) {
                SnackRouter.showNext(
                    "Debes seleccionar una foto primero",
                    top = true,
                    backgroundColor = R.color.ufps_error_claro,
                    textColor = R.color.ufps_error_principal
                )
                return@setOnClickListener
            }

            if (attemptsLeft <= 0) {
                SnackRouter.showNext(
                    "Has agotado el número de intentos",
                    top = true,
                    backgroundColor = R.color.ufps_error_claro,
                    textColor = R.color.ufps_error_principal
                )
                finish()
                return@setOnClickListener
            }

            compareFaces()
        }
    }

    // ---------------- COMPARE FACES ----------------

    private fun compareFaces() {

        val mime = contentResolver.getType(imageUri!!) ?: "image/jpeg"
        val selectedBase64 = "data:$mime;base64," + uriToBase64(imageUri!!)
        val referenceBase64 = intent.getStringExtra("referenceImageBase64")

        Log.d(TAG, "compareFaces() iniciado")
        Log.d(TAG, "imageUri: $imageUri")

        if (referenceBase64.isNullOrBlank()) {
            Log.e(TAG, "Imagen de referencia NULL o vacía")

            SnackRouter.showNext(
                "No se encontró la imagen de referencia",
                top = true,
                backgroundColor = R.color.ufps_error_claro,
                textColor = R.color.ufps_error_principal
            )
            return
        }

        lifecycleScope.launch {

            Log.d(TAG, "Mostrando loading")
            showLoading()
            btnConfirmPhoto.isEnabled = false

            try {
                // 1️⃣ Comparación facial
                Log.d(TAG, "Llamando a compareFaces API")

                val response = repository.compareFaces(
                    sourceImageBase64 = referenceBase64,
                    targetImageBase64 = selectedBase64
                )

                val similarity = response.matches.firstOrNull()?.similarity ?: 0.0
                val isApproved = similarity >= 90.0

                Log.d(TAG, "Similarity obtenida: $similarity")
                Log.d(TAG, "isApproved: $isApproved")
                Log.d(TAG, "URI Foto: $selectedBase64")

                var s3Key = ""
                val user = UserSession.currentUser
                if (isApproved) {
                    Log.d(TAG, "Similarity > 90 → subir foto a S3 (simulado)")
                    try {
                        val response = repository.uploadPhotoBase64(selectedBase64)

                        Log.d(TAG, "Foto subida exitosamente")
                        Log.d(TAG, "Respuesta del backend: $response")

                        s3Key = response.photoKey

                        if(user != null && s3Key.isNotBlank()){
                            val updatedUser = user.copy(
                                card_photo_key = s3Key
                            )

                            UserSession.setUser(updatedUser)

                            Log.d(TAG, "✅ UserSession actualizada con photoKey")
                        }

                    } catch (e: Exception) {
                        if (e is retrofit2.HttpException) {
                            val errorBody = e.response()?.errorBody()?.string()

                            Log.e(TAG, "HTTP ERROR ${e.code()}")
                            Log.e(TAG, "Error body: $errorBody")

                            showSnack(
                                errorBody ?: "Error HTTP ${e.code()}",
                                top = true,
                                backgroundColor = R.color.ufps_error_claro,
                                textColor = R.color.ufps_error_principal
                            )

                        } else {
                            Log.e(TAG, "Error inesperado al subir la foto", e)

                            showSnack(
                                "Error inesperado al subir la foto",
                                top = true,
                                backgroundColor = R.color.ufps_error_claro,
                                textColor = R.color.ufps_error_principal
                            )
                        }
                    }
                } else {
                    Log.d(TAG, "Similarity < 90 → NO se sube foto")
                }


                if (user != null) {
                    Log.d(
                        TAG,
                        "Usuario logeado: ${user.student_id}, enviando updateBiometricProfile"
                    )

                    studentRepository.updateBiometricProfile(
                        studentId = user.student_id,
                        cardPhotoKey = s3Key,
                        similarity = similarity
                    )


                    Log.d(TAG, "updateBiometricProfile enviado correctamente")
                } else {
                    Log.e(TAG, "UserSession.currentUser == null")
                }

                Log.d(TAG, "Ocultando loading")
                hideLoading()

                // 3️⃣ UI
                if (isApproved) {
                    Log.d(TAG, "Mostrando snack APROBADO")

                    SnackRouter.showNext(
                        "Verificación facial exitosa. Porcentaje de similitud: (${similarity.toInt()}%)",
                        top = true,
                        backgroundColor = R.color.ufps_success_claro,
                        textColor = R.color.ufps_success_oscuro
                    )

                    Log.d(TAG, "Navegando a HomeActivity")

                    startActivity(
                        Intent(this@LoadImageFirstActivity, HomeActivity::class.java)
                    )
                    finish()

                } else {
                    attemptsLeft--
                    btnConfirmPhoto.isEnabled = true

                    Log.d(TAG, "Mostrando snack RECHAZADO, attemptsLeft=$attemptsLeft")

                    showSnack(
                        "La foto no coincide. Intentos restantes: $attemptsLeft",
                        top = true,
                        backgroundColor = R.color.ufps_error_claro,
                        textColor = R.color.ufps_error_principal
                    )

                    if (attemptsLeft <= 0) {
                        Log.e(TAG, "Intentos agotados → cerrando Activity")
                        SnackRouter.showNext(
                            "Has agotado los 3 intentos. Regresarás a la pantalla de inicio",
                            top = true,
                            backgroundColor = R.color.ufps_error_claro,
                            textColor = R.color.ufps_error_principal
                        )
                        startActivity(
                            Intent(this@LoadImageFirstActivity, HomeScreenActivity::class.java)
                        )
                        finish()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error en compareFaces()", e)

                hideLoading()
                btnConfirmPhoto.isEnabled = true

                if (e is retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()

                    Log.e(TAG, "HTTP ERROR ${e.code()}")
                    Log.e(TAG, "Error body: $errorBody")

                    showSnack(
                        errorBody ?: "Error HTTP ${e.code()}",
                        top = true,
                        backgroundColor = R.color.ufps_error_claro,
                        textColor = R.color.ufps_error_principal
                    )

                } else {
                    Log.e(TAG, "Error inesperado al subir la foto", e)

                    showSnack(
                        "Error al verificar el rostro",
                        top = true,
                        backgroundColor = R.color.ufps_error_claro,
                        textColor = R.color.ufps_error_principal
                    )
                }
            }
        }
    }




    // ---------------- UTILITIES ----------------

    private fun uriToBase64(uri: Uri): String {
        val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: byteArrayOf()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun enableConfirmButton() {
        btnConfirmPhoto.isEnabled = true
        btnConfirmPhoto.alpha = 1f
    }

    // ---------------- CHOOSER ----------------

    private fun openChooser() {

        val galleryIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }

        val tempFile = File.createTempFile("photo_temp", ".jpg", cacheDir)
        tempCameraUri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            tempFile
        )

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempCameraUri)
        }

        val chooser = Intent.createChooser(galleryIntent, "Seleccionar foto")
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        chooserLauncher.launch(chooser)
    }

    // ---------------- UCROP ----------------

    private fun startCrop(sourceUri: Uri) {
        val destUri = Uri.fromFile(
            File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        val options = UCrop.Options().apply {
            setCompressionQuality(90)
            setToolbarTitle("Recorta tu foto")
            setFreeStyleCropEnabled(false)
            setToolbarColor(getColor(R.color.ufps_principal))
            setToolbarWidgetColor(getColor(android.R.color.white))
        }

        val uCrop = UCrop.of(sourceUri, destUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1080, 1080)
            .withOptions(options)

        cropLauncher.launch(uCrop.getIntent(this))
    }

    // ---------------- DIALOG ----------------

    private fun showAttemptsInfoDialog() {

        val view = View.inflate(this, R.layout.dialog_attempts_load_photo, null)

        val dialog = MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
            .setView(view)
            .setCancelable(false)
            .create()

        view.findViewById<MaterialButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showLoading() {
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }
}




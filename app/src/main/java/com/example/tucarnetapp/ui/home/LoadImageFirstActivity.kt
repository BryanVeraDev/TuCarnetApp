package com.example.tucarnetapp.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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

    private var imageUri: Uri? = null
    private var tempCameraUri: Uri? = null

    private var attemptsLeft = 3

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

        val selectedBase64 = uriToBase64(imageUri!!)
        val referenceBase64 = intent.getStringExtra("referenceImageBase64")

        if (referenceBase64.isNullOrBlank()) {
            SnackRouter.showNext(
                "No se encontró la imagen de referencia",
                top = true,
                backgroundColor = R.color.ufps_error_claro,
                textColor = R.color.ufps_error_principal
            )
            return
        }

        lifecycleScope.launch {

            showLoading()
            btnConfirmPhoto.isEnabled = false

            // ✅ SIEMPRE hay respuesta (no error)
            val response = repository.compareFaces(
                sourceImageBase64 = referenceBase64,
                targetImageBase64 = selectedBase64
            )

            val match = response.matches.firstOrNull()
            val similarity = match?.similarity ?: 0.0

            hideLoading()

            if (similarity >= 90.0) {
                // ✅ MATCH REAL
                SnackRouter.showNext(
                    "Verificación facial exitosa (${similarity.toInt()}%)",
                    top = true,
                    backgroundColor = R.color.ufps_success_claro,
                    textColor = R.color.ufps_success_oscuro
                )

                startActivity(
                    Intent(this@LoadImageFirstActivity, HomeActivity::class.java)
                )
                finish()

            } else {
                // ❌ NO MATCH
                attemptsLeft--
                btnConfirmPhoto.isEnabled = true

                showSnack(
                    "La foto no coincide. Intentos restantes: $attemptsLeft",
                    top = true,
                    backgroundColor = R.color.ufps_error_claro,
                    textColor = R.color.ufps_error_principal
                )

                if (attemptsLeft <= 0) {
                    finish()
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
            setToolbarColor(getColor(R.color.ufps_principal))
            setActiveControlsWidgetColor(getColor(R.color.ufps_principal))
            setFreeStyleCropEnabled(false)
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




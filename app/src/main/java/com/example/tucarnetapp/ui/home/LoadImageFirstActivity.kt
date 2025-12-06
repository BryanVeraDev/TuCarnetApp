package com.example.tucarnetapp.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tucarnetapp.R
import com.example.tucarnetapp.utils.SnackRouter
import com.google.android.material.button.MaterialButton
import android.view.View
import android.widget.ImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import java.io.File

class LoadImageFirstActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnGoHome: MaterialButton
    private lateinit var btnConfirmPhoto: MaterialButton

    private var imageUri: Uri? = null
    private var tempCameraUri: Uri? = null

    // ------------------ UCROP LAUNCHER ------------------

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {

                val resultUri = UCrop.getOutput(result.data!!)

                resultUri?.let { finalUri ->
                    imageUri = finalUri
                    imgProfile.setImageURI(finalUri)

                    // Activar botón después del recorte
                    btnConfirmPhoto.isEnabled = true
                    btnConfirmPhoto.setBackgroundTintList(
                        getColorStateList(R.color.ufps_secundario)
                    )
                    btnConfirmPhoto.setTextColor(
                        getColor(R.color.ufps_blanco_favorito)
                    )

                    btnConfirmPhoto.alpha = 0f
                    btnConfirmPhoto.animate().alpha(1f).setDuration(300).start()
                }
            }
        }

    // ------------------ CHOOSER LAUNCHER ------------------

    private val chooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data

                val uri = when {
                    data?.data != null -> data.data
                    tempCameraUri != null -> tempCameraUri
                    else -> null
                }

                uri?.let { selectedUri ->
                    startCrop(selectedUri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SnackRouter.deliver(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_load_image_first)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
        showAttemptsInfoDialog()
    }

    private fun initViews() {
        imgProfile = findViewById(R.id.imgProfile)
        btnEdit = findViewById(R.id.btnEdit)
        btnGoHome = findViewById(R.id.btnGoHome)
        btnConfirmPhoto = findViewById(R.id.btnConfirmPhoto)

        // Botón deshabilitado al inicio
        btnConfirmPhoto.isEnabled = false

        btnConfirmPhoto.setBackgroundTintList(
            getColorStateList(R.color.ufps_texto_claro)
        )
        btnConfirmPhoto.setTextColor(
            getColor(R.color.ufps_texto_principal)
        )
    }

    private fun setupListeners() {
        btnEdit.setOnClickListener {
            openChooser()
        }

        btnGoHome.setOnClickListener {
            val intent = Intent(this, HomeScreenActivity::class.java)
            startActivity(intent)
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
            } else {
                SnackRouter.showNext("Foto lista para enviar", top = true)
            }
        }
    }

    // ------------------ CHOOSER (Galería + Cámara) ------------------

    private fun openChooser() {

        val galleryIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }

        val tempFile = File.createTempFile("photo_temp", ".jpg", cacheDir)
        tempCameraUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            tempFile
        )

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempCameraUri)
        }

        val chooser = Intent.createChooser(galleryIntent, "Seleccionar foto")
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        chooserLauncher.launch(chooser)
    }

    // ------------------ UCROP ------------------

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(
            File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        val options = UCrop.Options().apply {
            setCompressionQuality(90)
            setToolbarColor(getColor(R.color.ufps_principal))
            setActiveControlsWidgetColor(getColor(R.color.ufps_principal))
            setFreeStyleCropEnabled(false)
        }

        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)         // recorte cuadrado
            .withMaxResultSize(1080, 1080)
            .withOptions(options)

        cropLauncher.launch(uCrop.getIntent(this))
    }

    private fun showAttemptsInfoDialog() {
        val dialogView = View.inflate(this, R.layout.dialog_attempts_load_photo, null)

        val dialog = MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

package com.example.tucarnetapp.ui.home.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.tucarnetapp.R
import com.example.tucarnetapp.data.cache.PhotoUrlMemoryCache
import com.example.tucarnetapp.data.repository.LivenessRepository
import com.example.tucarnetapp.data.repository.PhotoRequestRepository
import com.example.tucarnetapp.session.QRPreferences
import com.example.tucarnetapp.session.SessionManager
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.ui.home.HomeScreenActivity
import com.example.tucarnetapp.utils.showSnack
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class HomeFragment : Fragment() {

    private lateinit var editButton: Button
    private lateinit var logoutButton: Button
    private lateinit var loadingOverlay: View

    private val auth = FirebaseAuth.getInstance()

    private val photoRequestRepository = PhotoRequestRepository()
    private val livenessRepository = LivenessRepository()

    private var tempCameraUri: Uri? = null

    companion object {
        private const val TAG = "HomeFragment"
    }

    // ---------------- CHOOSER ----------------

    private val chooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: tempCameraUri
                uri?.let { startCrop(it) }
            }
        }

    // ---------------- UCROP ----------------

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let { handleCroppedImage(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topBar = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        val tvName = view.findViewById<TextView>(R.id.textName)
        val tvCode = view.findViewById<TextView>(R.id.textCode)
        val tvCareer = view.findViewById<TextView>(R.id.textCareer)
        val tvStatus = view.findViewById<TextView>(R.id.textStatus)
        val imgProfile = view.findViewById<ImageView>(R.id.imgProfile)

        editButton = view.findViewById(R.id.btnEdit)
        logoutButton = view.findViewById(R.id.btnLogout)
        loadingOverlay = view.findViewById(R.id.loading_overlay)

        UserSession.currentUser?.let { user ->
            topBar.title = "¡Hola, ${firstWord(user.name)} ${firstWord(user.last_name)}!"

            tvName.text = " ${user.name} ${user.last_name}"
            tvCode.text = " ${user.student_code}"
            tvCareer.text = " ${user.career}"
            tvStatus.text = " ${user.status}"

            user.card_photo_key?.takeIf { it.isNotBlank() }?.let {
                PhotoLoader.load(requireContext(), imgProfile, it)
            }
        }

        editButton.setOnClickListener { openChooser() }
        logoutButton.setOnClickListener { showLogoutDialog() }
    }

    // ---------------- CHOOSER ----------------

    private fun openChooser() {

        val galleryIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }

        val tempFile = File.createTempFile(
            "photo_temp",
            ".jpg",
            requireContext().cacheDir
        )

        tempCameraUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
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
            File(
                requireContext().cacheDir,
                "cropped_${System.currentTimeMillis()}.jpg"
            )
        )

        val options = UCrop.Options().apply {
            setCompressionQuality(90)
            setToolbarTitle("Recorta tu foto")
            setFreeStyleCropEnabled(false)
            setToolbarColor(requireContext().getColor(R.color.ufps_principal))
            setToolbarWidgetColor(requireContext().getColor(android.R.color.white))
        }

        val uCrop = UCrop.of(sourceUri, destUri)
            .withAspectRatio(3f, 4f)
            .withMaxResultSize(1080, 1440)
            .withOptions(options)

        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    // ---------------- RESULT ----------------

    private fun handleCroppedImage(uri: Uri) {
        showConfirmSendPhotoDialog(uri) {

            showSnack(
                "Foto enviada para validación",
                Snackbar.LENGTH_LONG,
                true,
                R.color.ufps_success_claro,
                R.color.ufps_success_oscuro
            )

            // 🔥 Aquí luego:
            // compareFaces(uri)
            // uploadPhoto(uri)
        }
    }


    // ---------------- CONFIRMAR FOTO -----------
    private fun showConfirmSendPhotoDialog(
        photoUri: Uri,
        onConfirm: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_confirm_send_photo, null)

        val imgPreview =
            dialogView.findViewById<com.google.android.material.imageview.ShapeableImageView>(
                R.id.imgPreview
            )

        Glide.with(this)
            .load(photoUri)
            .into(imgPreview)

        val dialog = MaterialAlertDialogBuilder(
            requireContext(),
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel)
            .setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<Button>(R.id.btnConfirm)
            .setOnClickListener {
                dialog.dismiss()

                val user = UserSession.currentUser
                if (user == null) {
                    Log.e(TAG, "Usuario no disponible en sesión")
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "🔍 Verificando si puede solicitar cambio de foto...")
                        val eligibility =
                            photoRequestRepository.canRequestPhotoUpdate(user.student_id)

                        if (!eligibility.canRequest) {
                            Log.w(TAG, "❌ No puede solicitar cambio de foto")

                            showSnack(
                                "Debes esperar ${eligibility.daysRemaining} días para cambiar tu foto",
                                Snackbar.LENGTH_LONG,
                                true,
                                R.color.ufps_informacion_claro,
                                R.color.ufps_informacion_oscuro
                            )
                            return@launch
                        }

                        Log.d(TAG, "✅ Puede solicitar cambio de foto")

                        // ---------------- SUBIR FOTO ----------------
                        showSnack(
                            "Subiendo foto...",
                            Snackbar.LENGTH_SHORT,
                            true
                        )

                        val base64 =
                            "data:image/jpeg;base64," + uriToBase64(photoUri)

                        Log.d(TAG, "📤 Subiendo foto a S3")
                        val uploadResponse =
                            livenessRepository.uploadPhotoBase64(base64)

                        val photoKey = uploadResponse.photoKey
                        Log.d(TAG, "✅ Foto subida. photoKey = $photoKey")

                        // ---------------- CREAR SOLICITUD ----------------
                        Log.d(TAG, "📝 Creando solicitud de cambio de foto")

                        val createResponse =
                            photoRequestRepository.createPhotoRequest(
                                studentId = user.student_id,
                                new_photo_url = photoKey
                            )

                        Log.d(TAG, "✅ Solicitud creada exitosamente")
                        Log.d(TAG, "Mensaje backend = ${createResponse.message}")

                        showSnack(
                            createResponse.message,
                            Snackbar.LENGTH_LONG,
                            true,
                            R.color.ufps_success_claro,
                            R.color.ufps_success_oscuro
                        )

                    } catch (e: retrofit2.HttpException) {
                        Log.e(TAG, "HTTP ERROR ${e.code()}", e)

                        val message = parseHttpError(e)

                        showSnack(
                            message,
                            Snackbar.LENGTH_LONG,
                            true,
                            R.color.ufps_error_claro,
                            R.color.ufps_error_principal
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al crear solicitud de foto", e)

                        showSnack(
                            "Error al procesar la solicitud",
                            Snackbar.LENGTH_LONG,
                            true,
                            R.color.ufps_error_claro,
                            R.color.ufps_error_principal
                        )
                    }
                }
            }



        dialog.show()
    }


    // ---------------- LOGOUT ----------------

    private fun showLogoutDialog() {

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_logout, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel)
            .setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<Button>(R.id.btnConfirm)
            .setOnClickListener {
                dialog.dismiss()
                performLogout()
            }

        dialog.show()
    }

    private fun performLogout() {
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                auth.signOut()
                SessionManager.getInstance(requireContext()).clearSession()
                QRPreferences.getInstance(requireContext()).clearQR()
                UserSession.clear()
                PhotoUrlMemoryCache.clear()

                startActivity(
                    Intent(requireContext(), HomeScreenActivity::class.java)
                        .apply {
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                )
                requireActivity().finish()

            } catch (e: Exception) {
                loadingOverlay.visibility = View.GONE
                showSnack(
                    "Error al cerrar sesión",
                    Snackbar.LENGTH_LONG,
                    true,
                    R.color.ufps_error_claro,
                    R.color.ufps_error_principal
                )
            }
        }
    }

    // ---------------- HELPERS ----------------

    private fun firstWord(text: String?): String =
        text?.trim()?.split(" ")?.firstOrNull() ?: ""

    private fun uriToBase64(uri: Uri): String {
        val bytes =
            requireContext().contentResolver.openInputStream(uri)?.readBytes()
                ?: byteArrayOf()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    private fun parseHttpError(e: retrofit2.HttpException): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string()

            if (errorBody.isNullOrBlank()) {
                "Error inesperado del servidor"
            } else {
                val json = org.json.JSONObject(errorBody)
                json.optString("message", "Error inesperado del servidor")
            }
        } catch (ex: Exception) {
            "Error inesperado del servidor"
        }
    }


}

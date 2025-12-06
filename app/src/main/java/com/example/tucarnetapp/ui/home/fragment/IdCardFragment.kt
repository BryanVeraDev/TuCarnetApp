package com.example.tucarnetapp.ui.home.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import com.bumptech.glide.Glide
import com.example.tucarnetapp.R
import com.example.tucarnetapp.session.QRPreferences
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.data.StudentData
import com.example.tucarnetapp.viewmodel.QRViewModel
import com.example.tucarnetapp.utils.showSnack
import com.example.tucarnetapp.viewmodel.QRState
import com.example.tucarnetapp.ui.BaseActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IdCardFragment : Fragment() {

    // Views
    private lateinit var contentLayout: ConstraintLayout
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentCode: TextView
    private lateinit var tvStudentCarrera: TextView
    private lateinit var tvStudentType: TextView
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var ivQRCode: ImageView
    private lateinit var loadingOverlay: View
    private lateinit var progressBar: View


    // ViewModel
    private val qrViewModel: QRViewModel by viewModels()

    // Variable para almacenar el QR generado
    // QR Preferences para persistencia
    private lateinit var qrPrefs: QRPreferences

    //variable state snackbar
    private var hasShownHint = false

    // Variables para controlar el estado de carga
    private var isPhotoLoaded = false
    private var isQRLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasShownHint = savedInstanceState?.getBoolean("has_shown_hint") ?: false
        // Inicializar QRPreferences
        qrPrefs = QRPreferences.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_id_card, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("has_shown_hint", hasShownHint)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bloquear screenshots
        //(activity as? BaseActivity)?.setScreenshotsBlocked(true)

        // Inicializar vistas
        initViews(view)

        // Resetear estados de carga
        isPhotoLoaded = false
        isQRLoaded = false

        // Ocultar contenido hasta que cargue
        showLoading()

        // Cargar datos
        loadStudentData()
        loadQRCode()
        observeQRState()
        setupClickListeners()

        // Mostrar hint solo la primera vez
        //showInitialHint()
    }

    // ========== INICIALIZACIÓN ==========

    private fun initViews(view: View) {
        contentLayout = view.findViewById(R.id.content_root)
        tvStudentName = view.findViewById(R.id.tvStudentName)
        tvStudentCode = view.findViewById(R.id.tvStudentCode)
        tvStudentCarrera = view.findViewById(R.id.tvStudentCarrera)
        tvStudentType = view.findViewById(R.id.tvStudentType)
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto)
        ivQRCode = view.findViewById(R.id.ivQRCode)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
    }

    // ========== CARGA DE DATOS ==========

    private fun loadStudentData() {
        val user = UserSession.currentUser

        if (user != null) {
            val fullName = "${user.name}\n${user.last_name}"
            val studentData = StudentData(
                name = fullName,
                code = user.student_code ?: "N/A",
                career = user.career ?: "N/A",
                status = user.status ?: "N/A",
                studentType = user.student_type ?: "ESTUDIANTE"
            )

            updateUI(studentData)

            // Cargar foto
            loadProfilePhoto(user.card_photo_key)
        } else {
            isPhotoLoaded = true
            checkIfReadyToShow()
            //showContent() // Mostrar aunque no haya datos
        }
    }

    private fun loadProfilePhoto(photoKey: String?) {
        PhotoLoader.load(
            context = requireContext(),
            imageView = ivProfilePhoto,
            photoKey = photoKey
        ) {
            isPhotoLoaded = true
            checkIfReadyToShow()
        }
    }

    private fun updateUI(data: StudentData) {
        tvStudentName.text = data.name
        tvStudentCode.text = "CÓDIGO: ${data.code}"
        tvStudentCarrera.text = "CARRERA: ${data.career}"
        tvStudentType.text = data.studentType
    }

    // ========== QR CODE ==========

    private fun loadQRCode() {
        val studentCode = UserSession.currentUser?.student_code

        if (studentCode != null) {
            qrViewModel.loadQR(studentCode)
        } else {
            showSnack(
                "No se pudo obtener el código de estudiante",
                duration = Snackbar.LENGTH_LONG
            )
            isQRLoaded = true
            checkIfReadyToShow()
        }
    }

    private fun observeQRState() {
        viewLifecycleOwner.lifecycleScope.launch {
            qrViewModel.qrState.collect { state ->
                when (state) {
                    is QRState.Initial -> {
                        //loadingOverlay.visibility = View.GONE
                    }

                    is QRState.Loading -> {
                        //loadingOverlay.visibility = View.VISIBLE
                    }

                    is QRState.Success -> {
                        //progressBar.visibility = View.GONE
                        displayQR(state.qrBase64)
                        isQRLoaded = true
                        checkIfReadyToShow()

                        // Mostrar mensaje si es un QR nuevo
                        if (state.isNew) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                showSnack(
                                    "Nuevo código QR generado",
                                    duration = Snackbar.LENGTH_SHORT
                                )
                            }
                        }
                    }

                    is QRState.Error -> {
                        //progressBar.visibility = View.GONE
                        isQRLoaded = true
                        checkIfReadyToShow()
                        showSnack(
                            state.message,
                            duration = Snackbar.LENGTH_LONG
                        )
                    }
                }
            }
        }
    }

    private fun displayQR(base64: String) {
        val qrBitmap = base64ToBitmap(base64)
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap)
        }
    }

    // ========== CONTROL DE CARGA ==========

    /**
     * Verifica si tanto la foto como el QR ya están cargados
     * para mostrar el contenido
     */
    private fun checkIfReadyToShow() {
        if (isPhotoLoaded && isQRLoaded) {
            viewLifecycleOwner.lifecycleScope.launch {
                showContent()
            }
        }
    }

    // ========== INTERACCIONES ==========

    private fun setupClickListeners() {
        // Click en QR para verlo en pantalla completa
        ivQRCode.setOnClickListener {
            val qrBase64 = qrPrefs.getQRBase64()

            if (qrBase64 != null) {
                val bundle = Bundle().apply {
                    putString("qr_image", qrBase64)
                }
                val qrProfileFragment = QRProfileFragment().apply {
                    arguments = bundle
                }

                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(R.id.fragmentContainer, qrProfileFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                showSnack("Código QR no disponible")
            }
        }

        // Long click para forzar renovación (opcional)
        ivQRCode.setOnLongClickListener {
            val studentCode = UserSession.currentUser?.student_code
            if (studentCode != null) {
                qrViewModel.forceRefresh(studentCode)
                showSnack("Renovando código QR...", duration = Snackbar.LENGTH_SHORT)
            }
            true
        }
    }

    // ========== UI HELPERS ==========

    private fun showLoading() {
        loadingOverlay.visibility = View.VISIBLE
        //loadingOverlay.bringToFront()
        contentLayout.visibility = View.INVISIBLE
    }

    private fun showContent() {
        loadingOverlay.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE

        // Mostrar hint solo después de que todo esté cargado
        showInitialHint()
    }

    private fun showInitialHint() {
        if (!hasShownHint) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.withStarted {
                    showSnack(
                        "Presiona el código QR para obtener el QR de tu perfil.",
                        duration = Snackbar.LENGTH_LONG,
                        top = true,
                        R.color.ufps_informacion_claro,
                        R.color.ufps_informacion_oscuro
                    )
                }
            }
            hasShownHint = true
        }
    }

    // ========== UTILIDADES ==========

    private fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            // Remover el prefijo data:image si existe
            val pureBase64 = if (base64.contains("base64,")) {
                base64.substringAfter("base64,")
            } else {
                base64
            }

            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        // Restaurar al salir
        //(activity as? BaseActivity)?.setScreenshotsBlocked(false)
        super.onDestroyView()
    }
}
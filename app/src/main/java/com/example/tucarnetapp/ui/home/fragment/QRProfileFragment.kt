package com.example.tucarnetapp.ui.home.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tucarnetapp.R
import com.example.tucarnetapp.session.QRPreferences
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.ui.BaseActivity
import com.example.tucarnetapp.viewmodel.QRViewModel
import com.example.tucarnetapp.utils.showSnack
import com.example.tucarnetapp.viewmodel.QRState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

private const val ARG_QR_IMAGE = "qr_image"

/**
 * A simple [Fragment] subclass.
 * Use the [QRProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QRProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var appBar: MaterialToolbar
    private lateinit var imgQr: ImageView
    private lateinit var imgProfile: ImageView
    private lateinit var txtUserName: TextView
    private lateinit var txtInfo: TextView
    private lateinit var txtQRTimer: TextView
    private lateinit var txtQRStatus: TextView
    private lateinit var progressBar: View

    // Data
    private var qrImageBase64: String? = null
    private lateinit var qrPrefs: QRPreferences

    // ViewModel
    private val qrViewModel: QRViewModel by viewModels()

    // Timer
    private var timerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener el QR desde los argumentos
        arguments?.let {
            qrImageBase64 = it.getString(ARG_QR_IMAGE)
        }
        qrPrefs = QRPreferences.getInstance(requireContext())

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_qr_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? BaseActivity)?.setScreenshotsBlocked(true)
        initViews(view)
        setupAppBar()
        loadQRCode()
        loadStudentInfo()
        observeQRState()
        startTimer()
        /*
        appBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Cargar y mostrar el QR
        loadQRCode()

        // Cargar información del estudiante
        loadStudentInfo()*/
    }

    private fun initViews(view: View) {
        appBar = view.findViewById(R.id.topAppBar)
        imgQr = view.findViewById(R.id.imgQr)
        imgProfile = view.findViewById(R.id.imgProfile)
        txtUserName = view.findViewById(R.id.txtUserName)
        txtInfo = view.findViewById(R.id.txtInfo)
        txtQRTimer = view.findViewById(R.id.txtQRTimer)
        txtQRStatus = view.findViewById(R.id.txtQRStatus)
        progressBar = view.findViewById(R.id.loading_overlay)
    }

    private fun setupAppBar() {
        appBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Carga y muestra el código QR
     */
    private fun loadQRCode() {
        // Prioridad 1: Usar el QR pasado por argumentos
        if (qrImageBase64 != null) {
            displayQR(qrImageBase64!!)
            return
        }

        // Prioridad 2: Intentar cargar desde QRPreferences
        val cachedQR = qrPrefs.getQRBase64()
        if (cachedQR != null) {
            displayQR(cachedQR)
            return
        }

        // No hay QR disponible
        Toast.makeText(
            context,
            "No hay código QR disponible",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Observa cambios en el estado del QR
     */
    private fun observeQRState() {
        viewLifecycleOwner.lifecycleScope.launch {
            qrViewModel.qrState.collect { state ->
                when (state) {
                    is QRState.Loading -> {
                        //progressBar.visibility = View.VISIBLE
                        txtQRStatus.text = "Renovando código..."
                        txtQRStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.ufps_texto_oscuro)
                        )
                    }

                    is QRState.Success -> {
                        progressBar.visibility = View.GONE
                        displayQR(state.qrBase64)

                        if (state.isNew) {
                            showSnack(
                                "✓ Código QR renovado correctamente",
                                duration = Snackbar.LENGTH_SHORT,
                                backgroundColor = R.color.ufps_success_claro,
                                textColor = R.color.ufps_texto_oscuro
                            )
                            updateStatusBadge(valid = true, renewed = true)
                        } else {
                            updateStatusBadge(valid = true, renewed = false)
                        }
                    }

                    is QRState.Error -> {
                        progressBar.visibility = View.GONE
                        updateStatusBadge(valid = false)
                        showSnack(
                            "Error: ${state.message}",
                            duration = Snackbar.LENGTH_LONG
                        )
                    }

                    else -> {
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * Inicia el timer usando Coroutines
     */
    private fun startTimer() {
        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                if (isAdded && context != null) {
                    updateTimer()
                }
                delay(1000) // Actualizar cada segundo
            }
        }
    }

    /**
     * Actualiza el timer y verifica si necesita renovación
     */
    private fun updateTimer() {
        if (!isAdded || context == null) return

        val timeRemaining = qrPrefs.getTimeRemaining()

        when {
            timeRemaining <= 0 -> {
                txtQRTimer.text = "Renovando..."
                txtQRTimer.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.ufps_texto_oscuro)
                )
                updateStatusBadge(valid = false)

                val studentCode = UserSession.currentUser?.student_code
                if (studentCode != null && qrPrefs.hasQR()) {
                    qrViewModel.forceRefresh(studentCode)
                }
            }

            timeRemaining < 300 -> {
                txtQRTimer.text = formatTime(timeRemaining)
                txtQRTimer.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.ufps_texto_oscuro)
                )
                updateStatusBadge(valid = true, warning = true)
            }

            else -> {
                txtQRTimer.text = formatTime(timeRemaining)
                txtQRTimer.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.ufps_texto_oscuro)
                )
                updateStatusBadge(valid = true)
            }
        }
    }

    /**
     * Actualiza el badge de estado del QR
     */
    private fun updateStatusBadge(valid: Boolean, warning: Boolean = false, renewed: Boolean = false) {

        if (!isAdded || context == null) return

        when {
            !valid -> {
                txtQRStatus.text = "⚠ Código expirado"
                txtQRStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.ufps_principal)
                )
            }
            renewed -> {
                txtQRStatus.text = "✓ Código renovado"
                txtQRStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.ufps_texto_oscuro)
                )
            }
            warning -> {
                txtQRStatus.text = "⏱ Expira pronto"
                txtQRStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                )
            }
            else -> {
                txtQRStatus.text = "✓ Código válido"
                txtQRStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.ufps_texto_oscuro)
                )
            }
        }
    }

    /**
     * Formatea el tiempo restante en MM:SS
     */
    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    /**
     * Muestra el QR en el ImageView
     */
    private fun displayQR(base64: String) {
        val qrBitmap = base64ToBitmap(base64)

        if (qrBitmap != null) {
            imgQr.setImageBitmap(qrBitmap)
        } else {
            Toast.makeText(
                context,
                "Error al cargar el código QR",
                Toast.LENGTH_SHORT
            ).show()
            // Si hay un placeholder, usarlo
            // imgQr.setImageResource(R.drawable.qr_placeholder)
        }
    }

    /**
     * Carga la información del estudiante desde UserSession
     */
    private fun loadStudentInfo() {
        val currentUser = UserSession.currentUser

        if (currentUser != null) {
            val fullName = "${currentUser.name} ${currentUser.last_name}"
            txtUserName.text = fullName

            // Cargar foto de perfil si existe
            currentUser.card_photo_url?.let { photoUrl ->
                if (photoUrl.startsWith("http")) {
                    // Si es URL, usar Glide (descomentar cuando agregues Glide)
                    Glide.with(this).load(photoUrl).into(imgProfile)
                } else if (photoUrl.startsWith("data:image")) {
                    // Si es base64, convertir y mostrar
                    val photoBitmap = base64ToBitmap(photoUrl)
                    photoBitmap?.let { imgProfile.setImageBitmap(it) }
                }
            }


        } else {
            txtUserName.text = "Estudiante"
        }
    }

    /**
     * Convierte base64 a Bitmap
     */
    private fun base64ToBitmap(base64: String): Bitmap? {
        return try {
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
        (activity as? BaseActivity)?.setScreenshotsBlocked(false)
        super.onDestroyView()
        // Cancelar el timer automáticamente
        timerJob?.cancel()
        timerJob = null
    }

    companion object {
        @JvmStatic
        fun newInstance(qrBase64: String) =
            QRProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QR_IMAGE, qrBase64)
                }
            }
    }
}
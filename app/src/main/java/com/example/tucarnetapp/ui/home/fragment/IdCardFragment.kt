package com.example.tucarnetapp.ui.home.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.lifecycle.withStarted
import com.bumptech.glide.Glide
import com.example.tucarnetapp.R
import com.example.tucarnetapp.session.QRPreferences
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.data.StudentData
import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.viewmodel.QRViewModel
import com.example.tucarnetapp.utils.showSnack
import com.example.tucarnetapp.viewmodel.QRState
import com.example.tucarnetapp.data.remote.dto.StudentResponse
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [IdCardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class IdCardFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // Views
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentCode: TextView
    private lateinit var tvStudentCarrera: TextView
    private lateinit var tvStudentType: TextView
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var ivQRCode: ImageView
    private lateinit var progressBar: View


    // ViewModel
    private val qrViewModel: QRViewModel by viewModels()

    // Variable para almacenar el QR generado
    // QR Preferences para persistencia
    private lateinit var qrPrefs: QRPreferences

    //variable state snackbar
    private var hasShownHint = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasShownHint = savedInstanceState?.getBoolean("has_shown_hint") ?: false
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

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

        // Inicializar vistas
        initViews(view)

        // Cargar datos del estudiante
        loadStudentData()

        //Ver estado del QR y renovarlo si es necesario
        observeQRState()

        //
        setupClickListeners()

        //
        loadQRCode()


        // Generar QR desde la API
        //generateQRCode()

        // Mostrar snackbar cuando la vista esté al menos en estado STARTED
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

        /*// Click listener para ver el QR en pantalla completa
        ivQRCode.setOnClickListener {
            // Obtener el QR desde QRPreferences
            val qrBase64 = qrPrefs.getQRBase64()

            if (qrBase64 != null) {
                // Pasar el QR al fragmento de perfil
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
                Toast.makeText(
                    context,
                    "No hay código QR disponible. Generando...",
                    Toast.LENGTH_SHORT
                ).show()
                generateQRCode(silent = false)
            }
        }*/
    }

    private fun initViews(view: View) {
        tvStudentName = view.findViewById(R.id.tvStudentName)
        tvStudentCode = view.findViewById(R.id.tvStudentCode)
        tvStudentCarrera = view.findViewById(R.id.tvStudentCarrera)
        tvStudentType = view.findViewById(R.id.tvStudentType)
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto)
        ivQRCode = view.findViewById(R.id.ivQRCode)
        progressBar = view.findViewById(R.id.loading_overlay)
    }

    private fun loadStudentData() {
        val sessionUser = UserSession.currentUser

        if (sessionUser != null) {
            // Ya tenemos datos en la sesión
            updateUIFromSession(sessionUser)
        } else {
            // Si no hay sesión, cargar datos de respaldo
            loadFallbackData()
        }
    }

    private fun loadQRCode() {
        val studentCode = UserSession.currentUser?.student_code

        if (studentCode != null) {
            qrViewModel.loadQR(studentCode)
        } else {
            showSnack(
                "No se pudo obtener el código de estudiante",
                duration = Snackbar.LENGTH_LONG
            )
        }
    }

    private fun observeQRState() {
        viewLifecycleOwner.lifecycleScope.launch {
            qrViewModel.qrState.collect { state ->
                when (state) {
                    is QRState.Initial -> {
                        progressBar.visibility = View.GONE
                    }

                    is QRState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }

                    is QRState.Success -> {
                        progressBar.visibility = View.GONE
                        displayQR(state.qrBase64)

                        // Mostrar mensaje si es un QR nuevo
                        if (state.isNew) {
                            showSnack(
                                "Nuevo código QR generado",
                                duration = Snackbar.LENGTH_SHORT
                            )
                        }
                    }

                    is QRState.Error -> {
                        progressBar.visibility = View.GONE
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

    /**
     * Carga el QR desde SharedPreferences o lo genera si no existe/expiró
     */
    private fun loadOrGenerateQR() {
        if (qrPrefs.isQRValid()) {
            // Cargar QR desde SharedPreferences
            loadQRFromPreferences()

            // Si necesita renovación, regenerar en segundo plano
            if (qrPrefs.needsRenewal()) {
                generateQRCode(silent = true)
            }
        } else {
            // No hay QR o expiró, generar nuevo
            generateQRCode(silent = false)
        }
    }

    /**
     * Carga el QR desde SharedPreferences
     */
    private fun loadQRFromPreferences() {
        val qrBitmap = qrPrefs.getQRBitmap()
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap)
        }
    }

    private fun generateQRCode(silent: Boolean = false) {
        val sessionUser = UserSession.currentUser

        if (sessionUser?.student_code == null) {
            if (!silent) {
                Toast.makeText(context, "No se pudo obtener el código de estudiante", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Mostrar indicador de carga (opcional)
        // progressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Crear el DTO para generar el QR
                val createQrDto = mapOf(
                    "student_code" to sessionUser.student_code
                )

                // Llamar a la API para generar el QR
                val response = ApiClient.qrApi.generateQr(createQrDto)

                if (response.isSuccessful && response.body() != null) {
                    val qrResponse = response.body()!!
                    //qrImageBase64 = qrResponse.qr

                    // Convertir el base64 a Bitmap y mostrarlo
                    val qrBitmap = base64ToBitmap(qrResponse.qr)
                    if (qrBitmap != null) {
                        ivQRCode.setImageBitmap(qrBitmap)

                        // Guardar en SharedPreferences
                        qrPrefs.saveQR(
                            qrBase64 = qrResponse.qr,
                            jwt = qrResponse.jwt,
                            expiresIn = qrResponse.expiresIn,
                            studentCode = sessionUser.student_code
                        )

                    } else {
                        handleQRError("Error al procesar la imagen del QR")
                    }
                } else {
                    if (!silent) {
                        handleQRError("Error al generar el código QR: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    handleQRError("Error de conexión: ${e.message}")
                }
            } finally {
                // Ocultar indicador de carga
                // progressBar?.visibility = View.GONE
            }
        }
    }

    private fun updateUIFromSession(studentResponse: StudentResponse) {
        val fullName = "${studentResponse.name} ${studentResponse.last_name}"

        val studentData = StudentData(
            name = fullName ?: "N/A",
            code = studentResponse.student_code ?: "N/A",
            career = studentResponse.career ?: "N/A",
            status = studentResponse.status ?: "N/A",
            studentType = studentResponse.student_type ?: "ESTUDIANTE"
        )
        updateUI(studentData)

        // Cargar foto de perfil si existe
        studentResponse.card_photo_url?.let { photoUrl ->
            // Si card_photo_url es una URL, usar Glide
            if (photoUrl.startsWith("http")) {
                Glide.with(this).load(photoUrl).into(ivProfilePhoto)
            } else if (photoUrl.startsWith("data:image")) {
                val photoBitmap = base64ToBitmap(photoUrl)
                photoBitmap?.let { ivProfilePhoto.setImageBitmap(it) }
            }
        }
    }

    private fun loadFallbackData() {
        val studentData = StudentData(
            name = "OLIVIA ISABEL\nRODRIGO",
            code = "1152810",
            career = "Ing Sistemas",
            status = "Matriculado",
            studentType = "ESTUDIANTE"
        )
        updateUI(studentData)
    }

    private fun handleQRError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        // Puedes mostrar un placeholder o imagen de error en el QR
        // ivQRCode.setImageResource(R.drawable.qr_error_placeholder)
    }

    private fun updateUI(data: StudentData) {
        tvStudentName.text = data.name
        tvStudentCode.text = "CÓDIGO: ${data.code}"
        tvStudentCarrera.text = "CARRERA: ${data.career}"
        tvStudentType.text = data.studentType
    }

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

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            IdCardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
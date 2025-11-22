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
import com.bumptech.glide.Glide
import com.example.tucarnetapp.R
import com.example.tucarnetapp.session.QRPreferences
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.utils.showSnack
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar

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

    private var qrImageBase64: String? = null
    private lateinit var qrPrefs: QRPreferences

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

        initViews(view)

        appBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Cargar y mostrar el QR
        loadQRCode()

        // Cargar información del estudiante
        loadStudentInfo()
    }

    private fun initViews(view: View) {
        appBar = view.findViewById(R.id.topAppBar)
        imgQr = view.findViewById(R.id.imgQr)
        imgProfile = view.findViewById(R.id.imgProfile)
        txtUserName = view.findViewById(R.id.txtUserName)
        txtInfo = view.findViewById(R.id.txtInfo)
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



    companion object {
        /**
         * Crea una instancia del fragment con el QR en base64
         * @param qrBase64 El código QR en formato base64
         */
        @JvmStatic
        fun newInstance(qrBase64: String) =
            QRProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QR_IMAGE, qrBase64)
                }
            }
    }
}
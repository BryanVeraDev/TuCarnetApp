package com.example.tucarnetapp.ui.home.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.tucarnetapp.R
import com.example.tucarnetapp.data.StudentData
import com.example.tucarnetapp.utils.showSnack
import com.google.android.material.snackbar.Snackbar

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
    private lateinit var tvStudentCC: TextView
    private lateinit var tvStudentCarrera: TextView
    private lateinit var tvStudentType: TextView
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var ivQRCode: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        return inflater.inflate(R.layout.fragment_id_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        initViews(view)

        // Cargar datos del estudiante
        loadStudentData()

        // Mostrar snackbar al cargar el fragmento
        Handler(Looper.getMainLooper()).post {
            showSnack(
                "Presiona el código QR para obtener el QR de tu perfil.",
                duration = Snackbar.LENGTH_LONG,
                top = true,
                R.color.ufps_informacion_claro,
                R.color.ufps_informacion_oscuro
            )
        }
    }

    private fun initViews(view: View) {
        tvStudentName = view.findViewById(R.id.tvStudentName)
        tvStudentCode = view.findViewById(R.id.tvStudentCode)
        tvStudentCC = view.findViewById(R.id.tvStudentCC)
        tvStudentCarrera = view.findViewById(R.id.tvStudentCarrera)
        tvStudentType = view.findViewById(R.id.tvStudentType)
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto)
        ivQRCode = view.findViewById(R.id.ivQRCode)
    }

    private fun loadStudentData() {
        // Opción 1: Datos de prueba (temporal)
        val studentData = StudentData(
            name = "OLIVIA ISABEL\nRODRIGO",
            code = "1152810",
            documentId = "1093123452",
            career = "Ing Sistemas",
            status = "Matriculado",
            studentType = "ESTUDIANTE"
        )

        updateUI(studentData)

        // Opción 2: Cargar desde API (descomentar cuando tengas la API lista)
        /*
        lifecycleScope.launch {
            try {
                val response = apiService.getStudentData("codigo_estudiante")
                updateUI(response)
            } catch (e: Exception) {
                // Manejar error
                Toast.makeText(context, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        }
        */
    }

    private fun updateUI(data: StudentData) {
        tvStudentName.text = data.name
        tvStudentCode.text = "CÓDIGO : ${data.code}"
        tvStudentCC.text = "CC : ${data.documentId}"
        tvStudentCarrera.text = "CARRERA: ${data.career}"
        tvStudentType.text = data.studentType

        // Si tienes URLs de imágenes, cargarlas aquí
        // Ejemplo con Glide:
        /*
        data.profileImageUrl?.let { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.profile_example_image)
                .error(R.drawable.profile_example_image)
                .into(ivProfilePhoto)
        }

        data.qrCodeUrl?.let { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.qr_placeholder)
                .into(ivQRCode)
        }
        */
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment IdCardFragment.
         */
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
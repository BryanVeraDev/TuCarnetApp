package com.example.tucarnetapp.ui.home

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tucarnetapp.R
import com.google.android.material.button.MaterialButton
import com.example.tucarnetapp.data.StudentData

class StudentProfileActivity : AppCompatActivity() {

    private lateinit var textValidationMessage: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var textName: TextView
    private lateinit var textCode: TextView
    private lateinit var textCareer: TextView
    private lateinit var textStatus: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var infoContainer: View

    // Lista estática de estudiantes para pruebas
    private val mockStudents = listOf(
        StudentData(
            code = "1152669",
            name = "Juan David Rodriguez Cordoba",
            career = "Ingeniería de Sistemas",
            status = "Matriculado",
            documentId = "1152669",
            profileImageUrl = null
        ),
        StudentData(
            code = "1151234",
            name = "María Fernanda García López",
            career = "Ingeniería Industrial",
            status = "Matriculado",
            documentId = "1151234",
            profileImageUrl = null
        ),
        StudentData(
            code = "1159876",
            name = "Carlos Andrés Pérez Ramírez",
            career = "Ingeniería Electrónica",
            status = "Activo",
            documentId = "1159876",
            profileImageUrl = null
        ),
        StudentData(
            code = "1154567",
            name = "Ana Sofía Martínez Torres",
            career = "Ingeniería Civil",
            status = "Matriculado",
            documentId = "1154567",
            profileImageUrl = null
        ),
        StudentData(
            code = "1158901",
            name = "Luis Eduardo Sánchez Díaz",
            career = "Arquitectura",
            status = "Inactivo",
            documentId = "1158901",
            profileImageUrl = null
        )
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_profile)
        initViews()

        // Obtener datos del Intent (código escaneado)
        val studentCode = intent.getStringExtra("STUDENT_CODE")

        // Validar estudiante
        validateStudent(studentCode)

        setupListeners()
    }

    private fun initViews() {
        textValidationMessage = findViewById(R.id.textValidationMessage)
        imgProfile = findViewById(R.id.imgProfile)
        textName = findViewById(R.id.textName)
        textCode = findViewById(R.id.textCode)
        textCareer = findViewById(R.id.textCareer)
        textStatus = findViewById(R.id.textStatus)
        btnLogout = findViewById(R.id.btnLogout)
        infoContainer = findViewById(R.id.infoContainer)
    }

    private fun validateStudent(code: String?) {
        // Aquí harías la llamada a tu API o base de datos
        // Por ahora simularé la validación

        if (code.isNullOrEmpty()) {
            showInvalidStudent()
            return
        }

        // Simular búsqueda de estudiante
        val student = searchStudentByCode(code)

        if (student != null) {
            showValidStudent(student)
        } else {
            showInvalidStudent()
        }
    }

    private fun showValidStudent(student: StudentData) {
        // Configurar mensaje de éxito
        textValidationMessage.apply {
            text = "✓ El estudiante pertenece a la UFPS"
            setBackgroundColor(resources.getColor(R.color.ufps_success_claro, null))
            setTextColor(resources.getColor(R.color.ufps_success_oscuro, null))
        }

        // Mostrar información del estudiante
        infoContainer.visibility = View.VISIBLE

        // Cargar datos del estudiante
        textName.text = " ${student.name}"
        textCode.text = " ${student.code}"
        textCareer.text = " ${student.career}"
        textStatus.text = " ${student.status}"

        // Cargar imagen del estudiante
        // Si tienes una URL de imagen:
        // Glide.with(this).load(student.imageUrl).into(imgProfile)
        // Por ahora usar imagen de ejemplo
        imgProfile.setImageResource(R.drawable.profile_example_image)
    }

    private fun showInvalidStudent() {
        // Configurar mensaje de error
        textValidationMessage.apply {
            text = "✗ El código escaneado no es válido"
            setBackgroundColor(resources.getColor(R.color.ufps_error_claro, null))
            setTextColor(resources.getColor(R.color.ufps_error_principal, null))
        }

        // Cargar datos del estudiante
        textName.text = ""
        textCode.text = ""
        textCareer.text = ""
        textStatus.text = ""

        // Ocultar información del estudiante
        //infoContainer.visibility = View.GONE
        imgProfile.setImageResource(R.drawable.profile_blank)
        imgProfile.setBackgroundColor(resources.getColor(R.color.ufps_texto_principal,null))
    }

    private fun searchStudentByCode(code: String): StudentData? {
        // Aquí harías la llamada a tu API/Database
        // Ejemplo de simulación:
        // Buscar en la lista estática de estudiantes
        return mockStudents.find { it.code == code }
    }

    private fun setupListeners() {
        btnLogout.setOnClickListener {
            finish() // Volver a la pantalla anterior
        }
    }

}
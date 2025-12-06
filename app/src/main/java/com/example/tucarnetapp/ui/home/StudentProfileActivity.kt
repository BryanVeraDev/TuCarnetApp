package com.example.tucarnetapp.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.tucarnetapp.R
import com.example.tucarnetapp.data.cache.PhotoUrlMemoryCache
import com.example.tucarnetapp.ui.BaseActivity
import com.google.android.material.button.MaterialButton

class StudentProfileActivity : BaseActivity() {

    private lateinit var textValidationMessage: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var textName: TextView
    private lateinit var textCode: TextView
    private lateinit var textCareer: TextView
    private lateinit var textStatus: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var infoContainer: View

    companion object {
        private const val TAG = "StudentProfile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_profile)
        initViews()

        loadValidatedStudent()

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

    /**
     * Carga los datos del estudiante validado desde el backend
     */
    private fun loadValidatedStudent() {
        // Extraer datos del Intent
        val isValidated = intent.getBooleanExtra("IS_VALIDATED", false)

        if (!isValidated) {
            showInvalidStudent("No se recibieron datos de validación")
            return
        }

        // Extraer datos del Intent
        val studentCode = intent.getStringExtra("STUDENT_CODE") ?: ""
        val name = intent.getStringExtra("STUDENT_NAME") ?: ""
        val lastName = intent.getStringExtra("STUDENT_LAST_NAME") ?: ""
        val email = intent.getStringExtra("STUDENT_EMAIL") ?: ""
        val career = intent.getStringExtra("STUDENT_CAREER") ?: ""
        val status = intent.getStringExtra("STUDENT_STATUS") ?: ""
        val studentType = intent.getStringExtra("STUDENT_TYPE") ?: ""
        val cardPhotoKey = intent.getStringExtra("CARD_PHOTO_KEY")

        // Validar que tengamos los datos mínimos
        if (studentCode.isNullOrEmpty() || name.isNullOrEmpty()) {
            showInvalidStudent("Datos incompletos del estudiante")
            return
        }

        // Mostrar estudiante válido
        showValidStudent(
            studentCode = studentCode,
            fullName = "$name ${lastName ?: ""}".trim(),
            email = email,
            career = career ?: "N/A",
            status = status ?: "N/A",
            studentType = studentType,
            cardPhotoKey = cardPhotoKey
        )
    }


    /**
     * Muestra los datos de un estudiante válido
     * @param isFromBackend true si viene de validación de QR, false si es mock data
     */
    private fun showValidStudent(
        studentCode: String,
        fullName: String,
        email: String?,
        career: String,
        status: String,
        studentType: String?,
        cardPhotoKey: String?
    ) {
        // Configurar mensaje de validación exitosa
        textValidationMessage.apply {
            text = "✓ Carnet validado exitosamente"
            setBackgroundColor(resources.getColor(R.color.ufps_success_claro, null))
            setTextColor(resources.getColor(R.color.ufps_success_oscuro, null))
            visibility = View.VISIBLE
        }

        // Mostrar información del estudiante
        infoContainer.visibility = View.VISIBLE

        // Cargar datos del estudiante
        textName.text = " $fullName"
        textCode.text = " $studentCode"
        textCareer.text = " $career"
        textStatus.text = " ${formatStatus(status)}"

        // Cargar imagen del estudiante
        PhotoLoader.load(
            context = this,
            imageView = imgProfile,
            photoKey = cardPhotoKey
        )
    }

    private fun showInvalidStudent(reason: String) {
        // Configurar mensaje de error
        textValidationMessage.apply {
            text = "✗ Validación fallida: $reason"
            setBackgroundColor(resources.getColor(R.color.ufps_error_claro, null))
            setTextColor(resources.getColor(R.color.ufps_error_principal, null))
            visibility = View.VISIBLE
        }

        // Ocultar información del estudiante
        infoContainer.visibility = View.GONE

        // Mostrar imagen en blanco
        imgProfile.setImageResource(R.drawable.profile_blank)
        imgProfile.setBackgroundColor(resources.getColor(R.color.ufps_texto_principal, null))
    }

    /**
     * Formatea el estado del estudiante
     */
    private fun formatStatus(status: String): String {
        return when (status) {
            "MATRICULADO" -> "Matriculado"
            "NO_ACTIVO" -> "No Activo"
            else -> status
        }
    }

    /**
     * Formatea el tipo de estudiante
     */
    private fun formatStudentType(type: String): String {
        return when (type) {
            "PREGRADO" -> "Pregrado"
            "POSGRADO" -> "Posgrado"
            else -> type
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

    private fun setupListeners() {
        btnLogout.setOnClickListener {
            PhotoUrlMemoryCache.clear()
            finish() // Volver a la pantalla anterior
        }
    }

    override fun onInternetAvailable() {
        super.onInternetAvailable()
        Log.d(TAG, "✅ Internet restaurado")
    }

    override fun onInternetLost() {
        super.onInternetLost()
        Log.d(TAG, "❌ Internet perdido")
    }

}
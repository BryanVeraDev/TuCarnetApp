package com.example.tucarnetapp.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tucarnetapp.R
import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.utils.showSnack
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.*
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.launch
import com.example.tucarnetapp.data.remote.dto.StudentValidationData
import com.example.tucarnetapp.ui.BaseActivity

class QRScannerActivity : BaseActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var flashlightButton: ImageButton
    private lateinit var backButton: ImageButton
    private var flashlightStatus: Boolean = false
    private val CAMERA_PERMISSION_CODE = 1001
    private val PREF_NAME = "app_permissions"
    private val KEY_CAMERA_REQUESTED = "camera_requested"

    private var lastErrorTime = 0L
    private val ERROR_COOLDOWN_MS = 2000L


    // Formato esperado del QR
    companion object {
        private const val QR_PREFIX = "UFPSCARNET:"
        private const val TAG = "QRScanner"
    }

    // Flag para evitar múltiples escaneos
    private var isProcessing = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_qrscanner)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        barcodeView = findViewById(R.id.barcodeScannerView)
        backButton = findViewById(R.id.btnBack)
        flashlightButton = findViewById(R.id.btnFlash)

        barcodeView.statusView.visibility = View.GONE

        // 🔹 Si no tiene permiso, no dejar cargar la pantalla
        if (!haveCameraPermission()) {
            checkCameraPermission()
        } else {
            startScanner()
        }

        backButton.setOnClickListener {
            val intent = Intent(this, HomeScreenActivity::class.java)
            startActivity(intent)
            finish()
        }

        flashlightButton.setOnClickListener {
            // ✅ Verificar si el dispositivo tiene flash antes de usarlo
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                flashlightStatus = !flashlightStatus
                if (flashlightStatus) {
                    barcodeView.setTorchOn()
                    flashlightButton.postDelayed({
                        showSnack("Linterna encendida", Snackbar.LENGTH_SHORT, false, R.color.ufps_informacion_claro, R.color.ufps_informacion_oscuro)
                    }, 150)
                } else {
                    barcodeView.setTorchOff()
                    flashlightButton.postDelayed({
                        showSnack("Linterna apagada", Snackbar.LENGTH_SHORT, false, R.color.ufps_informacion_claro, R.color.ufps_informacion_oscuro)
                    }, 150)
                }
            } else {
                showSnack("Tu dispositivo no tiene flash", Snackbar.LENGTH_SHORT, false, R.color.ufps_informacion_claro, R.color.ufps_informacion_oscuro)
            }
        }
    }

    private fun haveCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val checkActualCameraPermission = prefs.getBoolean(KEY_CAMERA_REQUESTED, false)

        when {
            // 🚀 Primera vez → pedir permiso
            !checkActualCameraPermission -> {
                prefs.edit().putBoolean(KEY_CAMERA_REQUESTED, true).apply()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }

            // ⚠️ Ya se negó → no mostrar escáner, cerrar Activity
            else -> {
                showSnack("Debes habilitar el permiso de cámara desde Ajustes para poder escanear.", Snackbar.LENGTH_SHORT, false, R.color.ufps_informacion_claro, R.color.ufps_informacion_oscuro)
                Handler(Looper.getMainLooper()).postDelayed({
                    openConfiguration()
                    finish()
                }, 2000) // espera 2 segundos
            }
        }
    }

    private fun startScanner() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { qrContent ->
                    // Evitar procesar múltiples veces el mismo código
                    if (isProcessing) return

                    Log.d("QRScanner", "QR detectado: $qrContent")

                    // Validar que el QR tenga el formato correcto
                    if (validateQRFormat(qrContent)) {
                        isProcessing = true
                        processValidQR(qrContent)
                    } else {
                        // Mostrar error si no tiene el formato correcto
                        showInvalidFormatError()
                    }
                }
            }
        })
        barcodeView.resume()
    }

    /**
     * Valida que el QR tenga el formato UFPSCARNET:token
     */
    private fun validateQRFormat(qrContent: String): Boolean {
        return qrContent.startsWith(QR_PREFIX) &&
                qrContent.length > QR_PREFIX.length
    }

    /**
     * Extrae el token JWT del QR
     * Formato: UFPSCARNET:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     */
    private fun extractToken(qrContent: String): String {
        return qrContent.removePrefix(QR_PREFIX).trim()
    }

    /**
     * Procesa un QR válido y navega a la pantalla de validación
     */
    private fun processValidQR(qrContent: String) {
        val token = extractToken(qrContent)

        Log.d(TAG, "Token extraído del QR")

        // Pausar el scanner
        barcodeView.pause()

        // Mostrar feedback al usuario
        showSnack("Validando código QR...", Snackbar.LENGTH_SHORT, false, R.color.ufps_informacion_claro, R.color.ufps_informacion_oscuro)

        // Validar contra el backend
        validateQRWithBackend(token)
    }

    /**
     * Valida el token del QR con el backend
     */
    private fun validateQRWithBackend(token: String) {
        lifecycleScope.launch {
            try {
                // Crear DTO de validación
                val validateDto = mapOf("token" to token)
                // Llamar a la API
                val response = ApiClient.qrApi.validateQr(validateDto)
                if (response.isSuccessful && response.body() != null) {
                    val validationResponse = response.body()!!

                    if (validationResponse.valid && validationResponse.student != null) {
                        // ✅ QR válido
                        val student = "${validationResponse.student.card_photo_key}"
                        Log.d(TAG, student)
                        handleValidQR(validationResponse.student)
                    } else {
                        // ❌ QR inválido
                        handleInvalidQR("QR no válido o expirado")
                    }
                } else {
                    // Error en la respuesta
                    val errorMsg = when (response.code()) {
                        401 -> "QR inválido o expirado"
                        404 -> "Estudiante no encontrado"
                        else -> "Error al validar el QR"
                    }
                    handleInvalidQR(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al validar QR", e)
                handleInvalidQR("Error de conexión: ${e.message}")
            }
        }
    }

    /**
     * Maneja un QR válido
     */
    private fun handleValidQR(student: StudentValidationData) {
        Log.d(TAG, "QR válido para estudiante: ${student.name} ${student.last_name}")

        // Mostrar feedback positivo
        showSnack(
            "✅ Carnet válido: ${student.name} ${student.last_name}",
            Snackbar.LENGTH_SHORT,
            false,
            R.color.ufps_error_claro,
            R.color.ufps_texto_oscuro
        )

        // Navegar a la pantalla de perfil del estudiante
        val intent = Intent(this, StudentProfileActivity::class.java).apply {
            // Pasar los datos del estudiante validado
            putExtra("STUDENT_ID", student.student_id)
            putExtra("STUDENT_CODE", student.student_code)
            putExtra("STUDENT_NAME", student.name)
            putExtra("STUDENT_LAST_NAME", student.last_name)
            putExtra("STUDENT_EMAIL", student.email)
            putExtra("STUDENT_CAREER", student.career)
            putExtra("STUDENT_STATUS", student.status)
            putExtra("STUDENT_TYPE", student.student_type)
            putExtra("CARD_PHOTO_KEY", student.card_photo_key)
            putExtra("IS_VALIDATED", true)
        }

        startActivity(intent)
        finish()
    }

    /**
     * Maneja un QR inválido
     */
    private fun handleInvalidQR(message: String) {

        showSnack(
            "❌ $message",
            Snackbar.LENGTH_LONG,
            false,
            R.color.ufps_error_claro,
            R.color.ufps_error_principal
        )

        // Reiniciar el scanner después de 2 segundos
        barcodeView.postDelayed({
            isProcessing = false
            barcodeView.resume()
        }, 2000)
    }

    /**
     * Muestra un error cuando el QR no tiene el formato correcto
     */
    private fun showInvalidFormatError() {
        val now = System.currentTimeMillis()

        // ⛔ evitar spam visual
        if (now - lastErrorTime < ERROR_COOLDOWN_MS) return

        lastErrorTime = now

        showSnack(
            "⚠️ Formato de QR no válido. Debe ser un carnet UFPS",
            Snackbar.LENGTH_SHORT,
            false,
            R.color.ufps_error_claro,
            R.color.ufps_error_principal
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanner()
            } else {
                showSnack("Permiso de cámara denegado. No podrás escanear códigos.", Snackbar.LENGTH_SHORT, false, R.color.ufps_error_claro, R.color.ufps_error_principal)
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 2000) // espera 2 segundos
            }
        }
    }

    private fun openConfiguration() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized && haveCameraPermission()) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) barcodeView.pause()
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

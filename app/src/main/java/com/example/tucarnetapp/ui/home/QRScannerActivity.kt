package com.example.tucarnetapp.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tucarnetapp.R
import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.data.remote.dto.StudentValidationData
import com.example.tucarnetapp.ui.BaseActivity
import com.example.tucarnetapp.utils.showSnack
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.launch

class QRScannerActivity : BaseActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var flashlightButton: ImageButton
    private lateinit var backButton: ImageButton
    private var flashlightStatus = false

    private var isProcessing = false
    private var lastErrorTime = 0L
    private val ERROR_COOLDOWN_MS = 2000L

    companion object {
        private const val QR_PREFIX = "UFPSCARNET:"
        private const val TAG = "QRScanner"
    }

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

        // ✅ SOLO verificar permiso (NO pedirlo aquí)
        if (!haveCameraPermission()) {
            showSnack(
                "Se requiere permiso de cámara para escanear códigos",
                Snackbar.LENGTH_SHORT,
                false,
                R.color.ufps_error_claro,
                R.color.ufps_error_principal
            )

            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 1500)

            return
        }

        startScanner()

        backButton.setOnClickListener {
            startActivity(Intent(this, HomeScreenActivity::class.java))
            finish()
        }

        flashlightButton.setOnClickListener {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                flashlightStatus = !flashlightStatus
                if (flashlightStatus) {
                    barcodeView.setTorchOn()
                    showSnack(
                        "Linterna encendida",
                        Snackbar.LENGTH_SHORT,
                        false,
                        R.color.ufps_informacion_claro,
                        R.color.ufps_informacion_oscuro
                    )
                } else {
                    barcodeView.setTorchOff()
                    showSnack(
                        "Linterna apagada",
                        Snackbar.LENGTH_SHORT,
                        false,
                        R.color.ufps_informacion_claro,
                        R.color.ufps_informacion_oscuro
                    )
                }
            } else {
                showSnack(
                    "Tu dispositivo no tiene flash",
                    Snackbar.LENGTH_SHORT,
                    false,
                    R.color.ufps_informacion_claro,
                    R.color.ufps_informacion_oscuro
                )
            }
        }
    }

    private fun haveCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startScanner() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { qrContent ->
                    if (isProcessing) return

                    if (validateQRFormat(qrContent)) {
                        isProcessing = true
                        processValidQR(qrContent)
                    } else {
                        showInvalidFormatError()
                    }
                }
            }
        })
        barcodeView.resume()
    }

    private fun validateQRFormat(qrContent: String): Boolean {
        return qrContent.startsWith(QR_PREFIX) &&
                qrContent.length > QR_PREFIX.length
    }

    private fun extractToken(qrContent: String): String {
        return qrContent.removePrefix(QR_PREFIX).trim()
    }

    private fun processValidQR(qrContent: String) {
        val token = extractToken(qrContent)
        barcodeView.pause()

        showSnack(
            "Validando código QR...",
            Snackbar.LENGTH_SHORT,
            false,
            R.color.ufps_informacion_claro,
            R.color.ufps_informacion_oscuro
        )

        validateQRWithBackend(token)
    }

    private fun validateQRWithBackend(token: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.qrApi.validateQr(mapOf("token" to token))

                if (response.isSuccessful && response.body()?.valid == true) {
                    response.body()?.student?.let { handleValidQR(it) }
                } else {
                    handleInvalidQR("QR no válido o expirado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error validando QR", e)
                handleInvalidQR("Error de conexión")
            }
        }
    }

    private fun handleValidQR(student: StudentValidationData) {
        showSnack(
            "✅ Carnet válido: ${student.name} ${student.last_name}",
            Snackbar.LENGTH_SHORT,
            false,
            R.color.ufps_informacion_claro,
            R.color.ufps_texto_oscuro
        )

        startActivity(
            Intent(this, StudentProfileActivity::class.java).apply {
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
        )

        finish()
    }

    private fun handleInvalidQR(message: String) {
        showSnack(
            "❌ $message",
            Snackbar.LENGTH_LONG,
            false,
            R.color.ufps_error_claro,
            R.color.ufps_error_principal
        )

        barcodeView.postDelayed({
            isProcessing = false
            barcodeView.resume()
        }, 2000)
    }

    private fun showInvalidFormatError() {
        val now = System.currentTimeMillis()
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
}

package com.example.tucarnetapp.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tucarnetapp.R
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QRScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var flashlightButton: ImageButton
    private lateinit var backButton: ImageButton
    private var flashlightStatus: Boolean = false
    private val CAMERA_PERMISSION_CODE = 1001
    private val PREF_NAME = "app_permissions"
    private val KEY_CAMERA_REQUESTED = "camera_requested"

    // Formato esperado del QR
    private val QR_PREFIX = "UFPSCARNET:"

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
                    Toast.makeText(this, "Linterna encendida", Toast.LENGTH_SHORT).show()
                } else {
                    barcodeView.setTorchOff()
                    Toast.makeText(this, "Linterna apagada", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Tu dispositivo no tiene flash", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(
                    this,
                    "Debes habilitar el permiso de cámara desde Ajustes para poder escanear.",
                    Toast.LENGTH_LONG
                ).show()
                openConfiguration()
                finish() // 🔹 Cerrar la pantalla
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
     * Valida que el QR tenga el formato UFPSCARNET:codigo
     */
    private fun validateQRFormat(qrContent: String): Boolean {
        return qrContent.startsWith(QR_PREFIX) &&
                qrContent.length > QR_PREFIX.length
    }

    /**
     * Extrae el código del estudiante del QR
     * Formato: UFPSCARNET:1152669 → devuelve "1152669"
     */
    private fun extractStudentCode(qrContent: String): String {
        return qrContent.removePrefix(QR_PREFIX).trim()
    }

    /**
     * Procesa un QR válido y navega a la pantalla de validación
     */
    private fun processValidQR(qrContent: String) {
        val studentCode = extractStudentCode(qrContent)

        Log.d("QRScanner", "Código de estudiante extraído: $studentCode")

        // Pausar el scanner
        barcodeView.pause()

        // Mostrar feedback al usuario
        Toast.makeText(this, "Código detectado: $studentCode", Toast.LENGTH_SHORT).show()

        // Navegar a la pantalla de validación
        val intent = Intent(this, StudentProfileActivity::class.java)
        intent.putExtra("STUDENT_CODE", studentCode)
        startActivity(intent)

        // Cerrar esta actividad
        finish()
    }

    /**
     * Muestra un error cuando el QR no tiene el formato correcto
     */
    private fun showInvalidFormatError() {
        Toast.makeText(
            this,
            "⚠️ QR no válido. Debe ser un carnet UFPS",
            Toast.LENGTH_SHORT
        ).show()

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
                Toast.makeText(
                    this,
                    "Permiso de cámara denegado. No podrás escanear códigos.",
                    Toast.LENGTH_SHORT
                ).show()
                finish() // 🔹 Cierra la pantalla si no lo dio
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
}

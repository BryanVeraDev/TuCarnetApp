package com.example.tucarnetapp.ui.liveness

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AWSCredentialsProvider
import com.amplifyframework.auth.AWSTemporaryCredentials
import com.amplifyframework.auth.AuthException
import com.amplifyframework.core.Consumer
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.liveness.ui.FaceLivenessDetector
import com.amplifyframework.ui.liveness.ui.LivenessColorScheme
import com.example.tucarnetapp.data.repository.LivenessRepository
import com.example.tucarnetapp.ui.home.LoadImageFirstActivity
import com.example.tucarnetapp.utils.SnackRouter
import com.example.tucarnetapp.R
import com.example.tucarnetapp.ui.home.HomeScreenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FaceLivenessActivity : ComponentActivity() {

    private val repository = LivenessRepository()

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) startLivenessFlow()
            else {
                Toast.makeText(this, "Se requieren cámara y audio", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SnackRouter.deliver(this)
        enableEdgeToEdge()
        checkPermissions()
    }

    private fun checkPermissions() {
        val allGranted = requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) startLivenessFlow()
        else permissionLauncher.launch(requiredPermissions)
    }

    private fun startLivenessFlow() {

        lifecycleScope.launch {

            val response = try {
                withContext(Dispatchers.IO) {
                    repository.createLivenessSession()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@FaceLivenessActivity, "Error creando sesión", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            Log.d("DEBUG_LIVENESS", "Respuesta backend completa: $response")

            val sessionId = response.sessionId
            val creds = response.credentials

            if (sessionId.isNullOrBlank()) {
                Toast.makeText(this@FaceLivenessActivity, "Backend retornó sessionId vacío", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            setContent {

                MaterialTheme(colorScheme = LivenessColorScheme.default()) {
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)){
                        FaceLivenessDetector(
                            sessionId = sessionId,
                            region = "us-east-1",

                            credentialsProvider =
                                object : AWSCredentialsProvider<AWSTemporaryCredentials> {

                                    override fun fetchAWSCredentials(
                                        onSuccess: Consumer<AWSTemporaryCredentials>,
                                        onError: Consumer<AuthException>
                                    ) {

                                        try {
                                            val awsCreds = AWSTemporaryCredentials(
                                                creds.accessKeyId,
                                                creds.secretAccessKey,
                                                creds.sessionToken,
                                                Instant.fromIso8601(creds.expiration)
                                            )

                                            onSuccess.accept(awsCreds)

                                        } catch (e: Exception) {
                                            onError.accept(AuthException("Credenciales inválidas", "Credenciales inválidas"))
                                        }
                                    }
                                },

                            onComplete = {
                                Log.i("Liveness", "Verificación completada")
                                val intent = Intent(
                                    this@FaceLivenessActivity,
                                    LoadImageFirstActivity::class.java
                                )
                                SnackRouter.showNext(
                                    message = "Verificación exitosa",
                                    top = true,
                                    backgroundColor = R.color.ufps_success_claro,
                                    textColor = R.color.ufps_success_oscuro
                                )
                                startActivity(intent)
                                finish()
                            },

                            onError = { error: FaceLivenessDetectionException ->
                                Log.e("Liveness", "Error en verificación")
                                val intent = Intent(
                                    this@FaceLivenessActivity,
                                    HomeScreenActivity::class.java
                                )
                                SnackRouter.showNext(
                                    message = "Error en verificación",
                                    top = true,
                                    backgroundColor = R.color.ufps_error_claro,
                                    textColor = R.color.ufps_error_principal
                                )
                                startActivity(intent)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}

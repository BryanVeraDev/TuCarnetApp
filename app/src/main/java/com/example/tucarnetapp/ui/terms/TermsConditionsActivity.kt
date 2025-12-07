package com.example.tucarnetapp.ui.terms

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import com.example.tucarnetapp.ui.home.HomeScreenActivity
import com.example.tucarnetapp.ui.liveness.FaceLivenessActivity
import com.example.tucarnetapp.ui.liveness.StartLivenessDialogXML
import com.example.tucarnetapp.ui.theme.TuCarnetAppTheme
import com.example.tucarnetapp.utils.SnackRouter
import tu.paquete.ui.terms.TermsConditionsScreen

class TermsConditionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SnackRouter.deliver(this)
        setContent {

            TuCarnetAppTheme {

                var showDialog by remember { mutableStateOf(false) }
                var startLiveness by remember { mutableStateOf(false) }

                // Lanzar actividad después de confirmar
                if (startLiveness) {
                    LaunchedEffect(Unit) {
                        val intent = Intent(
                            this@TermsConditionsActivity,
                            FaceLivenessActivity::class.java
                        )
                        startActivity(intent)
                        finish()
                    }
                }

                // === Pantalla de términos ===
                TermsConditionsScreen(
                    onCancel = {
                        val intent = Intent(
                            this@TermsConditionsActivity,
                            HomeScreenActivity::class.java
                        )
                        startActivity(intent)
                        finish()
                    },
                    onAccept = {
                        showDialog = true   // Mostrar diálogo XML
                    }
                )

                // Mostrar el diálogo XML desde Compose
                if (showDialog) {
                    StartLivenessDialogXML(
                        onCancel = { showDialog = false },
                        onConfirm = {
                            showDialog = false
                            startLiveness = true
                        }
                    )
                }
            }
        }
    }
}

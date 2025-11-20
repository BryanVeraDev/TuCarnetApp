package com.example.tucarnetapp.ui.terms

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.example.tucarnetapp.ui.liveness.FaceLivenessActivity
import com.example.tucarnetapp.ui.theme.TuCarnetAppTheme
import tu.paquete.ui.terms.TermsConditionsScreen

class TermsConditionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            TuCarnetAppTheme {

                // Estado que controla cuándo iniciar el Liveness
                var startLiveness by remember { mutableStateOf(false) }

                // Este efecto se ejecuta UNA VEZ cuando startLiveness cambia a true
                if (startLiveness) {
                    LaunchedEffect(Unit) {
                        val intent = Intent(
                            this@TermsConditionsActivity,
                            FaceLivenessActivity::class.java
                        )
                        startActivity(intent)
                        finish() // cerrar la pantalla actual
                    }
                }

                // Pantalla de términos
                TermsConditionsScreen(
                    onCancel = { finish() },

                    onAccept = {
                        // Solo cambiamos estado, NO lanzamos Activity desde Compose
                        startLiveness = true
                    }
                )
            }
        }
    }
}

package com.example.tucarnetapp.ui.terms

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tucarnetapp.R
import com.example.tucarnetapp.ui.home.HomeActivity
import com.example.tucarnetapp.ui.theme.TuCarnetAppTheme
import tu.paquete.ui.terms.TermsConditionsScreen

class TermsConditionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TuCarnetAppTheme {
                TermsConditionsScreen (
                    onCancel = { finish() },
                    onAccept = {
                        // Aquí navegas a tu activity de cámara:
                        // startActivity(Intent(this, CameraActivity::class.java))
                        startActivity(Intent(this, HomeActivity::class.java))
                    }
                )
            }
        }

    }
}
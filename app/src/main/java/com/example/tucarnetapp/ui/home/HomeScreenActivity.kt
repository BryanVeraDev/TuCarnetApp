package com.example.tucarnetapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tucarnetapp.R

class HomeScreenActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var scannerButton: Button
    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_screen)

        startButton = findViewById(R.id.btnStart)
        scannerButton = findViewById(R.id.btnValidate)

        startButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("open_section", "home")
            startActivity(intent)
            finish()
        }

        scannerButton.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            startActivity(intent)
        }

        // ✅ Manejo moderno del botón "atrás"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finishAffinity() // Cierra la app completamente
                } else {
                    doubleBackToExitPressedOnce = true
                    Toast.makeText(this@HomeScreenActivity, "Presiona nuevamente para salir", Toast.LENGTH_SHORT).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000)
                }
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

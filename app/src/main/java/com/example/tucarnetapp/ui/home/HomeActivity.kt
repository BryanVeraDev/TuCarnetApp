package com.example.tucarnetapp.ui.home

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tucarnetapp.R
import com.example.tucarnetapp.ui.home.fragment.IdCardFragment
import com.example.tucarnetapp.ui.home.fragment.HomeFragment
import org.w3c.dom.Text

class HomeActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navCarnet: LinearLayout
    private lateinit var iconHome: ImageView
    private lateinit var textNavHome: TextView
    private lateinit var iconCarnet: ImageView
    private lateinit var textNavCarnet: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Referencias a los elementos de la barra
        navHome = findViewById(R.id.navHome)
        navCarnet = findViewById(R.id.navCarnet)
        iconHome = findViewById(R.id.iconHome)
        textNavHome = findViewById(R.id.textNavHome)
        iconCarnet = findViewById(R.id.iconCarnet)
        textNavCarnet = findViewById(R.id.textNavCarnet)

        // Fragment inicial según el intent
        val sectionToOpen = intent.getStringExtra("open_section")
        when (sectionToOpen) {
            "carnet" -> {
                replaceFragment(IdCardFragment())
                highlightIcon(iconCarnet)
            }
            else -> {
                replaceFragment(HomeFragment())
                highlightIcon(iconHome)
            }
        }

        // Escuchadores de clic
        navHome.setOnClickListener {
            replaceFragment(HomeFragment())
            highlightIcon(iconHome)
            animateIcon(iconHome)
        }

        navCarnet.setOnClickListener {
            replaceFragment(IdCardFragment())
            highlightIcon(iconCarnet)
            animateIcon(iconCarnet)
        }

        // Ajuste de padding para barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainActivityHome)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /** Cambia el fragmento actual */
    private fun replaceFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /** Cambia el color del ícono y del texto seleccionado */
    private fun highlightIcon(selectedIcon: ImageView) {
        val gray = ContextCompat.getColor(this, R.color.ufps_texto_principal)
        val red = ContextCompat.getColor(this, R.color.ufps_principal)

        // RESET (todos grises)
        iconHome.setColorFilter(gray)
        iconCarnet.setColorFilter(gray)
        textNavHome.setTextColor(gray)
        textNavCarnet.setTextColor(gray)

        // ACTIVAR (icono + texto)
        when (selectedIcon.id) {
            R.id.iconHome -> {
                iconHome.setColorFilter(red)
                textNavHome.setTextColor(red)
            }
            R.id.iconCarnet -> {
                iconCarnet.setColorFilter(red)
                textNavCarnet.setTextColor(red)
            }
        }
    }


    /** Pequeña animación de toque */
    private fun animateIcon(icon: ImageView) {
        icon.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(100)
            .withEndAction {
                icon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }.start()
    }
}

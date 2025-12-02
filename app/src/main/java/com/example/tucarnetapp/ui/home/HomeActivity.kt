package com.example.tucarnetapp.ui.home

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tucarnetapp.R
import com.example.tucarnetapp.ui.BaseActivity
import com.example.tucarnetapp.ui.home.fragment.IdCardFragment
import com.example.tucarnetapp.ui.home.fragment.HomeFragment
import org.w3c.dom.Text

class HomeActivity : BaseActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navCarnet: LinearLayout
    private lateinit var iconHome: ImageView
    private lateinit var textNavHome: TextView
    private lateinit var iconCarnet: ImageView
    private lateinit var textNavCarnet: TextView
    private var selectedTabId: Int = R.id.navHome

    companion object {
        private const val TAG = "HomeActivity"
        private const val KEY_SELECTED_TAB = "selected_tab"
    }

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

        if (savedInstanceState == null) {
            val sectionToOpen = intent.getStringExtra("open_section")
            when (sectionToOpen) {
                "carnet" -> {
                    replaceFragment(IdCardFragment())
                    selectedTabId = R.id.navCarnet
                    highlightIcon(iconCarnet)
                }
                else -> {
                    replaceFragment(HomeFragment())
                    selectedTabId = R.id.navHome
                    highlightIcon(iconHome)
                }
            }
        } else {
            selectedTabId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.navHome)
            // Solo ajustamos los iconos, NO reemplazamos el fragment
            when (selectedTabId) {
                R.id.navHome -> highlightIcon(iconHome)
                R.id.navCarnet -> highlightIcon(iconCarnet)
            }
        }

        // Escuchadores de clic
        navHome.setOnClickListener {
            // Si YA estoy en Home, no recreo el fragment
            if (selectedTabId == R.id.navHome) {
                // Si quieres, solo haces la animación
                animateIcon(iconHome)
                return@setOnClickListener
            }
            replaceFragment(HomeFragment())
            selectedTabId = R.id.navHome
            highlightIcon(iconHome)
            animateIcon(iconHome)
        }

        navCarnet.setOnClickListener {
            // Si YA estoy en Carnet, no recreo el fragment
            if (selectedTabId == R.id.navCarnet) {
                animateIcon(iconCarnet)
                return@setOnClickListener
            }
            replaceFragment(IdCardFragment())
            selectedTabId = R.id.navCarnet
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, selectedTabId)
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

    override fun onInternetAvailable() {
        super.onInternetAvailable()
        Log.d(TAG, "✅ Internet restaurado")
    }

    override fun onInternetLost() {
        super.onInternetLost()
        Log.d(TAG, "❌ Internet perdido")
    }
}

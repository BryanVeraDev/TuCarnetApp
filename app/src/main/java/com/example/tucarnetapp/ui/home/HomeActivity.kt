package com.example.tucarnetapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tucarnetapp.R
import com.example.tucarnetapp.session.SessionManager
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.ui.BaseActivity
import com.example.tucarnetapp.ui.home.fragment.IdCardFragment
import com.example.tucarnetapp.ui.home.fragment.HomeFragment
import com.example.tucarnetapp.utils.SnackRouter

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

        // 1. Intentar desde memoria
        var student = UserSession.currentUser

        // 2. Si no está en memoria, intentar desde persistencia
        if (student == null) {
            val sessionManager = SessionManager.getInstance(this)
            student = sessionManager.getStudent()

            if (student != null) {
                UserSession.currentUser = student
            }
        }

        // 3. Si sigue siendo null → forzar login
        if (student == null) {
            startActivity(Intent(this, HomeScreenActivity::class.java))
            finish()
            return
        }

        // Bloquea capturas de pantalla (Para el activity en general)
        setScreenshotsBlocked(true)
        //window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        SnackRouter.deliver(this)

        // Restaurar UserSession
        ensureUserSessionRestored()

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
            .setCustomAnimations(
                android.R.anim.fade_in,   // Animación de entrada del nuevo fragment
                android.R.anim.fade_out,  // Animación de salida del fragment actual
                android.R.anim.fade_in,   // (opcional) al hacer popBackStack
                android.R.anim.fade_out   // (opcional) al hacer popBackStack
            )
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

    /**
     * Restaura UserSession desde SharedPreferences si está vacía
     * Esta es la línea de defensa principal
     */
    private fun ensureUserSessionRestored() {
        if (UserSession.currentUser != null) {
            Log.d(TAG, "✅ UserSession OK")
            return
        }

        Log.w(TAG, "⚠️ UserSession vacía, restaurando...")

        val sessionManager = SessionManager.getInstance(this)
        val student = sessionManager.getStudent()

        if (student != null) {
            Log.d(TAG, "🔄 UserSession restaurada: ${student.name}")
            UserSession.setUser(student)

            // Notificar al fragment actual (si existe)
            //notifyFragmentToRefresh()
        } else {
            Log.e(TAG, "❌ No hay sesión guardada, redirigir a login")
            // Opcional: redirigir a login
        }
    }

    /**
     * Notifica al fragment actual para que se refresque
     */
    private fun notifyFragmentToRefresh() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        when (currentFragment) {
            is HomeFragment -> {
                // El fragment se refrescará automáticamente en onResume
                Log.d(TAG, "📢 HomeFragment será notificado")
            }
            is IdCardFragment -> {
                Log.d(TAG, "📢 IdCardFragment será notificado")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Verificar en cada resume por si acaso
        ensureUserSessionRestored()
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

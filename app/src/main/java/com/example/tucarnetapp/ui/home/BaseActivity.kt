package com.example.tucarnetapp.ui

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tucarnetapp.ui.common.NoInternetDialog
import com.example.tucarnetapp.utils.NetworkMonitor
import com.example.tucarnetapp.utils.NetworkState
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Activity base que monitoriza la conectividad y muestra un diálogo cuando no hay internet.
 */
abstract class BaseActivity : AppCompatActivity(), NoInternetDialog.Listener {

    private lateinit var networkMonitor: NetworkMonitor
    private var isInternetAvailable = true
    private var isMonitoringStarted = false
    // Contador de fragments que requieren bloqueo
    private var screenshotBlockCount = 0
    // Flag para evitar múltiples diálogos
    private var isDialogShowing = false

    companion object {
        private const val DIALOG_TAG = "NoInternetDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkMonitor = NetworkMonitor(this)
    }

    /**
     * Se llama automáticamente DESPUÉS de setContentView()
     */
    override fun onContentChanged() {
        super.onContentChanged()

        if (!isMonitoringStarted) {
            isMonitoringStarted = true
            checkInitialConnectivity()
            observeNetworkChanges()
        }
    }

    private fun checkInitialConnectivity() {
        if (!networkMonitor.isConnected()) {
            isInternetAvailable = false
            showNoInternetDialog()
        }
    }

    private fun observeNetworkChanges() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.observeConnectivity().collect { state ->
                    when (state) {
                        is NetworkState.Available -> {
                            isInternetAvailable = true
                            dismissNoInternetDialog()
                            onInternetAvailable()
                        }

                        is NetworkState.Lost,
                        is NetworkState.Unavailable -> {
                            isInternetAvailable = false
                            showNoInternetDialog()
                            onInternetLost()
                        }

                        is NetworkState.Connecting -> {
                            // Opcional
                        }
                    }
                }
            }
        }
    }

    private fun showNoInternetDialog() {

        // Verificar que la Activity no esté finalizando o destruida
        if (isFinishing || isDestroyed) {
            return
        }

        // Evitar múltiples diálogos
        if (isDialogShowing) {
            return
        }

        val existingDialog = supportFragmentManager.findFragmentByTag(DIALOG_TAG)
        if (existingDialog != null) return

        // Ahora se crea sin parámetros: constructor vacío
        try {
            val dialog = NoInternetDialog()
            dialog.show(supportFragmentManager, DIALOG_TAG)
            isDialogShowing = true
        } catch (e: IllegalStateException) {
            // La Activity guardó su estado, no podemos mostrar el diálogo
            Log.w("BaseActivity", "No se pudo mostrar el diálogo: Activity en segundo plano", e)
        }
    }

    private fun dismissNoInternetDialog() {
        // Verificar que la Activity no esté finalizando o destruida
        if (isFinishing || isDestroyed) {
            return
        }

        try {
            val dialog = supportFragmentManager.findFragmentByTag(DIALOG_TAG) as? NoInternetDialog
            if (dialog != null) {
                dialog.dismissAllowingStateLoss()
                isDialogShowing = false
            }
        } catch (e: Exception) {
            Log.w("BaseActivity", "Error al cerrar el diálogo", e)
        }
    }

    protected fun showSnackbar(message: String) {
        findViewById<android.view.View>(android.R.id.content)?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    protected fun requireInternet(action: () -> Unit): Boolean {
        return if (isInternetAvailable) {
            action()
            true
        } else {
            false
        }
    }

    private val isDebugBuild: Boolean
        get() = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /**
     * Controla si se permiten screenshots
     * @param block true = bloquear, false = desbloquear
     */
    fun setScreenshotsBlocked(block: Boolean) {
        // En modo DEBUG, no aplicamos bloqueo para facilitar el desarrollo
        if (isDebugBuild) {
            Log.d("BaseActivity", "Modo DEBUG (FLAG_DEBUGGABLE): ignorando bloqueo (block=$block)")
            return
        }

        if (block) {
            screenshotBlockCount++
            if (screenshotBlockCount == 1) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
                Log.d("BaseActivity", "🚫 Screenshots bloqueados (count: $screenshotBlockCount)")
            }
        } else {
            screenshotBlockCount = maxOf(0, screenshotBlockCount - 1)
            if (screenshotBlockCount == 0) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                Log.d("BaseActivity", "✅ Screenshots habilitados (count: $screenshotBlockCount)")
            }
        }
    }

    protected open fun onInternetAvailable() {
        // Implementación por defecto vacía
    }

    protected open fun onInternetLost() {
        // Por defecto no hace nada
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Cerrar el diálogo antes de guardar el estado para evitar problemas
        dismissNoInternetDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        isDialogShowing = false
        dismissNoInternetDialog()
    }

    // =========================================================
    // Implementación de NoInternetDialog.Listener
    // =========================================================

    override fun onRetryFromNoInternetDialog() {
        if (networkMonitor.isConnected()) {
            dismissNoInternetDialog()
            onInternetAvailable()
        } else {
            // Si sigue sin internet, podemos dejar el diálogo abierto
            // o volver a mostrarlo (aunque ya está visible)
            showNoInternetDialog()
        }
    }

    override fun onCloseFromNoInternetDialog() {
        isDialogShowing = false
        finish()
    }
}

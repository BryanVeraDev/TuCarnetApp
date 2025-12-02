package com.example.tucarnetapp.ui

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    private fun showNoInternetDialog() {
        val existingDialog = supportFragmentManager.findFragmentByTag(DIALOG_TAG)
        if (existingDialog != null) return

        // Ahora se crea sin parámetros: constructor vacío
        val dialog = NoInternetDialog()
        dialog.show(supportFragmentManager, DIALOG_TAG)
    }

    private fun dismissNoInternetDialog() {
        val dialog = supportFragmentManager.findFragmentByTag(DIALOG_TAG) as? NoInternetDialog
        dialog?.dismiss()
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

    /**
    * Controla si se permiten screenshots
    * @param block true = bloquear, false = desbloquear
    */
    fun setScreenshotsBlocked(block: Boolean) {
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

    override fun onDestroy() {
        super.onDestroy()
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
        finish()
    }
}

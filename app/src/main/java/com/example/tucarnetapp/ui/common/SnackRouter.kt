package com.example.tucarnetapp.utils

import android.app.Activity
import androidx.annotation.ColorRes
import com.google.android.material.snackbar.Snackbar

/**
 * ROUTER GLOBAL DE SNACKBARS ENTRE ACTIVIDADES
 */
object SnackRouter {

    private var pendingMessage: String? = null
    private var pendingDuration: Int = Snackbar.LENGTH_SHORT
    private var pendingTop: Boolean = false
    @ColorRes private var pendingBackground: Int = 0
    @ColorRes private var pendingTextColor: Int = 0

    /**
     * Guarda el snackbar para mostrarlo en la próxima Activity o Fragment.
     */
    fun showNext(
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        top: Boolean = false,
        @ColorRes backgroundColor: Int = 0,
        @ColorRes textColor: Int = 0
    ) {
        pendingMessage = message
        pendingDuration = duration
        pendingTop = top
        pendingBackground = backgroundColor
        pendingTextColor = textColor
    }

    /**
     * Muestra el snackbar si había uno pendiente.
     */
    fun deliver(activity: Activity) {
        val message = pendingMessage ?: return

        // Consumir mensaje
        pendingMessage = null

        // Aplicar colores por defecto si no se enviaron
        val bg = if (pendingBackground == 0) com.example.tucarnetapp.R.color.ufps_principal else pendingBackground
        val tc = if (pendingTextColor == 0) com.example.tucarnetapp.R.color.white else pendingTextColor

        // Llamar tu extensión REAL
        activity.showSnack(
            message = message,
            duration = pendingDuration,
            top = pendingTop,
            backgroundColor = bg,
            textColor = tc
        )
    }
}

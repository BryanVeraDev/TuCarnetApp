package com.example.tucarnetapp.utils

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.tucarnetapp.R
import com.google.android.material.snackbar.Snackbar

// ---------------------------
// Función central
// ---------------------------
private fun showSnackInternal(
    rootView: View,
    context: Context,
    message: String,
    duration: Int,
    top: Boolean,
    @ColorRes backgroundColor: Int,
    @ColorRes textColor: Int
) {
    // -------------------------------------------------------------------------
    // SI EXISTE UN SNACKBAR → DESCARTARLO PARA FORZAR CREACIÓN DE UNO NUEVO
    // -------------------------------------------------------------------------
    val currentSnack = rootView.getTag(R.id.snackbar_tag) as? Snackbar
    currentSnack?.dismiss()

    // -------------------------------------------------------------------------
    // CREAR UN SNUEVO SNACKBAR (SE APLICA SIEMPRE)
    // -------------------------------------------------------------------------
    val snack = Snackbar.make(rootView, message, duration)
    snack.animationMode = Snackbar.ANIMATION_MODE_FADE

    // Fondo y texto
    snack.setBackgroundTint(ContextCompat.getColor(context, backgroundColor))
    snack.setTextColor(ContextCompat.getColor(context, textColor))

    // Centrar texto y aplicar fuente
    val textView = snack.view.findViewById<TextView>(
        com.google.android.material.R.id.snackbar_text
    )
    textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
    textView.gravity = Gravity.CENTER
    textView.maxLines = 3
    textView.typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)

    // -------------------------------------------------------------------------
    // ⬆ Posicionarlo ARRIBA si se pidió
    // -------------------------------------------------------------------------
    if (top) {
        val view = snack.view
        val params = view.layoutParams

        when (params) {
            is FrameLayout.LayoutParams -> {
                params.gravity = Gravity.TOP
                params.topMargin = 120
                view.layoutParams = params
            }
            is RelativeLayout.LayoutParams -> {
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                params.topMargin = 120
                view.layoutParams = params
            }
        }
    }

    // Guardarlo para futuras referencias
    rootView.setTag(R.id.snackbar_tag, snack)

    // Limpiar tag cuando desaparezca
    snack.addCallback(object : Snackbar.Callback() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            rootView.setTag(R.id.snackbar_tag, null)
        }
    })

    snack.show()
}

// ---------------------------
// EXTENSIONES
// ---------------------------

fun Activity.showSnack(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    top: Boolean = false,
    @ColorRes backgroundColor: Int = R.color.ufps_principal,
    @ColorRes textColor: Int = R.color.white
) {
    val rootView = findViewById<View>(android.R.id.content)
    showSnackInternal(rootView, this, message, duration, top, backgroundColor, textColor)
}

fun Fragment.showSnack(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    top: Boolean = false,
    @ColorRes backgroundColor: Int = R.color.ufps_principal,
    @ColorRes textColor: Int = R.color.white
) {
    val activity = requireActivity()
    val rootView = activity.findViewById<View>(android.R.id.content)
    showSnackInternal(rootView, activity, message, duration, top, backgroundColor, textColor)
}

fun View.showSnack(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    top: Boolean = false,
    @ColorRes backgroundColor: Int = R.color.ufps_principal,
    @ColorRes textColor: Int = R.color.white
) {
    showSnackInternal(this, this.context, message, duration, top, backgroundColor, textColor)
}

fun Context.showSnackFromContext(
    rootView: View,
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    top: Boolean = false,
    @ColorRes backgroundColor: Int = R.color.ufps_principal,
    @ColorRes textColor: Int = R.color.white
) {
    showSnackInternal(rootView, this, message, duration, top, backgroundColor, textColor)
}

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
// 🚀 FUNCIÓN CENTRAL
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
    val currentSnack = rootView.getTag(R.id.snackbar_tag) as? Snackbar

    // -------------------------------------------------------------------------
    // 🔁 SI YA EXISTE UN SNACKBAR visible → ACTUALIZARLO, NO CREAR OTRO
    // -------------------------------------------------------------------------
    if (currentSnack != null && currentSnack.isShown) {

        // 🔄 Texto
        currentSnack.setText(message)

        // 🎨 Colores
        currentSnack.setBackgroundTint(ContextCompat.getColor(context, backgroundColor))

        val textView = currentSnack.view.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        )
        textView.setTextColor(ContextCompat.getColor(context, textColor))

        // 📍 Reposicionamiento si es top
        if (top) {
            val view = currentSnack.view
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

        return
    }

    // -------------------------------------------------------------------------
    // ⚡ CREAR UN NUEVO SNACKBAR
    // -------------------------------------------------------------------------
    val snack = Snackbar.make(rootView, message, duration)
    snack.setBackgroundTint(ContextCompat.getColor(context, backgroundColor))
    snack.setTextColor(ContextCompat.getColor(context, textColor))
    snack.animationMode = Snackbar.ANIMATION_MODE_FADE

    // Centrar texto
    val textView = snack.view.findViewById<TextView>(
        com.google.android.material.R.id.snackbar_text
    )
    textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
    textView.gravity = Gravity.CENTER
    textView.maxLines = 3
    textView.typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)

    // Posicionar arriba
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

    // Guardar referencia
    rootView.setTag(R.id.snackbar_tag, snack)

    snack.addCallback(object : Snackbar.Callback() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            rootView.setTag(R.id.snackbar_tag, null)
        }
    })

    snack.show()
}

// ---------------------------
// 🚀 EXTENSIONES
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

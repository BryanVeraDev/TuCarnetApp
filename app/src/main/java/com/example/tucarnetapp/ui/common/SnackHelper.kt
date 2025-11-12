package com.example.tucarnetapp.utils

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tucarnetapp.R
import com.google.android.material.snackbar.Snackbar

fun AppCompatActivity.showSnack(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    top: Boolean = false,
    @ColorRes backgroundColor: Int = R.color.ufps_principal,
    @ColorRes textColor: Int = R.color.white
) {
    val rootView = findViewById<View>(android.R.id.content)

    // Reutiliza el Snackbar si ya hay uno visible (no crea otro)
    val currentSnack = rootView.getTag(R.id.snackbar_tag) as? Snackbar
    if (currentSnack != null && currentSnack.isShown) {
        currentSnack.setText(message)
        return
    }

    val snack = Snackbar.make(rootView, message, duration)
    snack.setBackgroundTint(ContextCompat.getColor(this, backgroundColor))
    snack.setTextColor(ContextCompat.getColor(this, textColor))
    snack.animationMode = Snackbar.ANIMATION_MODE_FADE

    // Centrar el texto del Snackbar
    val textView = snack.view.findViewById<TextView>(
        com.google.android.material.R.id.snackbar_text
    )
    textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
    textView.gravity = Gravity.CENTER
    textView.maxLines = 3 // opcional: por si el mensaje es largo

    if (top) {
        val view = snack.view
        val params = view.layoutParams
        when (params) {
            is FrameLayout.LayoutParams -> {
                params.gravity = Gravity.TOP
                params.topMargin = 80
                view.layoutParams = params
            }
            is RelativeLayout.LayoutParams -> {
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                params.topMargin = 80
                view.layoutParams = params
            }
        }
    }

    // Guarda el snackbar actual como tag en la vista raíz (evita duplicados por Activity)
    rootView.setTag(R.id.snackbar_tag, snack)

    // Limpia la referencia cuando el Snackbar desaparece
    snack.addCallback(object : Snackbar.Callback() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            rootView.setTag(R.id.snackbar_tag, null)
        }
    })

    snack.show()
}

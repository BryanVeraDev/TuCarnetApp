package com.example.tucarnetapp.utils

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.tucarnetapp.R
import com.google.android.material.snackbar.Snackbar

// ---------------------------
// 🚀 FUNCIÓN CENTRAL (NO USAR DIRECTO)
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
    // Previene duplicados
    val currentSnack = rootView.getTag(R.id.snackbar_tag) as? Snackbar
    if (currentSnack != null && currentSnack.isShown) {
        currentSnack.setText(message)
        return
    }

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

    // Guardar referencia para evitar duplicados
    rootView.setTag(R.id.snackbar_tag, snack)

    snack.addCallback(object : Snackbar.Callback() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            rootView.setTag(R.id.snackbar_tag, null)
        }
    })

    snack.show()
}

// ---------------------------
// 🚀 EXTENSIONES UNIVERSALES
// ---------------------------

// Activity
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

// Fragment
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

// View (por si quieres mostrarlo desde un adapter)
fun View.showSnack(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    top: Boolean = false,
    @ColorRes backgroundColor: Int = R.color.ufps_principal,
    @ColorRes textColor: Int = R.color.white
) {
    showSnackInternal(this, this.context, message, duration, top, backgroundColor, textColor)
}

// Context (último recurso)
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

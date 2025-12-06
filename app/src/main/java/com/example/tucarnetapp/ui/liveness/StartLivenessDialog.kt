package com.example.tucarnetapp.ui.liveness

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.tucarnetapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Composable
fun StartLivenessDialogXML(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {

        val dialogView = View.inflate(context, R.layout.dialog_confirm_rekognition, null)

        // 📌 USAMOS MATERIAL DIALOG (bordes redondos, estilo moderno)
        val dialog = MaterialAlertDialogBuilder(
            context,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Botón cancelar
        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        // Botón continuar
        dialogView.findViewById<MaterialButton>(R.id.btnConfirm).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }
}

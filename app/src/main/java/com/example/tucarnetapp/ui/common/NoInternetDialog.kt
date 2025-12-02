package com.example.tucarnetapp.ui.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.tucarnetapp.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Diálogo que se muestra cuando no hay internet.
 * Importante: debe tener un constructor vacío para que Android pueda recrearlo.
 */
class NoInternetDialog : BottomSheetDialogFragment() {

    /**
     * Interfaz para comunicar eventos a la Activity/Fragment que lo muestra.
     */
    interface Listener {
        fun onRetryFromNoInternetDialog()
        fun onCloseFromNoInternetDialog()
    }

    private var listener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Buscamos un listener ya sea en el fragment padre o en la Activity
        listener = when {
            parentFragment is Listener -> parentFragment as Listener
            context is Listener -> context as Listener
            else -> null
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_no_internet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // No permitir cerrar tocando fuera
        isCancelable = false

        view.findViewById<Button>(R.id.btnRetry).setOnClickListener {
            // Notificamos al listener
            listener?.onRetryFromNoInternetDialog()
        }

        view.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dismiss()
            listener?.onCloseFromNoInternetDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // Hacer que el BottomSheet sea no cancelable
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }
}

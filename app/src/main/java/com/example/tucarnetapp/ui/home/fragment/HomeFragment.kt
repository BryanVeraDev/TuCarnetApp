package com.example.tucarnetapp.ui.home.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tucarnetapp.R
import com.example.tucarnetapp.session.QRPreferences
import com.example.tucarnetapp.session.SessionManager
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.ui.home.HomeScreenActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.appbar.MaterialToolbar
import com.example.tucarnetapp.utils.showSnack
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    lateinit var editButton: Button
    lateinit var logoutButton: Button
    lateinit var loadingOverlay: View

    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Referenciar elementos UI
        val topBar  = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        val tvName = view.findViewById<TextView>(R.id.textName)
        val tvCode = view.findViewById<TextView>(R.id.textCode)
        val tvCareer = view.findViewById<TextView>(R.id.textCareer)
        val tvStatus = view.findViewById<TextView>(R.id.textStatus)
        val imgProfile = view.findViewById<ImageView>(R.id.imgProfile)

        editButton = view.findViewById(R.id.btnEdit)
        logoutButton = view.findViewById(R.id.btnLogout)
        loadingOverlay = view.findViewById(R.id.loading_overlay)

        // 2. Obtener el usuario en sesión
        val user = UserSession.currentUser

        if (user != null) {

            // TopBar dinámico
            val firstName = getFirstName(user.name)
            val firstLastName = getFirstLastName(user.last_name)

            topBar.title = "¡Hola, $firstName $firstLastName!"

            // Llenar datos del estudiante
            tvName.text = " ${user.name} ${user.last_name}"
            tvCode.text = " ${user.student_code}"
            tvCareer.text = " ${user.career}"
            tvStatus.text = " ${user.status}"

            // Cargar foto si existe
            if (!user.card_photo_url.isNullOrBlank() && user.card_photo_url.startsWith("http")) {
                Glide.with(requireContext())
                    .load(user.card_photo_url)
                    .placeholder(R.drawable.profile_example_image)
                    .error(R.drawable.profile_example_image)
                    .into(imgProfile)
            } else {
                imgProfile.setImageResource(R.drawable.profile_example_image)
            }
        }

        // 3. Listeners de botones
        editButton.setOnClickListener {
            loadingOverlay.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                loadingOverlay.visibility = View.GONE
                checkPhotoStatus(boolean = false)
            }, 2000)
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    /**
     * Muestra diálogo de confirmación antes de cerrar sesión
     */
    private fun showLogoutConfirmationDialog() {
        // Inflar el layout custom
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_logout, null)

        // Crear el diálogo
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Configurar botones
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        // Mostrar con animación
        dialog.show()
    }

    /**
     * Cierra sesión completamente
     */
    private fun performLogout() {
        Log.d(TAG, "🚪 Cerrando sesión...")

        // Mostrar loading
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // 1. Cerrar sesión de Firebase
                auth.signOut()
                Log.d(TAG, "✅ Sesión Firebase cerrada")

                // 2. Limpiar SessionManager (SharedPreferences)
                val sessionManager = SessionManager.getInstance(requireContext())
                sessionManager.clearSession()
                Log.d(TAG, "✅ SessionManager limpio")

                // 3. Limpiar QR cache
                val qrPrefs = QRPreferences.getInstance(requireContext())
                qrPrefs.clearQR()
                Log.d(TAG, "✅ QR cache limpio")

                // 4. Limpiar UserSession (memoria)
                UserSession.clear()
                Log.d(TAG, "✅ UserSession limpio")

                // 5. Redirigir a HomeScreen
                val intent = Intent(requireContext(), HomeScreenActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

                Log.d(TAG, "✅ Logout completado exitosamente")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cerrar sesión: ${e.message}")

                // Ocultar loading en caso de error
                loadingOverlay.visibility = View.GONE

                showSnack(
                    "Error al cerrar sesión",
                    Snackbar.LENGTH_LONG,
                    true,
                    R.color.ufps_principal,
                    R.color.ufps_principal
                )
            }
        }
    }

    private fun checkPhotoStatus(boolean: Boolean) {
        if (boolean) {
            showSnack("Faltan días para cambiar tu foto.", Snackbar.LENGTH_LONG, true,
                R.color.ufps_informacion_claro, R.color.ufps_informacion_oscuro)
        } else {
            showSnack("Tu foto se ha enviado para validación", Snackbar.LENGTH_LONG, true,
                R.color.ufps_success_claro, R.color.ufps_success_oscuro)
        }
    }

    fun getFirstName(fullName: String?): String {
        if (fullName.isNullOrBlank()) return ""
        return fullName.trim().split(" ").first()
    }

    fun getFirstLastName(fullLastName: String?): String {
        if (fullLastName.isNullOrBlank()) return ""
        return fullLastName.trim().split(" ").first()
    }
}

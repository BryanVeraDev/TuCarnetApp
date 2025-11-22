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
import com.bumptech.glide.Glide
import com.example.tucarnetapp.R
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.ui.home.HomeScreenActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.appbar.MaterialToolbar
import com.example.tucarnetapp.utils.showSnack

class HomeFragment : Fragment() {

    lateinit var editButton: Button
    lateinit var logoutButton: Button
    lateinit var loadingOverlay: View

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
            val intent = Intent(requireContext(), HomeScreenActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
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

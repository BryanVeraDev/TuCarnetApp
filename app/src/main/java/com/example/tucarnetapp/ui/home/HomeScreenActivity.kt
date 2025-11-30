package com.example.tucarnetapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.tucarnetapp.R
import com.example.tucarnetapp.config.Client
import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.data.remote.dto.AuthRequest
import com.example.tucarnetapp.data.remote.dto.StudentResponse
import com.example.tucarnetapp.service.LoginRequest
import com.example.tucarnetapp.session.SessionManager
import com.example.tucarnetapp.session.UserSession
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.tucarnetapp.ui.terms.TermsConditionsActivity

class HomeScreenActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var scannerButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private var doubleBackToExitPressedOnce = false

    companion object {
        private const val TAG = "HomeScreenActivity"
        private const val UFPS_DOMAIN = "@ufps.edu.co"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_screen)

        initializeViews()
        setupFirebase()
        setupButtons()
        setupBackPress()
        setupWindowInsets()

        // Verifica si ya hay sesión activa
        //checkExistingSession()
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.btnStart)
        scannerButton = findViewById(R.id.btnValidate)
    }

    private fun setupFirebase() {
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
    }

    private fun setupButtons() {
        startButton.setOnClickListener {
            signIn()
        }

        scannerButton.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signIn() {
        // Configura el diálogo de Credential Manager
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = getString(R.string.default_web_client_id)
        ).build()

        // Crea la solicitud
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        launchCredentialManager(request)
    }

    private fun launchCredentialManager(request: GetCredentialRequest) {
        lifecycleScope.launch {
            try {
                println(request)
                // Lanza la UI de Credential Manager
                val result = credentialManager.getCredential(
                    context = this@HomeScreenActivity,
                    request = request
                )
                println(result)

                // Extrae las credenciales del resultado
                handleCredential(result.credential)

            } catch (e: GetCredentialException) {
                Log.e(TAG, "Error al obtener credenciales: ${e.message}", e)
                Toast.makeText(
                    this@HomeScreenActivity,
                    "Error al iniciar sesión: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleCredential(credential: Credential) {
        // Verifica si la credencial es de tipo Google ID Token
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

            // Crea el Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Valida que el email termine en @ufps.edu.co
            val email = googleIdTokenCredential.id // El email del usuario
            Log.d(TAG, "Email recibido: $email")

            if (!email.endsWith(UFPS_DOMAIN)) {
                Toast.makeText(
                    this,
                    "Debes usar tu correo institucional (@ufps.edu.co)",
                    Toast.LENGTH_LONG
                ).show()

                // Cierra la sesión
                signOut()
                return
            }

            // Autentica en Firebase
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)

        } else {
            Log.w(TAG, "La credencial no es de tipo Google ID Token")
            Toast.makeText(this, "Tipo de credencial no válido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        lifecycleScope.launch {
            try {
                auth.signInWithCredential(credential).await()
                Log.d(TAG, "Firebase authentication exitosa")

                // Obtén el ID Token de Firebase
                val user = auth.currentUser
                val firebaseIdToken = user?.getIdToken(false)?.await()?.token

                if (firebaseIdToken != null) {
                    sendTokenToBackend(firebaseIdToken)
                } else {
                    Toast.makeText(this@HomeScreenActivity, "Error al obtener token", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error en autenticación Firebase", e)
                Toast.makeText(
                    this@HomeScreenActivity,
                    "Error de autenticación: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendTokenToBackend(firebaseIdToken: String) {

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Construir el body
        val request = AuthRequest(
            uid = user.uid,
            email = user.email ?: "",
            name = user.displayName ?: ""
        )

        lifecycleScope.launch {
            try {
                // Llamada Retrofit
                val response = ApiClient.authApi.login(
                    authHeader = "Bearer $firebaseIdToken",
                    request = request
                )

                if (response.isSuccessful) {

                    val student = response.body()

                    if (student != null) {
                        Log.d("AUTH", "Login exitoso: $student")

                        // Guardar usuario en sesión
                        val sessionManager = SessionManager.getInstance(this@HomeScreenActivity)
                        sessionManager.saveStudent(student)
                        //UserSession.currentUser = student

                        // Tomar estado biométrico para redirigir
                        val userStatus = student.biometric_profile?.status
                        redirectUserBasedOnStatus(userStatus, student)

                    } else {
                        Toast.makeText(this@HomeScreenActivity, "Respuesta vacía del servidor", Toast.LENGTH_LONG).show()
                    }

                } else {
                    val errorText = response.errorBody()?.string()
                    Log.e("AUTH", "Error backend: $errorText")

                    Toast.makeText(
                        this@HomeScreenActivity,
                        "Error del servidor: $errorText",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("AUTH", "Error conexión backend: ${e.message}")
                Toast.makeText(
                    this@HomeScreenActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun redirectUserBasedOnStatus(userStatus: String?, student: StudentResponse) {
        when (userStatus) {
            "APROBADO" -> {
                // Usuario aprobado, ir a la pantalla principal
                val intent = Intent(this, LoadingActivity::class.java)
                intent.putExtra("firebaseId", student.email)
                startActivity(intent)
                finish()
            }
            "PENDIENTE" -> {
                // Usuario pendiente de aprobación
                val intent = Intent(this, TermsConditionsActivity::class.java)
                startActivity(intent)
                finish()
                Toast.makeText(
                    this,
                    "Tu registro está pendiente. Completa la verificación biométrica.",
                    Toast.LENGTH_LONG
                ).show()
            }
            "RECHAZADO" -> {
                // Usuario rechazado
                val intent = Intent(this, TermsConditionsActivity::class.java)
                startActivity(intent)
                finish()
                Toast.makeText(
                    this,
                    "Verificación biométrica rechazada. Debes intentarlo nuevamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Estado desconocido. Completa tu registro.",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this, TermsConditionsActivity::class.java)
                startActivity(intent)
                finish()
                // val intent = Intent(this, RegistrationActivity::class.java)
                // startActivity(intent)
                // finish()
            }
        }
    }

    private fun checkExistingSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Sesión activa detectada: ${currentUser.email}")

            lifecycleScope.launch {
                try {
                    val idToken = currentUser.getIdToken(false).await().token
                    if (idToken != null) {
                        //sendTokenToBackend(idToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al verificar sesión", e)
                }
            }
        }
    }

    private fun signOut() {
        // Cierra sesión en Firebase
        auth.signOut()

        // Limpia las credenciales almacenadas
        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
                Log.d(TAG, "Credenciales limpiadas exitosamente")
            } catch (e: ClearCredentialException) {
                Log.e(TAG, "Error al limpiar credenciales: ${e.message}", e)
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finishAffinity()
                } else {
                    doubleBackToExitPressedOnce = true
                    Toast.makeText(
                        this@HomeScreenActivity,
                        "Presiona nuevamente para salir",
                        Toast.LENGTH_SHORT
                    ).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000)
                }
            }
        })
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

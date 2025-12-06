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
import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.data.remote.dto.AuthRequest
import com.example.tucarnetapp.data.remote.dto.StudentResponse
import com.example.tucarnetapp.session.SessionManager
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.ui.BaseActivity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.tucarnetapp.ui.terms.TermsConditionsActivity
import com.example.tucarnetapp.utils.SnackRouter
import com.example.tucarnetapp.utils.showSnack
import org.json.JSONObject

class HomeScreenActivity : BaseActivity() {

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
        SnackRouter.deliver(this)
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
            requireInternet {
                val intent = Intent(this, QRScannerActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun signIn() {
        requireInternet {
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
                showSnack(
                    message = "Error al iniciar sesión",
                    top = true,
                    backgroundColor = R.color.ufps_error_claro,
                    textColor = R.color.ufps_error_principal
                )
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
                showSnack(
                    message = "Debes usar tu correo institucional (@ufps.edu.co)",
                    top = true,
                    backgroundColor = R.color.ufps_error_claro,
                    textColor = R.color.ufps_error_principal
                )
                // Cierra la sesión
                signOut()
                return
            }

            // Autentica en Firebase
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)

        } else {
            Log.w(TAG, "La credencial no es de tipo Google ID Token")
            showSnack(
                message = "Tipo de credencial no válido",
                top = true,
                backgroundColor = R.color.ufps_error_claro,
                textColor = R.color.ufps_error_principal
            )
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

                if (user != null) {
                    // 🔥 ESTE ES EL ID DE FIREBASE
                    Log.d(TAG, "🔥 Firebase UID: ${user.uid}")
                    Log.d(TAG, "📧 Firebase Email: ${user.email}")
                }

                val firebaseIdToken = user?.getIdToken(false)?.await()?.token

                if (firebaseIdToken != null) {
                    sendTokenToBackend(firebaseIdToken)
                } else {
                    showSnack(
                        message = "Error al obtener token",
                        top = true,
                        backgroundColor = R.color.ufps_error_claro,
                        textColor = R.color.ufps_error_principal
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error en autenticación Firebase", e)
                showSnack(
                    message = "Error de autenticación",
                    top = true,
                    backgroundColor = R.color.ufps_error_claro,
                    textColor = R.color.ufps_error_principal
                )
            }
        }
    }

    private fun sendTokenToBackend(firebaseIdToken: String) {

        val user = auth.currentUser
        if (user == null) {
            showSnack(
                message = "Usuario no encontrado",
                top = true,
                backgroundColor = R.color.ufps_error_claro,
                textColor = R.color.ufps_error_principal
            )
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
                        showSnack(
                            message = "Respuesta vacía del servidor",
                            top = true,
                            backgroundColor = R.color.ufps_error_claro,
                            textColor = R.color.ufps_error_principal
                        )
                    }

                } else {
                    val errorText = response.errorBody()?.string()
                    val errorMessage = try {
                        errorText?.let {
                            JSONObject(it).getString("message")
                        }
                    } catch (e: Exception) {
                        null
                    }

                    Log.e("AUTH", "Error backend: $errorText")

                    showSnack(
                        message = "$errorMessage",
                        top = true,
                        backgroundColor = R.color.ufps_error_claro,
                        textColor = R.color.ufps_error_principal
                    )
                }

            } catch (e: Exception) {
                Log.e("AUTH", "Error conexión backend: ${e.message}")
                showSnack(
                    message = "Error de conexión",
                    top = true,
                    backgroundColor = R.color.ufps_error_claro,
                    textColor = R.color.ufps_error_principal
                )
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
                SnackRouter.showNext(
                    message = "Tu registro está pendiente. Completa la verificación biométrica.",
                    top = true,
                    backgroundColor = R.color.ufps_informacion_claro,
                    textColor = R.color.ufps_informacion_oscuro
                )
                startActivity(intent)
                finish()
            }
            "RECHAZADO" -> {
                // Usuario rechazado
                val intent = Intent(this, TermsConditionsActivity::class.java)
                SnackRouter.showNext(
                    message = "Verificación biométrica rechazada. Debes intentarlo nuevamente.",
                    top = true,
                    backgroundColor = R.color.ufps_error_claro,
                    textColor = R.color.ufps_error_principal
                )
                startActivity(intent)
                finish()
            }
            else -> {
                val intent = Intent(this, TermsConditionsActivity::class.java)
                SnackRouter.showNext(
                    message = "Estado desconocido. Completa tu registro",
                    top = true,
                    backgroundColor = R.color.ufps_error_claro,
                    textColor = R.color.ufps_error_principal
                )
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
                    /*Toast.makeText(
                        this@HomeScreenActivity,
                        "Presiona nuevamente para salir",
                        Toast.LENGTH_SHORT
                    ).show()*/
                    showSnack(
                        message = "Presiona nuevamente para salir",
                        top = true,
                        backgroundColor = R.color.ufps_informacion_claro,
                        textColor = R.color.ufps_informacion_oscuro
                    )

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

    override fun onInternetAvailable() {
        super.onInternetAvailable()
        Log.d(TAG, "✅ Internet restaurado")

        // Verificar si están inicializadas
        if (::startButton.isInitialized && ::scannerButton.isInitialized) {
            startButton.isEnabled = true
            scannerButton.isEnabled = true
        }
    }

    override fun onInternetLost() {
        super.onInternetLost()
        Log.d(TAG, "❌ Internet perdido")

        // Verificar si están inicializadas
        if (::startButton.isInitialized && ::scannerButton.isInitialized) {
            startButton.isEnabled = false
            scannerButton.isEnabled = false
        }
    }
}
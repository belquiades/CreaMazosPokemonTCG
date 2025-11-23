package com.cesar.creamazospoketcg.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

/**
 * LoginFragment - versión de diagnóstico.
 *
 * - Fuerza clickable/focusable y bringToFront en botones.
 * - Registra onTouchListeners en root y en los botones para comprobar llegada de eventos.
 * - Loggea todo con TAG = "LoginDebug".
 *
 * Reemplaza el fichero actual por este para depurar. Ejecuta, abre Logcat y filtra por "LoginDebug".
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private lateinit var googleClient: GoogleSignInClient
    private lateinit var lanzadorGoogleSignIn: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "LoginDebug"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lanzadorGoogleSignIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            Log.d(TAG, "lanzadorGoogleSignIn: resultCode=${resultado.resultCode}")
            val intent = resultado.data
            if (resultado.resultCode == android.app.Activity.RESULT_OK && intent != null) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                    val cuenta = task.result
                    val tokenId = cuenta?.idToken
                    Log.d(TAG, "Google tokenId != null? ${!tokenId.isNullOrBlank()}")
                    if (!tokenId.isNullOrBlank()) {
                        firebaseAuthWithGoogle(tokenId)
                    } else {
                        toast("No se obtuvo token de Google")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en Google result", e)
                    toast("Error Google Sign-In")
                }
            } else {
                toast("Google Sign-In cancelado o no OK")
            }
        }

        // Config Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(requireContext(), gso)
        Log.d(TAG, "onCreate: GoogleSignInClient creado")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated - inicio")
        super.onViewCreated(view, savedInstanceState)

        // Forzamos que el root acepte eventos táctiles (evita overlays que no dejen pasar)
        try {
            binding.root.isClickable = true
            binding.root.isFocusable = true
            binding.root.isFocusableInTouchMode = true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo configurar root clickable/focusable: ${e.message}")
        }

        // FORCE: make interactive views above any overlay
        try {
            // elevate and bring to front
            binding.btnContinuar.apply {
                isClickable = true
                isFocusable = true
                isFocusableInTouchMode = true
                elevation = 20f
                bringToFront()
            }
            binding.llBtnGoogle.apply {
                isClickable = true
                isFocusable = true
                isFocusableInTouchMode = true
                elevation = 20f
                bringToFront()
            }
            binding.tvNoTienesCuenta.apply {
                isClickable = true
                isFocusable = true
                elevation = 20f
                bringToFront()
            }
            binding.tvOlvidaste.apply {
                isClickable = true
                isFocusable = true
                elevation = 20f
                bringToFront()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error forzando bringToFront/elevation: ${e.message}")
        }

        // Registro de touch listener en root: detecta cualquier toque en la pantalla
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "ROOT touch ACTION_DOWN at (${event.x}, ${event.y})")
            } else if (event.action == MotionEvent.ACTION_UP) {
                Log.d(TAG, "ROOT touch ACTION_UP at (${event.x}, ${event.y})")
            }
            // No consumir (return false) para que las vistas hijas también reciban eventos.
            false
        }

        // Registramos touch listeners en las vistas que deberían recibir clicks
        safeRegisterTouchAndClick(binding.btnContinuar, "btnContinuar") {
            // click action: same as before
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            Log.d(TAG, "CLICK btnContinuar: emailLen=${email.length}, passLen=${password.length}")
            if (email.isBlank() || password.isBlank()) {
                toast("Introduce email y contraseña")
                return@safeRegisterTouchAndClick
            }
            binding.btnContinuar.isEnabled = false
            iniciarSesionEmail(email, password)
        }

        safeRegisterTouchAndClick(binding.llBtnGoogle, "llBtnGoogle") {
            Log.d(TAG, "CLICK llBtnGoogle")
            iniciarSignInGoogle()
        }

        safeRegisterTouchAndClick(binding.tvNoTienesCuenta, "tvNoTienesCuenta") {
            Log.d(TAG, "CLICK tvNoTienesCuenta")
            try {
                findNavController().navigate(R.id.action_loginFragment_to_registroFragment)
            } catch (e: Exception) {
                Log.w(TAG, "Nav a registro falló", e)
                toast("Registro no disponible")
            }
        }

        safeRegisterTouchAndClick(binding.tvOlvidaste, "tvOlvidaste") {
            Log.d(TAG, "CLICK tvOlvidaste")
            try {
                findNavController().navigate(R.id.action_loginFragment_to_recuperarPasswordFragment)
            } catch (e: Exception) {
                Log.w(TAG, "Nav recuperar pw falló", e)
                toast("Recuperar contraseña no disponible")
            }
        }

        // Mensaje visible para confirmar que fragment está activo
        toast("Login listo (diagnóstico). Mira Logcat (LoginDebug) al tocar botones.")
        Log.d(TAG, "onViewCreated - listeners registrados")
    }

    /**
     * Registra un touch listener que loggea la llegada del evento y además setea el click listener.
     * El bloque 'onClickAction' se ejecuta cuando se detecta ACTION_UP dentro del view (comportamiento click).
     */
    private fun safeRegisterTouchAndClick(view: View, name: String, onClickAction: () -> Unit) {
        try {
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.d(TAG, "$name touch ACTION_DOWN at (${event.x},${event.y})")
                        // devolver false para permitir que el sistema trate el click (y visual ripple)
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.d(TAG, "$name touch ACTION_UP at (${event.x},${event.y}) -> interpretamos como click")
                        // Ejecutar acción click
                        onClickAction()
                        true // consumimos ACTION_UP para evitar doble ejecución
                    }
                    else -> false
                }
            }
            // también registramos un OnClickListener simple por si onTouch no se dispara
            view.setOnClickListener {
                Log.d(TAG, "$name onClick callback - ejecutando acción")
                onClickAction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "safeRegisterTouchAndClick($name) fallo", e)
            toast("Error interno en $name")
        }
    }

    private fun iniciarSesionEmail(email: String, password: String) {
        Log.d(TAG, "iniciarSesionEmail -> iniciando Firebase signIn")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.btnContinuar.isEnabled = true
                if (task.isSuccessful) {
                    Log.d(TAG, "SignIn OK -> ${auth.currentUser?.email}")
                    toast("Inicio de sesión correcto")
                    navegarAPerfilYLimpiarStack()
                } else {
                    val mensaje = task.exception?.localizedMessage ?: "Error autenticando"
                    Log.w(TAG, "SignIn error: $mensaje", task.exception)
                    binding.tvErrorPassword.text = mensaje
                    toast(mensaje)
                }
            }
    }

    private fun iniciarSignInGoogle() {
        Log.d(TAG, "iniciarSignInGoogle -> lanzando intent Google")
        try {
            val intent = googleClient.signInIntent
            lanzadorGoogleSignIn.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando GoogleSignIn", e)
            toast("No se pudo iniciar Google Sign-In")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "firebaseAuthWithGoogle -> procesando token")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        binding.llBtnGoogle.isEnabled = false
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                binding.llBtnGoogle.isEnabled = true
                if (task.isSuccessful) {
                    Log.d(TAG, "Auth Google OK -> ${auth.currentUser?.email}")
                    toast("Autenticado con Google")
                    navegarAPerfilYLimpiarStack()
                } else {
                    val msg = task.exception?.localizedMessage ?: "Error autenticando con Google"
                    Log.w(TAG, "Auth Google error: $msg", task.exception)
                    toast(msg)
                }
            }
    }

    private fun navegarAPerfilYLimpiarStack() {
        Log.d(TAG, "navegarAPerfilYLimpiarStack -> navegación a perfil")
        val opciones = NavOptions.Builder()
            .setPopUpTo(R.id.loginFragment, true)
            .build()
        try {
            findNavController().navigate(R.id.action_loginFragment_to_perfilFragment, null, opciones)
        } catch (e: Exception) {
            Log.w(TAG, "Acción nav no encontrada, intentando por id", e)
            try {
                findNavController().navigate(R.id.perfilFragment, null, opciones)
            } catch (ex: Exception) {
                Log.e(TAG, "Navegación a perfil fallida", ex)
                toast("No se pudo abrir perfil")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        Log.d(TAG, "TOAST -> $msg")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView - liberando binding")
        _binding = null
    }
}

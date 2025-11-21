package com.cesar.creamazospoketcg.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var gso: GoogleSignInOptions

    private val TAG = "LoginFragment"

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: run {
                mostrarError("No hubo respuesta de Google")
                return@registerForActivityResult
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account?.idToken)
            } catch (e: ApiException) {
                mostrarError("Error Google Sign-In")
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        auth = Firebase.auth

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Si ya está logueado → navegar directamente
        auth.currentUser?.let {
            findNavController().navigate(R.id.fragmentoBusquedaCartas)
            return
        }

        aplicarClickSoloEnRegistrate()
        configurarEventos()
    }

    /** 🔥 Hace clic solo en la palabra "Regístrate" */
    private fun aplicarClickSoloEnRegistrate() {
        val texto = "¿No tienes cuenta? Regístrate"
        val spannable = SpannableString(texto)

        val palabra = "Regístrate"
        val inicio = texto.indexOf(palabra)
        val fin = inicio + palabra.length

        val clickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(R.id.action_loginFragment_to_registroFragment)
            }
        }

        spannable.setSpan(clickable, inicio, fin, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvNoTienesCuenta.text = spannable
        binding.tvNoTienesCuenta.movementMethod = LinkMovementMethod.getInstance()
        binding.tvNoTienesCuenta.highlightColor = Color.TRANSPARENT
    }

    private fun configurarEventos() {

        binding.btnContinuar.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                mostrarError("Introduce correo y contraseña")
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        findNavController().navigate(R.id.fragmentoBusquedaCartas)
                    } else {
                        mostrarError("Datos incorrectos")
                    }
                }
        }

        // Recuperar contraseña
        binding.tvOlvidaste.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_recuperarPasswordFragment)
        }

        // Google
        binding.llBtnGoogle.setOnClickListener {
            val cliente = GoogleSignIn.getClient(requireContext(), gso)
            googleSignInLauncher.launch(cliente.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken == null) {
            mostrarError("Token inválido")
            return
        }
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credencial)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    findNavController().navigate(R.id.fragmentoBusquedaCartas)
                } else mostrarError("Error autenticando con Google")
            }
    }

    private fun mostrarError(msg: String) {
        binding.tvErrorPassword.visibility = View.VISIBLE
        binding.tvErrorPassword.text = msg
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

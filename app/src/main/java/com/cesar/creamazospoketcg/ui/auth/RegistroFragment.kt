package com.cesar.creamazospoketcg.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.FragmentRegistroBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * RegistroFragment
 * Permite crear una cuenta con email y contraseña usando Firebase.
 */
class RegistroFragment : Fragment() {

    private var _binding: FragmentRegistroBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegistroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnCrearCuenta.setOnClickListener {
            val email = binding.etEmailReg.text.toString().trim()
            val pass1 = binding.etPasswordReg.text.toString()
            val pass2 = binding.etPasswordRegConfirm.text.toString()

            if (email.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
                mostrarError("Todos los campos son obligatorios")
                return@setOnClickListener
            }

            if (pass1 != pass2) {
                mostrarError("Las contraseñas no coinciden")
                return@setOnClickListener
            }

            crearCuenta(email, pass1)
        }

        binding.tvVolverLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registroFragment_to_loginFragment)
        }
    }

    private fun crearCuenta(email: String, password: String) {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Opcional: envío de correo de verificación
                    Firebase.auth.currentUser?.sendEmailVerification()
                    findNavController().navigate(R.id.action_registroFragment_to_loginFragment)
                } else {
                    mostrarError("Error creando la cuenta: ${task.exception?.localizedMessage}")
                }
            }
    }

    private fun mostrarError(msg: String) {
        binding.tvErrorRegistro.text = msg
        binding.tvErrorRegistro.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

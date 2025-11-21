package com.cesar.creamazospoketcg.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cesar.creamazospoketcg.databinding.FragmentRecuperarPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class RecuperarPasswordFragment : Fragment() {

    private var _binding: FragmentRecuperarPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecuperarPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // BOTÓN ENVIAR CORREO
        binding.btnEnviarCorreo.setOnClickListener {
            val email = binding.etEmailRecuperar.text.toString().trim()

            if (email.isEmpty()) {
                binding.tvErrorRecuperar.text = "Introduce tu correo"
                binding.tvErrorRecuperar.visibility = View.VISIBLE
                return@setOnClickListener
            }

            enviarCorreo(email)
        }

        // TEXTO "VOLVER AL LOGIN"
        binding.tvVolverLoginRecuperar.setOnClickListener {
            findNavController().navigate(
                com.cesar.creamazospoketcg.R.id.action_recuperarPasswordFragment_to_loginFragment
            )
        }
    }

    private fun enviarCorreo(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                binding.tvErrorRecuperar.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                binding.tvErrorRecuperar.text = "Correo enviado correctamente"
                binding.tvErrorRecuperar.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                binding.tvErrorRecuperar.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                binding.tvErrorRecuperar.text = "Error: ${e.message}"
                binding.tvErrorRecuperar.visibility = View.VISIBLE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

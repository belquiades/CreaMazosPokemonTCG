package com.cesar.creamazospoketcg.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.FragmentPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.snackbar.Snackbar

/**
 * PerfilFragment
 *
 * Pantalla que aparece después del login.
 * Muestra información básica del usuario y permite navegar
 * hacia otras partes de la aplicación.
 */
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usuario = auth.currentUser

        // Mostramos nombre y email
        binding.tvEmailUsuario.text = usuario?.email ?: "Correo no disponible"
        binding.tvNombreUsuario.text = usuario?.displayName ?: usuario?.email ?: "Usuario"

        // BOTÓN: Mis Cartas
        binding.cardMisCartas.setOnClickListener {
            try {
                findNavController().navigate(R.id.fragmentoMisCartas)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "No se pudo abrir Mis Cartas", Snackbar.LENGTH_SHORT).show()
            }
        }

        // BOTÓN: Cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            auth.signOut()
            try {
                findNavController().navigate(R.id.loginFragment)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Sesión cerrada, pero no se pudo navegar.", Snackbar.LENGTH_SHORT).show()
            }
        }

        // BOTÓN: Mis Mazos (aún sin implementar)
        binding.cardMisMazos.setOnClickListener {
            Snackbar.make(binding.root, "Próximamente: Mis Mazos", Snackbar.LENGTH_SHORT).show()
        }

        // BOTÓN: Optimizar mazo (aún sin implementar)
        binding.cardOptimizarMazo.setOnClickListener {
            Snackbar.make(binding.root, "Próximamente: Optimizar Mazo", Snackbar.LENGTH_SHORT).show()
        }

        // En un futuro puedes implementar avatar y sobrenombre aquí
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

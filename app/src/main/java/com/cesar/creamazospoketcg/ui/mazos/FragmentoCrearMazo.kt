package com.cesar.creamazospoketcg.ui.mazos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cesar.creamazospoketcg.databinding.FragmentCrearMazoBinding
import com.cesar.creamazospoketcg.data.repository.RepositorioMazos

class FragmentoCrearMazo : Fragment() {

    private var _binding: FragmentCrearMazoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrearMazoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnCrearMazo.setOnClickListener {
            crearMazo()
        }
    }

    private fun crearMazo() {
        val nombreMazo = binding.etNombreMazo.text.toString().trim()

        if (nombreMazo.isEmpty()) {
            binding.etNombreMazo.error = "Introduce un nombre"
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        RepositorioMazos.crearMazo(nombreMazo)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Mazo creado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                // En el siguiente paso navegaremos al detalle del mazo
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Error creando el mazo",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

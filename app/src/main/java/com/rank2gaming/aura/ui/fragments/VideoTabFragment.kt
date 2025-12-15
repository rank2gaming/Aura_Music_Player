package com.rank2gaming.aura.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rank2gaming.aura.databinding.FragmentVideoTabBinding
// Imports from the :youtube module
import com.rank2gaming.aura.youtube.LocalVideoListActivity
import com.rank2gaming.aura.youtube.YouTubeDashboardActivity

class VideoTabFragment : Fragment() {

    private var _binding: FragmentVideoTabBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Launch Local Video List
        binding.cardLocalVideo.setOnClickListener {
            val intent = Intent(requireContext(), LocalVideoListActivity::class.java)
            startActivity(intent)
        }

        // Launch Online YouTube Dashboard
        binding.cardOnlineVideo.setOnClickListener {
            val intent = Intent(requireContext(), YouTubeDashboardActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
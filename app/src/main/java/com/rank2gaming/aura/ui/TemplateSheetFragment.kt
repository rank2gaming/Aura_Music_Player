package com.rank2gaming.aura.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rank2gaming.aura.R
import com.rank2gaming.aura.utils.TemplateManager

class TemplateSheetFragment(private val onApply: () -> Unit) : BottomSheetDialogFragment() {

    private var selectedIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_template_sheet, container, false)

        selectedIndex = TemplateManager.getLayoutIndex(requireContext())

        // Header Back Button
        view.findViewById<ImageView>(R.id.btn_sheet_back).setOnClickListener { dismiss() }

        // Setup Slider
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_templates)
        recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recycler)

        val adapter = LayoutLiveAdapter()
        recycler.adapter = adapter
        recycler.scrollToPosition(selectedIndex)

        // Setup Apply Button
        view.findViewById<Button>(R.id.btn_apply_template).setOnClickListener {
            TemplateManager.saveLayoutIndex(requireContext(), selectedIndex)
            onApply()
            dismiss()
        }

        return view
    }

    // --- NEW: Force Full Screen & Disable Swipe to Dismiss ---
    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED // Force Full Height
            behavior.skipCollapsed = true
            behavior.isDraggable = false // Disable Swipe Down

            // Ensure the container fills the screen
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    inner class LayoutLiveAdapter : RecyclerView.Adapter<LayoutLiveAdapter.VH>() {
        private val list = TemplateManager.layouts

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_template_card, parent, false)
            view.layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]

            holder.layoutContainer.removeAllViews()

            try {
                // Inflate Layout
                val livePreview = LayoutInflater.from(holder.itemView.context).inflate(item.layoutRes, holder.layoutContainer, false)

                // --- FIX FOR VISIBILITY (Play Button & Seekbar) ---
                // We manually find views in the preview and set them to look "active"

                // 1. Fix Play Button (Common ID)
                val btnPlay = livePreview.findViewById<ImageView>(R.id.btn_play_pause)
                btnPlay?.setImageResource(R.drawable.ic_play)
                btnPlay?.visibility = View.VISIBLE

                // 2. Fix Seekbar (Standard Templates)
                val seekBar = livePreview.findViewById<SeekBar>(R.id.seekBar)
                seekBar?.progress = 30 // Visual progress
                seekBar?.visibility = View.VISIBLE

                // 3. Fix Custom Seekbars (Templates like Player 1, 2, etc. using View/ConstraintLayout)
                val progressActive = livePreview.findViewById<View>(R.id.progressActive)
                if (progressActive != null) {
                    val params = progressActive.layoutParams
                    params.width = 300 // Force a width so the "bar" is visible
                    progressActive.layoutParams = params
                    progressActive.visibility = View.VISIBLE
                }

                holder.layoutContainer.addView(livePreview)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            holder.indicator.visibility = if (item.index == selectedIndex) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                val previousIndex = selectedIndex
                selectedIndex = item.index
                notifyItemChanged(previousIndex)
                notifyItemChanged(selectedIndex)
            }
        }

        override fun getItemCount() = list.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val layoutContainer: FrameLayout = v.findViewById(R.id.container_live_layout)
            val indicator: ImageView = v.findViewById(R.id.img_selected_indicator)
        }
    }
}
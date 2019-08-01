package com.robertlevonyan.demo.camerax.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.robertlevonyan.demo.camerax.R

class PreviewFragment : Fragment() {

    companion object {
        fun newInstance() = PreviewFragment()
    }

    private lateinit var viewModel: PreviewViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(PreviewViewModel::class.java)
        // TODO: Use the ViewModel
    }

}

package com.centsnippers.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.centsnippers.R

class HelpFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help, container, false)
    }
}

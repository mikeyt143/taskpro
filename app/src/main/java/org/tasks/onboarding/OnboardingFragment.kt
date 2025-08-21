package org.tasks.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import org.tasks.R
import org.tasks.preferences.Preferences
import com.google.android.material.button.MaterialButton

class OnboardingFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)
        val pager = view.findViewById<ViewPager2>(R.id.onboarding_pager)
        val btnDone = view.findViewById<MaterialButton>(R.id.onboarding_done)
        pager.adapter = OnboardingAdapter()
        btnDone.setOnClickListener {
            Preferences(requireContext()).setBoolean("p_onboarding_shown", true)
            dismiss()
        }
        return view
    }
}

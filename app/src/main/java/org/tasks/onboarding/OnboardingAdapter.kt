package org.tasks.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.tasks.R

class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.Holder>() {
    private val pages = listOf(R.layout.onboarding_page_1, R.layout.onboarding_page_2, R.layout.onboarding_page_3)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return Holder(v)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {}
    override fun getItemCount(): Int = pages.size
    override fun getItemViewType(position: Int): Int = pages[position]
    class Holder(v: View) : RecyclerView.ViewHolder(v)
}

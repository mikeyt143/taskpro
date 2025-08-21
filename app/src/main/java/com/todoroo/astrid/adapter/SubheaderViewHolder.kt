package com.todoroo.astrid.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.tasks.R
import org.tasks.databinding.FilterAdapterSubheaderBinding
import org.tasks.filters.NavigationDrawerSubheader

internal class SubheaderViewHolder(
    itemView: View,
    private val clickHandler: ClickHandler,
): RecyclerView.ViewHolder(itemView) {

    interface ClickHandler {
        fun onClick(subheader: NavigationDrawerSubheader)
        fun onAdd(subheader: NavigationDrawerSubheader)
        fun showError()
    }

    private val text: TextView
    private val chevron: ImageView
    private val add: ImageView
    private val errorIcon: ImageView
    private var rotation = 0f

    private lateinit var subheader: NavigationDrawerSubheader

    fun bind(subheader: NavigationDrawerSubheader) {
        add.isVisible = subheader.addIntentRc > 0
        this.subheader = subheader
        text.text = subheader.title
        when {
            subheader.error ->
                with(errorIcon) {
                setColorFilter(ContextCompat.getColor(itemView.context, R.color.overdue))
                visibility = View.VISIBLE
            }
            else -> errorIcon.visibility = View.GONE
        }
        rotation = if (subheader.isCollapsed) -180f else 0f
        chevron.rotation = rotation
    }

    private fun rotate() {
        rotation = if (rotation == 0f) -180f else 0f
        chevron.animate().rotation(rotation).setDuration(250).start()
    }

    init {
        FilterAdapterSubheaderBinding.bind(itemView).let {
            text = it.text
            errorIcon = it.iconError
            chevron = it.chevron
            add = it.addItem
            it.subheaderRow.setOnClickListener {
                rotate()
                clickHandler.onClick(subheader)
            }
            it.addItem.setOnClickListener {
                clickHandler.onAdd(subheader)
            }
        }
        errorIcon.setOnClickListener { clickHandler.showError() }
    }
}
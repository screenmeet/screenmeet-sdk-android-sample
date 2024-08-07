package com.screenmeet.live.tools

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class ParticipantsHorizontalItemDecoration(private val spacing: Int) : ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        outRect.left = if (position == 0) spacing else 0
        outRect.right = spacing
        outRect.bottom = spacing
        outRect.top = spacing
    }
}

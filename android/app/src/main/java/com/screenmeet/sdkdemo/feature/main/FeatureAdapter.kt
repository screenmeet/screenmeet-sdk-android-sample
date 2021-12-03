package com.screenmeet.sdkdemo.feature.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenmeet.sdkdemo.R
import com.screenmeet.sdkdemo.databinding.ItemFeatureBinding

typealias onItemClick = (Pair<String, Int>) -> Unit

class FeatureAdapter (
    private val click: onItemClick
) : ListAdapter<Pair<String, Int>, FeatureAdapter.ViewHolder>(FeatureComparator()) {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemFeatureBinding.inflate(LayoutInflater.from(viewGroup.context)))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = getItem(position)
        viewHolder.binding.featureTv.text = item.first
        viewHolder.binding.root.setOnClickListener { click(item) }

        val resources = viewHolder.binding.root.context.resources
        val params = viewHolder.binding.featureTv.layoutParams as ViewGroup.MarginLayoutParams
        when(position){
            1 -> {
                params.topMargin = resources.getDimension(R.dimen.feature_grid_top_margin_large).toInt()
                params.bottomMargin = 0
            }
            itemCount - 1 -> {
                params.topMargin = resources.getDimension(R.dimen.feature_grid_top_margin).toInt()
                params.bottomMargin = resources.getDimension(R.dimen.feature_grid_top_margin).toInt()
            }
            else -> {
                params.topMargin = resources.getDimension(R.dimen.feature_grid_top_margin).toInt()
                params.bottomMargin = 0
            }
        }
    }

    class ViewHolder(val binding: ItemFeatureBinding) : RecyclerView.ViewHolder(binding.root)

    class FeatureComparator: DiffUtil.ItemCallback<Pair<String, Int>>() {
        override fun areItemsTheSame(oldItem: Pair<String, Int>, newItem: Pair<String, Int>) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Pair<String, Int>, newItem: Pair<String, Int>) = oldItem == newItem
    }

}
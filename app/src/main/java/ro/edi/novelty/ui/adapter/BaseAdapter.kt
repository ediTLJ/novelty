/*
* Copyright 2019 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package ro.edi.novelty.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ro.edi.novelty.BR

abstract class BaseAdapter<T>(itemCallback: ItemCallback<T>) :
    ListAdapter<T, BaseAdapter<T>.BaseViewHolder>(itemCallback) {
    protected abstract fun getModel(): ViewModel

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ViewDataBinding>(
            layoutInflater, viewType, parent, false
        )
        return BaseViewHolder(binding)
    }

    final override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(getModel(), position)
    }

    final override fun getItemViewType(position: Int): Int {
        return getItemLayoutId(position)
    }

    protected abstract fun getItemLayoutId(position: Int): Int

    protected abstract fun bind(position: Int, binding: ViewDataBinding)

    protected abstract fun onClick(context: Context, position: Int)

    protected abstract fun onLongClick(context: Context, position: Int): Boolean

    inner class BaseViewHolder(private val binding: ViewDataBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ViewModel, position: Int) {
            binding.root.setOnClickListener { onClick(binding.root.context, position) }
            binding.root.setOnLongClickListener { onLongClick(binding.root.context, position) }
            binding.setVariable(BR.model, model)
            binding.setVariable(BR.position, position)
            bind(position, binding)
            binding.executePendingBindings()
        }
    }
}
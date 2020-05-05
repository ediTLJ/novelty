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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ro.edi.novelty.BR

abstract class BaseAdapter<T>(itemCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, BaseAdapter<T>.BaseViewHolder>(itemCallback) {
    protected abstract fun getModel(): ViewModel

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ViewDataBinding>(
            layoutInflater, viewType, parent, false
        )
        binding.setVariable(BR.model, getModel())
        return BaseViewHolder(binding)
    }

    final override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(position)
    }

    final override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.bind(position, payloads)
    }

    final override fun getItemViewType(position: Int): Int {
        return getItemLayoutId(position)
    }

    protected abstract fun getItemLayoutId(position: Int): Int

    protected open fun onItemTouch(
        itemView: View,
        event: MotionEvent?,
        holder: RecyclerView.ViewHolder,
        position: Int
    ): Boolean {
        return false
    }

    protected open fun onItemClick(itemView: View, position: Int) {

    }

    protected open fun onItemLongClick(itemView: View, position: Int): Boolean {
        return false
    }

    protected open fun getTouchableViewIds(): IntArray? {
        return null
    }

    protected open fun onTouch(
        v: View,
        event: MotionEvent?,
        holder: RecyclerView.ViewHolder,
        position: Int
    ): Boolean {
        return false
    }

    protected open fun getClickableViewIds(): IntArray? {
        return null
    }

    protected open fun onClick(v: View, position: Int) {

    }

    protected open fun bind(binding: ViewDataBinding, position: Int) {

    }

    protected open fun bind(binding: ViewDataBinding, position: Int, payloads: MutableList<Any>) {

    }

    inner class BaseViewHolder(private val binding: ViewDataBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnTouchListener, View.OnClickListener,
        View.OnLongClickListener {

        init {
            itemView.setOnTouchListener(this)
            getTouchableViewIds()?.let { ids ->
                for (id in ids) {
                    itemView.findViewById<View>(id)?.setOnTouchListener(this)
                }
            }

            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)

            getClickableViewIds()?.let { ids ->
                for (id in ids) {
                    itemView.findViewById<View>(id)?.setOnClickListener(this)
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            v?.let {
                return if (v.id == itemView.id) {
                    onItemTouch(it, event, this, bindingAdapterPosition)
                } else {
                    onTouch(it, event, this, bindingAdapterPosition)
                }
            }

            return false
        }

        override fun onClick(v: View?) {
            v?.let {
                if (v.id == itemView.id) {
                    onItemClick(it, bindingAdapterPosition)
                } else {
                    onClick(it, bindingAdapterPosition)
                }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            v?.let {
                return onItemLongClick(it, bindingAdapterPosition)
            }

            return false
        }

        fun bind(position: Int) {
            binding.setVariable(BR.position, position)
            bind(binding, position)
            binding.executePendingBindings()
        }

        fun bind(position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) {
                bind(position)
                return
            }

            binding.setVariable(BR.position, position)
            bind(binding, position, payloads)
        }
    }
}
package com.geminiauto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PromptAdapter(
    private var prompts: List<Prompt>,
    private val onDelete: (Prompt) -> Unit
) : RecyclerView.Adapter<PromptAdapter.ViewHolder>() {

    fun update(list: List<Prompt>) {
        prompts = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prompt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = prompts[position]
        holder.tvText.text = "${position + 1})\n${p.text}"
        holder.tvInterval.text = "Every ${p.interval}s"
        holder.btnDelete.setOnClickListener { onDelete(p) }
    }

    override fun getItemCount() = prompts.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvPromptText)
        val tvInterval: TextView = view.findViewById(R.id.tvPromptInterval)
        val btnDelete: Button = view.findViewById(R.id.btnDeletePrompt)
    }
}

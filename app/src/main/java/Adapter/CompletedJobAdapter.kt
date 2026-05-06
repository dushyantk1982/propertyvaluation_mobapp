package com.propval.propertyvaluation.Adapter

import Class.CompletedJobData
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.propval.propertyvaluation.CompletedJobEdit
import com.propval.propertyvaluation.R
import java.util.regex.Pattern

class CompletedJobAdapter(private var dataList: List<CompletedJobData>) : RecyclerView.Adapter<CompletedJobAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val applicationNumber: TextView = itemView.findViewById(R.id.applicationNumber)
        val name: TextView = itemView.findViewById(R.id.name)
        val bankName: TextView = itemView.findViewById(R.id.bankName)
        val city: TextView = itemView.findViewById(R.id.city)
        val editButton: ImageButton = itemView.findViewById(R.id.edit_report_button)
        val viewButton: ImageButton = itemView.findViewById(R.id.view_report_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.completed_job_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataList[position]
        holder.applicationNumber.text = data.applicationNumber
        holder.name.text = data.name
        holder.bankName.text = data.bankName
        holder.city.text = data.city

        holder.editButton.setOnClickListener {
            navigateToEdit(holder, data.url)
        }

        holder.viewButton.setOnClickListener {
            navigateToEdit(holder, data.url, true)
        }
    }

    private fun navigateToEdit(holder: ViewHolder, url: String, isView: Boolean = false) {
        val pattern = Pattern.compile(".*/(\\d+)/$")
        val matcher = pattern.matcher(url)
        if (matcher.find()) {
            val jobId = matcher.group(1)
            val context = holder.itemView.context
            val intent = Intent(context, CompletedJobEdit::class.java)
            intent.putExtra("job_id", jobId)
            if (isView) intent.putExtra("isView", "yes")
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateData(newData: List<CompletedJobData>) {
        dataList = newData
        notifyDataSetChanged()
    }
}

package Adapter

import Class.EngineerJobData
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.propval.propertyvaluation.R
import com.propval.propertyvaluation.SubmitReport
import com.propval.propertyvaluation.ViewReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

class EngineerJobAdapter(private val dataList: List<EngineerJobData>, private val isSearch: String?) : RecyclerView.Adapter<EngineerJobAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val applicationNumber: TextView = itemView.findViewById(R.id.applicationNumber)
        val name: TextView = itemView.findViewById(R.id.name)
        val id : TextView = itemView.findViewById(R.id.id)
        val bankName: TextView = itemView.findViewById(R.id.bankName)
        val city: TextView = itemView.findViewById(R.id.city)
        val address: TextView = itemView.findViewById(R.id.address)
        val casetype: TextView = itemView.findViewById(R.id.caseType)
        val partcase: TextView = itemView.findViewById(R.id.partCase)
        val status: TextView = itemView.findViewById(R.id.status)
        val priority: TextView = itemView.findViewById(R.id.priority)
        val phoneNumber: TextView = itemView.findViewById(R.id.phoneNumber)
        val appDate: TextView = itemView.findViewById(R.id.appDate)
        val addReportButton: ImageButton = itemView.findViewById(R.id.add_report_button)
        val putOnHoldButton: ImageButton = itemView.findViewById(R.id.add_hold_job)
        val putInProgressButton: ImageButton = itemView.findViewById(R.id.add_in_progress)
        val cardView: CardView = itemView.findViewById(R.id.jobCardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.engineer_job_item, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemdata = dataList[position]
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH) // adjust the format according to your input
        val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

        val applicationDateTime = inputFormat.parse(itemdata.applicationDate)
        val formattedDate = outputFormat.format(applicationDateTime)

        holder.applicationNumber.text = itemdata.applicationNumber
        holder.name.text = itemdata.name
        holder.bankName.text = itemdata.bankName
        holder.city.text = itemdata.city
        holder.address.text = itemdata.address1
        holder.casetype.text = if(itemdata.npa) "NPA" else "Normal"
        holder.partcase.text = if(itemdata.partcase) "Yes" else "No"
        holder.priority.text = if(itemdata.priority) "High" else "Normal"
        holder.status.text = if(itemdata.engineer =="Submitted") "Completed" else if(itemdata.engineer == "InProgress") "Pending" else itemdata.engineer
        holder.phoneNumber.text = itemdata.phoneNumber
        holder.appDate.text = formattedDate

        val context = holder.itemView.context
        holder.phoneNumber.setOnClickListener {
            val phoneNumber = itemdata.phoneNumber.replace(Regex("[^0-9]"), "")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)

        }


        if (itemdata.npa){
            holder.cardView.setBackgroundColor(Color.YELLOW)
        }
        if (itemdata.priority){
            holder.cardView.setBackgroundColor(Color.MAGENTA)
        }
        // Get the SharedPreferences
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedStatus = sharedPreferences.getString("jobStatus_${itemdata.engineer}", null)

        // Check if the job status is already "In Progress" in SharedPreferences
//        if (savedStatus == "InProgress") {
//            holder.putInProgressButton.visibility = View.GONE  // Hide the button if status is already "In Progress"
//        } else {
//            holder.putInProgressButton.visibility = View.VISIBLE  // Show the button otherwise
//        }

        if( isSearch == "Search"){
            Log.d("comp",itemdata.engineer)
            if (itemdata.engineer =="Completed") {
                holder.putOnHoldButton.visibility = View.GONE
                holder.addReportButton.visibility = View.GONE
            }
            holder.putInProgressButton.visibility = View.GONE

        }

// Set the icon based on the `engineer` field status
        if (itemdata.engineer == "Hold") {
            holder.putOnHoldButton.setImageResource(R.drawable.play_24px)  // Icon for "Hold" status
            holder.putOnHoldButton.apply {
                tooltipText="Click to change status to progress!"
            }
        } else {
            holder.putOnHoldButton.setImageResource(R.drawable.pause_circle_24px)  // Icon for "InProgress" or other statuses
            holder.putOnHoldButton.tooltipText="Click to change status to hold!"
        }

        holder.addReportButton.tooltipText="Click to submit job"
        holder.addReportButton.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, SubmitReport::class.java)
            intent.putExtra("engineerJobData", itemdata)
            context.startActivity(intent)
        }


        holder.putInProgressButton.setOnClickListener {
            val urlField = itemdata.url  // Assuming `data.url` contains the job URL like "http://127.0.0.1:8000/api/reception/23/"

            // Extract job ID from the URL
            val pattern = Pattern.compile(".*/(\\d+)/$")
            val matcher = pattern.matcher(urlField)

            if (matcher.find()) {
                val jobId = matcher.group(1)  // Extracted job ID from URL

                // Perform the network request directly in this scope
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        // Get the token from SharedPreferences
                        val token = sharedPreferences.getString("token", null)

                        // Create the PUT request URL using the jobId
//                        val url = URL("http://10.0.2.2:8000/api/reception/$jobId/")  // Replace with your actual API URL
                        val url = URL(Constants.getUrl("api/reception/$jobId/"))
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "PUT"
                        connection.setRequestProperty("Authorization", "token $token")
                        connection.setRequestProperty("Content-Type", "application/json")

                        // Create the JSON body to set status to "In Progress"
                        val jsonObject = JSONObject()
                        jsonObject.put("engineer", "InProgress")  // Set status to InProgress

                        // Write the JSON body to the request
                        connection.outputStream.write(jsonObject.toString().toByteArray())
                        connection.outputStream.flush()
                        connection.outputStream.close()

                        // Check if the response is successful
                        if (connection.responseCode == HttpURLConnection.HTTP_OK || connection.responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                            withContext(Dispatchers.Main) {
                                // Hide the putInProgressButton and save the status in SharedPreferences
                                holder.putInProgressButton.visibility = View.GONE
                                sharedPreferences.edit().putString("jobStatus_${itemdata.engineer}", "InProgress").apply()  // Save status
                                Toast.makeText(context, "Job marked as In Progress", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Handle failure (non-200 response)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to update job status. Response code: ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Handle the exception case
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error occurred while updating job status", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // Handle case where job ID extraction fails
                Toast.makeText(context, "Invalid job URL", Toast.LENGTH_SHORT).show()
            }
        }

        holder.putOnHoldButton.setOnClickListener {
            val context = holder.itemView.context
            val dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_layout, null)
            val input = dialogLayout.findViewById<TextInputEditText>(R.id.input_edit_text)
            val isOnHold = sharedPreferences.getString("onHold", null)
            val builder = AlertDialog.Builder(context)

            if (isOnHold=="yes"){
//                builder.setTitle("Job is already on hold")
//                   .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
//                   .show()
                val urlField =
                    itemdata.url  // Assuming `data.url` contains the job URL like "http://127.0.0.1:8000/api/reception/23/"

                // Extract job ID from the URL
                val pattern = Pattern.compile(".*/(\\d+)/$")
                val matcher = pattern.matcher(urlField)

                if (matcher.find()) {
                    val jobId = matcher.group(1)  // Extracted job ID from URL

                    // Now perform the network request directly in this scope
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            // Get the token from SharedPreferences
                            val sharedPreferences =
                                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            val token = sharedPreferences.getString("token", null)

                            // Check current "onHold" state


                            // Create the PUT request URL using the jobId
//                        val url = URL("http://10.0.2.2:8000/api/reception/$jobId/")
                            val url = URL(Constants.getUrl("api/reception/$jobId/"))
                            val connection = url.openConnection() as HttpURLConnection
                            connection.requestMethod = "PUT"
                            connection.setRequestProperty("Authorization", "token $token")
                            connection.setRequestProperty("Content-Type", "application/json")

                            // Create the JSON body dynamically based on current status
                            val jsonObject = JSONObject()

                            if (isOnHold == "yes") {
                                // If job is currently on hold, change status to "In Progress"
                                jsonObject.put("engineerholdcause", "")  // Empty hold cause
                                jsonObject.put(
                                    "engineer",
                                    "InProgress"
                                )  // Set status to InProgress
                            } else {
                                // If job is not on hold, change status to "Hold"
                                jsonObject.put(
                                    "engineerholdcause",
                                    ""
                                )  // Provide hold cause
                                jsonObject.put("engineer", "Hold")  // Set status to Hold
                            }

                            // Write the JSON body to the request
                            connection.outputStream.write(jsonObject.toString().toByteArray())
                            connection.outputStream.flush()
                            connection.outputStream.close()

                            // Check if the response is successful
                            if (connection.responseCode == HttpURLConnection.HTTP_OK || connection.responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                                withContext(Dispatchers.Main) {
                                    // Toggle the onHold state and change the icon accordingly
                                    val editor = sharedPreferences.edit()
                                    if (isOnHold == "yes") {
                                        // If job was on hold, change to InProgress
                                        holder.putOnHoldButton.setImageResource(R.drawable.pause_circle_24px)  // Revert to the original pause icon
                                        editor.putString(
                                            "onHold",
                                            "no"
                                        )  // Update flag in SharedPreferences
                                        Toast.makeText(
                                            context,
                                            "Job removed from hold and marked as In Progress",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        // If job was not on hold, put it on hold
                                        holder.putOnHoldButton.setImageResource(R.drawable.play_24px)  // Change icon to play (job is now on hold)
                                        editor.putString(
                                            "onHold",
                                            "yes"
                                        )  // Update flag in SharedPreferences
                                        Toast.makeText(
                                            context,
                                            "Job put on hold",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    editor.apply()
                                }
                            } else {
                                // Handle failure (non-200 response)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Failed to update job status. Response code: ${connection.responseCode}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Handle the exception case
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Error occurred while updating job status",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    // Handle case where job ID extraction fails
                    Toast.makeText(context, "Invalid job URL", Toast.LENGTH_SHORT).show()
                }

            }

            builder.setTitle("Enter Hold Cause")
                .setView(dialogLayout)
                .setPositiveButton("OK") { dialog, _ ->
                    val holdCause = input.text.toString() // Get the user's input
                    val urlField =
                        itemdata.url  // Assuming `data.url` contains the job URL like "http://127.0.0.1:8000/api/reception/23/"

                    // Extract job ID from the URL
                    val pattern = Pattern.compile(".*/(\\d+)/$")
                    val matcher = pattern.matcher(urlField)

                    if (matcher.find()) {
                        val jobId = matcher.group(1)  // Extracted job ID from URL

                        // Now perform the network request directly in this scope
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                // Get the token from SharedPreferences
                                val sharedPreferences =
                                    context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                val token = sharedPreferences.getString("token", null)

                                // Check current "onHold" state


                                // Create the PUT request URL using the jobId
//                        val url = URL("http://10.0.2.2:8000/api/reception/$jobId/")
                                val url = URL(Constants.getUrl("api/reception/$jobId/"))
                                val connection = url.openConnection() as HttpURLConnection
                                connection.requestMethod = "PUT"
                                connection.setRequestProperty("Authorization", "token $token")
                                connection.setRequestProperty("Content-Type", "application/json")

                                // Create the JSON body dynamically based on current status
                                val jsonObject = JSONObject()

                                if (isOnHold == "yes") {
                                    // If job is currently on hold, change status to "In Progress"
                                    jsonObject.put("engineerholdcause", "")  // Empty hold cause
                                    jsonObject.put(
                                        "engineer",
                                        "InProgress"
                                    )  // Set status to InProgress
                                } else {
                                    // If job is not on hold, change status to "Hold"
                                    jsonObject.put(
                                        "engineerholdcause",
                                        holdCause
                                    )  // Provide hold cause
                                    jsonObject.put("engineer", "Hold")  // Set status to Hold
                                }

                                // Write the JSON body to the request
                                connection.outputStream.write(jsonObject.toString().toByteArray())
                                connection.outputStream.flush()
                                connection.outputStream.close()

                                // Check if the response is successful
                                if (connection.responseCode == HttpURLConnection.HTTP_OK || connection.responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                                    withContext(Dispatchers.Main) {
                                        // Toggle the onHold state and change the icon accordingly
                                        val editor = sharedPreferences.edit()
                                        if (isOnHold == "yes") {
                                            // If job was on hold, change to InProgress
                                            holder.putOnHoldButton.setImageResource(R.drawable.pause_circle_24px)  // Revert to the original pause icon
                                            editor.putString(
                                                "onHold",
                                                "no"
                                            )  // Update flag in SharedPreferences
                                            Toast.makeText(
                                                context,
                                                "Job removed from hold and marked as In Progress",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            // If job was not on hold, put it on hold
                                            holder.putOnHoldButton.setImageResource(R.drawable.play_24px)  // Change icon to play (job is now on hold)
                                            editor.putString(
                                                "onHold",
                                                "yes"
                                            )  // Update flag in SharedPreferences
                                            Toast.makeText(
                                                context,
                                                "Job put on hold",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        editor.apply()
                                    }
                                } else {
                                    // Handle failure (non-200 response)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Failed to update job status. Response code: ${connection.responseCode}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                // Handle the exception case
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Error occurred while updating job status",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } else {
                        // Handle case where job ID extraction fails
                        Toast.makeText(context, "Invalid job URL", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                .setCancelable(false)  // Optional: Prevent dialog from being canceled by clicking outside

            // Create the dialog
            val dialog = builder.create()

            // Set up TextWatcher to enable/disable OK button
            input.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !s.isNullOrEmpty()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Show the dialog
            if (isOnHold !="yes"){
            dialog.show()
            // Initially disable the OK button
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false}
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}

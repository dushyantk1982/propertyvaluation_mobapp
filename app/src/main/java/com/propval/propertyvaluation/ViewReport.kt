package com.propval.propertyvaluation

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.propval.propertyvaluation.databinding.ActivitySubmitReportBinding
import org.json.JSONObject

class ViewReport : AppCompatActivity() {

    private lateinit var binding: ActivitySubmitReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reportData = intent.getStringExtra("reportData")
        if (reportData != null) {
            populateFields(JSONObject(reportData))
        }

        // Disable all input fields to make them read-only
        disableFields()
    }

    private fun populateFields(data: JSONObject) {
        binding.applicationNumber.setText(data.getString("applicationnumber"))
        binding.name.setText(data.getString("name"))
        binding.bankName.setText(data.getString("bankname"))
        binding.city.setText(data.getString("city"))
        binding.phoneNumber.setText(data.getString("phoneNumber"))
        binding.visitInPresence.setText(data.getString("visitinpresence"))
        binding.caseType.setText(data.getString("casetype"))
        binding.add1.setText(data.getString("add1"))
        binding.add2.setText(data.getString("add2"))
        binding.region.setText(data.getString("region"))
        binding.zip.setText(data.getString("zip"))
        binding.country.setText(data.getString("country"))
        binding.east.setText(data.getString("east"))
        binding.west.setText(data.getString("west"))
        binding.north.setText(data.getString("north"))
        binding.south.setText(data.getString("south"))
        binding.gfArea.setText(data.getString("gfarea"))
        binding.ffArea.setText(data.getString("ffarea"))
        binding.sfArea.setText(data.getString("sfarea"))
        binding.tfArea.setText(data.getString("tfarea"))
        binding.propertyAge.setText(data.getString("propertyage"))
        binding.landRate.setText(data.getString("landrate"))
        binding.occupant.setText(data.getString("Occupant"))
        binding.rented.setText(data.getString("rented"))
        binding.landmark.setText(data.getString("landmark"))
        binding.roadWidth.setText(data.getString("roadwidth"))
        binding.highTensionLine.isChecked = data.getBoolean("hightensionline")
        binding.railwayLine.isChecked = data.getBoolean("railwayline")
        binding.nala.isChecked = data.getBoolean("nala")
        binding.river.isChecked = data.getBoolean("river")
        binding.pahad.isChecked = data.getBoolean("pahad")
        binding.roadComesUnderRoadBinding.isChecked = data.getBoolean("roadcomesunderroadbinding")
        binding.propertyAccessIssue.isChecked = data.getBoolean("propertyaccessissue")
        binding.otherCheck.isChecked = data.getBoolean("othercheck")
        binding.others.setText(data.getString("others"))
        binding.remark.setText(data.getString("remark"))
        binding.reporter.setText(data.getString("reporter"))
        binding.priority.isChecked = data.getBoolean("priority")
    }

    private fun disableFields() {
        binding.applicationNumber.isEnabled = false
        binding.name.isEnabled = false
        binding.bankName.isEnabled = false
        binding.city.isEnabled = false
        binding.phoneNumber.isEnabled = false
        binding.visitInPresence.isEnabled = false
        binding.caseType.isEnabled = false
        binding.add1.isEnabled = false
        binding.add2.isEnabled = false
        binding.region.isEnabled = false
        binding.zip.isEnabled = false
        binding.country.isEnabled = false
        binding.east.isEnabled = false
        binding.west.isEnabled = false
        binding.north.isEnabled = false
        binding.south.isEnabled = false
        binding.gfArea.isEnabled = false
        binding.ffArea.isEnabled = false
        binding.sfArea.isEnabled = false
        binding.tfArea.isEnabled = false
        binding.propertyAge.isEnabled = false
        binding.landRate.isEnabled = false
        binding.occupant.isEnabled = false
        binding.rented.isEnabled = false
        binding.landmark.isEnabled = false
        binding.roadWidth.isEnabled = false
        binding.highTensionLine.isEnabled = false
        binding.railwayLine.isEnabled = false
        binding.nala.isEnabled = false
        binding.river.isEnabled = false
        binding.pahad.isEnabled = false
        binding.roadComesUnderRoadBinding.isEnabled = false
        binding.propertyAccessIssue.isEnabled = false
        binding.otherCheck.isEnabled = false
        binding.others.isEnabled = false
        binding.remark.isEnabled = false
        binding.reporter.isEnabled = false
        binding.priority.isEnabled = false
        binding.submitReportButton.visibility = View.GONE
    }
}

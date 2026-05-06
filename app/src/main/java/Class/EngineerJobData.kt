package Class
import java.io.Serializable

data class EngineerJobData(
    val url: String,
    val applicationDate: String,
    val applicationNumber: String,
    val name: String,
    val bankName: String,
    val bankId:String,
    val bankVertical: String,
    val address1: String,
    val address2: String,
    val city: String,
    val region: String,
    val zip: String,
    val country: String,
    val phoneNumber: String,
    val visitingPerson: Int,
    val reportPerson: Int,
    val visitingPersonName: String,
    val reportPersonName: String,
    val engineer: String,
    val reporter: String?,
    val priority: Boolean,
    val dateCreated: String,
    val updatedAt: String,
    val userId:String,
    val npa:Boolean,
    var partcase:Boolean
) : Serializable

data class CompletedJobData(
    val applicationNumber: String,
    val name: String,
    val bankName: String,
    val city: String,
    val url : String

)
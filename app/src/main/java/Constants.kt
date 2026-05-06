class Constants private constructor() {
    companion object {
//        const val BASE_URL = "http://193.203.163.121/"
        const val BASE_URL = "http://193.203.163.121/"

        // Helper function to construct URLs with the base URL
        fun getUrl(endpoint: String): String {
            return "$BASE_URL$endpoint"
        }
    }
}

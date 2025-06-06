enum class Property(val key: String) {
    CentralPortalToken("CENTRALPORTALTOKEN"),
    GpgKey("GPG_KEY"),
    GpgKeyId("GPG_KEY_ID"),
    GpgPassword("GPG_PASSWORD");

    companion object {
        fun get(property: Property): String? =
            System.getProperty(property.key) ?: System.getenv(property.key)
    }
}
package tw.idv.niugnep.pwamaker.model

import java.util.UUID

enum class UaMode {
    BASIC_MOBILE,
    BASIC_DESKTOP,
    ADVANCED
}

data class PwaConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val iconUri: String? = null,
    val uaMode: UaMode = UaMode.BASIC_MOBILE,
    val customUa: String = ""
) {
    companion object {
        const val DEFAULT_MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"
        const val DEFAULT_DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
    }

    fun getEffectiveUserAgent(): String {
        return when (uaMode) {
            UaMode.BASIC_MOBILE -> DEFAULT_MOBILE_UA
            UaMode.BASIC_DESKTOP -> DEFAULT_DESKTOP_UA
            UaMode.ADVANCED -> customUa.ifBlank { DEFAULT_MOBILE_UA }
        }
    }

    fun getFormattedUrl(): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return "https://"
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }
}

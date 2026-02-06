package com.v7lthronyx.scamynx.data.qrcode

import com.v7lthronyx.scamynx.domain.model.QRCodeContent
import com.v7lthronyx.scamynx.domain.model.QRCodeType
import com.v7lthronyx.scamynx.domain.model.WifiSecurityType
import java.net.URI
import java.net.URLDecoder
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for QR code content that identifies the content type and extracts structured data.
 * Supports various QR code formats including URLs, WiFi credentials, contacts (vCard/MeCard),
 * phone numbers, emails, SMS, geographic locations, calendar events, crypto payments, and more.
 */
@Singleton
class QRCodeParser @Inject constructor() {

    companion object {
        // URL patterns
        private val URL_PATTERN = Pattern.compile(
            "^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?$",
            Pattern.CASE_INSENSITIVE
        )
        private val URL_EXTRACTION_PATTERN = Pattern.compile(
            "https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?",
            Pattern.CASE_INSENSITIVE
        )

        // WiFi pattern: WIFI:T:WPA;S:network_name;P:password;;
        private val WIFI_PATTERN = Pattern.compile(
            "^WIFI:([^;]*;)*;?\$",
            Pattern.CASE_INSENSITIVE
        )

        // Email patterns
        private val EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        )
        private val MAILTO_PATTERN = Pattern.compile(
            "^mailto:([^?]+)(\\?(.*))?$",
            Pattern.CASE_INSENSITIVE
        )

        // Phone patterns
        private val PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9\\s\\-().]{7,20}$"
        )
        private val TEL_PATTERN = Pattern.compile(
            "^tel:(.+)$",
            Pattern.CASE_INSENSITIVE
        )

        // SMS pattern
        private val SMS_PATTERN = Pattern.compile(
            "^(sms|smsto):([^?]+)(\\?body=(.*))?$",
            Pattern.CASE_INSENSITIVE
        )

        // Geo pattern
        private val GEO_PATTERN = Pattern.compile(
            "^geo:([\\-0-9.]+),([\\-0-9.]+)(,([\\-0-9.]+))?(\\?(.*))?$",
            Pattern.CASE_INSENSITIVE
        )

        // vCard pattern
        private val VCARD_PATTERN = Pattern.compile(
            "^BEGIN:VCARD",
            Pattern.CASE_INSENSITIVE
        )

        // MeCard pattern
        private val MECARD_PATTERN = Pattern.compile(
            "^MECARD:",
            Pattern.CASE_INSENSITIVE
        )

        // Calendar event pattern (iCal)
        private val VEVENT_PATTERN = Pattern.compile(
            "^BEGIN:VEVENT",
            Pattern.CASE_INSENSITIVE
        )

        // Crypto patterns
        private val BITCOIN_PATTERN = Pattern.compile(
            "^bitcoin:([a-zA-Z0-9]+)(\\?(.*))?$",
            Pattern.CASE_INSENSITIVE
        )
        private val ETHEREUM_PATTERN = Pattern.compile(
            "^ethereum:([a-zA-Z0-9]+)(\\?(.*))?$",
            Pattern.CASE_INSENSITIVE
        )

        // UPI pattern (India)
        private val UPI_PATTERN = Pattern.compile(
            "^upi://pay\\?(.+)$",
            Pattern.CASE_INSENSITIVE
        )

        // App link patterns
        private val APP_LINK_SCHEMES = setOf(
            "fb", "twitter", "instagram", "whatsapp", "telegram",
            "snapchat", "tiktok", "youtube", "spotify", "slack",
            "zoom", "teams", "discord", "reddit", "linkedin"
        )
    }

    /**
     * Parses raw QR code content and returns the detected type and parsed content.
     */
    fun parse(rawContent: String): Pair<QRCodeType, QRCodeContent> {
        val trimmed = rawContent.trim()

        return when {
            // WiFi credentials
            WIFI_PATTERN.matcher(trimmed).matches() ||
                trimmed.startsWith("WIFI:", ignoreCase = true) -> parseWifi(trimmed)

            // vCard contact
            VCARD_PATTERN.matcher(trimmed).find() -> parseVCard(trimmed)

            // MeCard contact
            MECARD_PATTERN.matcher(trimmed).find() -> parseMeCard(trimmed)

            // Calendar event
            VEVENT_PATTERN.matcher(trimmed).find() -> parseCalendarEvent(trimmed)

            // Email (mailto: or plain email)
            MAILTO_PATTERN.matcher(trimmed).matches() -> parseMailto(trimmed)
            EMAIL_PATTERN.matcher(trimmed).matches() -> {
                QRCodeType.EMAIL to QRCodeContent.Email(address = trimmed)
            }

            // Phone (tel: or plain number)
            TEL_PATTERN.matcher(trimmed).matches() -> parseTel(trimmed)
            PHONE_PATTERN.matcher(trimmed).matches() -> {
                QRCodeType.PHONE to QRCodeContent.Phone(number = trimmed)
            }

            // SMS
            SMS_PATTERN.matcher(trimmed).matches() -> parseSms(trimmed)

            // Geographic location
            GEO_PATTERN.matcher(trimmed).matches() -> parseGeo(trimmed)

            // Cryptocurrency payment
            BITCOIN_PATTERN.matcher(trimmed).matches() -> parseBitcoin(trimmed)
            ETHEREUM_PATTERN.matcher(trimmed).matches() -> parseEthereum(trimmed)

            // UPI payment
            UPI_PATTERN.matcher(trimmed).matches() -> parseUpi(trimmed)

            // URL
            URL_PATTERN.matcher(trimmed).matches() -> parseUrl(trimmed)

            // App link (custom schemes)
            isAppLink(trimmed) -> parseAppLink(trimmed)

            // Check for URLs embedded in text
            containsUrl(trimmed) -> parseTextWithUrls(trimmed)

            // Default: plain text
            else -> QRCodeType.TEXT to QRCodeContent.Text(text = trimmed)
        }
    }

    private fun parseWifi(content: String): Pair<QRCodeType, QRCodeContent> {
        val params = parseWifiParams(content)

        val ssid = params["S"] ?: params["s"] ?: ""
        val password = params["P"] ?: params["p"]
        val securityType = when ((params["T"] ?: params["t"])?.uppercase()) {
            "WPA", "WPA2", "WPA3" -> WifiSecurityType.WPA
            "WEP" -> WifiSecurityType.WEP
            "NOPASS", "" -> WifiSecurityType.OPEN
            else -> WifiSecurityType.WPA
        }
        val isHidden = (params["H"] ?: params["h"])?.equals("true", ignoreCase = true) ?: false

        return QRCodeType.WIFI to QRCodeContent.Wifi(
            ssid = ssid,
            password = password,
            securityType = securityType,
            isHidden = isHidden,
        )
    }

    private fun parseWifiParams(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val data = content.removePrefix("WIFI:").removeSuffix(";")

        var i = 0
        while (i < data.length) {
            val keyEnd = data.indexOf(':', i)
            if (keyEnd == -1) break

            val key = data.substring(i, keyEnd)
            i = keyEnd + 1

            val valueBuilder = StringBuilder()
            while (i < data.length && data[i] != ';') {
                if (data[i] == '\\' && i + 1 < data.length) {
                    valueBuilder.append(data[i + 1])
                    i += 2
                } else {
                    valueBuilder.append(data[i])
                    i++
                }
            }
            result[key] = valueBuilder.toString()
            i++ // Skip semicolon
        }

        return result
    }

    private fun parseVCard(content: String): Pair<QRCodeType, QRCodeContent> {
        val lines = content.lines()
        var name: String? = null
        var phone: String? = null
        var email: String? = null
        var org: String? = null
        var address: String? = null
        var website: String? = null

        for (line in lines) {
            when {
                line.startsWith("FN:", ignoreCase = true) -> {
                    name = line.substringAfter(":").trim()
                }

                line.startsWith("N:", ignoreCase = true) && name == null -> {
                    val parts = line.substringAfter(":").split(";")
                    name = "${parts.getOrNull(1)?.trim() ?: ""} ${parts.getOrNull(0)?.trim() ?: ""}".trim()
                }

                line.startsWith("TEL", ignoreCase = true) -> {
                    phone = line.substringAfter(":").trim()
                }

                line.startsWith("EMAIL", ignoreCase = true) -> {
                    email = line.substringAfter(":").trim()
                }

                line.startsWith("ORG:", ignoreCase = true) -> {
                    org = line.substringAfter(":").trim()
                }

                line.startsWith("ADR", ignoreCase = true) -> {
                    address = line.substringAfter(":").replace(";", " ").trim()
                }

                line.startsWith("URL:", ignoreCase = true) -> {
                    website = line.substringAfter(":").trim()
                }
            }
        }

        return QRCodeType.VCARD to QRCodeContent.Contact(
            name = name,
            phone = phone,
            email = email,
            organization = org,
            address = address,
            website = website,
        )
    }

    private fun parseMeCard(content: String): Pair<QRCodeType, QRCodeContent> {
        val params = parseMeCardParams(content)

        return QRCodeType.MECARD to QRCodeContent.Contact(
            name = params["N"],
            phone = params["TEL"],
            email = params["EMAIL"],
            organization = params["ORG"],
            address = params["ADR"],
            website = params["URL"],
        )
    }

    private fun parseMeCardParams(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val data = content.removePrefix("MECARD:").removeSuffix(";")

        for (part in data.split(";")) {
            val colonIndex = part.indexOf(':')
            if (colonIndex > 0) {
                val key = part.substring(0, colonIndex).uppercase()
                val value = part.substring(colonIndex + 1)
                result[key] = value
            }
        }

        return result
    }

    private fun parseCalendarEvent(content: String): Pair<QRCodeType, QRCodeContent> {
        val lines = content.lines()
        var title = ""
        var startDate: String? = null
        var endDate: String? = null
        var location: String? = null
        var description: String? = null

        for (line in lines) {
            when {
                line.startsWith("SUMMARY:", ignoreCase = true) -> {
                    title = line.substringAfter(":").trim()
                }

                line.startsWith("DTSTART", ignoreCase = true) -> {
                    startDate = line.substringAfter(":").trim()
                }

                line.startsWith("DTEND", ignoreCase = true) -> {
                    endDate = line.substringAfter(":").trim()
                }

                line.startsWith("LOCATION:", ignoreCase = true) -> {
                    location = line.substringAfter(":").trim()
                }

                line.startsWith("DESCRIPTION:", ignoreCase = true) -> {
                    description = line.substringAfter(":").trim()
                }
            }
        }

        return QRCodeType.CALENDAR to QRCodeContent.CalendarEvent(
            title = title,
            startDate = startDate,
            endDate = endDate,
            location = location,
            description = description,
        )
    }

    private fun parseMailto(content: String): Pair<QRCodeType, QRCodeContent> {
        val matcher = MAILTO_PATTERN.matcher(content)
        if (!matcher.matches()) {
            return QRCodeType.EMAIL to QRCodeContent.Email(address = content)
        }

        val address = matcher.group(1) ?: ""
        val queryString = matcher.group(3)

        var subject: String? = null
        var body: String? = null

        queryString?.split("&")?.forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                when (parts[0].lowercase()) {
                    "subject" -> subject = URLDecoder.decode(parts[1], "UTF-8")
                    "body" -> body = URLDecoder.decode(parts[1], "UTF-8")
                }
            }
        }

        return QRCodeType.EMAIL to QRCodeContent.Email(
            address = address,
            subject = subject,
            body = body,
        )
    }

    private fun parseTel(content: String): Pair<QRCodeType, QRCodeContent> {
        val number = content.removePrefix("tel:").removePrefix("TEL:")
        return QRCodeType.PHONE to QRCodeContent.Phone(number = number)
    }

    private fun parseSms(content: String): Pair<QRCodeType, QRCodeContent> {
        val matcher = SMS_PATTERN.matcher(content)
        if (!matcher.matches()) {
            return QRCodeType.SMS to QRCodeContent.Sms(number = content)
        }

        val number = matcher.group(2) ?: ""
        val message = matcher.group(4)?.let { URLDecoder.decode(it, "UTF-8") }

        return QRCodeType.SMS to QRCodeContent.Sms(
            number = number,
            message = message,
        )
    }

    private fun parseGeo(content: String): Pair<QRCodeType, QRCodeContent> {
        val matcher = GEO_PATTERN.matcher(content)
        if (!matcher.matches()) {
            return QRCodeType.GEO to QRCodeContent.GeoLocation(
                latitude = 0.0,
                longitude = 0.0,
            )
        }

        val latitude = matcher.group(1)?.toDoubleOrNull() ?: 0.0
        val longitude = matcher.group(2)?.toDoubleOrNull() ?: 0.0
        val altitude = matcher.group(4)?.toDoubleOrNull()
        val queryString = matcher.group(6)
        var label: String? = null

        queryString?.split("&")?.forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2 && parts[0].equals("q", ignoreCase = true)) {
                label = URLDecoder.decode(parts[1], "UTF-8")
            }
        }

        return QRCodeType.GEO to QRCodeContent.GeoLocation(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            label = label,
        )
    }

    private fun parseBitcoin(content: String): Pair<QRCodeType, QRCodeContent> {
        val matcher = BITCOIN_PATTERN.matcher(content)
        if (!matcher.matches()) {
            return QRCodeType.CRYPTO to QRCodeContent.CryptoPayment(
                currency = "BTC",
                address = content,
            )
        }

        val address = matcher.group(1) ?: ""
        val queryString = matcher.group(3)

        var amount: String? = null
        var label: String? = null
        var message: String? = null

        queryString?.split("&")?.forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                when (parts[0].lowercase()) {
                    "amount" -> amount = parts[1]
                    "label" -> label = URLDecoder.decode(parts[1], "UTF-8")
                    "message" -> message = URLDecoder.decode(parts[1], "UTF-8")
                }
            }
        }

        return QRCodeType.CRYPTO to QRCodeContent.CryptoPayment(
            currency = "BTC",
            address = address,
            amount = amount,
            label = label,
            message = message,
        )
    }

    private fun parseEthereum(content: String): Pair<QRCodeType, QRCodeContent> {
        val matcher = ETHEREUM_PATTERN.matcher(content)
        if (!matcher.matches()) {
            return QRCodeType.CRYPTO to QRCodeContent.CryptoPayment(
                currency = "ETH",
                address = content,
            )
        }

        val address = matcher.group(1) ?: ""
        val queryString = matcher.group(3)

        var amount: String? = null
        var label: String? = null

        queryString?.split("&")?.forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                when (parts[0].lowercase()) {
                    "value" -> amount = parts[1]
                    "label" -> label = URLDecoder.decode(parts[1], "UTF-8")
                }
            }
        }

        return QRCodeType.CRYPTO to QRCodeContent.CryptoPayment(
            currency = "ETH",
            address = address,
            amount = amount,
            label = label,
        )
    }

    private fun parseUpi(content: String): Pair<QRCodeType, QRCodeContent> {
        val matcher = UPI_PATTERN.matcher(content)
        if (!matcher.matches()) {
            return QRCodeType.UPI to QRCodeContent.UpiPayment(payeeAddress = content)
        }

        val queryString = matcher.group(1) ?: ""
        val params = mutableMapOf<String, String>()

        queryString.split("&").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                params[parts[0].lowercase()] = URLDecoder.decode(parts[1], "UTF-8")
            }
        }

        return QRCodeType.UPI to QRCodeContent.UpiPayment(
            payeeAddress = params["pa"] ?: "",
            payeeName = params["pn"],
            amount = params["am"],
            transactionNote = params["tn"],
            merchantCode = params["mc"],
        )
    }

    private fun parseUrl(content: String): Pair<QRCodeType, QRCodeContent> {
        try {
            val uri = URI(content)
            val isHttps = uri.scheme.equals("https", ignoreCase = true)

            return QRCodeType.URL to QRCodeContent.Url(
                url = content,
                domain = uri.host ?: "",
                isHttps = isHttps,
            )
        } catch (e: Exception) {
            return QRCodeType.URL to QRCodeContent.Url(
                url = content,
                domain = extractDomain(content),
                isHttps = content.startsWith("https", ignoreCase = true),
            )
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val withoutProtocol = url
                .removePrefix("https://")
                .removePrefix("http://")
            withoutProtocol.substringBefore("/").substringBefore("?")
        } catch (e: Exception) {
            ""
        }
    }

    private fun isAppLink(content: String): Boolean {
        val schemeEnd = content.indexOf("://")
        if (schemeEnd <= 0) return false

        val scheme = content.substring(0, schemeEnd).lowercase()
        return APP_LINK_SCHEMES.contains(scheme)
    }

    private fun parseAppLink(content: String): Pair<QRCodeType, QRCodeContent> {
        val schemeEnd = content.indexOf("://")
        if (schemeEnd <= 0) {
            return QRCodeType.APP_LINK to QRCodeContent.AppLink(
                scheme = "unknown",
                host = null,
                path = null,
            )
        }

        val scheme = content.substring(0, schemeEnd)
        val rest = content.substring(schemeEnd + 3)

        val pathStart = rest.indexOf("/")
        val host: String?
        val path: String?

        if (pathStart >= 0) {
            host = rest.substring(0, pathStart)
            path = rest.substring(pathStart)
        } else {
            host = rest
            path = null
        }

        return QRCodeType.APP_LINK to QRCodeContent.AppLink(
            scheme = scheme,
            host = host.takeIf { it.isNotEmpty() },
            path = path,
        )
    }

    private fun containsUrl(content: String): Boolean {
        return URL_EXTRACTION_PATTERN.matcher(content).find()
    }

    private fun parseTextWithUrls(content: String): Pair<QRCodeType, QRCodeContent> {
        val matcher = URL_EXTRACTION_PATTERN.matcher(content)
        val urls = mutableListOf<String>()

        while (matcher.find()) {
            urls.add(matcher.group())
        }

        return QRCodeType.TEXT to QRCodeContent.Text(
            text = content,
            containsUrls = urls.isNotEmpty(),
            extractedUrls = urls,
        )
    }
}

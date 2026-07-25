package com.example.ipcamviewer.discovery

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Minimal ONVIF SOAP client: enough to fetch a camera's RTSP stream URI
 * (GetCapabilities -> GetProfiles -> GetStreamUri) using WS-Security
 * UsernameToken digest authentication. Response parsing is intentionally
 * lightweight (regex-based) since ONVIF responses vary a lot between
 * vendors and a full namespace-aware DOM parser adds little robustness
 * in practice for this narrow use case.
 */
class OnvifClient(
    private val username: String = "",
    private val password: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val soapMediaType = "application/soap+xml; charset=utf-8".toMediaType()

    private fun wsSecurityHeader(): String {
        if (username.isBlank()) return ""
        val nonceBytes = ByteArray(16).also { Random.nextBytes(it) }
        val nonceB64 = Base64.getEncoder().encodeToString(nonceBytes)
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val created = sdf.format(Date())

        val digest = MessageDigest.getInstance("SHA-1").digest(
            nonceBytes + created.toByteArray(Charsets.UTF_8) + password.toByteArray(Charsets.UTF_8)
        )
        val digestB64 = Base64.getEncoder().encodeToString(digest)

        return """
            <e:Header>
                <Security xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" e:mustUnderstand="1">
                    <UsernameToken>
                        <Username>$username</Username>
                        <Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">$digestB64</Password>
                        <Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">$nonceB64</Nonce>
                        <Created xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">$created</Created>
                    </UsernameToken>
                </Security>
            </e:Header>
        """.trimIndent()
    }

    private fun envelope(body: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <e:Envelope xmlns:e="http://www.w3.org/2003/05/soap-envelope">
            ${wsSecurityHeader()}
            <e:Body>
                $body
            </e:Body>
        </e:Envelope>
    """.trimIndent()

    private fun post(url: String, soapBody: String): String? {
        val body = envelope(soapBody).toRequestBody(soapMediaType)
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    /** Returns the media service XAddr, falling back to the device XAddr. */
    fun getMediaXAddr(deviceXAddr: String): String {
        val body = "<GetCapabilities xmlns=\"http://www.onvif.org/ver10/device/wsdl\">" +
            "<Category>Media</Category></GetCapabilities>"
        val resp = post(deviceXAddr, body) ?: return deviceXAddr
        return extractMediaXAddr(resp) ?: deviceXAddr
    }

    /** Returns the first available media profile token, or null. */
    fun getFirstProfileToken(mediaXAddr: String): String? {
        val body = "<GetProfiles xmlns=\"http://www.onvif.org/ver10/media/wsdl\"/>"
        val resp = post(mediaXAddr, body) ?: return null
        return Regex("token=\"([^\"]+)\"").find(resp)?.groupValues?.get(1)
    }

    /** Returns the RTSP stream URI for a given profile token. */
    fun getStreamUri(mediaXAddr: String, profileToken: String): String? {
        val body = """
            <GetStreamUri xmlns="http://www.onvif.org/ver10/media/wsdl">
                <StreamSetup>
                    <Stream xmlns="http://www.onvif.org/ver10/schema">RTP-Unicast</Stream>
                    <Transport xmlns="http://www.onvif.org/ver10/schema"><Protocol>RTSP</Protocol></Transport>
                </StreamSetup>
                <ProfileToken>$profileToken</ProfileToken>
            </GetStreamUri>
        """.trimIndent()
        val resp = post(mediaXAddr, body) ?: return null
        return Regex("<[^>]*Uri>([^<]+)</[^>]*Uri>").find(resp)?.groupValues?.get(1)
    }

    /** Full flow: device XAddr -> stream URI (with credentials embedded). */
    fun resolveStreamUri(deviceXAddr: String): String? {
        val mediaXAddr = getMediaXAddr(deviceXAddr)
        val token = getFirstProfileToken(mediaXAddr) ?: return null
        val rawUri = getStreamUri(mediaXAddr, token) ?: return null
        return injectCredentials(rawUri)
    }

    private fun injectCredentials(uri: String): String {
        if (username.isBlank() || !uri.startsWith("rtsp://") || uri.contains("@")) return uri
        return uri.replaceFirst("rtsp://", "rtsp://$username:$password@")
    }

    private fun extractMediaXAddr(xml: String): String? {
        val mediaBlock = Regex("Media[\\s\\S]*?</\\S*Media\\S*>", RegexOption.IGNORE_CASE)
            .find(xml)?.value ?: xml
        return Regex("<[^>]*XAddr>([^<]+)</[^>]*XAddr>").find(mediaBlock)?.groupValues?.get(1)
    }
}

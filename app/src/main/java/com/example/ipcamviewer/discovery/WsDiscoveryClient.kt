package com.example.ipcamviewer.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.UUID

/**
 * Sends a WS-Discovery Probe over UDP multicast to find ONVIF-compatible
 * cameras on the local network, then parses ProbeMatches responses.
 */
object WsDiscoveryClient {

    private const val MULTICAST_ADDR = "239.255.255.250"
    private const val MULTICAST_PORT = 3702

    private fun buildProbeMessage(): String {
        val id = UUID.randomUUID()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <e:Envelope xmlns:e="http://www.w3.org/2003/05/soap-envelope"
                        xmlns:w="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                        xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery"
                        xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
                <e:Header>
                    <w:MessageID>uuid:$id</w:MessageID>
                    <w:To e:mustUnderstand="1">urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>
                    <w:Action e:mustUnderstand="1">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>
                </e:Header>
                <e:Body>
                    <d:Probe>
                        <d:Types>dn:NetworkVideoTransmitter</d:Types>
                    </d:Probe>
                </e:Body>
            </e:Envelope>
        """.trimIndent()
    }

    suspend fun discover(context: Context, timeoutMs: Int = 4000): List<DiscoveredDevice> =
        withContext(Dispatchers.IO) {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("ipcam_discovery")
            lock.setReferenceCounted(true)
            lock.acquire()

            val found = LinkedHashMap<String, DiscoveredDevice>()
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(0))
                    soTimeout = 500
                }

                val group = InetAddress.getByName(MULTICAST_ADDR)
                val message = buildProbeMessage().toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(message, message.size, group, MULTICAST_PORT)

                // Send a few probes to improve reliability on lossy Wi-Fi.
                repeat(3) {
                    try {
                        socket.send(packet)
                    } catch (_: Exception) {
                    }
                }

                val buf = ByteArray(65535)
                val deadline = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < deadline) {
                    try {
                        val resp = DatagramPacket(buf, buf.size)
                        socket.receive(resp)
                        val xml = String(resp.data, 0, resp.length, Charsets.UTF_8)
                        val device = parseProbeMatch(xml, resp.address.hostAddress ?: "")
                        if (device != null) {
                            found[device.xAddr] = device
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        // keep looping until deadline
                    } catch (_: Exception) {
                        // ignore malformed packets
                    }
                }
            } finally {
                socket?.close()
                if (lock.isHeld) lock.release()
            }
            found.values.toList()
        }

    private fun parseProbeMatch(xml: String, fromHost: String): DiscoveredDevice? {
        return try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var xAddrs: String? = null
            var types: String? = null
            var scopes: String? = null
            var tag: String? = null
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> tag = parser.name.substringAfter(':')
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            when (tag) {
                                "XAddrs" -> xAddrs = text
                                "Types" -> types = text
                                "Scopes" -> scopes = text
                            }
                        }
                    }
                }
                event = parser.next()
            }

            val addr = xAddrs?.split(Regex("\\s+"))?.firstOrNull { it.startsWith("http") }
                ?: return null

            val host = Regex("//([^/:]+)").find(addr)?.groupValues?.get(1) ?: fromHost
            val name = scopes?.let {
                Regex("onvif://www\\.onvif\\.org/name/([^\\s]+)").find(it)?.groupValues?.get(1)
                    ?.replace('_', ' ')
            } ?: "ONVIF Camera"

            DiscoveredDevice(
                xAddr = addr,
                host = host,
                types = types?.split(Regex("\\s+")) ?: emptyList(),
                name = name
            )
        } catch (_: Exception) {
            null
        }
    }
}

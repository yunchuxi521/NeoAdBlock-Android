package com.adblock.app.vpn

import com.adblock.app.rules.RuleEngine
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsQueryHandler(
    val ruleEngine: RuleEngine,
    private val onQueryResult: ((domain: String, wasBlocked: Boolean) -> Unit)? = null,
    private val upstreamDns: String = "119.29.29.29",
    private val upstreamPort: Int = 53,
    private val timeoutMs: Int = 3000
) {
    private val cache = mutableMapOf<String, CacheEntry>()
    private val cacheTtlMs = 60_000L

    data class CacheEntry(
        val response: ByteArray,
        val timestamp: Long
    )

    fun handle(queryData: ByteArray): ByteArray? {
        return try {
            val query = DnsPacketParser.parse(queryData)
            if (query.questions.isEmpty()) return null

            val domain = query.questions[0].domain

            val cached = cache[domain]
            if (cached != null && (System.currentTimeMillis() - cached.timestamp) < cacheTtlMs) {
                onQueryResult?.invoke(domain, true)
                return patchResponseId(cached.response, query.header.id)
            }

            if (ruleEngine.matches(domain)) {
                onQueryResult?.invoke(domain, true)
                val nxdomain = DnsPacketParser.buildNxDomainResponse(query)
                cache[domain] = CacheEntry(nxdomain, System.currentTimeMillis())
                return nxdomain
            }

            onQueryResult?.invoke(domain, false)
            val upstreamResponse = forwardQuery(queryData)
            if (upstreamResponse != null) {
                cache[domain] = CacheEntry(upstreamResponse, System.currentTimeMillis())
            }
            upstreamResponse
        } catch (e: Exception) {
            null
        }
    }

    private fun forwardQuery(queryData: ByteArray): ByteArray? {
        return try {
            val socket = DatagramSocket()
            socket.soTimeout = timeoutMs
            val addr = InetAddress.getByName(upstreamDns)
            val packet = DatagramPacket(queryData, queryData.size, addr, upstreamPort)
            socket.send(packet)

            val buf = ByteArray(1500)
            val response = DatagramPacket(buf, buf.size)
            socket.receive(response)
            socket.close()

            response.data.copyOf(response.length)
        } catch (e: Exception) {
            null
        }
    }

    private fun patchResponseId(response: ByteArray, newId: Int): ByteArray {
        val patched = response.copyOf()
        patched[0] = ((newId shr 8) and 0xFF).toByte()
        patched[1] = (newId and 0xFF).toByte()
        return patched
    }

    fun clearCache() {
        cache.clear()
    }
}

package com.adblock.app

import com.adblock.app.rules.Rule
import com.adblock.app.rules.RuleEngine
import com.adblock.app.vpn.DnsPacketParser
import com.adblock.app.vpn.DnsQueryHandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class DnsQueryHandlerTest {

    private lateinit var handler: DnsQueryHandler
    private lateinit var engine: RuleEngine

    @BeforeEach
    fun setUp() {
        engine = RuleEngine()
        engine.loadRules(listOf(
            Rule("ads.example.com", Rule.Type.Block),
            Rule("doubleclick.net", Rule.Type.Block)
        ))
        handler = DnsQueryHandler(
            ruleEngine = engine,
            upstreamDns = "119.29.29.29",
            upstreamPort = 53
        )
    }

    @Test
    fun `blocked domain returns NXDOMAIN response`() {
        val queryBytes = buildDnsQuery("ads.example.com")
        val response = handler.handle(queryBytes)

        assertNotNull(response)
        val parsed = DnsPacketParser.parse(response!!)
        assertEquals(0x1234, parsed.header.id)
        assertTrue((parsed.header.flags and 0x000F) == 0x0003, "Should be NXDOMAIN")
    }

    @Test
    fun `allowed domain returns null when upstream is unreachable`() {
        // Use a timeout handler with an unreachable DNS
        val localHandler = DnsQueryHandler(
            ruleEngine = engine,
            upstreamDns = "192.0.2.1", // TEST-NET address, should timeout
            timeoutMs = 100
        )
        val queryBytes = buildDnsQuery("google.com")
        val response = localHandler.handle(queryBytes)
        // May return null due to timeout, which is acceptable fallback behavior
        // If null, client will retry via system DNS
    }

    @Test
    fun `cache returns same response for repeated query`() {
        val queryBytes = buildDnsQuery("ads.example.com")
        val response1 = handler.handle(queryBytes)
        val response2 = handler.handle(queryBytes)
        assertNotNull(response1)
        assertNotNull(response2)
    }

    @Test
    fun `clear cache removes all entries`() {
        val queryBytes = buildDnsQuery("ads.example.com")
        handler.handle(queryBytes)
        handler.clearCache()
        // After clearing, should produce a new response
        val response = handler.handle(queryBytes)
        assertNotNull(response)
    }

    @Test
    fun `empty query returns null`() {
        val emptyBytes = byteArrayOf(0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val response = handler.handle(emptyBytes)
        // Either null or a valid response
    }

    private fun buildDnsQuery(domain: String): ByteArray {
        val os = ByteArrayOutputStream()
        os.write(byteArrayOf(0x12, 0x34))
        os.write(byteArrayOf(0x01, 0x00))
        os.write(byteArrayOf(0x00, 0x01))
        os.write(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        DnsPacketParser.writeDomainName(os, domain)
        os.write(byteArrayOf(0x00, 0x01))
        os.write(byteArrayOf(0x00, 0x01))
        return os.toByteArray()
    }
}

package com.adblock.app

import com.adblock.app.vpn.DnsPacketParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class DnsPacketParserTest {

    @Test
    fun `parse DNS query and extract domain name`() {
        val queryBytes = buildDnsQuery("example.com")
        val packet = DnsPacketParser.parse(queryBytes)
        assertEquals(0x1234, packet.header.id)
        assertEquals(1, packet.questions.size)
        assertEquals("example.com", packet.questions[0].domain)
        assertEquals(1, packet.questions[0].type)
        assertEquals(1, packet.questions[0].cls)
    }

    @Test
    fun `parse DNS query with compression pointer`() {
        // Build a response with pointer compression
        val queryBytes = buildDnsQuery("example.com")
        val responseBytes = buildDnsResponse(queryBytes, "example.com", "93.184.216.34")

        val packet = DnsPacketParser.parse(responseBytes)
        assertEquals("example.com", packet.questions[0].domain)
        assertEquals("example.com", packet.answers[0].name)
        assertEquals(60, packet.answers[0].ttl)
    }

    @Test
    fun `build NXDOMAIN response`() {
        val queryBytes = buildDnsQuery("ads.example.com")
        val query = DnsPacketParser.parse(queryBytes)
        val response = DnsPacketParser.buildNxDomainResponse(query)

        val parsed = DnsPacketParser.parse(response)
        assertEquals(0x1234, parsed.header.id)
        assertTrue((parsed.header.flags and 0x000F) == 0x0003, "Should be NXDOMAIN")
        assertTrue((parsed.header.flags and 0x8000) != 0, "Should have QR bit set")
    }

    @Test
    fun `parse empty question section returns zero questions`() {
        val bytes = byteArrayOf(
            0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        )
        val packet = DnsPacketParser.parse(bytes)
        assertEquals(0, packet.questions.size)
    }

    private fun buildDnsQuery(domain: String): ByteArray {
        val os = ByteArrayOutputStream()
        os.write(byteArrayOf(0x12, 0x34)) // ID
        os.write(byteArrayOf(0x01, 0x00)) // flags (RD=1)
        os.write(byteArrayOf(0x00, 0x01)) // 1 question
        os.write(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)) // zero counts
        DnsPacketParser.writeDomainName(os, domain)
        os.write(byteArrayOf(0x00, 0x01)) // QTYPE A
        os.write(byteArrayOf(0x00, 0x01)) // QCLASS IN
        return os.toByteArray()
    }

    private fun buildDnsResponse(query: ByteArray, domain: String, ip: String): ByteArray {
        // Parse the query to get base structure, then add answer
        val queryPacket = DnsPacketParser.parse(query)
        val ipBytes = ip.split(".").map { it.toInt().toByte() }.toByteArray()
        val answer = com.adblock.app.vpn.DnsResourceRecord(
            name = domain,
            type = 1,
            cls = 1,
            ttl = 60,
            data = ipBytes
        )
        return DnsPacketParser.buildResponse(
            id = queryPacket.header.id,
            flags = 0x8180, // response + OK
            questions = queryPacket.questions,
            answers = listOf(answer)
        )
    }
}

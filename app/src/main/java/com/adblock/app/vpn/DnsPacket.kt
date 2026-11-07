package com.adblock.app.vpn

data class DnsHeader(
    val id: Int,
    val flags: Int,
    val questionCount: Int,
    val answerCount: Int,
    val authorityCount: Int,
    val additionalCount: Int
)

data class DnsQuestion(
    val domain: String,
    val type: Int,
    val cls: Int
)

data class DnsResourceRecord(
    val name: String,
    val type: Int,
    val cls: Int,
    val ttl: Long,
    val data: ByteArray
)

data class DnsPacket(
    val header: DnsHeader,
    val questions: List<DnsQuestion>,
    val answers: List<DnsResourceRecord> = emptyList()
)

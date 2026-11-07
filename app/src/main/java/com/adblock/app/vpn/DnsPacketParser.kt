package com.adblock.app.vpn

import java.io.ByteArrayOutputStream

object DnsPacketParser {

    fun parse(data: ByteArray): DnsPacket {
        val buffer = DnsBuffer(data)
        val header = readHeader(buffer)
        val questions = (0 until header.questionCount).map { readQuestion(buffer) }
        val answers = (0 until header.answerCount).map { readRecord(buffer) }
        return DnsPacket(header, questions, answers)
    }

    private fun readHeader(buf: DnsBuffer): DnsHeader {
        return DnsHeader(
            id = buf.readU16(),
            flags = buf.readU16(),
            questionCount = buf.readU16(),
            answerCount = buf.readU16(),
            authorityCount = buf.readU16(),
            additionalCount = buf.readU16()
        )
    }

    private fun readQuestion(buf: DnsBuffer): DnsQuestion {
        val domain = readDomainName(buf)
        val type = buf.readU16()
        val cls = buf.readU16()
        return DnsQuestion(domain, type, cls)
    }

    private fun readRecord(buf: DnsBuffer): DnsResourceRecord {
        val name = readDomainName(buf)
        val type = buf.readU16()
        val cls = buf.readU16()
        val ttl = buf.readU32()
        val rdlength = buf.readU16()
        val data = buf.readBytes(rdlength)
        return DnsResourceRecord(name, type, cls, ttl, data)
    }

    fun readDomainName(buf: DnsBuffer): String {
        val parts = mutableListOf<String>()
        while (true) {
            val len = buf.readU8()
            if (len == 0) break
            // Compression pointer (top 2 bits = 11)
            if (len and 0xC0 == 0xC0) {
                val pointer = ((len and 0x3F) shl 8) or buf.readU8()
                val saved = buf.position
                buf.position = pointer
                parts.add(readDomainName(buf))
                buf.position = saved
                break
            }
            val label = buf.readString(len)
            parts.add(label)
        }
        return parts.joinToString(".")
    }

    fun buildNxDomainResponse(query: DnsPacket): ByteArray {
        val header = query.header
        val responseFlags = (header.flags or 0x8000) or 0x0003
        return buildResponse(header.id, responseFlags, query.questions, emptyList())
    }

    fun buildResponse(
        id: Int,
        flags: Int,
        questions: List<DnsQuestion>,
        answers: List<DnsResourceRecord>
    ): ByteArray {
        val os = ByteArrayOutputStream()
        writeU16(os, id)
        writeU16(os, flags)
        writeU16(os, questions.size)
        writeU16(os, answers.size)
        writeU16(os, 0)
        writeU16(os, 0)
        for (q in questions) {
            writeDomainName(os, q.domain)
            writeU16(os, q.type)
            writeU16(os, q.cls)
        }
        for (a in answers) {
            writeDomainName(os, a.name)
            writeU16(os, a.type)
            writeU16(os, a.cls)
            writeU32(os, a.ttl)
            writeU16(os, a.data.size)
            os.write(a.data)
        }
        return os.toByteArray()
    }

    private fun writeU16(os: ByteArrayOutputStream, v: Int) {
        os.write((v shr 8) and 0xFF)
        os.write(v and 0xFF)
    }

    private fun writeU32(os: ByteArrayOutputStream, v: Long) {
        os.write(((v shr 24) and 0xFF).toInt())
        os.write(((v shr 16) and 0xFF).toInt())
        os.write(((v shr 8) and 0xFF).toInt())
        os.write((v and 0xFF).toInt())
    }

    fun writeDomainName(os: ByteArrayOutputStream, domain: String) {
        domain.split(".").forEach { label ->
            os.write(label.length)
            os.write(label.toByteArray())
        }
        os.write(0)
    }
}

class DnsBuffer(val data: ByteArray) {
    var position: Int = 0

    fun readU8(): Int = data[position++].toInt() and 0xFF

    fun readU16(): Int = (readU8() shl 8) or readU8()

    fun readU32(): Long = (readU8().toLong() shl 24) or
                          (readU8().toLong() shl 16) or
                          (readU8().toLong() shl 8) or
                          readU8().toLong()

    fun readBytes(n: Int): ByteArray {
        val result = data.copyOfRange(position, position + n)
        position += n
        return result
    }

    fun readString(n: Int): String {
        val result = String(data, position, n, Charsets.UTF_8)
        position += n
        return result
    }
}

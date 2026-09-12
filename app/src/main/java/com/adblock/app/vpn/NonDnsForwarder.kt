package com.adblock.app.vpn

import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap

class NonDnsForwarder {

    private val tcpConnections = ConcurrentHashMap<Long, SocketChannel>()
    private var nextConnectionId = 1L

    fun forwardTcpSyn(
        srcPort: Int,
        dstIp: ByteArray,
        dstPort: Int
    ): Long {
        return try {
            val dstAddr = InetSocketAddress(
                InetAddress.getByAddress(dstIp).hostAddress, dstPort
            )
            val channel = SocketChannel.open()
            channel.configureBlocking(false)
            channel.connect(dstAddr)

            val id = nextConnectionId++
            tcpConnections[id] = channel
            id
        } catch (e: Exception) {
            -1L
        }
    }

    fun forwardTcpData(connectionId: Long, data: ByteArray): Boolean {
        val channel = tcpConnections[connectionId] ?: return false
        return try {
            channel.write(ByteBuffer.wrap(data))
            true
        } catch (e: Exception) {
            removeConnection(connectionId)
            false
        }
    }

    fun readTcpResponse(connectionId: Long): ByteArray? {
        val channel = tcpConnections[connectionId] ?: return null
        return try {
            val buf = ByteBuffer.allocate(65535)
            val bytesRead = channel.read(buf)
            if (bytesRead > 0) {
                buf.flip()
                val data = ByteArray(bytesRead)
                buf.get(data)
                data
            } else {
                if (bytesRead == -1) removeConnection(connectionId)
                null
            }
        } catch (e: Exception) {
            removeConnection(connectionId)
            null
        }
    }

    fun removeConnection(id: Long) {
        tcpConnections.remove(id)?.close()
    }

    fun closeAll() {
        tcpConnections.values.forEach { it.close() }
        tcpConnections.clear()
    }
}

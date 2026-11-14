package com.adblock.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.adblock.app.MainActivity
import com.adblock.app.db.AppDatabase
import com.adblock.app.rules.Rule
import com.adblock.app.rules.RuleEngine
import com.adblock.app.stats.StatsTracker
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class AdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var packetJob: Job? = null

    lateinit var ruleEngine: RuleEngine
        private set
    lateinit var dnsHandler: DnsQueryHandler
        private set
    private lateinit var forwarder: NonDnsForwarder
    lateinit var statsTracker: StatsTracker
        private set
    lateinit var requestLogger: DnsRequestLogger
        private set

    private val defaultBlockedDomains = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "googletagmanager.com", "adservice.google.com",
        "ads.facebook.com", "pixel.facebook.com", "an.facebook.com",
        "adzerk.net", "exelator.com", "scorecardresearch.com",
        "outbrain.com", "taboola.com", "criteo.com",
        "adsrvr.org", "adnxs.com", "rubiconproject.com",
        "casalemedia.com", "moatads.com", "adsafeprotected.com",
        "applovin.com", "vungle.com", "unityads.unity3d.com",
        "chartboost.com", "crosspromo.com", "ironsrc.mobi",
        "appsflyer.com", "adjust.com", "branch.io"
    )

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "VPN 服务",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification(this, "正在启动 VPN..."))
                startVpn()
            }
            ACTION_STOP -> stopVpn()
        }
        currentInstance = this
        return START_STICKY
    }

    private fun startVpn() {
        ruleEngine = RuleEngine().also { engine ->
            engine.loadRules(defaultBlockedDomains.map {
                Rule(it, Rule.Type.Block)
            })
        }
        requestLogger = DnsRequestLogger(AppDatabase.getInstance(this))
        dnsHandler = DnsQueryHandler(
            ruleEngine,
            onQueryResult = { domain, blocked ->
                requestLogger.logQuery(domain, "", blocked)
            }
        )
        forwarder = NonDnsForwarder()
        statsTracker = StatsTracker(AppDatabase.getInstance(this))

        val builder = Builder()
        builder.setSession("AdBlock DNS Filter")
        builder.setMtu(1500)

        // VPN interface address
        builder.addAddress("10.0.0.2", 32)
        // Route all traffic through TUN
        builder.addRoute("0.0.0.0", 0)
        // Upstream DNS server routes
        builder.addRoute("119.29.29.29", 32)
        builder.addRoute("223.5.5.5", 32)
        // DNS server for the VPN
        builder.addDnsServer("10.0.0.1")

        builder.setBlocking(true)

        builder.setConfigureIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        try {
            // Load app bypass preferences
            try {
                val db = AppDatabase.getInstance(this)
                val bypassApps = runBlocking(Dispatchers.IO) {
                    db.appPreferenceDao().getBypassApps()
                }
                for (app in bypassApps) {
                    builder.addDisallowedApplication(app.packageName)
                    android.util.Log.d("AdBlockVPN", "Bypassing VPN for: ${app.appName} (${app.packageName})")
                }
            } catch (e: Exception) {
                android.util.Log.w("AdBlockVPN", "Failed to load app preferences", e)
            }

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                startPacketLoop()
                startForeground(NOTIFICATION_ID, createNotification(this, "Ad blocking active"))
            }
        } catch (e: Exception) {
            stopVpn()
        }
    }

    private fun startPacketLoop() {
        packetJob = serviceScope.launch {
            val input = FileInputStream(vpnInterface!!.fileDescriptor)
            val output = FileOutputStream(vpnInterface!!.fileDescriptor)
            val buffer = ByteBuffer.allocate(65535)

            while (isActive) {
                try {
                    buffer.clear()
                    val bytesRead = input.channel.read(buffer)
                    if (bytesRead <= 0) continue

                    buffer.flip()
                    val packet = ByteArray(bytesRead)
                    buffer.get(packet)

                    processPacket(packet, output)
                } catch (e: Exception) {
                    if (isActive) {
                        android.util.Log.e("AdBlockVPN", "Packet error", e)
                    }
                }
            }
        }
    }

    private fun processPacket(packet: ByteArray, output: FileOutputStream) {
        if (packet.size < 20) return
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) return

        val ihl = (packet[0].toInt() and 0x0F) * 4
        val totalLength = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
        if (totalLength > packet.size) return

        val protocol = packet[9].toInt() and 0xFF
        val srcIp = packet.copyOfRange(12, 16)
        val dstIp = packet.copyOfRange(16, 20)

        when (protocol) {
            6 -> handleTcp(packet, ihl, totalLength, output)
            17 -> handleUdp(packet, ihl, totalLength, srcIp, dstIp, output)
        }
    }

    private fun handleUdp(
        packet: ByteArray,
        ihl: Int,
        totalLength: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
        output: FileOutputStream
    ) {
        if (totalLength < ihl + 8) return
        val udpOffset = ihl
        val srcPort = readU16(packet, udpOffset)
        val dstPort = readU16(packet, udpOffset + 2)

        // Only intercept DNS (port 53)
        if (dstPort == 53 || srcPort == 53) {
            val payloadOffset = udpOffset + 8
            val payloadLength = totalLength - payloadOffset
            if (payloadLength <= 0) return

            val dnsData = packet.copyOfRange(payloadOffset, payloadOffset + payloadLength)

            statsTracker.incrementQueries()
            val response = dnsHandler.handle(dnsData)
            if (response != null) {
                statsTracker.incrementBlocked()
                val responsePacket = buildUdpResponse(
                    packet, ihl, udpOffset, response
                )
                if (responsePacket != null) {
                    output.write(responsePacket)
                    output.flush()
                }
            }
        }
    }

    private fun handleTcp(
        packet: ByteArray,
        ihl: Int,
        totalLength: Int,
        output: FileOutputStream
    ) {
        if (totalLength < ihl + 20) return
        val tcpOffset = ihl
        val dstPort = readU16(packet, tcpOffset + 2)

        // Skip DNS-over-TCP
        if (dstPort == 53) return

        // Forward TCP traffic through TUN unchanged
        output.write(packet, 0, totalLength)
        output.flush()
    }

    private fun buildUdpResponse(
        originalPacket: ByteArray,
        ipHeaderLen: Int,
        udpOffset: Int,
        dnsResponse: ByteArray
    ): ByteArray? {
        val udpLen = 8 + dnsResponse.size
        val totalLen = ipHeaderLen + udpLen
        val response = ByteArray(totalLen)

        // Copy IP header (will modify necessary fields)
        originalPacket.copyInto(response, 0, 0, ipHeaderLen)

        // Swap source and destination IP
        for (i in 0..3) {
            response[12 + i] = originalPacket[16 + i]
            response[16 + i] = originalPacket[12 + i]
        }

        // Update total length
        response[2] = ((totalLen shr 8) and 0xFF).toByte()
        response[3] = (totalLen and 0xFF).toByte()

        // Clear IP checksum for recalculation
        response[10] = 0
        response[11] = 0

        // UDP header: swap ports, set length
        response[ipHeaderLen] = originalPacket[udpOffset + 2]
        response[ipHeaderLen + 1] = originalPacket[udpOffset + 3]
        response[ipHeaderLen + 2] = originalPacket[udpOffset]
        response[ipHeaderLen + 3] = originalPacket[udpOffset + 1]
        response[ipHeaderLen + 4] = ((udpLen shr 8) and 0xFF).toByte()
        response[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
        response[ipHeaderLen + 6] = 0
        response[ipHeaderLen + 7] = 0

        // DNS payload
        dnsResponse.copyInto(response, ipHeaderLen + 8)

        // Recalculate IP checksum
        val checksum = ipChecksum(response, ipHeaderLen)
        response[10] = ((checksum shr 8) and 0xFF).toByte()
        response[11] = (checksum and 0xFF).toByte()

        return response
    }

    private fun readU16(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 8) or
                (data[offset + 1].toInt() and 0xFF)
    }

    private fun ipChecksum(header: ByteArray, len: Int): Int {
        var sum = 0
        var i = 0
        while (i < len) {
            sum += ((header[i].toInt() and 0xFF) shl 8) or
                    (header[i + 1].toInt() and 0xFF)
            i += 2
        }
        while (sum > 0xFFFF) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private fun stopVpn() {
        packetJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null
        forwarder.closeAll()
        statsTracker.shutdown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        currentInstance = null
        stopSelf()
    }

    override fun onDestroy() {
        currentInstance = null
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var currentInstance: AdBlockVpnService? = null

        const val ACTION_START = "com.adblock.app.START_VPN"
        const val ACTION_STOP = "com.adblock.app.STOP_VPN"
        private const val NOTIFICATION_CHANNEL_ID = "adblock_vpn"
        private const val NOTIFICATION_ID = 1001

        private fun createNotification(ctx: Context, text: String): Notification {
            val channelId = NOTIFICATION_CHANNEL_ID
            return Notification.Builder(ctx, channelId)
                .setContentTitle("AdBlock")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build()
        }
    }
}

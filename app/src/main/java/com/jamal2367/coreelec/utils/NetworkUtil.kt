package com.jamal2367.coreelec.utils

import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.nio.ByteOrder

object NetworkUtil {
    private fun getWifiManager(context: Context): WifiManager {
        return context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    fun getGateWayIp(context: Context): String? {
        val int2String: String?
        val dhcpInfo = getDhcpInfo(context)!!
        int2String = if (dhcpInfo.ipAddress == 0) {
            localIp
        } else {
            int2String(dhcpInfo.ipAddress)
        }
        return int2String
    }

    private val localIp: String?
        get() {
            var str: String? = null
            try {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val inetAddresses = networkInterfaces.nextElement().inetAddresses
                    while (inetAddresses.hasMoreElements()) {
                        val nextElement = inetAddresses.nextElement()
                        if (!nextElement.isLoopbackAddress && nextElement is Inet4Address) {
                            str = nextElement.getHostAddress()
                        }
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            return str
        }

    private fun int2String(i: Int): String {
        return if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            (i and 255).toString() + "." + (i shr 8 and 255) + "." + (i shr 16 and 255) + "." + (i shr 24 and 255)
        } else (i shr 24 and 255).toString() + "." + (i shr 16 and 255) + "." + (i shr 8 and 255) + "." + (i and 255)
    }

    @Suppress("DEPRECATION")
    private fun getDhcpInfo(context: Context): DhcpInfo? {
        val wifiManager = getWifiManager(context)
        return wifiManager.dhcpInfo
    }
}

/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketException

class MockMulticastSocket : MulticastSocket() {
    var sendPacket: DatagramPacket? = null
        private set
    private var _inetAddress: InetAddress? = null
    private var receiveData: ByteArray? = null
    private var wait: Long = 0

    @Throws(IOException::class)
    override fun setTimeToLive(ttl: Int) {
    }

    @Throws(IOException::class)
    override fun joinGroup(mcastaddr: InetAddress) {
    }

    @Throws(IOException::class)
    override fun leaveGroup(mcastaddr: InetAddress) {
    }

    @Throws(SocketException::class)
    override fun setNetworkInterface(netIf: NetworkInterface) {
    }

    @Throws(IOException::class)
    override fun send(p: DatagramPacket) {
        sendPacket = p
    }

    fun setReceiveData(address: InetAddress, data: ByteArray, wait: Long) {
        _inetAddress = address
        receiveData = data
        this.wait = wait
    }

    @Synchronized
    @Throws(IOException::class)
    override fun receive(p: DatagramPacket) {
        if (receiveData == null) {
            try {
                Thread.sleep(100000L)
            } catch (e: InterruptedException) {
                throw IOException()
            }
        }
        try {
            Thread.sleep(wait)
        } catch (e: InterruptedException) {
            throw IOException()
        }
        val receiveData = receiveData ?: throw IOException()
        System.arraycopy(receiveData, 0, p.data, 0, receiveData.size)
        p.length = receiveData.size
        p.address = _inetAddress
        this.receiveData = null
    }
}

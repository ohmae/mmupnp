/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.server

import java.io.IOException
import java.net.*

class MockMulticastSocket : MulticastSocket() {
    var sendPacket: DatagramPacket? = null
        private set
    private var _inetAddress: InetAddress? = null
    private var _port: Int = 0
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

    fun setReceiveData(address: InetAddress, port: Int, data: ByteArray, wait: Long) {
        _inetAddress = address
        _port = port
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
        p.port = _port
        this.receiveData = null
    }
}

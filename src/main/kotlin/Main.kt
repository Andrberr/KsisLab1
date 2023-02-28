import java.io.IOException
import java.lang.String.format
import java.lang.String.join
import java.net.*
import java.util.*

private var networkInterfaces = NetworkInterface.getNetworkInterfaces()
private var mask = -1

@Throws(SocketException::class, UnknownHostException::class)
fun main() {
    getPCInfo()
    printAllMACs()

    val ip = getScanInterface() //ip of the first node of this interface
    println()
    println("Mask = $mask")
    showMACAddresses(ip)
}

@Throws(UnknownHostException::class, SocketException::class)
fun getPCInfo() {
    val localHost = InetAddress.getLocalHost()
    val ni = NetworkInterface.getByInetAddress(localHost)
    val hardwareAddress = ni.hardwareAddress //mac
    val hexadecimal = arrayOfNulls<String>(hardwareAddress.size)
    if (hardwareAddress != null) {
        for (i in hardwareAddress.indices) {
            hexadecimal[i] = format("%02X", hardwareAddress[i])
        }
        println("1: Computer's localhost: " + ni.displayName + ", Mac address: " + join("-", *hexadecimal))
    }
}

@Throws(SocketException::class)
fun printAllMACs() {
    println("Other network interfaces on computer:")
    var number = 2
    var j = 1
    while (networkInterfaces.hasMoreElements()) {
        val ni = networkInterfaces.nextElement()
        val hardwareAddress = ni.hardwareAddress
        if (hardwareAddress != null) {
            val hexadecimalFormat = arrayOfNulls<String>(hardwareAddress.size)
            for (i in hardwareAddress.indices) {
                hexadecimalFormat[i] = format("%02X", hardwareAddress[i])
            }
            if (j != 1) {
                println(
                    number.toString() + " interface: Name = " + ni.displayName + ", Mac = " + join(
                        "-",
                        *hexadecimalFormat
                    )
                )
                number++
            }
            j++
        }
    }
}

@Throws(SocketException::class)
fun getScanInterface(): Int {
    networkInterfaces = NetworkInterface.getNetworkInterfaces()
    var host: InterfaceAddress? = null //IP address, a subnet mask and a broadcast address
    try {
        println("Enter the number of interface to scan:")
        val number = readlnOrNull()?.toInt()
        var num = 1
        while (networkInterfaces.hasMoreElements()) {
            val ni = networkInterfaces.nextElement()
            val hardwareAddress = ni.hardwareAddress //MAC
            if (hardwareAddress != null) {
                if (num == number) {
                    host = ni.interfaceAddresses[0]
                    println("Current interface name = " + ni.displayName)
                    mask = if (!ni.isUp || ni.isLoopback || ni.isVirtual) {
                        -1
                    } else host!!.networkPrefixLength.toInt()
                    break
                }
                num++
            }
        }
        return getIp(host!!.address)
    } catch (e: SocketException) {
        e.printStackTrace()
        return 0
    }
}

private fun getIp(localHost: InetAddress): Int {
    val rawIpAddress = localHost.address
    var ip = rawIpAddress[0].toInt() and 255
    ip = ip shl 8
    ip += rawIpAddress[1].toInt() and 255
    ip = ip shl 8
    ip += rawIpAddress[2].toInt() and 255
    ip = ip shl 8
    ip += rawIpAddress[3].toInt() and 255
    return ip
}

@Throws(SocketException::class)
private fun getMask(networkInterfaces: Enumeration<NetworkInterface>, oldValue: Int): Int {
    val current = networkInterfaces.nextElement()
    if (!current.isUp || current.isLoopback || current.isVirtual) {
        return oldValue
    }
    val host = current.interfaceAddresses[0]
    return host.networkPrefixLength.toInt()
}

private fun showMACAddresses(ip: Int) {
    val binaryMask: Int = createBinaryMask(mask)
    val address = ip and binaryMask
    val amount = binaryMask.inv() - 1
    printConnections(address, amount)
}

private fun createBinaryMask(mask: Int): Int {
    var binaryMask = 0
    for (i in 0..30) {
        if (i < mask) binaryMask++
        binaryMask = binaryMask shl 1
    }
    return binaryMask
}

private fun printConnections(address: Int, amount: Int) {
    val timeout = 300
    println()
    for (i in 1..amount) {
        val nodeAddress = (address + i).toLong()
        val anotherApi = createIP(nodeAddress)
        var inetAddress: InetAddress
        try {
            inetAddress = InetAddress.getByName(anotherApi)
            try {
                println("IP address: " + inetAddress.hostAddress)
                val state = inetAddress.isReachable(timeout)
                println("Is reachable: $state")
                println(getARP(inetAddress.hostAddress))
                Thread.sleep(100)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
    }
}

private fun createIP(nodeAddress: Long): String {
    val buf = ShortArray(4)
    buf[3] = (nodeAddress and 255L).toShort()
    buf[2] = (nodeAddress shr 8 and 255L).toShort()
    buf[1] = (nodeAddress shr 16 and 255L).toShort()
    buf[0] = (nodeAddress shr 24 and 255L).toShort()
    return buf[0].toString() + "." + buf[1].toString() + "." + buf[2].toString() + "." + buf[3].toString()
}

@Throws(IOException::class)
private fun getARP(ip: String): String {
    val str = Scanner(Runtime.getRuntime().exec("arp -a $ip").inputStream).useDelimiter("\\A")
    return str.next()
}



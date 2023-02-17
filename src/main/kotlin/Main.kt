
import java.lang.String.format
import java.lang.String.join
import java.net.*
import java.util.*

private val networkInterfaces = NetworkInterface.getNetworkInterfaces()

@Throws(SocketException::class, UnknownHostException::class)
fun main() {
    println("Mac adress of the computer: " + getPCMac())
    printAllMACs()



    var mask = -1
    while (networkInterfaces.hasMoreElements()) {
        mask = getMaskValue(mask)
    }
}

@Throws(UnknownHostException::class, SocketException::class)
fun getPCMac(): String {
    val localHost = InetAddress.getLocalHost()
    val ni = NetworkInterface.getByInetAddress(localHost)
    val hardwareAddress = ni.hardwareAddress
    val hexadecimal = arrayOfNulls<String>(hardwareAddress.size)
    for (i in hardwareAddress.indices) {
        hexadecimal[i] = format("%02X", hardwareAddress[i])
    }
    return join("-", *hexadecimal)
}

@Throws(SocketException::class)
fun printAllMACs() {
    var number = 1
    while (networkInterfaces.hasMoreElements()) {
        val ni = networkInterfaces.nextElement()
        val hardwareAddress = ni.hardwareAddress
        if (hardwareAddress != null) {
            val hexadecimalFormat = arrayOfNulls<String>(hardwareAddress.size)
            for (i in hardwareAddress.indices) {
                hexadecimalFormat[i] = format("%02X", hardwareAddress[i])
            }
            println(
                number.toString() + " interface: Name = " + ni.displayName + ", Mac = " + join(
                    "-",
                    *hexadecimalFormat
                )
            )
            number++
        }
    }
}

private fun createIPReadingForm(localHost: InetAddress): Int {
    val ipAddress = localHost.address
    var ip = ipAddress[0].toInt() and 255
    ip = ip shl 8
    ip += ipAddress[1].toInt() and 255
    ip = ip shl 8
    ip += ipAddress[2].toInt() and 255
    ip = ip shl 8
    ip += ipAddress[3].toInt() and 255
    return ip
}

@Throws(SocketException::class)
fun getScanInterface(): Int {
    var host: InterfaceAddress? = null
    try {
        println("Enter the number of interface to scan:")
        val number = readlnOrNull()?.toInt()
        var num = 1
        while (networkInterfaces.hasMoreElements()) {
            val ni = networkInterfaces.nextElement()
            val hardwareAddress = ni.hardwareAddress
            if (hardwareAddress != null) {
                if (num == number) {
                    host = ni.interfaceAddresses[0]
                    break
                }
                num++
            }
        }
        return createIPReadingForm(host!!.address)
    } catch (e: SocketException) {
        e.printStackTrace()
        return 0
    }
}

@Throws(SocketException::class)
private fun getMaskValue(oldValue: Int): Int {
    val current = networkInterfaces.nextElement()
    if (!current.isUp || current.isLoopback || current.isVirtual) {
        return oldValue
    }
    val host = current.interfaceAddresses[0]
    println(current.displayName)
    return host.networkPrefixLength.toInt()
}
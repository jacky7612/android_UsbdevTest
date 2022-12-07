package com.tech.usbdevtest

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.util.Log
import java.text.ParseException
import java.util.*

class UsbCardreader {
    private val ACTION_USB_PERMISSION   = "com.android.example.USB_PERMISSION"
    private val TAB_STR                 = "\t\t"
    private val TAB_ENABLE              = false
    /* USB system service */
    public  lateinit var    permissionIntent            : PendingIntent
    private val             model_CardReader_vendorid   : Int = 3238
    private val             model_CardReader_productid  : Int = 16
    private lateinit var    model_epOut                 : UsbEndpoint
    private lateinit var    model_epIn                  : UsbEndpoint
    public  lateinit var    usbManager                  : UsbManager
    public  lateinit var    model_Msg                   : String

    private fun convertByteToHexadecimal(byteArray: ByteArray): String
    {
        var hex=""

        // Iterating through each byte in the array
        for (i in byteArray) {
            hex += String.format(" %02X", i)
        }
        return hex;
    }
    private fun getUsbTypeString(itype: Int): String
    {
        if  (itype == UsbConstants.USB_ENDPOINT_XFER_BULK)
            return "USB_ENDPOINT_XFER_BULK"
        else if  (itype == UsbConstants.USB_ENDPOINT_XFER_CONTROL)
            return "USB_ENDPOINT_XFER_CONTROL"
        else if  (itype == UsbConstants.USB_ENDPOINT_XFER_ISOC)
            return "USB_ENDPOINT_XFER_ISOC"
        else
            return "USB_ENDPOINT_XFER_INT"
    }
    private fun getUsbDirectString(itype: Int): String
    {
        if  (itype == UsbConstants.USB_DIR_IN)
            return "USB_DIR_IN"
        else
            return "USB_DIR_OUT"
    }
    private fun getEndpoint(connection: UsbDeviceConnection, devIface: UsbInterface): String
    {
        // Get the endpoint from interface
        var ret: String = ""
        for (i in 0 until devIface.endpointCount) {
            val ep = devIface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.attributes == 0x02) {
                if (ep.direction == UsbConstants.USB_DIR_OUT) model_epOut = ep
                if (ep.direction == UsbConstants.USB_DIR_IN)  model_epIn  = ep
                ret += tracelog("end point type :" + ep.type + " ==" + UsbConstants.USB_ENDPOINT_XFER_BULK + "(" + getUsbTypeString(ep.type) + ")\n")
                ret += tracelog("ep.direction :" + ep.direction + " ==" + UsbConstants.USB_DIR_IN + "(" + getUsbDirectString(ep.direction) +")\n")
            }
        }
        return ret
    }

    private fun checkPermission(device: UsbDevice, hasPermision: Boolean, permissionIntent: PendingIntent): String
    {
        var ret = ""
        if (hasPermision)
        {
            ret += "device :" + device.deviceId +" Permission ok!\n"
        }
        else
        {
            ret += "device :" + device.deviceId +" do Permission procedure!"
            usbManager.requestPermission(device, permissionIntent)
        }
        return tracelog(ret);
    }
    private fun write2usb(connection: UsbDeviceConnection, buffer: ByteArray): String
    {
        var ret: String = ""
        try {
            val response = connection.bulkTransfer(model_epOut, buffer, buffer.size, 1000)
            Log.d(ContentValues.TAG, "Was response from read successful? $response\n")
            ret += "\n" + tracelog("Was response from read successful: $response\n")
            ret += tracelog("buffer size : " + buffer.size + "\n")

            if (response == buffer.size) {
                ret += tracelog("response ok; buffer size : " + buffer.size + "\n")
            }
            var hex = convertByteToHexadecimal(buffer)
            Log.d(ContentValues.TAG, TAB_STR + "request Hex: $hex")
            ret += tracelog("*                     Hex: " + hex + "\n")

            // 2. 接收Reader回傳資料
            var Receiveytes = ByteArray(0xFF)
            var ret_code = connection.bulkTransfer(model_epIn, Receiveytes, Receiveytes.size, 10000)
            Receiveytes = Arrays.copyOfRange(Receiveytes,0, ret_code)
            Log.d(ContentValues.TAG, "Was response from read successful? $ret_code\n")
            ret += "\n" + tracelog(" Was response from read ret_code: $ret_code\n")
            ret += tracelog("Receiveytes size : " + Receiveytes.size + "\n")

            hex = convertByteToHexadecimal(Receiveytes)
            Log.d(ContentValues.TAG, "response Hex: $hex")
            ret += tracelog("* response Hex: " + hex + "\n")
            ////connection.close()
            //ret += "close device connection"
        } catch (e: IllegalArgumentException) {
            ret += tracelog("Invalid device connect" + e.message)
        }
        return ret
    }
    // ---------------------------------------------------------------------------------------------
    public fun tracelog(sval: String): String
    {
        var msg = String()
        when {
            (TAB_ENABLE) -> msg += TAB_STR
        }
        msg += sval
        return msg
    }
    /**
     * Broadcast receiver to handle USB disconnect events.
     */
    public val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            //call method to set up device communication
                        }
                    } else {
                        Log.d(ContentValues.TAG, "permission denied for device $device")
                    }
                }
            }
        }
    }
    /**
     * prepare for receiver to handle USB disconnect events.
     */
    public fun initFilter(context: Context): IntentFilter
    {
        // 建立 授權推播訊息
        model_Msg = String()
        permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION),0)
        return IntentFilter(ACTION_USB_PERMISSION)
    }
    /**
     * detect USB plug in/out events.
     */
    public fun detectCardreader(): Boolean {
        val deviceList = usbManager.getDeviceList()
        var fRet             = false

        model_Msg += "* Detect card reader Entry <<<\n\n"
        try {
            if (deviceList.isNullOrEmpty()) {
                model_Msg += tracelog("No Devices Currently Connected\n")
            } else {
                val builder = buildString {
                    append(tracelog("Connected Device Count: " + deviceList.size + "\n"))

                    var device: UsbDevice
                    for (dev in deviceList) {
                        device = dev.value
                        //Use the last device detected (if multiple) to open
                        if (device == null) {
                            append(tracelog("device == null\n"))
                            continue
                        }
                        if (device.vendorId != model_CardReader_vendorid || device.productId != model_CardReader_productid) continue;
                        append(tracelog("deviceId :" + device.deviceId.toString() + "\n") + tracelog("vendorId :" + device.vendorId.toString() + "\n") + tracelog("deviceName" + device.deviceName.toString() + "\n"))

                        var hasPermision = usbManager.hasPermission(device)
                        append(checkPermission(device, hasPermision, permissionIntent))
                        append(checkPermission(device, hasPermision, permissionIntent))

                        var connection = usbManager.openDevice(device) as UsbDeviceConnection
                        if (connection != null) {
                            Log.d(ContentValues.TAG, "讀卡機 已連線")
                            append(tracelog("get :" + device.deviceId + " connection ok\n"))
                            fRet=true
                        } else {
                            Log.d(ContentValues.TAG, "讀卡機 沒連線")
                            append(tracelog("(X) get :" + device.deviceId + " connection failure\n"))
                            fRet=false
                        }
                        //printDeviceDetails(device, connectedDevices)
                        /*
                        Ks : key structure = 0x00
                            ※ Currently we only support
                                1. card key (Mifare key)
                                2. plain transmission
                                3. load to volatile memory (data will lost when power off)
                        Kn : key number = 0x00
                        Kl : key length
                        K : key
                        */
                        var Rt: Int = 0x00
                        var Ri: Int = 0xFF
                        var buffer = byteArrayOf(
                            0xFF.toByte(),
                            0x82.toByte(),
                            0x00.toByte(),
                            0x00.toByte(),
                            0x06.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte()
                        )
                        var devIface: UsbInterface = device.getInterface(device.interfaceCount - 1)
                        append(tracelog("devIface ok\n"))

                        var msg = getEndpoint(connection, devIface)
                        append(msg)

                        if (connection != null) {
                            msg = write2usb(connection, buffer)
                            append(msg)
                        }
                    }
                }
                model_Msg += builder
            }
        } catch (e: IllegalArgumentException) {
            model_Msg += tracelog("Exception error :") + e.message
        } finally {
            model_Msg += "\n*Detect card reader Exit >>>\n"
        }
        return fRet
    }

    /**
     * Initiate a control transfer to request the device information
     * from its descriptors.
     *
     * @param device USB device to query.
     */
    public fun printDeviceDetails(device: UsbDevice): String {
        val connection = usbManager.openDevice(device)
        var msg = String()

        for (describe in connection.rawDescriptors)
            msg += describe.toString()

        val deviceDescriptor = try {
            //Parse the raw device descriptor
            connection.rawDescriptors.toString()
        } catch (e: IllegalArgumentException) {
            Log.w(ContentValues.TAG, "Invalid device descriptor", e)
            null
        }

        val configDescriptor = try {
            //Parse the raw configuration descriptor
            connection.rawDescriptors.toString()
        } catch (e: IllegalArgumentException) {
            Log.w(ContentValues.TAG, "Invalid config descriptor", e)
            null
        } catch (e: ParseException) {
            Log.w(ContentValues.TAG, "Unable to parse config descriptor", e)
            null
        }

        return msg + "\n"
    }
}
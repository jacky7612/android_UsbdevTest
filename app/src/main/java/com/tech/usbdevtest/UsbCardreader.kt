package com.tech.usbdevtest

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.util.Log
import java.text.ParseException

class UsbCardreader {
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    /* USB system service */
    private lateinit var permissionIntent : PendingIntent
    public lateinit var usbManager        : UsbManager
    private val model_CardReader_vendorid : Int = 3238
    private val model_CardReader_productid: Int = 16
    private lateinit var model_epOut      : UsbEndpoint
    private lateinit var model_epIn       : UsbEndpoint

    private fun convertByteToHexadecimal(byteArray: ByteArray): String
    {
        var hex=""

        // Iterating through each byte in the array
        for (i in byteArray) {
            hex += String.format("%02X", i)
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
                ret += "end point type :" + ep.type + " ==" + UsbConstants.USB_ENDPOINT_XFER_BULK + "(" + getUsbTypeString(ep.type) + ")\n"
                ret += "ep.direction :" + ep.direction + " ==" + UsbConstants.USB_DIR_IN + "(" + getUsbDirectString(ep.direction) +")\n"
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
        return ret;
    }
    private fun write2usb(connection: UsbDeviceConnection, buffer: ByteArray): String
    {
        var ret: String = ""
        try {
            val response = connection.bulkTransfer(model_epOut, buffer, buffer.size, 1000)
            Log.d(ContentValues.TAG, "Was response from read successful? $response\n")
            ret += "Was response from read successful: $response\n"
            ret += "buffer size : " + buffer.size + "\n"

            if (response == buffer.size) {
                ret += "response ok; buffer size : " + buffer.size + "\n"
            }
            var hex = convertByteToHexadecimal(buffer)
            Log.d(ContentValues.TAG, "Hex: $hex")
            ret += "Hex: " + hex + "\n"

            // 2. 接收Reader回傳資料
            var Receiveytes = ByteArray(0xFF)
            var ret_code = connection.bulkTransfer(model_epIn, Receiveytes, Receiveytes.size, 10000)
            Log.d(ContentValues.TAG, "Was response from read successful? $ret_code\n")
            ret += "Was response from read ret_code: $ret_code\n"
            ret += "Receiveytes size : " + Receiveytes.size + "\n"

            hex = convertByteToHexadecimal(Receiveytes)
            Log.d(ContentValues.TAG, "Hex: $hex")
            ret += "Hex: " + hex + "\n"
            ////connection.close()
            //ret += "close device connection"
        } catch (e: IllegalArgumentException) {
            ret += "Invalid device connect" + e.message
        }
        return ret
    }
    // ---------------------------------------------------------------------------------------------
    /**
     * Broadcast receiver to handle USB disconnect events.
     */
    public var usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String=intent.action.toString()
            if (ACTION_USB_PERMISSION.equals(action)) {
                var device =intent.getParcelableExtra<UsbDevice> (UsbManager.EXTRA_DEVICE)
                //if (device.vendorId == 0x0403) {
                //    model_UsbCr.usbManager .grantDevicePermission(device, ai.uid)
                //}
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true)) {
                    if (device != null) {
                        // call method to set up device communication
                    }
                } else {
                }
            }
        }
    }
    //private var usbReceiver = object : BroadcastReceiver() {
    //    override fun onReceive(context: Context, intent: Intent) {
    //        if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
    //            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
    //            device?.let {
    //                printStatus(getString(R.string.status_removed))
    //                printDeviceDescription(it)
    //            }
    //        }
    //    }
    //}

    /**
     * prepare for receiver to handle USB disconnect events.
     */
    public fun init(context: Context): IntentFilter
    {
        // 建立 授權推播訊息
        permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(
                ACTION_USB_PERMISSION
            ), 0
        )
        return IntentFilter(ACTION_USB_PERMISSION)
    }
    /**
     * detect USB plug in/out events.
     */
    public fun detectCardreader(): String {
        val connectedDevices = usbManager.deviceList

        if (connectedDevices.isEmpty()) {
            return "No Devices Currently Connected\n"
        } else {
            val builder = buildString {
                append("Connected Device Count: ")
                append(connectedDevices.size)
                append("\n\n")

                for (dev in connectedDevices.values) {
                    var device: UsbDevice
                    device = dev
                    //Use the last device detected (if multiple) to open
                    if (device == null) {
                        append("device == null\n")
                        continue
                    }
                    if (device.vendorId != model_CardReader_vendorid || device.productId != model_CardReader_productid) continue;
                    append(device.deviceId.toString() + "\n")
                    append(device.vendorId.toString() + "\n")
                    append(device.deviceName.toString() + "\n")

                    var hasPermision = usbManager.hasPermission(device)
                    append(checkPermission(device, hasPermision, permissionIntent))
                    append(checkPermission(device, hasPermision, permissionIntent))

                    var connection = usbManager.openDevice(device) as UsbDeviceConnection
                    if (connection != null) {
                        Log.d(ContentValues.TAG, "讀卡機 已連線")
                        append("get :" + device.deviceId +" connection ok\n")
                    } else {
                        Log.d(ContentValues.TAG, "讀卡機 沒連線")
                        append("(X) get :" + device.deviceId +" connection failure\n")
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
                    var buffer = byteArrayOf(0xFF.toByte(), 0x82.toByte(), 0x00.toByte(), 0x00.toByte(), 0x06.toByte()
                        , 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                    var devIface: UsbInterface = device.getInterface(device.interfaceCount - 1)
                    append("devIface ok\n")

                    var msg = getEndpoint(connection, devIface)
                    append(msg)

                    if (connection != null) {
                        msg = write2usb(connection, buffer)
                        append(msg)
                    }
                }
            }
            return builder
        }
    }

    /**
     * Initiate a control transfer to request the device information
     * from its descriptors.
     *
     * @param device USB device to query.
     */
    public fun printDeviceDetails(device: UsbDevice): String {
        val connection = usbManager.openDevice(device)

        val deviceDescriptor = try {
            //Parse the raw device descriptor
            connection.rawDescriptors
        } catch (e: IllegalArgumentException) {
            Log.w(ContentValues.TAG, "Invalid device descriptor", e)
            null
        }

        val configDescriptor = try {
            //Parse the raw configuration descriptor
            connection.rawDescriptors
            //accessDevice(device, connection)
        } catch (e: IllegalArgumentException) {
            Log.w(ContentValues.TAG, "Invalid config descriptor", e)
            null
        } catch (e: ParseException) {
            Log.w(ContentValues.TAG, "Unable to parse config descriptor", e)
            null
        }

        return "$deviceDescriptor\n\n$configDescriptor"
    }
}
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
    private val TAB_ENABLE              = true
    /* USB system service */
    public  lateinit var    permissionIntent            : PendingIntent
    private val             model_CardReader_vendorid   : Int = 3238
    private val             model_CardReader_productid  : Int = 16
    private lateinit var    model_epOut                 : UsbEndpoint
    private lateinit var    model_epIn                  : UsbEndpoint
    public  lateinit var    model_usbManager            : UsbManager
    public  lateinit var    model_Device                : UsbDevice
    public  lateinit var    model_Msg                   : String
    public  lateinit var    model_DeviceConnection      : UsbDeviceConnection

    public  var             model_PrevPlugin            : Boolean = false
    public  var             model_Plugin                : Boolean = false
    public  var             model_initCardreader_Succeed: Boolean = false
    public  lateinit var    model_Receiveytes           : ByteArray//接收資料
    public  var             model_cardID                : String = ""
    public  var             model_Name                  : String = ""
    public  var             model_Identity              : String = ""
    public  var             model_Birthday              : String = ""
    public  var             model_Sex                   : String = ""

    // sample variant
    private val slot=0
    private var sequence=0

    fun UsbCardreader()
    {
        model_PrevPlugin                = false
        model_initCardreader_Succeed    = false
        model_Plugin                    = false
        model_Msg                       = ""
        model_cardID                    = ""
        model_Name                      = ""
        model_Identity                  = ""
        model_Birthday                  = ""
        model_Sex                       = ""
    }

    /**
     * big5 to unicode
     */
    private fun big5ToUnicode(str: String): String? {
        // 請在此寫出本方法的程式碼
        var strResult=""
        try {
            strResult=String(str.toByteArray(charset("ISO8859_1")), charset("Big5"))
        } catch (e: Exception) {
        }
        return strResult
    }

    /**
     * parse Health Card Info
     */
    private fun parseHealthCardInfo(Receiveytes: ByteArray) {
        sequence=(sequence + 1) % 0xFF
        //mResponseTextView.append("=====================");
        var temp=""
        var offset: Int
        offset=10
        for (temp_i in 0..11) temp+=String.format(
            "%C",
            Receiveytes.get(temp_i + offset)
        )
        //logMsg("Card ID = " + temp);
        model_cardID = ("Card ID = $temp")
        offset+=12
        temp=""
        run {
            var temp_i=0
            while (temp_i < 20) {
                if (Receiveytes.get(temp_i + offset).toInt() != 0 &&
                    Receiveytes.get(temp_i + offset + 1).toInt() != 0)
                {
                    var temp2=""
                    temp2+=if (Receiveytes.get(temp_i + offset) < 0) {
                        "" + (256 + Receiveytes.get(temp_i + offset)).toChar() //轉char
                    } else {
                        "" + Char(Receiveytes.get(temp_i + offset).toUShort())
                    }
                    temp2+=if (Receiveytes.get(temp_i + offset + 1) < 0) {
                        "" + (256 + Receiveytes.get(temp_i + offset + 1)).toChar() //轉char
                    } else {
                        "" + Char(Receiveytes.get(temp_i + offset + 1).toUShort())
                    }
                    temp+=big5ToUnicode(temp2)
                }
                temp_i+=2
            }
        }
        //logMsg("Name = " + temp);
        model_Name = "Name = $temp"
        offset+=20
        temp=""
        for (temp_i in 0..9) temp+=String.format("%C", Receiveytes.get(temp_i + offset))
        //logMsg("Identity Card = " + temp);
        model_Identity = "Identity Card = $temp"
        offset+=10
        temp=""
        for (temp_i in 0..6) temp+=String.format("%C", Receiveytes.get(temp_i + offset))
        //logMsg("Birthday = " + temp);
        model_Birthday = "Birthday = $temp"
        offset+=7
        temp=""
        if (Receiveytes.get(offset).toInt() == 0x4D) {
            //logMsg("Sex =  男");
            model_Sex = "Sex =  男"
        } else {
            //logMsg("Sex =  女");
            model_Sex = "Sex =  女"
        }
    }

    /**
     * convert Byte To Hex a decimal
     */
    private fun convertByteToHexadecimal(byteArray: ByteArray): String
    {
        var hex=""

        // Iterating through each byte in the array
        for (i in byteArray) {
            hex += String.format(" %02X", i)
        }
        return hex;
    }

    /**
     * get Usb Type String
     */
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

    /**
     * get Usb Direct String
     */
    private fun getUsbDirectString(itype: Int): String
    {
        if  (itype == UsbConstants.USB_DIR_IN)
            return "USB_DIR_IN"
        else
            return "USB_DIR_OUT"
    }

    /**
     * get End point
     */
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

    /**
     * check Permission
     */
    private fun checkPermission(device: UsbDevice, hasPermision: Boolean, permissionIntent: PendingIntent): String
    {
        var ret = ""
        if (hasPermision)
        {
            ret += tracelog("device :" + device.deviceId +" Permission ok!\n")
        }
        else
        {
            ret += tracelog("device :" + device.deviceId +" do Permission procedure!\n")
            model_usbManager.requestPermission(device, permissionIntent)
        }
        return tracelog(ret);
    }

    /**
     * access usb device
     */
    private fun access_Usbdevice(connection: UsbDeviceConnection, buffer: ByteArray): String
    {
        var ret: String = ""
        try {
            val response = connection.bulkTransfer(model_epOut, buffer, buffer.size, 1000)
            Log.d(ContentValues.TAG, "Was response from read successful? $response")
            ret += "\n" + tracelog("Was response from read successful: $response\n")
            ret += tracelog("buffer size : " + buffer.size + "\n")

            if (response == buffer.size) {
                ret += tracelog("response ok; buffer size : " + buffer.size + "\n")
            }
            var hex = convertByteToHexadecimal(buffer)
            Log.d(ContentValues.TAG, TAB_STR + "request Hex: $hex")
            ret += tracelog("*                     Hex: " + hex + "\n")

            // 2. 接收Reader回傳資料
            model_Receiveytes = ByteArray(0xFF)
            var ret_code = connection.bulkTransfer(model_epIn, model_Receiveytes, model_Receiveytes.size, 10000)
            model_Receiveytes = Arrays.copyOfRange(model_Receiveytes,0, ret_code)
            Log.d(ContentValues.TAG, "Was response from read successful? $ret_code")
            ret += "\n" + tracelog(" Was response from read ret_code: $ret_code\n")
            ret += tracelog("Receiveytes size : " + model_Receiveytes.size + "\n")

            hex = convertByteToHexadecimal(model_Receiveytes)
            Log.d(ContentValues.TAG, "response Hex: $hex")
            ret += tracelog("* response Hex: " + hex + "\n")
            ////connection.close()
            //ret += "close device connection"
        } catch (e: Exception) {
            ret += tracelog("Invalid device connect" + e.message + "\n")
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

    public fun initCardreader(): Boolean {
        val deviceList = model_usbManager.getDeviceList()
        var fRet       = false

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

                        var hasPermision = model_usbManager.hasPermission(device)
                        append(checkPermission(device, hasPermision, permissionIntent))
                        append(checkPermission(device, hasPermision, permissionIntent))
                        hasPermision = model_usbManager.hasPermission(device)
                        if (hasPermision) model_Device = device // 取得讀卡機 usb device

                        var connection = model_usbManager.openDevice(device) as UsbDeviceConnection
                        if (connection != null) {
                            Log.d(ContentValues.TAG, "讀卡機 已連線")
                            append(tracelog("get :" + device.deviceId + " connection ok\n"))
                            fRet=true
                        } else {
                            Log.d(ContentValues.TAG, "讀卡機 沒連線")
                            append(tracelog("(X) get :" + device.deviceId + " connection failure\n"))
                            fRet=false
                        }

                        // 取得裝置連結
                        model_DeviceConnection = connection
                        model_initCardreader_Succeed = true

                        var devIface: UsbInterface = device.getInterface(device.interfaceCount - 1)
                        append(tracelog("devIface ok\n"))

                        var msg = getEndpoint(connection, devIface)
                        append(msg)
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

    public fun detectCardreader(): String
    {
        var msg=""
        val GetHealthIDCardCmd1=byteArrayOf(0x65)

        if (model_DeviceConnection == null) {
            return tracelog("No Card reader device Found !\n")
        }
        val spGetHealthIDCardCmd1=ByteArray(GetHealthIDCardCmd1.size + 10)
        spGetHealthIDCardCmd1[0]=0x57
        spGetHealthIDCardCmd1[1]=0x00
        spGetHealthIDCardCmd1[2]=0x00
        spGetHealthIDCardCmd1[3]=0x00
        spGetHealthIDCardCmd1[4]=GetHealthIDCardCmd1.size.toByte()
        spGetHealthIDCardCmd1[5]=slot.toByte()
        spGetHealthIDCardCmd1[6]=sequence.toByte()
        spGetHealthIDCardCmd1[7]=0x00
        spGetHealthIDCardCmd1[8]=0x00
        spGetHealthIDCardCmd1[9]=0x00
        System.arraycopy(GetHealthIDCardCmd1, 0, spGetHealthIDCardCmd1, 10, GetHealthIDCardCmd1.size)
        //-------------------------
        msg = access_Usbdevice(model_DeviceConnection, spGetHealthIDCardCmd1)

        if (model_Receiveytes.get(0) == 0x80.toByte() && model_Receiveytes.get(7).toInt() == 0x42) {
            msg+=tracelog("No Card !\n")
            msg += tracelog("沒有插卡!\n")
            model_Plugin = false
        } else {
            msg+=tracelog("Card Exists!\n")
            msg += tracelog("偵測到卡!\n")
            model_Plugin = true
        }
        return msg
    }
    /**
     * detect USB plug in/out events.
     */
    public fun detectCardreaderTest(): Boolean {
        val deviceList = model_usbManager.getDeviceList()
        var fRet       = false

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

                        var hasPermision = model_usbManager.hasPermission(device)
                        append(checkPermission(device, hasPermision, permissionIntent))
                        append(checkPermission(device, hasPermision, permissionIntent))
                        hasPermision = model_usbManager.hasPermission(device)
                        if (hasPermision) model_Device = device // 取得讀卡機 usb device

                        var connection = model_usbManager.openDevice(device) as UsbDeviceConnection
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
                        //var Rt: Int = 0x00
                        //var Ri: Int = 0xFF
                        //var buffer = byteArrayOf(
                        //    0xFF.toByte(),
                        //    0x82.toByte(),
                        //    0x00.toByte(),
                        //    0x00.toByte(),
                        //    0x06.toByte(),
                        //    0xFF.toByte(),
                        //    0xFF.toByte(),
                        //    0xFF.toByte(),
                        //    0xFF.toByte(),
                        //    0xFF.toByte(),
                        //    0xFF.toByte()
                        //)
                        var devIface: UsbInterface = device.getInterface(device.interfaceCount - 1)
                        append(tracelog("devIface ok\n"))

                        var msg = getEndpoint(connection, devIface)
                        append(msg)

                        //if (connection != null) {
                        //    msg = write2usb(connection, buffer)
                        //    append(msg)
                        //}
                    }
                }
                model_Msg += builder
            }
        } catch (e: Exception) {
            model_Msg += tracelog("Exception error :") + e.message
        } finally {
            model_Msg += "\n*Detect card reader Exit >>>\n"
        }
        return fRet
    }


    public fun readHealthCardData(): String {
        var msg = ""
        var GetHealthIDCardCmd1 = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
                                              0x10.toByte(), 0xD1.toByte(), 0x58.toByte(), 0x00.toByte(),
                                              0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
                                              0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                                              0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x11.toByte(),
                                              0x00.toByte())
        var GetHealthIDCardCmd2 = byteArrayOf(0x00.toByte(), 0xCA.toByte(), 0x11.toByte(), 0x00.toByte(),
                                              0x02.toByte(), 0x00.toByte(), 0x00.toByte());

        try {
            msg+=detectCardreader()
            if (model_Plugin == false) return msg

            msg+=cmdPowerON()
            if (model_DeviceConnection == null) {
                msg+=tracelog("No Card reader device Found !\n")
                msg+=cmdPowerOFF()
                return msg
            }

            val spGetHealthIDCardCmd1=ByteArray(GetHealthIDCardCmd1.size + 10)
            spGetHealthIDCardCmd1[0]=0x57 //type
            spGetHealthIDCardCmd1[1]=0x00 //we don't support long length
            spGetHealthIDCardCmd1[2]=0x00 //we don't support long length
            spGetHealthIDCardCmd1[3]=0x00 //we don't support long length
            spGetHealthIDCardCmd1[4]=GetHealthIDCardCmd1.size.toByte() // 長度
            spGetHealthIDCardCmd1[5]=slot.toByte()
            spGetHealthIDCardCmd1[6]=sequence.toByte()
            spGetHealthIDCardCmd1[7]=0x00
            spGetHealthIDCardCmd1[8]=0x00
            spGetHealthIDCardCmd1[9]=0x00
            System.arraycopy(GetHealthIDCardCmd1, 0, spGetHealthIDCardCmd1, 10, GetHealthIDCardCmd1.size)

            //-------------------------
            msg+=access_Usbdevice(model_DeviceConnection, spGetHealthIDCardCmd1)
            //-------------------------
            // 2. 接收Reader回傳資料
            //回傳資料
            sequence=(sequence + 1) % 0xFF

            if (model_Receiveytes.get(model_Receiveytes.size - 2) == 0x90.toByte() &&
                model_Receiveytes.get(model_Receiveytes.size - 1).toInt() == 0x00)
            {
                val spGetHealthIDCardCmd2=ByteArray(GetHealthIDCardCmd2.size + 10)
                spGetHealthIDCardCmd2[0]=0x57 //type
                spGetHealthIDCardCmd2[1]=0x00 //we don't support long length
                spGetHealthIDCardCmd2[2]=0x00 //we don't support long length
                spGetHealthIDCardCmd2[3]=0x00 //we don't support long length
                spGetHealthIDCardCmd2[4]=GetHealthIDCardCmd2.size.toByte() // 長度
                spGetHealthIDCardCmd2[5]=slot.toByte()
                spGetHealthIDCardCmd2[6]=sequence.toByte()
                spGetHealthIDCardCmd2[7]=0x00
                spGetHealthIDCardCmd2[8]=0x00
                spGetHealthIDCardCmd2[9]=0x00
                System.arraycopy(
                    GetHealthIDCardCmd2,
                    0,
                    spGetHealthIDCardCmd2,
                    10,
                    GetHealthIDCardCmd2.size
                )
                msg+=access_Usbdevice(model_DeviceConnection, spGetHealthIDCardCmd2)

                //-------------------------
                // 3. 接收Reader回傳資料
                parseHealthCardInfo(model_Receiveytes)
                //mResponseTextView.append("\n=====================");
            }
        } catch (e: Exception) {
            msg+=tracelog("getCardData :$e\n")
        } finally {
            cmdPowerOFF()
        }
        //-------------------------

        return msg
    }
    /**
     * Initiate a control transfer to request the device information
     * from its descriptors.
     *
     * @param device USB device to query.
     */
    public fun printDeviceDetails(device: UsbDevice): String {
        val connection = model_usbManager.openDevice(device)
        var msg = String()

        for (describe in connection.rawDescriptors)
            msg += describe.toString()

        val deviceDescriptor = try {
            //Parse the raw device descriptor
            connection.rawDescriptors.toString()
        } catch (e: Exception) {
            Log.w(ContentValues.TAG, "Invalid device descriptor", e)
            null
        }

        val configDescriptor = try {
            //Parse the raw configuration descriptor
            connection.rawDescriptors.toString()
        } catch (e: Exception) {
            Log.w(ContentValues.TAG, "Invalid config descriptor", e)
            null
        } catch (e: ParseException) {
            Log.w(ContentValues.TAG, "Unable to parse config descriptor", e)
            null
        }

        return msg + "\n"
    }

    public fun cmdPowerON(): String {
        var msg = ""
        val PowerOnCmd=byteArrayOf(
            0x62.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            slot.toByte(),
            sequence.toByte(),
            0x00,
            0x00,
            0x00
        )
        if (model_DeviceConnection == null) {
            return "No Reader Found !"
        }
        //-------------------------
        msg = access_Usbdevice(model_DeviceConnection, PowerOnCmd)

        //-------------------------
        // 2. 接收Reader回傳資料
        //回傳資料
        sequence=(sequence + 1) % 0xFF
        if (model_Receiveytes.get(0) == 0x80.toByte() && model_Receiveytes.get(7).toInt() == 0x00) {
            val bATR=ByteArray(model_Receiveytes.get(4) - 1)
            msg += tracelog("ART " + (model_Receiveytes.get(4) - 1).toString() + " bytes :")
            System.arraycopy(model_Receiveytes, 11, bATR, 0, model_Receiveytes.get(4) - 1)
            msg += tracelog("$bATR, $bATR.size")
            if (bATR[0] != 0x3B.toByte() && bATR[0] != 0x3F.toByte()) {
                msg += tracelog("It's memory card , don't send APDU !\n")
            }
        } else if (model_Receiveytes.get(0) == 0x80.toByte() && model_Receiveytes.get(7).toInt() == 0x42) {
            msg+=tracelog("No Card !\n")
            msg+=tracelog("沒有插卡!\n")
        } else if (model_Receiveytes.get(0) == 0x80.toByte() && model_Receiveytes.get(7).toInt() == 0x41) {
            msg+=tracelog("Connect Card Fail !\n")
            msg+=tracelog("卡片有問題!\n")
        } else {
            msg+=tracelog("Connect Card Fail2 !\n")
            msg+=tracelog("卡片有問題!\n")
        }
        //-------------------------

        return msg
    }

    public fun cmdPowerOFF(): String {
        var ret=-100
        var msg = ""
        val PowerOffCmd=byteArrayOf(
            0x63.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            slot.toByte(),
            sequence.toByte(),
            0x00,
            0x00,
            0x00
        )
        if (model_DeviceConnection == null) {
            return "No Reader Found !"
        }
        //-------------------------
        msg = access_Usbdevice(model_DeviceConnection, PowerOffCmd)
        //-------------------------

        // 2. 接收Reader回傳資料
        // 回傳資料
        sequence=(sequence + 1) % 0xFF
        if (model_Receiveytes.get(0) == 0x81.toByte() && model_Receiveytes.get(7).toInt() == 0x01) {
            msg += tracelog("Disconnect Card OK !")
        }
        else if (model_Receiveytes.get(0) == 0x81.toByte() && model_Receiveytes.get(7).toInt() == 0x02)
            msg += tracelog("No Card !")
        else
            msg += tracelog("Disconnect Card Fail2 !")
        //-------------------------

        return msg
    }
}
package com.tech.usbdevtest

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    /* USB class */
    private lateinit var model_UsbCr: UsbCardreader

    /* UI elements */
    private lateinit var statusView: TextView
    private lateinit var resultView: TextView

    private lateinit var sysmsg: String
    /* Helpers to display user content */
    private fun printDeviceDescription(device: UsbDevice) {
        val result = device.describeContents().toString() + "\n\n"
        printResult(result)
    }

    private fun printStatus(status: String) {
        statusView.text = status
        Log.i(TAG, status)
    }

    private fun printResult(result: String) {
        resultView.text = result
        Log.i(TAG, result)
    }
    private fun showResult(result: StringBuilder) {
        resultView.text = result
    }

    /**
     * Determine whether to list all devices or query a specific device from
     * the provided intent.
     * @param intent Intent to query.
     */
    private fun handleIntent(intent: Intent) {
        // connect to usb device
        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        if (device != null) {
            printStatus(getString(R.string.status_added))
            model_UsbCr.printDeviceDetails(device)
        } else {
            // List all devices connected to USB host on startup
            printStatus(getString(R.string.status_list))
            //printDeviceList()
            var usb_ok = model_UsbCr.detectCardreader()
            printResult(resultView.text.toString() + model_UsbCr.model_Msg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.text_status)
        resultView = findViewById(R.id.text_result)

        // 取得 USB Manager
        // 取得 USB 裝置清單
        model_UsbCr = UsbCardreader()
        model_UsbCr.usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        // 建立 授權推播訊息
        var filter = model_UsbCr.initFilter(this)
        registerReceiver(model_UsbCr.usbReceiver, filter)
        printResult("App start...\n\n")
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(model_UsbCr.usbReceiver)
    }
}
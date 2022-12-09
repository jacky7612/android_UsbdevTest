package com.tech.usbdevtest

import android.content.*
import android.content.ContentValues.TAG
import android.hardware.usb.*
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible


class MainActivity : AppCompatActivity() {
    /* USB class */
    private lateinit var model_UsbCr: UsbCardreader
    private          val MAX_LINES = 100
    private          var usb_prepare_succeed: Boolean = false
    private          var model_count = 0
    private lateinit var model_api: ApiInfo

    /* UI elements */
    private lateinit var statusView: TextView
    private lateinit var resultView: TextView

    private lateinit var sysmsg: String
    private lateinit var cardID: TextView
    private lateinit var Name: TextView
    private lateinit var Identity: TextView
    private lateinit var Birthday: TextView
    private lateinit var Sex: TextView

    private lateinit var btnClearLog: Button
    private lateinit var btnReadCard: Button
    private lateinit var model_webview: WebView

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

    private fun clear_info()
    {
        statusView.text = "clear info"
        cardID.text     = ""
        Name.text       = ""
        Identity.text   = ""
        Birthday.text   = ""
        Sex.text        = ""
    }
    private fun Proc()
    {
        var msg = ""

        try {
            clear_info()
            resultView.text = ""
            if (model_UsbCr.model_initCardreader_Succeed == false) {
                msg+=model_UsbCr.tracelog("EZ110PX Card Reader is null\n")
                return
            }
            if (model_UsbCr.model_Plugin == true) {
                msg+=model_UsbCr.tracelog(model_UsbCr.readHealthCardData())
                cardID.text     = "\t\t" + model_UsbCr.model_cardID
                Name.text       = "\t\t" + model_UsbCr.model_Name
                Identity.text   = "\t\t" + model_UsbCr.model_Identity
                Birthday.text   = "\t\t" + model_UsbCr.model_Birthday
                Sex.text        = "\t\t" + model_UsbCr.model_Sex
            }
        } catch (e: IllegalArgumentException) {
            msg+=model_UsbCr.tracelog("btnReadCard :$e\n")
        } finally {
            if (model_UsbCr.model_Plugin)
                msg+=model_UsbCr.tracelog("偵測到卡片!\n")
            else
                msg+=model_UsbCr.tracelog("請插入卡片!\n")
            printResult(msg)
        }
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
            printResult(model_UsbCr.printDeviceDetails(device))
        }
        try {
            // List all devices connected to USB host on startup
            printStatus(getString(R.string.status_list))
            usb_prepare_succeed=model_UsbCr.initCardreader()
            printResult(resultView.text.toString() + model_UsbCr.model_Msg)
        } catch (e: IllegalArgumentException) {
            printResult("Invalid Exception error :$e")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usb_prepare_succeed = false
        statusView = findViewById(R.id.st_status)
        resultView = findViewById(R.id.st_response)
        resultView.movementMethod = ScrollingMovementMethod()
        resultView.maxLines = MAX_LINES
        resultView.text = ""

        cardID          = findViewById(R.id.info_cardID)
        Name            = findViewById(R.id.info_Name)
        Identity        = findViewById(R.id.info_Identity)
        Birthday        = findViewById(R.id.info_Birthday)
        Sex             = findViewById(R.id.info_Sex)
        model_webview   = findViewById(R.id.webview)
        //btnClearLog = findViewById(R.id.btclear)
        //btnReadCard = findViewById(R.id.btsend)

        // 取得 USB Manager
        // 取得 USB 裝置清單
        model_UsbCr = UsbCardreader()
        model_UsbCr.model_usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        // 建立 授權推播訊息
        var filter = model_UsbCr.initFilter(this)
        registerReceiver(model_UsbCr.usbReceiver, filter)
        printResult("App start...\n\n")
        handleIntent(intent)

        model_api = ApiInfo()
        model_webview.loadUrl(model_api.model_url)
        model_webview.isVisible = false
        if (usb_prepare_succeed == false) return
        var msg = ""
        // 執行於Background Thread
        Thread(Runnable {
            while(true) {
                Thread.sleep(500)
                msg+=model_UsbCr.tracelog(model_UsbCr.detectCardreader())
                if (model_UsbCr.model_Plugin != model_UsbCr.model_PrevPlugin) {
                    Proc()
                    try {
                        if (model_webview.isVisible) {
                            model_webview.loadUrl(model_api.model_url)
                        }
                    } catch (e:Exception) {

                    }
                    model_UsbCr.model_PrevPlugin = model_UsbCr.model_Plugin
                }
            }
        }).start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(model_UsbCr.usbReceiver)
    }

    fun btnClear_Click(view: View) {
        try {
            statusView.text = "clear log"
            resultView.text = ""
        } catch (e: Exception) {
        }
    }
}
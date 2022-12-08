package com.tech.usbdevtest

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL

class ApiInfo {
    public val model_url = "https://www.google.com.tw/"
    // 測試機：https://clinic.healthme.com.tw/
    // 依德正式機：https://stj.jotangi.net/
    // 佑聖診所：http://203.145.215.204/
    private val model_URL001="https://clinic.healthme.com.tw/"
    private val model_URL002="clinic1/api/inquiryreg.php"

    // clinic1: 公司內部
    // clinic2: 診所
    // sy: 佑聖
    private val RRI_ID="?PID=PartnerTest&CCodes=3543091231&ID="
    private val RRI_BDate="&BDate="

    // 01, 02, 03, ...
    private val RRI_MCode="&MCode="

    // 網路掛號/預約掛號查詢
    private val TASK_RRI="TASK_RRI"

    // 檔案名稱
    private val API_PARAMETRIC="API"

    // MCode
    private val MCode="MCode"

    // 網路掛號/預約掛號查詢
    fun queryHealthData(PID: String?, CCodes: String?, ID: String,
                        BDate: String, MCode: String)
    {
        val url = URL(model_URL001 + model_URL002)
        val postData =  RRI_ID.toString()       + ID    +
                        RRI_BDate.toString()    + BDate +
                        RRI_MCode.toString()    + MCode

        val conn = url.openConnection()
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("Content-Length", postData.length.toString())

        DataOutputStream(conn.getOutputStream()).use { it.writeBytes(postData) }
        BufferedReader(InputStreamReader(conn.getInputStream())).use { bf ->
            var line: String?
            while (bf.readLine().also { line = it } != null) {
                println(line)
            }
        }
    }
}
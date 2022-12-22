package com.tech.usbdevtest

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL

class ApiInfo {
    // 測試機：https://clinic.healthme.com.tw/
    // 依德正式機：https://stj.jotangi.net/
    // 佑聖診所：http://203.145.215.204/
    public val model_URL001="https://clinic.healthme.com.tw/"
    public val model_URL002="clinic1/api/inquiryreg.php"

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

    companion object {
        // api url
        const val URL = "https://clinic.healthme.com.tw/clinic1/cliniclist2.php?MCode=01"

        // 網路掛號/預約掛號查詢
        const val TASK_RRI="TASK_RRI"

        // clinic1: 公司內部
        // clinic2: 診所
        // sy: 佑聖
        const val RRI_ID="?PID=PartnerTest&CCodes=3543091231&ID="
        const val RRI_BDate="&BDate="

        // 01, 02, 03, ...
        const val RRI_MCode="&MCode="

        // 檔案名稱
        const val API_PARAMETRIC="API"

        // MCode
        const val MCode="MCode"
    }
}
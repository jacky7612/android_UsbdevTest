package com.tech.usbdevtest;

import android.util.Log;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiConnect {

    private String TAG = getClass().getSimpleName() + "(TAG)";

    public interface resultListener {
        void onSuccess(String message);

        void onFailure(String task, String message);
    }

    private resultListener listener;

    public String runTask = "";

    // 網路掛號/預約掛號查詢
    public void reserveRegisterInquiry(
            String PID,
            String CCodes,
            String ID,
            String BDate,
            String MCode,
            resultListener listener
    ) {

        this.listener = listener;
        runTask = ApiInfo.TASK_RRI;


        String url = ApiInfo.URL +
                ApiInfo.RRI_ID + ID +
                ApiInfo.RRI_BDate + BDate +
                ApiInfo.RRI_MCode + MCode;
        Log.d(TAG, "URL: " + url);

        Request request = new Request.Builder()
                .url(url).method("GET", null)
                .build();

        runEnqueue(request);
    }

    private void runEnqueue(Request request) {

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "------------------- " + runTask + " onFailure" + " -------------------");
                e.printStackTrace();

                listener.onFailure(runTask, "連線失敗");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                ResponseBody responseBody = response.body();

                if (responseBody != null) {
                    String body = responseBody.string();
                    Log.w(TAG, runTask + " - body: " + body);

                    processBody(body);
                } else {

                    listener.onFailure(runTask, "無回傳資料");
                }
            }
        });

    }

    private void processBody(String body) {

        switch (runTask) {
            case ApiInfo.TASK_RRI:
                taskRri_reserveRegisterInquiry(body);
                break;
        }
    }

    // 網路掛號/預約掛號查詢
    private void taskRri_reserveRegisterInquiry(String body) {

        try {
            // {"status":"true","code":"0x0201","responseMessage":"查無預掛資料!"}
            JSONObject jsonObject = new JSONObject(body);
            String status = jsonObject.getString("status");
            String responseMessage = jsonObject.getString("responseMessage");

            if ("true".equals(status)) {
                // 預掛識別碼
                listener.onSuccess(responseMessage);
            } else {
                listener.onFailure("失敗", responseMessage);
            }

        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailure("失敗", "解析錯誤");
        }

    }

}

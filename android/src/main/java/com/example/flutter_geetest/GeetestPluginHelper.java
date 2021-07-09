package com.example.flutter_geetest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.geetest.sdk.GT3ConfigBean;
import com.geetest.sdk.GT3ErrorBean;
import com.geetest.sdk.GT3GeetestUtils;
import com.geetest.sdk.GT3Listener;


import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.flutter.plugin.common.MethodChannel;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;


@SuppressWarnings("ConstantConditions")
class GeetestPluginHelper {
    private Activity        mActivity;
    private GT3GeetestUtils gt3GeetestUtils;
    private GT3ConfigBean   gt3ConfigBean;
    private Handler         mHandler = new Handler(Looper.getMainLooper());
    private ProgressDialog  mLoading;

    GeetestPluginHelper(Activity activity) {
        this.mActivity = activity;
    }

    String sdkVersion() {
        return GT3GeetestUtils.getVersion();
    }

    private String mApi2;

    // return {"msg":"xxxx", data:{"xxx":"xxx"}};
    void launchGeetest(final String api1, String api2, String gt, String challenge, int success, final MethodChannel.Result result) {
        try {
            if(mActivity == null) {
                result.success(errorString("Host activity has been destroy."));
                return;
            }
            gt3GeetestUtils = new GT3GeetestUtils(mActivity);
            this.mApi2 = api2;
            //api1 - Not empty, SDK handles api1 requests on behalf of
            if(api1 != null && api1.length() > 0) {
                // Configuration parameter
                initConfigBean(result);
                //request api1 - Interface and pull up the pop-up window
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        LogUtil.log("request api1 start");
                        final String api1Result = requestGet(api1);
                        LogUtil.log("response api1: "+api1Result);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                JSONObject jsonObject = null;
                                try {jsonObject = new JSONObject(api1Result); } catch(Exception ignored) { }
                                if(jsonObject == null) {
                                    gt3GeetestUtils.showFailedDialog();
                                    result.success(errorString("api1 request error."));
                                } else {
                                    // SDK - The recognizable format is
                                    // {"success":1,"challenge":"06fbb267def3c3c9530d62aa2d56d018","gt":"019924a82c70bb123aae90d483087f94"}
                                    gt3ConfigBean.setApi1Json(jsonObject);
                                    gt3GeetestUtils.getGeetest();
                                }
                            }
                        });
                    }
                }).start();
            }
            //api1 - The result parameter is not empty，SDK - Call up the verification popup directly
            else if(gt.length() > 0 && challenge.length() > 0 && success != -1) {
                // Configuration parameter
                initConfigBean(result);
                // Pull up the pop-up window directly
                // SDK {"success":1,"challenge":"06fbb267def3c3c9530d62aa2d56d018","gt":"019924a82c70bb123aae90d483087f94"}
                try {
                    JSONObject parmas = new JSONObject();
                    parmas.put("gt", gt);
                    parmas.put("challenge", challenge);
                    parmas.put("success", success);
                    gt3ConfigBean.setApi1Json(parmas);
                    gt3GeetestUtils.getGeetest();
                } catch(Exception ignored) {}
            }
      
            else {
                result.success(errorString("Parameter error, please check the parameter"));
            }
        } catch(Exception e) {
            LogUtil.log(e.getMessage());
            result.success(errorString((e.getMessage())));
        }
    }


    private void initConfigBean(final MethodChannel.Result resultCallback) {
        
        gt3ConfigBean = new GT3ConfigBean();
        
        // Set how captcha is presented，1：bind，2：unbind
        gt3ConfigBean.setPattern(1);
        
        // The default is false
        gt3ConfigBean.setCanceledOnTouchOutside(false);

        // Set language. Use system default language if null
        gt3ConfigBean.setLang(null);
        
        // Set the timeout for loading webview static files
        gt3ConfigBean.setTimeout(10000);
       
        // Set the timeout for webview request after user finishing the CAPTCHA verification. The default is 10,000
        gt3ConfigBean.setWebviewTimeout(10000);
      
        // Set callback listener
        gt3ConfigBean.setListener(new GT3Listener() {

    /**
     * CAPTCHA loading is completed
     * @param duration Loading duration and version info，in JSON format
     */
            @Override
            public void onDialogReady(String duration) {
                LogUtil.log("GT3BaseListener-->onDialogReady-->"+duration);
            }

     /**
     * Verification result callback
     * @param code 1:success, 0:fail
     */
            @Override
            public void onReceiveCaptchaCode(int code) {
                // Log.e(TAG, "GT3BaseListener-->onReceiveCaptchaCode-->" + code);
            }

      /**
      * api2 custom call
      * @param result
      */
            @Override
            public void onDialogResult(final String result) {
                LogUtil.log("GT3BaseListener-->onDialogResult-->"+result);

                if(!TextUtils.isEmpty(mApi2)) {
                    if(mLoading == null) mLoading = ProgressDialog.show(mActivity, null, "加载中");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.log("request api2 start");
                            final String api2Result = requestPost(mApi2, result);
                            LogUtil.log("response api2: "+api2Result);
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mLoading.dismiss(); mLoading = null;
                                    gt3GeetestUtils.showSuccessDialog();
                                    resultCallback.success(successString(api2Result));
                                }
                            });
                        }
                    }).start();
                } else {
                    gt3GeetestUtils.showSuccessDialog();
                    resultCallback.success(successString(result));
                }
            }

     /**
     * Statistic info.
     * @param result
     */
            @Override
            public void onStatistics(String result) {
                LogUtil.log("GT3BaseListener-->onStatistics-->"+result);
            }

    /**
     * Close the CAPTCHA
     * @param num 1 Click the close button to close the CAPTCHA, 2 Click anyplace on screen to close the CAPTCHA, 3 Click return button the close
     */
            @Override
            public void onClosed(int num) {
                LogUtil.log("GT3BaseListener-->onClosed-->"+num);
            }

    /**
     * Verfication succeeds
     * @param result
     */
            @Override
            public void onSuccess(String result) {
                LogUtil.log("GT3BaseListener-->onSuccess-->"+result);
            }

    /**
     * Verification fails
     * @param errorBean Version info, error code & description, etc.
     */
            @Override
            public void onFailed(GT3ErrorBean errorBean) {
                // Log.e(TAG, "GT3BaseListener-->onFailed-->" + errorBean.toString());
            }

     /**
     * api1 custom call
     */
            @Override
            public void onButtonClick() { }
        });

        gt3GeetestUtils.init(gt3ConfigBean);

        // Start CAPTCHA verification
        gt3GeetestUtils.startCustomFlow();
    }

    // return {"msg":"xxxx", data:{"xxx":"xxx"}};
    private String errorString(String msg) {
        try {
            JSONObject result = new JSONObject();
            result.put("msg", msg);
            result.put("data", null);
            return result.toString();
        } catch(Exception ignored) {}
        return "";
    }

    // return {"msg":"xxxx", data:{"xxx":"xxx"}};
    private String successString(String datas) {
        try {
            JSONObject result = new JSONObject();
            result.put("msg", "");
            result.put("data", datas);
            return result.toString();
        } catch(Exception ignored) {}
        return "";
    }


    private static String requestPost(String urlString, String postParam) {
        MediaType   mediaType   = MediaType.parse("application/json");
        RequestBody requestBody = RequestBody.create(mediaType, postParam);
        Request     request     = new Request.Builder().post(requestBody).url(urlString).build();
        try {
            Response response = httpClient().newCall(request).execute();
            return response.body().string();
        } catch(Exception ignored) {}
        return null;
    }

    private static String requestGet(String urlString) {
        Request request = new Request.Builder().url(urlString).build();
        try {
            Response response = httpClient().newCall(request).execute();
            return response.body().string();
        } catch(Exception ignored) {}
        return null;
    }

    private static OkHttpClient httpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.followRedirects(true);
        builder.followSslRedirects(true);
        builder.retryOnConnectionFailure(false);
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(30, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);
        try {
            SSLContext                 sc              = SSLContext.getInstance("TLS");
            AllTrustedX509TrustManager trustAllManager = new AllTrustedX509TrustManager();
            sc.init(null, new TrustManager[]{trustAllManager}, new SecureRandom());
            builder.sslSocketFactory(sc.getSocketFactory(), trustAllManager);
            builder.hostnameVerifier(new AllTrustedHostnameVerifier());
        } catch(Exception ignored) { }
        return builder.build();
    }

    private static class AllTrustedHostnameVerifier implements HostnameVerifier {
        @SuppressLint("BadHostnameVerifier")
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private static class AllTrustedX509TrustManager implements X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
    }
}

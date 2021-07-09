package com.example.flutter_geetest;

import android.app.Activity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;


public class FlutterGeetestPlugin implements MethodCallHandler {
   
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_geetest");
        channel.setMethodCallHandler(new FlutterGeetestPlugin(registrar.activity()));
    }

    private Activity            mActivity;
    private GeetestPluginHelper mHelper;

    private FlutterGeetestPlugin(Activity activity) {
        mActivity = activity;
        mHelper = new GeetestPluginHelper(mActivity);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if(mHelper == null) mHelper = new GeetestPluginHelper(mActivity);
        String method = call.method;
        LogUtil.log("onMethodCall: method="+method);
       
        if(method.equals("getPlatformVersion")) {
            result.success(mHelper.sdkVersion());
        }
        
        else if(method.equals("launchGeetest")) {
            String api1      = call.hasArgument("api1") ? (String) call.argument("api1") : "";
            String api2      = call.hasArgument("api2") ? (String) call.argument("api2") : "";
            String gt        = call.hasArgument("gt") ? (String) call.argument("gt") : "";
            String challenge = call.hasArgument("challenge") ? (String) call.argument("challenge") : "";
            int    success   = call.hasArgument("success") ? (int) call.argument("success") : -1;
            mHelper.launchGeetest(api1, api2, gt, challenge, success, result);
        } else {
            result.notImplemented();
        }
    }
}

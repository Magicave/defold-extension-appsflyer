package com.defold.appsflyer;

import android.util.Log;
import android.app.Activity;

import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AFAdRevenueData;
import com.appsflyer.MediationNetwork;

import java.util.Map;

import org.json.JSONObject;
import org.json.JSONException;

public class AppsflyerJNI {
    private static final String TAG = "AppsflyerJNI";

    public static native void appsflyerAddToQueue(int msg, String json);

    private static final int CONVERSION_DATA_SUCCESS = 1;
    private static final int CONVERSION_DATA_FAIL = 2;

    private Activity activity;
    private boolean isInitialized;
    private boolean debugLogEnabled;

    public AppsflyerJNI(Activity activity) {
        this.activity = activity;
        this.isInitialized = false;
        this.debugLogEnabled = false;
    }

    public void initializeSDK(final String key) {
        if (isInitialized) {
            Log.d(TAG, "Initialize SDK skipped: already initialized");
            return;
        }
        Log.d(TAG, "Initialize SDK");
        AppsFlyerConversionListener conversionDataListener =
                new AppsFlyerConversionListener() {
                    @Override
                    public void onConversionDataSuccess(Map<String, Object> conversionData) {
                        Log.d(TAG, "onConversionDataSuccess");
                        try {
                            JSONObject obj = new JSONObject();
                            for (Map.Entry<String, Object> entry : conversionData.entrySet()) {
                                Object value = entry.getValue();
                                if (value != null) {
                                    obj.put(entry.getKey(), value);
                                }
                            }
                            appsflyerAddToQueue(CONVERSION_DATA_SUCCESS, obj.toString());
                        } catch (JSONException e) {
                            Log.e(TAG, "Unable to encode message data: " + e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onConversionDataFail(String errorMessage) {
                        Log.d(TAG, "onConversionDataFail: " + errorMessage);
                        try {
                            JSONObject obj = new JSONObject();
                            obj.put("error", errorMessage);
                            appsflyerAddToQueue(CONVERSION_DATA_FAIL, obj.toString());
                        } catch (JSONException e) {
                            Log.e(TAG, "Unable to encode CONVERSION_DATA_FAIL message data: " + e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onAppOpenAttribution(Map<String, String> conversionData) {
                        Log.d(TAG, "onConversionDataSuccess");
                    }

                    @Override
                    public void onAttributionFailure(String errorMessage) {
                        Log.d(TAG, "onConversionDataFail: " + errorMessage);
                    }
                };
        AppsFlyerLib.getInstance().init(key, conversionDataListener, activity.getApplicationContext());
        AppsFlyerLib.getInstance().setDebugLog(debugLogEnabled);
        isInitialized = true;
    }

    public void startSDK() {
        if (!isInitialized) {
            Log.d(TAG, "Start SDK skipped: initializeSDK not called yet");
            return;
        }
        Log.d(TAG, "Start SDK");
        AppsFlyerLib.getInstance().start(activity);
    }

    public void setDebugLog(boolean is_enable) {
        debugLogEnabled = is_enable;
        Log.d(TAG, "Set debug log: " + String.valueOf(is_enable));
        if (isInitialized) {
            AppsFlyerLib.getInstance().setDebugLog(is_enable);
        }
    }

    public void logEvent(String eventName, Map<String, Object> eventValue) {
        Log.d(TAG, "Log event: " + eventName);
        AppsFlyerLib.getInstance().logEvent(activity, eventName, eventValue);
    }

    public void logAdRevenue(String monetizationNetwork, String mediationNetworkStr, String currencyIso4217Code, double eventRevenue, Map<String, Object> additionalParameters) {
        Log.d(TAG, "logAdRevenue: " + eventRevenue + " " + currencyIso4217Code);

        // Map String input to MediationNetwork Enum
        // Defaulting to AdMob as that is your primary use case, but logic exists for others
        MediationNetwork mediationNetwork = MediationNetwork.GOOGLE_ADMOB;
        
        if ("ironsource".equalsIgnoreCase(mediationNetworkStr)) {
            mediationNetwork = MediationNetwork.IRONSOURCE;
        } else if ("applovin".equalsIgnoreCase(mediationNetworkStr)) {
            mediationNetwork = MediationNetwork.APPLOVIN_MAX;
        } else if ("unity".equalsIgnoreCase(mediationNetworkStr)) {
            mediationNetwork = MediationNetwork.UNITY;
        }
        
        AFAdRevenueData adRevenueData = new AFAdRevenueData(
            monetizationNetwork,
            mediationNetwork,
            currencyIso4217Code,
            eventRevenue
        );

        AppsFlyerLib.getInstance().logAdRevenue(adRevenueData, additionalParameters);
    }

    public void setCustomerUserId(String userId) {
        Log.d(TAG, "Set customer user id: " + userId);
        AppsFlyerLib.getInstance().setCustomerUserId(userId);
    }

    public String getAppsFlyerUID() {
        return AppsFlyerLib.getInstance().getAppsFlyerUID(activity);
    }
}

package com.crossdrives.msgraph;

import android.content.Context;
import android.content.SharedPreferences;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.IAuthenticationResult;

public class SharedPrefsUtil {
    public static final String PREF_AUTH_TOKEN = "PREF_AUTH_TOKEN";
    public static final String PREF_USER_TENANT = "PREF_USER_TENANT";
    public static final String PREF_USER_ID = "PREF_USER_ID";

    public static SharedPreferences getSharedPreferences() {
        return SnippetApp.getApp().getSharedPreferences(AppModule.PREFS, Context.MODE_PRIVATE);
    }

    public static void persistUserID(AuthenticationResult result) {
        setPreference(PREF_USER_ID, result.getAccount().getUsername());
    }

    public static void persistAuthToken(IAuthenticationResult result) {
        setPreference(PREF_AUTH_TOKEN, result.getAccessToken());
    }

    public static void persistUserTenant(String tenant) {
        getSharedPreferences().edit().putString(PREF_USER_TENANT, tenant).commit();
    }

    private static void setPreference(String key, String accessToken) {
        getSharedPreferences().edit().putString(key, accessToken).commit();
    }
}

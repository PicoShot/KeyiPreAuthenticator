package com.keyipre.authenticator;


import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private String currentVersion = "1.1.0";
    private static final String UPDATE_CHECK_URL = "https://www.keyipre.com.tr/statics/apis/app_update_api.php";
    private WebView myWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        new UpdateCheckTask().execute();

        myWebView = findViewById(R.id.myWebView);
        myWebView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);

        CookieManager cookieManager = CookieManager.getInstance();
        String cookieString = "action=app; path=/ ";
        cookieManager.setCookie("https://www.keyipre.com.tr", cookieString);


        loadPreviousUrl();

    }

    @Override
    protected void onPause() {
        super.onPause();
        myWebView.saveState(new Bundle());
        CookieSyncManager.getInstance().sync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreWebViewState();
    }

    private void loadPreviousUrl() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String lastUrl = preferences.getString("lastUrl", null);

        if (lastUrl != null) {
            myWebView.loadUrl(lastUrl);
        } else {
            String myWebsiteURL = "https://www.keyipre.com.tr/statics/pages/app_login";
            myWebView.loadUrl(myWebsiteURL);
        }
    }

    private void saveUrl(String url) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lastUrl", url);
        editor.apply();
    }

    private void restoreWebViewState() {
        if (myWebView.restoreState(new Bundle()) == null) {
            loadPreviousUrl();
        }
    }



    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            saveUrl(url);
            CookieSyncManager.getInstance().sync();
        }
    }

    private class UpdateCheckTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("version", currentVersion)
                    .build();
            Request request = new Request.Builder()
                    .url(UPDATE_CHECK_URL)
                    .post(formBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                if (response != null) {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean updateAvailable = jsonResponse.getBoolean("status");
                    String updateUrl = jsonResponse.getString("url");
                    String lastVersion = jsonResponse.getString("version");
                    boolean forceUpdate = jsonResponse.getBoolean("force");

                    if (updateAvailable) {
                        Toast.makeText(MainActivity.this, "Update Available! New Version:" + lastVersion, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                        startActivity(intent);
                        if (forceUpdate) {
                            Toast.makeText(MainActivity.this, "Please Update app", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else
                        Toast.makeText(MainActivity.this, "App Is Updated! Version:" + lastVersion, Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(MainActivity.this, "Failed to Check Update", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Failed to Check Update", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

}
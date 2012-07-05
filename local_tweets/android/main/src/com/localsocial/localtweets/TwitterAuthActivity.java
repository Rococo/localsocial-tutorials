package com.localsocial.localtweets;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.localsocial.LocalSocial;
import com.localsocial.LocalSocialFactory;
import com.localsocial.json.Server;
import com.localsocial.oauth.AccessToken;
import com.localsocial.remote.exception.LocalSocialError;
import com.localsocial.remote.exception.UnauthorizedException;
import com.localsocial.remote.http.Response;

public class TwitterAuthActivity extends Activity implements View.OnClickListener {
	
	private String TAG = getClass().getName();
	private boolean d = true;
	
	private WebView m_wv;
    private Handler m_handler = new Handler();
    private Executor m_executor = Executors.newSingleThreadExecutor();
    private TwitterWebViewClient m_twitter;
    private String m_auth = null;

    private String m_network = null;
    private String m_api;
    private View m_web;

    private Button m_accept;
    private Button m_cancel;
    
    public static final int RESULT_LOCALSOCIAL_ERROR = RESULT_FIRST_USER;

	/**
     * @param activity
     * @param url
     * @param name
     * @param code
     */
    public static void startAuthTwitter(Activity activity, String url, String name, int code) {
        Intent intent = new Intent(activity, TwitterAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        LocalSocial ls = LocalSocialFactory.getLocalSocial();

        intent.putExtra("URL", url);
        intent.putExtra("NETWORK", "twitter");
        intent.putExtra("NAME", name);
        intent.putExtra("API", ls.getConfig().getBase());
        activity.startActivityForResult(intent, code);
    }
    
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        m_accept = new Button(this.getBaseContext());
        m_accept.setOnClickListener(this);
        m_cancel = new Button(this.getBaseContext());
        m_cancel.setOnClickListener(this);

        m_auth = getIntent().getStringExtra("URL");
        m_network = getIntent().getStringExtra("NETWORK");
        setContentView(R.layout.twitter_auth);
        LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.FILL_PARENT;
        params.width = LayoutParams.FILL_PARENT;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        m_web = findViewById(R.id.web);

        m_wv = (WebView) findViewById(R.id.webview);
        m_twitter = new TwitterWebViewClient();
        m_wv.setWebViewClient(m_twitter);
        m_wv.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        m_wv.getSettings().setJavaScriptEnabled(true);
        // Add the interface to record javascript events
        m_wv.addJavascriptInterface(m_accept, "accept");
        m_wv.addJavascriptInterface(m_cancel, "cancel");

        m_wv.clearCache(true);
        m_wv.clearFormData();
    }
    
    /**
     * Called when-ever the activity is displayed. The order of execution is :
     */
    @Override
    protected void onStart() {
        super.onStart();
        m_api = getIntent().getStringExtra("API");
        loadWebPage("file:///android_asset/localsocial_twitter.html");
    }

    private void loadWebPage(final String location) {
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                m_wv.loadUrl(location);
            }
        });
    }
    
    private void doFinish(int resultCode, Intent intent) {
        Log.i(TAG, "doFinish : resultCode = " + resultCode);
        if (null != intent) {
            setResult(resultCode, intent);
        } else {
            setResult(resultCode);
        }
        finish();
    }
    
    @Override
    public void onClick(View view) {
        if (view.equals(m_accept)) {
            System.out.println("TwitterAuthActivity.onClick - ACCEPT");
            doAuth();
        } else if (view.equals(m_cancel)) {
            System.out.println("TwitterAuthActivity.onClick - CANCEL");
            doFinish(RESULT_CANCELED, null);
        }
    }
    
    private void doAuth() {
        m_executor.execute(new Runnable() {

            @Override
            public void run() {
                LocalSocial ls = LocalSocialFactory.getLocalSocial();
                AccessToken token;
                try {
                    token = ls.getAccessToken();
                    String path = new StringBuffer(Server.API_VERSION_PATH).append("/networks/").append(m_network).toString();

                    CookieSyncManager.createInstance(TwitterAuthActivity.this);
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.removeSessionCookie();
                    Response response = token.post(path);

                    if (response.getStatus() == 302) {
                        String location = response.getLocation();
                        String cookie = response.getCookie();
                        Log.d(TAG, "Cookie String == " + cookie);
                        int i;
                        if (cookie != null && (i = cookie.indexOf(';')) != -1) {
                            cookie = cookie.substring(0, i);
                            if (d) Log.d(TAG, "Cookie not null");
                        } else {
                            cookie = null;
                            if (d) Log.d(TAG, "Cookie null");
                        }
                        Log.d(TAG, "Cookie == " + cookie);
                        //ToDo Either extract the domain from the cookie or set it from LS Config
                        cookieManager.setCookie(".mylocalsocial.com", cookie);
                        cookieManager.setCookie("mylocalsocial.com", cookie);
                        CookieSyncManager.getInstance().sync();
                        Log.d(TAG, "Redirecting to " + location + " :: Cookie = " + cookieManager.getCookie(location));
                        loadWebPage(location);
                    }
                } catch (LocalSocialError lsr) {
                    lsr.printStackTrace();
                    doFinish(RESULT_CANCELED, null);
                } catch (UnauthorizedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }



    private class TwitterWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!shouldContinue(view, url)) {
                return false;
            }
            view.loadUrl(url);
            return true;
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d("TwitterWebViewClient", "onPageStarted url = " + url);
            shouldContinue(view, url);
        }

        synchronized boolean shouldContinue(WebView view, String url) {
            // only send result once
            if (complete)
                return false;

            if (url.startsWith(m_api)) {
                view.setVisibility(WebView.INVISIBLE);

                Intent intent = new Intent();
                intent.putExtra("result", url);
                Log.d(TAG, "url=" + url);
                doFinish(RESULT_OK, intent);

                complete = true;
                return false;
            }
            return true;
        }
        boolean complete = false;
    }
}

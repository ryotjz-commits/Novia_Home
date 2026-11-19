package com.novia.home;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import com.novia.home.network.DeviceDiscovery;
import com.novia.home.network.ConnectionManager;
import com.novia.home.web.Phase5Bridge;
import com.novia.home.web.Phase6Bridge;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String DASHBOARD_URL = "https://ryotjz-commits.github.io/NOVIA-HOME-V4-DASHBOARD/";
    private static final String AP_FALLBACK_URL = "http://192.168.4.1/";
    private WebView webView;
    private ProgressBar progress;
    private TextView offline;
    private TextView connection;
    private DeviceDiscovery discovery;
    private ConnectionManager connectionManager;
    private String localUrl = null;
    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean discoveryRunning = false;
    private boolean destroyed = false;
    private String currentMode = "STARTING";
    private Phase6Bridge phase6Bridge;
    private android.widget.Button relayControlButton;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(17,25,26));
        getWindow().setNavigationBarColor(Color.rgb(17,25,26));
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(17,25,26));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        connection = new TextView(this);
        connection.setText("NOVIA HOME · menyiapkan koneksi… (ketuk untuk scan ulang)");
        connection.setTextColor(Color.rgb(190,220,203)); connection.setTextSize(12);
        connection.setTypeface(Typeface.DEFAULT, Typeface.BOLD); connection.setGravity(Gravity.CENTER);
        connection.setPadding(12,12,12,12); connection.setBackgroundColor(Color.rgb(31,47,40));
        connection.setOnClickListener(v -> startDiscovery());
        content.addView(connection, new LinearLayout.LayoutParams(-1,-2));
        LinearLayout quickActions = new LinearLayout(this); quickActions.setOrientation(LinearLayout.HORIZONTAL);
        android.widget.Button mainDashboardButton = new android.widget.Button(this); mainDashboardButton.setText("Dashboard");
        mainDashboardButton.setOnClickListener(v -> { if(localUrl!=null) webView.loadUrl(localUrl); else webView.loadUrl(DASHBOARD_URL); });
        relayControlButton = new android.widget.Button(this); relayControlButton.setText("Kontrol Relay Lokal"); relayControlButton.setEnabled(false);
        relayControlButton.setOnClickListener(v -> { if(localUrl!=null) webView.loadUrl("file:///android_asset/local_relay_control.html"); });
        quickActions.addView(mainDashboardButton,new LinearLayout.LayoutParams(0,-2,1)); quickActions.addView(relayControlButton,new LinearLayout.LayoutParams(0,-2,1));
        content.addView(quickActions,new LinearLayout.LayoutParams(-1,-2));
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true); settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true); settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.addJavascriptInterface(new Phase5Bridge(new Phase5Bridge.Actions() {
            @Override public void scan() { runOnUiThread(() -> startDiscovery()); }
            @Override public void openLocal() { runOnUiThread(() -> { String target = localUrl != null ? localUrl : AP_FALLBACK_URL; webView.loadUrl(target); }); }
            @Override public void openOnline() { runOnUiThread(() -> webView.loadUrl(DASHBOARD_URL)); }
        }), "NoviaApp");
        phase6Bridge = new Phase6Bridge("");
        webView.addJavascriptInterface(phase6Bridge, "NoviaRelay");
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                progress.setVisibility(View.VISIBLE); offline.setVisibility(View.GONE);
            }
            @Override public void onPageFinished(WebView view, String url) { progress.setVisibility(View.GONE); }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && !request.getUrl().toString().startsWith("file:///android_asset/")) showOfflineShell();
            }
        });
        content.addView(webView, new LinearLayout.LayoutParams(-1,0,1));
        root.addView(content, new FrameLayout.LayoutParams(-1,-1));
        progress = new ProgressBar(this); FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-2,-2); pp.gravity=Gravity.CENTER; root.addView(progress,pp);
        offline = new TextView(this); offline.setText("NOVIA HOME\n\nDashboard online tidak tersedia.\n\nPerangkat lokal belum terdeteksi. Ketuk tombol di bawah untuk mencari ulang.");
        offline.setTextColor(Color.WHITE); offline.setTextSize(16); offline.setGravity(Gravity.CENTER); offline.setPadding(24,24,24,24);
        offline.setBackgroundColor(Color.rgb(17,25,26)); offline.setVisibility(View.GONE); offline.setOnClickListener(v -> startDiscovery());
        root.addView(offline,new FrameLayout.LayoutParams(-1,-1)); setContentView(root);
        if (savedInstanceState != null) webView.restoreState(savedInstanceState); else webView.loadUrl(DASHBOARD_URL);
        connectionManager = new ConnectionManager(this, new ConnectionManager.Listener() {
            @Override public void onStatus(String message, String mode) { runOnUiThread(() -> setStatus(message, mode)); }
            @Override public void onLocal(String ip, int port, JSONObject identity, boolean ap) { runOnUiThread(() -> {
                discoveryRunning = false; String name = identity.optString("name", "NOVIA HOME");
                localUrl = "http://" + ip + (port == 80 ? "" : ":" + port) + "/";
                if(phase6Bridge!=null) phase6Bridge.setBaseUrl(localUrl);
                if(relayControlButton!=null) relayControlButton.setEnabled(true);
                setStatus((ap ? "LOCAL AP · " : "LOCAL LAN · ") + name + " · " + ip, ap ? "AP" : "LOCAL");
                if (!localUrl.equals(webView.getUrl())) webView.loadUrl(localUrl); offline.setVisibility(View.GONE);
            }); }
            @Override public void onCloud() { runOnUiThread(() -> { discoveryRunning = false; if(relayControlButton!=null)relayControlButton.setEnabled(false); offline.setVisibility(View.GONE); setStatus("CLOUD/ONLINE · perangkat lokal belum ditemukan", "ONLINE"); if (webView.getUrl() == null || !DASHBOARD_URL.equals(webView.getUrl())) webView.loadUrl(DASHBOARD_URL); }); }
            @Override public void onOffline() { runOnUiThread(() -> { discoveryRunning = false; if(relayControlButton!=null)relayControlButton.setEnabled(false); showOfflineShell(); }); }
        }); setupNetworkMonitor(); startDiscovery();
    }

    private void setupNetworkMonitor() {
        connectivity = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if (connectivity == null) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) { scheduleDiscovery("Jaringan berubah · mencari NOVIA HOME…"); }
            @Override public void onLost(Network network) { runOnUiThread(() -> setStatus("Jaringan terputus · menunggu koneksi…", "OFFLINE")); scheduleDiscovery("Mencoba koneksi kembali…"); }
        };
        try { connectivity.registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), networkCallback); }
        catch (Exception ignored) {
            try { connectivity.registerDefaultNetworkCallback(networkCallback); } catch (Exception ignored2) {}
        }
    }
    private void scheduleDiscovery(String msg) { handler.removeCallbacks(retryRunnable); handler.postDelayed(retryRunnable, 1200); }
    private final Runnable retryRunnable = () -> startDiscovery();

    private void startDiscovery() {
        if (destroyed || discoveryRunning) return;
        discoveryRunning = true;
        setStatus("Mencari perangkat lokal…", "SEARCHING");
        if (connectionManager != null) connectionManager.connect();
    }
    private boolean isNetworkConnected() {
        if(connectivity==null) return false;
        Network n=connectivity.getActiveNetwork(); if(n==null) return false;
        NetworkCapabilities c=connectivity.getNetworkCapabilities(n);
        return c!=null && (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)||c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
    private void setStatus(String text,String mode) { currentMode=mode; connection.setText("NOVIA HOME · "+text+"  (ketuk untuk scan ulang)"); }
    private void showOfflineShell() { progress.setVisibility(View.GONE); offline.setVisibility(View.GONE); setStatus("OFFLINE · recovery siap","OFFLINE"); if(webView!=null && (webView.getUrl()==null || !webView.getUrl().startsWith("file:///android_asset/phase5_offline.html"))) webView.loadUrl("file:///android_asset/phase5_offline.html"); }
    @Override public void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); if(webView!=null) webView.saveState(out); }
    @Override public void onBackPressed() { if(webView!=null&&webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { destroyed=true; handler.removeCallbacks(retryRunnable); if(connectivity!=null&&networkCallback!=null) try{connectivity.unregisterNetworkCallback(networkCallback);}catch(Exception ignored){} if(discovery!=null) discovery.cancel(); if(connectionManager!=null) connectionManager.cancel(); if(webView!=null)webView.destroy(); super.onDestroy(); }
}

package id.novia.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.net.DhcpInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "novia_app";
    private static final String DEFAULT_AP_URL = "http://192.168.4.1/";
    private static final String DEFAULT_ROUTER_URL = "http://192.168.0.2/";
    private static final int BG = Color.rgb(21, 26, 29);
    private static final int SURFACE = Color.rgb(32, 40, 43);
    private static final int TEXT = Color.rgb(241, 245, 242);
    private static final int MUTED = Color.rgb(145, 163, 154);
    private static final int ACCENT = Color.rgb(131, 201, 159);

    private WebView webView;
    private TextView hostLabel;
    private ProgressBar progress;
    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback networkCallback;
    private String dashboardUrl;
    private String routerUrl;
    private String onlineUrl;
    private boolean fallbackAttempted = false;
    private boolean activityReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        w.getDecorView().setSystemUiVisibility(0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        routerUrl = prefs.getString("router_url", DEFAULT_ROUTER_URL);
        onlineUrl = prefs.getString("online_url", "");
        dashboardUrl = prefs.getString("dashboard_url", DEFAULT_AP_URL);
        // Migrate an older manually-saved non-AP URL into the router slot.
        if (prefs.contains("dashboard_url") && !isApAddress(dashboardUrl)
                && !isProbablyOnline(dashboardUrl) && !prefs.contains("router_url")) {
            routerUrl = dashboardUrl;
            prefs.edit().putString("router_url", routerUrl).apply();
        }
        buildLayout();
        activityReady = true;
        registerNetworkMonitor();
        selectDashboardForCurrentNetwork(true);
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(8), dp(10), dp(8));
        bar.setBackgroundColor(SURFACE);
        TextView brand = new TextView(this);
        brand.setText("NOVIA HOME"); brand.setTextColor(TEXT); brand.setTextSize(16);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bar.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button address = smallButton("URL");
        address.setOnClickListener(v -> showHostDialog());
        bar.addView(address);
        Button refresh = smallButton("↻"); refresh.setTextSize(20);
        refresh.setOnClickListener(v -> selectDashboardForCurrentNetwork(true));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(46), dp(42)); rp.leftMargin = dp(6);
        bar.addView(refresh, rp);
        root.addView(bar);
        hostLabel = new TextView(this); hostLabel.setTextColor(MUTED); hostLabel.setTextSize(11);
        hostLabel.setPadding(dp(14), dp(5), dp(14), dp(5)); hostLabel.setBackgroundColor(BG);
        root.addView(hostLabel);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));
        webView = new WebView(this); webView.setBackgroundColor(BG);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false); settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        settings.setSupportZoom(false); settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false); settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isAllowedUrl(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !isAllowedUrl(android.net.Uri.parse(url));
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) handleDashboardFailure();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 500) handleDashboardFailure();
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root); updateHostLabel();
    }

    private Button smallButton(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(TEXT); b.setTextSize(11);
        b.setAllCaps(false); b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(dp(8), 0, dp(8), 0);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(55, 70, 65)));
        return b;
    }

    private void showHostDialog() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(4), dp(20), 0);
        EditText routerInput = field(routerUrl, "IP ESP32 pada WiFi router, contoh 192.168.0.2");
        EditText onlineInput = field(onlineUrl, "URL dashboard online, contoh https://domain.com");
        form.addView(label("IP ESP32 – WiFi Router")); form.addView(routerInput);
        form.addView(label("URL Dashboard Online – fallback")); form.addView(onlineInput);
        new AlertDialog.Builder(this).setTitle("Pengaturan koneksi NOVIA")
            .setMessage("Aplikasi memilih dashboard otomatis: WiFi NOVIA (192.168.4.1), WiFi router (IP ESP32 tersimpan), atau URL online jika WiFi tidak tersedia / dashboard lokal gagal.")
            .setView(form).setNegativeButton("Batal", null)
            .setNeutralButton("Pakai AP NOVIA", (d, w) -> {
                dashboardUrl = DEFAULT_AP_URL; fallbackAttempted = false; loadDashboard();
            })
            .setPositiveButton("Simpan", (d, w) -> {
                String r = normalizeDashboardUrl(routerInput.getText().toString().trim());
                String oText = onlineInput.getText().toString().trim();
                String o = oText.isEmpty() ? "" : normalizeDashboardUrl(oText);
                if (r == null || !isHttpUrl(r) || ( !oText.isEmpty() && o == null)) {
                    Toast.makeText(this, "Alamat tidak valid. Gunakan IP lokal atau URL http/https.", Toast.LENGTH_LONG).show(); return;
                }
                routerUrl = r; onlineUrl = o == null ? "" : o;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("router_url", routerUrl).putString("online_url", onlineUrl).apply();
                selectDashboardForCurrentNetwork(true);
            }).show();
    }

    private EditText field(String value, String hint) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setText(value); e.setHint(hint);
        e.setTextSize(14); e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        return e;
    }
    private TextView label(String value) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(TEXT); t.setTextSize(13); t.setPadding(0, dp(10), 0, 0); return t;
    }

    private String normalizeDashboardUrl(String value) {
        if (value == null || value.trim().isEmpty() || value.contains(" ")) return null;
        String candidate = value.trim();
        if (!candidate.matches("(?i)^https?://.*"))
            candidate = (candidate.matches("^[0-9.]+(:[0-9]+)?(/.*)?$") ? "http://" : "https://") + candidate;
        try {
            android.net.Uri uri = android.net.Uri.parse(candidate);
            String scheme = uri.getScheme(); String host = uri.getHost();
            if (scheme == null || host == null || host.isEmpty()) return null;
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return null;
            if (uri.getUserInfo() != null || uri.getPort() < -1 || uri.getPort() > 65535) return null;
            return uri.toString();
        } catch (Exception e) { return null; }
    }
    private boolean isHttpUrl(String u) { return u != null && (u.startsWith("http://") || u.startsWith("https://")); }
    private boolean isAllowedUrl(android.net.Uri uri) {
        if (uri == null) return false;
        android.net.Uri base = android.net.Uri.parse(dashboardUrl);
        return base.getScheme() != null && base.getScheme().equalsIgnoreCase(uri.getScheme())
            && base.getHost() != null && base.getHost().equalsIgnoreCase(uri.getHost()) && base.getPort() == uri.getPort();
    }
    private boolean isApAddress(String url) {
        android.net.Uri u = android.net.Uri.parse(url == null ? "" : url);
        return "192.168.4.1".equals(u.getHost());
    }
    private boolean isProbablyOnline(String url) {
        android.net.Uri u = android.net.Uri.parse(url == null ? "" : url);
        String h = u.getHost();
        return h != null && !h.equals("192.168.4.1") && !h.matches("^(10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[0-1])\\.).*") && !h.equals("localhost") && !h.equals("127.0.0.1");
    }

    private void registerNetworkMonitor() {
        connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) return;
        try {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network network) { runOnUiThread(() -> selectDashboardForCurrentNetwork(false)); }
                @Override public void onLost(Network network) { runOnUiThread(() -> selectDashboardForCurrentNetwork(false)); }
                @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) { runOnUiThread(() -> selectDashboardForCurrentNetwork(false)); }
            };
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                connectivity.registerDefaultNetworkCallback(networkCallback);
            } else {
                NetworkRequest request = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
                connectivity.registerNetworkCallback(request, networkCallback);
            }
        } catch (Exception ignored) { }
    }

    private boolean hasWifiTransport() {
        if (connectivity == null) return false;
        for (Network n : connectivity.getAllNetworks()) {
            NetworkCapabilities c = connectivity.getNetworkCapabilities(n);
            if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true;
        }
        return false;
    }

    private boolean wifiGatewayIsNoviaAp() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return false;
            DhcpInfo info = wm.getDhcpInfo();
            if (info == null || info.gateway == 0) return false;
            int g = info.gateway;
            String gateway = (g & 0xff) + "." + ((g >> 8) & 0xff) + "." + ((g >> 16) & 0xff) + "." + ((g >> 24) & 0xff);
            return "192.168.4.1".equals(gateway);
        } catch (Exception e) { return false; }
    }

    private void selectDashboardForCurrentNetwork(boolean forceReload) {
        if (!activityReady || webView == null) return;
        boolean wifi = hasWifiTransport();
        String selected;
        if (wifi) {
            bindToWifiNetwork();
            selected = wifiGatewayIsNoviaAp() ? DEFAULT_AP_URL : routerUrl;
        } else {
            bindToDefaultNetwork();
            selected = onlineUrl;
        }
        if (selected == null || selected.trim().isEmpty()) {
            updateHostLabel();
            if (forceReload) Toast.makeText(this, "Isi URL dashboard online melalui tombol URL untuk mode tanpa WiFi.", Toast.LENGTH_LONG).show();
            return;
        }
        if (forceReload || !selected.equals(dashboardUrl)) {
            dashboardUrl = selected; fallbackAttempted = false; loadDashboard();
        } else updateHostLabel();
    }

    private void bindToWifiNetwork() {
        try {
            if (connectivity == null) return;
            for (Network n : connectivity.getAllNetworks()) {
                NetworkCapabilities c = connectivity.getNetworkCapabilities(n);
                if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    connectivity.bindProcessToNetwork(n); return;
                }
            }
        } catch (Exception ignored) { }
    }
    private void bindToDefaultNetwork() {
        try { if (connectivity != null) connectivity.bindProcessToNetwork(null); } catch (Exception ignored) { }
    }

    private void loadDashboard() {
        updateHostLabel(); progress.setVisibility(View.VISIBLE); webView.loadUrl(dashboardUrl);
    }
    private void updateHostLabel() {
        if (hostLabel != null) hostLabel.setText("Dashboard otomatis: " + dashboardUrl);
    }
    private void handleDashboardFailure() {
        if (fallbackAttempted || onlineUrl == null || onlineUrl.isEmpty() || dashboardUrl.equals(onlineUrl)) {
            showConnectionHint(); return;
        }
        fallbackAttempted = true;
        runOnUiThread(() -> {
            Toast.makeText(this, "Dashboard lokal tidak merespons. Beralih ke dashboard online...", Toast.LENGTH_LONG).show();
            dashboardUrl = onlineUrl; bindToDefaultNetwork(); loadDashboard();
        });
    }
    private void showConnectionHint() {
        runOnUiThread(() -> Toast.makeText(this, "Dashboard tidak dapat dijangkau. Periksa WiFi, internet, dan URL yang tersimpan.", Toast.LENGTH_LONG).show());
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onResume() { super.onResume(); if (activityReady) selectDashboardForCurrentNetwork(false); }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() {
        if (networkCallback != null && connectivity != null) {
            try { connectivity.unregisterNetworkCallback(networkCallback); } catch (Exception ignored) { }
        }
        if (webView != null) { webView.stopLoading(); webView.destroy(); }
        try { if (connectivity != null) connectivity.bindProcessToNetwork(null); } catch (Exception ignored) { }
        super.onDestroy();
    }
}

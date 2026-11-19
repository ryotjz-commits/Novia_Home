package id.novia.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
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
    private static final String DEFAULT_HOST = "192.168.4.1";
    private static final int BG = Color.rgb(21, 26, 29);
    private static final int SURFACE = Color.rgb(32, 40, 43);
    private static final int TEXT = Color.rgb(241, 245, 242);
    private static final int MUTED = Color.rgb(145, 163, 154);
    private static final int ACCENT = Color.rgb(131, 201, 159);

    private WebView webView;
    private TextView hostLabel;
    private ProgressBar progress;
    private String host;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        w.getDecorView().setSystemUiVisibility(0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        host = getSharedPreferences(PREFS, MODE_PRIVATE).getString("host", DEFAULT_HOST);
        buildLayout();
        bindToWifiIfAvailable();
        loadDashboard();
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
        brand.setText("NOVIA HOME");
        brand.setTextColor(TEXT);
        brand.setTextSize(16);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bar.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));

        Button address = smallButton("IP ESP32");
        address.setOnClickListener(v -> showHostDialog());
        bar.addView(address);

        Button refresh = smallButton("↻");
        refresh.setTextSize(20);
        refresh.setOnClickListener(v -> { bindToWifiIfAvailable(); loadDashboard(); });
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(46), dp(42));
        rp.leftMargin = dp(6);
        bar.addView(refresh, rp);

        root.addView(bar);

        hostLabel = new TextView(this);
        hostLabel.setTextColor(MUTED);
        hostLabel.setTextSize(11);
        hostLabel.setPadding(dp(14), dp(5), dp(14), dp(5));
        hostLabel.setBackgroundColor(BG);
        root.addView(hostLabel);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));

        webView = new WebView(this);
        webView.setBackgroundColor(BG);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // Required by NOVIA dashboard controls and API polling.
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isAllowedHost(request.getUrl().getHost());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                android.net.Uri uri = android.net.Uri.parse(url);
                return !isAllowedHost(uri.getHost());
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) showConnectionHint();
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        updateHostLabel();
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(55, 70, 65)));
        return b;
    }

    private void loadDashboard() {
        updateHostLabel();
        progress.setVisibility(View.VISIBLE);
        webView.loadUrl("http://" + host + "/");
    }

    private void updateHostLabel() {
        if (hostLabel != null) hostLabel.setText("ESP32: http://" + host + "  •  Dual WiFi AP / Router LAN");
    }

    private void showHostDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(host);
        input.setHint("192.168.4.1 atau IP ESP32 di router");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this)
            .setTitle("Alamat ESP32")
            .setMessage("Gunakan 192.168.4.1 saat HP tersambung ke WiFi NOVIA-HOME. Jika HP tersambung ke router rumah, masukkan IP STA ESP32 yang terlihat di Serial Monitor.")
            .setView(input)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan & Hubungkan", (d, which) -> {
                String candidate = input.getText().toString().trim();
                if (!isPrivateIPv4(candidate)) {
                    Toast.makeText(this, "Masukkan alamat IPv4 lokal yang valid", Toast.LENGTH_LONG).show();
                    return;
                }
                host = candidate;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("host", host).apply();
                loadDashboard();
            }).show();
    }

    private boolean isPrivateIPv4(String value) {
        String[] p = value.split("\\.");
        if (p.length != 4) return false;
        int[] n = new int[4];
        try { for (int i = 0; i < 4; i++) { if (p[i].isEmpty() || (p[i].length() > 1 && p[i].startsWith("0"))) return false; n[i] = Integer.parseInt(p[i]); if (n[i] < 0 || n[i] > 255) return false; } }
        catch (NumberFormatException e) { return false; }
        return n[0] == 10 || (n[0] == 172 && n[1] >= 16 && n[1] <= 31) || (n[0] == 192 && n[1] == 168);
    }

    private boolean isAllowedHost(String value) {
        return value != null && value.equals(host);
    }

    private void bindToWifiIfAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            Network[] networks = cm.getAllNetworks();
            for (Network network : networks) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    cm.bindProcessToNetwork(network);
                    return;
                }
            }
            cm.bindProcessToNetwork(null);
        } catch (Exception ignored) { }
    }

    private void showConnectionHint() {
        Toast.makeText(this, "NOVIA tidak terjangkau. Sambungkan HP ke WiFi NOVIA-HOME atau masukkan IP ESP32 pada jaringan router.", Toast.LENGTH_LONG).show();
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) { webView.stopLoading(); webView.destroy(); }
        super.onDestroy();
    }
                               }

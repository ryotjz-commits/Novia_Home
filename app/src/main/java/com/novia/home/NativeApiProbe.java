package com.novia.home;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.net.URL;
import java.net.HttpURLConnection;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;

/** Phase 9.2 diagnostic only. Uses normal platform TLS validation; never bypasses SSL checks. */
public final class NativeApiProbe {
    private static final String TAG = "NOVIA_API_PROBE";
    private static final String STATUS_URL = "https://novia-home-api.ryo-tjz.workers.dev/api/status";
    private NativeApiProbe() {}

    public static void run(Handler mainHandler, Callback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String result;
            try {
                URL url = new URL(STATUS_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setUseCaches(false);
                int status = conn.getResponseCode();
                String body = "";
                java.io.InputStream in = status >= 200 && status < 400 ? conn.getInputStream() : conn.getErrorStream();
                if (in != null) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[2048]; int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    in.close(); body = out.toString("UTF-8");
                }
                String certInfo = "";
                if (conn instanceof HttpsURLConnection) {
                    Certificate[] certs = ((HttpsURLConnection) conn).getServerCertificates();
                    if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                        X509Certificate cert = (X509Certificate) certs[0];
                        certInfo = "\nCert Subject: " + cert.getSubjectX500Principal().getName() +
                                   "\nCert Issuer: " + cert.getIssuerX500Principal().getName();
                    }
                }
                result = "Native HTTPS result: HTTP " + status + "\n" + certInfo + "\nBody: " + body;
            } catch (Exception e) {
                result = "Native HTTPS failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                Log.e(TAG, result, e);
            } finally {
                if (conn != null) conn.disconnect();
            }
            final String finalResult = result;
            mainHandler.post(() -> callback.onResult(finalResult));
        }).start();
    }
    public interface Callback { void onResult(String result); }
}

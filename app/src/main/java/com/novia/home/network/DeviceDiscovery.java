package com.novia.home.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.DhcpInfo;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.HttpURLConnection;

/** NOVIA HOME LAN discovery. ESP32 responder protocol is documented in README. */
public class DeviceDiscovery {
    private static final String TAG = "NoviaDiscovery";
    public static final int UDP_PORT = 4210;
    public static final String QUERY = "NOVIA_DISCOVER_V1";
    private static final String PREFS = "novia_connection";
    private static final String KEY_LAST_IP = "last_ip";
    private static final String KEY_LAST_PORT = "last_port";

    public interface Callback {
        void onStatus(String status);
        void onFound(String ip, int port, JSONObject identity);
        void onNotFound();
    }

    private final Context context;
    private volatile boolean cancelled = false;

    public DeviceDiscovery(Context context) { this.context = context.getApplicationContext(); }
    public void cancel() { cancelled = true; }

    public void start(Callback callback) {
        cancelled = false;
        Thread worker = new Thread(() -> runDiscovery(callback), "NoviaDiscovery");
        worker.setDaemon(true);
        worker.start();
    }

    private void runDiscovery(Callback cb) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String cached = prefs.getString(KEY_LAST_IP, "");
        int cachedPort = prefs.getInt(KEY_LAST_PORT, 80);
        if (cached != null && !cached.trim().isEmpty()) {
            cb.onStatus("Mencoba perangkat terakhir: " + cached);
            JSONObject valid = probe(cached, cachedPort);
            if (!cancelled && valid != null) { cb.onFound(cached, cachedPort, valid); return; }
        }
        if (cancelled) return;
        cb.onStatus("Mencari NOVIA HOME di WiFi lokal…");
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock lock = null;
        try {
            if (wifi != null) {
                lock = wifi.createMulticastLock("novia-discovery");
                lock.setReferenceCounted(false);
                lock.acquire();
            }
            try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(1800);
            byte[] bytes = QUERY.getBytes("UTF-8");
            InetAddress broadcast = getBroadcastAddress(wifi);
            DatagramPacket request = new DatagramPacket(bytes, bytes.length, broadcast, UDP_PORT);
            socket.send(request);
            long end = System.currentTimeMillis() + 4500;
            while (!cancelled && System.currentTimeMillis() < end) {
                byte[] buffer = new byte[1024];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                try { socket.receive(reply); }
                catch (SocketTimeoutException timeout) { continue; }
                String response = new String(reply.getData(), reply.getOffset(), reply.getLength(), "UTF-8").trim();
                JSONObject parsed;
                try { parsed = new JSONObject(response); } catch (Exception malformed) { continue; }
                if (!"NOVIA_HOME".equals(parsed.optString("device"))) continue;
                String ip = parsed.optString("ip", reply.getAddress().getHostAddress());
                int port = parsed.optInt("port", 80);
                JSONObject identity = probe(ip, port);
                if (identity != null && !cancelled) {
                    prefs.edit().putString(KEY_LAST_IP, ip).putInt(KEY_LAST_PORT, port).apply();
                    cb.onFound(ip, port, identity);
                    return;
                }
            }
            }
            // Fallback: scan the current Wi-Fi subnet in parallel when broadcast is filtered.
            if (!cancelled) {
                cb.onStatus("Broadcast tidak menemukan perangkat; memindai jaringan lokal…");
                JSONObject found = scanSubnet(wifi, cb);
                if (found != null && !cancelled) {
                    String ip = found.optString("_probe_ip", found.optString("ip", ""));
                    int port = found.optInt("_probe_port", found.optInt("port", 80));
                    if (ip.length() > 0) {
                        prefs.edit().putString(KEY_LAST_IP, ip).putInt(KEY_LAST_PORT, port).apply();
                        cb.onFound(ip, port, found);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Discovery error", e);
        } finally {
            try { if (lock != null && lock.isHeld()) lock.release(); } catch (Exception ignored) {}
        }
        if (!cancelled) cb.onNotFound();
    }

    private JSONObject scanSubnet(WifiManager wifi, Callback cb) {
        if (wifi == null || cancelled) return null;
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null || dhcp.ipAddress == 0) return null;
        // Android DhcpInfo stores IPv4 integers in little-endian form.
        // Scan only the local /24, bounded to 254 candidates to avoid huge
        // task submissions on unusual subnet masks.
        final int local = dhcp.ipAddress;
        final int network24 = local & 0x00FFFFFF;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(16);
        java.util.concurrent.CompletionService<JSONObject> service = new java.util.concurrent.ExecutorCompletionService<>(pool);
        int submitted = 0;
        try {
            for (int last = 1; last <= 254 && !cancelled; last++) {
                final int candidate = network24 | (last << 24);
                if (candidate == local) continue;
                final String ip = ((candidate) & 0xff) + "." + ((candidate >> 8) & 0xff) + "." + ((candidate >> 16) & 0xff) + "." + ((candidate >> 24) & 0xff);
                service.submit(() -> { JSONObject result = probe(ip, 80); if (result != null) { result.put("_probe_ip", ip); result.put("_probe_port", 80); } return result; });
                submitted++;
            }
            long deadline = System.currentTimeMillis() + 9000;
            while (!cancelled && submitted > 0 && System.currentTimeMillis() < deadline) {
                java.util.concurrent.Future<JSONObject> future = service.poll(180, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (future == null) continue;
                submitted--;
                try {
                    JSONObject result = future.get();
                    if (result != null && "NOVIA_HOME".equals(result.optString("device"))) return result;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) { Log.w(TAG, "Bounded /24 scan", e); }
        finally { pool.shutdownNow(); }
        return null;
    }

    private JSONObject probe(String ip, int port) {
        for (String path : new String[]{"/api/device", "/api/state"}) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://" + ip + ":" + port + path);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(1200);
                connection.setReadTimeout(1200);
                connection.setRequestMethod("GET");
                int status = connection.getResponseCode();
                if (status != 200) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();
                JSONObject json = new JSONObject(body.toString());
                if ("NOVIA_HOME".equals(json.optString("device"))) return json;
                // V4.4 firmware does not define /api/device. Its /api/state
                // response has the relay channel signature instead.
                org.json.JSONArray channels = json.optJSONArray("channels");
                if ("/api/state".equals(path) && channels != null && channels.length() == 4 && json.has("wifi") && json.has("mode")) {
                    json.put("device", "NOVIA_HOME");
                    json.put("name", "NOVIA HOME V4.4-compatible");
                    json.put("apiProbe", "state");
                    json.put("ip", ip);
                    json.put("port", port);
                    return json;
                }
            } catch (Exception ignored) { }
            finally { if (connection != null) connection.disconnect(); }
        }
        return null;
    }

    private InetAddress getBroadcastAddress(WifiManager wifi) throws Exception {
        if (wifi != null) {
            DhcpInfo dhcp = wifi.getDhcpInfo();
            if (dhcp != null && dhcp.ipAddress != 0 && dhcp.netmask != 0) {
                int broadcast = (dhcp.ipAddress & dhcp.netmask) | (~dhcp.netmask);
                byte[] quads = new byte[4];
                for (int i = 0; i < 4; i++) quads[i] = (byte) ((broadcast >> (i * 8)) & 0xff);
                return InetAddress.getByAddress(quads);
            }
        }
        return InetAddress.getByName("255.255.255.255");
    }
  }

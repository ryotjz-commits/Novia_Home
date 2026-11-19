package com.novia.home.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;

/** Phase 4 connection orchestration: LAN discovery -> AP probe -> cloud/offline fallback. */
public final class ConnectionManager {
    public interface Listener {
        void onStatus(String message, String mode);
        void onLocal(String ip, int port, JSONObject identity, boolean ap);
        void onCloud();
        void onOffline();
    }
    private static final String AP_IP = "192.168.4.1";
    private static final int AP_PORT = 80;
    private final Context context;
    private final Listener listener;
    private final DeviceDiscovery discovery;
    private volatile boolean cancelled = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    public ConnectionManager(Context c, Listener l) { context=c.getApplicationContext(); listener=l; discovery=new DeviceDiscovery(context); }
    public void connect() { cancelled=false; listener.onStatus("Mencari NOVIA HOME di LAN…", "SEARCHING"); discovery.start(new DeviceDiscovery.Callback() {
        @Override public void onStatus(String s) { if(!cancelled) listener.onStatus(s, "SEARCHING"); }
        @Override public void onFound(String ip,int port,JSONObject id) { if(!cancelled) listener.onLocal(ip,port,id,false); }
        @Override public void onNotFound() { if(cancelled) return; listener.onStatus("LAN tidak menemukan perangkat · mencoba AP Direct…", "AP_SEARCH"); probeAp(); }
    }); }
    private void probeAp() { new Thread(() -> { JSONObject id=probe(AP_IP,AP_PORT); if(cancelled)return; if(id!=null) listener.onLocal(AP_IP,AP_PORT,id,true); else if(hasNetwork()) listener.onCloud(); else listener.onOffline(); }, "NoviaApProbe").start(); }
    private JSONObject probe(String ip,int port) { HttpURLConnection c=null; try { c=(HttpURLConnection)new URL("http://"+ip+":"+port+"/api/state").openConnection(); c.setConnectTimeout(1400); c.setReadTimeout(1400); c.setRequestMethod("GET"); if(c.getResponseCode()!=200)return null; java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream(); try(java.io.InputStream in=c.getInputStream()){byte[] buf=new byte[512];int n;while((n=in.read(buf))>0)b.write(buf,0,n);} JSONObject j=new JSONObject(b.toString("UTF-8")); org.json.JSONArray channels=j.optJSONArray("channels"); if(channels==null||channels.length()!=4||!j.has("wifi")||!j.has("mode"))return null; j.put("device","NOVIA_HOME"); j.put("name","NOVIA HOME V4.4-compatible"); j.put("ip",ip); j.put("port",port); return j; } catch(Exception e){return null;} finally{if(c!=null)c.disconnect();} }
    private boolean hasNetwork(){ ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE); if(cm==null)return false; Network n=cm.getActiveNetwork(); if(n==null)return false; NetworkCapabilities nc=cm.getNetworkCapabilities(n); return nc!=null&&nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)&&(nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)||nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)); }
    public void cancel(){cancelled=true; discovery.cancel(); handler.removeCallbacksAndMessages(null);}
}

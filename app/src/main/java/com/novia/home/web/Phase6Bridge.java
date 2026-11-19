package com.novia.home.web;
import android.webkit.JavascriptInterface;
import com.novia.home.control.RelayApiAdapter;
/** Local relay bridge backed by verified V4.4 API; calls only the current local host. */
public final class Phase6Bridge {
 private final RelayApiAdapter adapter=new RelayApiAdapter();
 public Phase6Bridge(String baseUrl){adapter.setBaseUrl(baseUrl);}
 @JavascriptInterface public void setBaseUrl(String url){adapter.setBaseUrl(url);}
 @JavascriptInterface public String getBaseUrl(){return adapter.getBaseUrl();}
 @JavascriptInterface public String getRelayStatus(){return adapter.getRelayStates().toString();}
 @JavascriptInterface public String toggleRelay(int relay, boolean state){return adapter.setRelay(relay,state).toString();}
}

package com.novia.home.control;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/** Adapter for endpoints verified in uploaded NOVIA HOME V4.4 source. */
public final class RelayApiAdapter {
    private volatile String baseUrl = "";
    public void setBaseUrl(String url) { baseUrl = normalize(url); }
    public String getBaseUrl() { return baseUrl; }
    private static String normalize(String u) { if (u == null) return ""; u=u.trim(); while(u.endsWith("/"))u=u.substring(0,u.length()-1); return u; }
    private JSONObject request(String path, String method, String form) {
        HttpURLConnection c=null;
        try {
            String base=baseUrl;
            if(base.isEmpty()) return error("LOCAL_DEVICE_NOT_CONNECTED");
            c=(HttpURLConnection)new URL(base+path).openConnection();
            c.setConnectTimeout(2500); c.setReadTimeout(3000); c.setRequestMethod(method);
            if("POST".equals(method)) { c.setDoOutput(true); c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8"); try(OutputStream o=c.getOutputStream()){o.write(form.getBytes("UTF-8"));} }
            int code=c.getResponseCode(); InputStream in=(code>=200&&code<400)?c.getInputStream():c.getErrorStream();
            ByteArrayOutputStream b=new ByteArrayOutputStream(); if(in!=null)try(InputStream x=in){byte[] buf=new byte[512];int n;while((n=x.read(buf))>0)b.write(buf,0,n);}
            String body=b.toString("UTF-8"); JSONObject j;
            try{j=new JSONObject(body);}catch(Exception parse){return error("HTTP_"+code+"_INVALID_JSON");}
            if(code<200||code>=300){j.put("http",code);return j;}
            j.put("http",code); return j;
        } catch(Exception e){ return error("NETWORK_ERROR: "+e.getClass().getSimpleName()); }
        finally { if(c!=null)c.disconnect(); }
    }
    private JSONObject error(String msg){JSONObject j=new JSONObject();try{j.put("ok",false);j.put("error",msg);}catch(Exception ignored){}return j;}
    public JSONObject getState(){return request("/api/state","GET",null);}
    public JSONObject setRelay(int relay, boolean state){
        if(relay<1||relay>4)return error("RELAY_RANGE");
        try { String body="ch="+URLEncoder.encode(String.valueOf(relay),"UTF-8")+"&state="+URLEncoder.encode(state?"1":"0","UTF-8"); JSONObject j=request("/api/set","POST",body); j.put("relay",relay); j.put("requestedState",state); return j; }
        catch(Exception e){return error("FORM_ENCODING_ERROR");}
    }
    public JSONObject getRelayStates(){
        JSONObject state=getState(); if(!state.has("channels"))return state;
        try {JSONArray ch=state.optJSONArray("channels"); JSONArray relays=new JSONArray(); if(ch!=null)for(int i=0;i<4;i++){JSONObject r=new JSONObject();r.put("id",i+1);r.put("state",ch.optInt(i,0)==1);relays.put(r);}state.put("relays",relays);state.put("verifiedApi","NOVIA_HOME_V4_4");}catch(Exception ignored){} return state;
    }
      }

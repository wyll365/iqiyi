package org.iqiyi.lib.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import okhttp3.internal.Util;
import org.iqiyi.lib.Api;

import java.io.IOException;
import java.net.Proxy;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HttpServer {
    private OkHttpClient.Builder clientBuilder;
    private Request.Builder requestBuilder;
    private HttpResponse httpResponse;


    public HttpServer(){
        this(null);
    }

    public HttpServer(Proxy proxy){
        this.clientBuilder=new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(20,TimeUnit.SECONDS);
        if(proxy!=null){
            this.clientBuilder.proxy(proxy);
        }
    }

    private String toHex(byte[] bytes) {
        char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
        StringBuilder ret = new StringBuilder(bytes.length * 2);

        for(int i = 0; i < bytes.length; ++i) {
            ret.append(HEX_DIGITS[bytes[i] >> 4 & 15]);
            ret.append(HEX_DIGITS[bytes[i] & 15]);
        }

        return ret.toString();
    }
    public String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(data.getBytes("UTF8"));
            return this.toHex(bytes).toLowerCase();
        } catch (Exception var4) {
            return null;
        }
    }


    private String makeSign(JSONObject params){
        TreeMap<String,Object> map=new TreeMap<String, Object>();
        map.putAll(params);
        StringBuilder sb=new StringBuilder();
        for (Map.Entry<String,Object> entry:map.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        sb.append("appkey").append("=").append(Api.appKey);
        return this.md5(sb.toString());
    }



    public HttpServer request(String url,JSONObject params,String postData,JSONObject header,JSONObject cookie) throws IOException {
        httpResponse=null;
        requestBuilder = new Request.Builder().url(url);
        if(url.contains(Api.apiUrl)){
            if(params==null)params=new JSONObject();
            params.put("_timestamp",System.currentTimeMillis());
            params.put("appid",Api.appId);
            params.put("sign",makeSign(params));
        }
        HttpUrl.Builder httpBuilder=HttpUrl.parse(url).newBuilder();
        if (params != null && !params.isEmpty()) {
            for (String key:params.keySet()) {
                httpBuilder.addQueryParameter(key,params.getString(key));
            }
        }
        requestBuilder.url(httpBuilder.build());
        if (postData != null) {
            if(postData.startsWith("{")&&postData.endsWith("}"))
                requestBuilder.post(RequestBody.create(postData, MediaType.parse("application/json")));
            else requestBuilder.post(RequestBody.create(postData, MediaType.parse("text/plain")));
        }else requestBuilder.post(Util.EMPTY_REQUEST);


        if (header != null && !header.isEmpty()) {
            for (String key : header.keySet()) {
                requestBuilder.header(key, header.getString(key));
            }
        }
        if (cookie != null && !cookie.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String key : cookie.keySet()) {
                sb.append(key).append(cookie.getString(key)).append(";");
            }
            requestBuilder.header("Cookie", sb.toString());
        }
        return this;
    }

    public HttpServer request(JSONObject data) throws IOException {
        if(data.containsKey("code")&&data.getIntValue("code")!=200)throw new RuntimeException(data.toJSONString());
        if(data.containsKey("data"))data=data.getJSONObject("data");
        return this.request(data.getString("url"),data.getJSONObject("params"),null,data.getJSONObject("header"),data.getJSONObject("cookie"));
    }

    public HttpServer request(String url,JSONObject params) throws IOException {
        return this.request(url,params,null,null,null);
    }


    public HttpResponse execute() throws IOException {
        Response response= this.clientBuilder.build().newCall(this.requestBuilder.build()).execute();
        httpResponse= new HttpResponse(response);
        return httpResponse;
    }

    public void close() {
        if(httpResponse!=null) httpResponse.close();
    }

    public byte[] download(String url) {
        Response response=null;
        try {
            response= this.clientBuilder.build().newCall(new Request.Builder().url(url).build()).execute();
            return response.body().bytes();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }finally {
            if(response!=null)response.close();
        }
    }

    public class HttpResponse {


        private Response response;
        private byte[] resData;

        public HttpResponse(Response response) {
            this.response = response;
            try {
                this.resData = response.body().bytes();
            }catch (Exception e){}
        }

        public String toString()  {
            try {
                if(resData==null) resData=this.response.body().bytes();
                return new String(resData,"utf8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public JSONObject toJSON() throws Exception {
            try {
                return JSON.parseObject(this.toString());
            }catch (Exception e) {throw e;}
        }

        public void close(){
            try {
                if (response != null) response.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        public List<String> getHeader(String s) {
            String s1=this.response.header(s);
            return this.response.headers(s);
        }

        public Map<String,String> getCookie() {
            try {
                Map<String, String> map = new HashMap<>();
                List<String> list = getHeader("Set-Cookie");
                for (String s : list) {
                    String[] ss = s.substring(0, s.indexOf(";")).split("=");
                    map.put(ss[0], ss[1]);
                }
                return map;
            }catch (Exception e){
                return null;
            }
        }



        public boolean isSuccess() throws Exception {
            if( this.response.isSuccessful()){
                return true;
            }else throw new Exception("httpStatus:"+this.response.code()+"\t "+this.response.message());
        }
        public HttpResponse success() throws Exception {
            this.isSuccess();
            return this;
        }
    }
}

package org.demo;

import com.alibaba.fastjson.JSONObject;
import org.iqiyi.lib.Api;
import org.iqiyi.lib.ILoginCallback;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;

public class Main {

    private static Api api=new Api("AppId","AppKey","apiUrl");

    public static void main(String[] args) throws Exception {
        Proxy proxy=new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",8080));
        JSONObject dfp=api.getDfp(proxy);
        api.login(dfp, "account phone", "password", proxy, new ILoginCallback() {
            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onSuccess(Map<String, String> cookie) {
                //login success return cookie
            }
        });



    }



}

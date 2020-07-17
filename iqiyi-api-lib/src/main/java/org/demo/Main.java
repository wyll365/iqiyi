package org.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.iqiyi.lib.Api;
import org.iqiyi.lib.ILoginCallback;
import org.iqiyi.lib.service.HttpServer;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;

public class Main {


    private static Api api=new Api("1aP6L9H4UPGn20YQUPb1",
            "ZxD23HOESTXVFZyWsXY13ufzKEL83My9",
            "http://127.0.0.1:8080/");

    public static void main(String[] args) throws Exception {
        HttpServer.HttpProxy proxy = new HttpServer.HttpProxy(new Proxy(Proxy.Type.HTTP,new InetSocketAddress("140.255.60.168", 20476)),"wyll365","qm4xbvmk");

        //爱奇艺指纹
        JSONObject dfp = api.getDfp();
        //滑块验证成功token
        String token=api.decodeCaptcha(dfp,"",null);
        //发送短信验证码
        api.sendSms(dfp,"手机号",proxy);
        //短信登陆
        api.smsLogin(dfp,"手机号","验证码",null);
        api.login(dfp, "手机号", "密码", null, new ILoginCallback() {
            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onSuccess(Map<String, String> cookie) {
                //login success return cookie
                System.out.println(JSON.toJSONString(cookie));
            }
        });

    }


}

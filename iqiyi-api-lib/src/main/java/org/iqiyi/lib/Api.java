package org.iqiyi.lib;

import com.alibaba.fastjson.JSONObject;
import org.iqiyi.lib.service.HttpServer;

import java.net.Proxy;
import java.util.Map;

public class Api {

    public static String appId;
    public static String appKey;
    public static  String apiUrl;

    public Api(String appId, String appKey, String apiUrl) {
        Api.appId = appId;
        Api.appKey = appKey;
        Api.apiUrl = apiUrl;
    }

    public Api(String appId, String appKey) {
        this(appId,appKey,"http://localhost:8080");
    }

    /**
     * 获取爱奇艺dfp
     * 成功后 会返回session和dfp指纹
     * @return
     * @throws Exception
     */
    public JSONObject getDfp() throws Exception {
        return this.getDfp(null);
    }
    /**
     * 获取爱奇艺dfp
     * 成功后 会返回session和dfp指纹
     * @return
     * @throws Exception
     */
    public JSONObject getDfp(Proxy proxy) throws Exception {
        HttpServer httpServer=new HttpServer();
        try {
            HttpServer.HttpResponse response = httpServer.request(Api.apiUrl+"/api/get_dfp", null).execute();
            JSONObject res= response.toJSON();
            if(res.getIntValue("code")==200){
                JSONObject iRes=  this.getDfp(res.getJSONObject("data"),proxy);
                return iRes;
            }else throw new Exception(res.getString("message"));
        }finally {
            httpServer.close();
        }
    }
    //请求爱奇艺接口
    private JSONObject getDfp(JSONObject data,Proxy proxy) throws Exception {
        HttpServer httpServer=new HttpServer(proxy);
        try{
            HttpServer.HttpResponse response= httpServer.request(data).execute();
            JSONObject res= response.toJSON();
            JSONObject result= res.getJSONObject("result");
            result.put("session",data.getString("session"));
            result.remove("extend");
            result.remove("expireAt");
            result.remove("ttl");
            return result;
        }catch (Exception e){
            throw e;
        }finally {
            httpServer.close();
        }
    }
    private Object passwordLogin(String session,String phone,String password,String dfp,Proxy proxy) throws Exception {
        JSONObject params=new JSONObject();
        params.put("session",session);
        params.put("phone",phone);
        params.put("password",password);
        params.put("dfp",dfp);
        return this.passwordLogin(params,proxy);
    }
    private Object passwordLogin(JSONObject params,Proxy proxy) throws Exception {
        HttpServer httpServer=new HttpServer();
        try {
            HttpServer.HttpResponse response = httpServer.request(Api.apiUrl+"/api/password_login_params", params).execute();
            JSONObject res= response.toJSON();
            if(res.getIntValue("code")==200){
                return this.iqiyiLogin(res.getJSONObject("data"),proxy);
            }else throw new Exception(res.getString("message"));
        }finally {
            httpServer.close();
        }
    }
    private Object iqiyiLogin(JSONObject data,Proxy proxy) throws Exception {
        HttpServer httpServer=new HttpServer(proxy);
        try{
            HttpServer.HttpResponse response= httpServer.request(data).execute();
            JSONObject res= response.toJSON();
            if("A00000".equals(res.getString("code"))){
               return response.getCookie();
            }else if("P00223".equals(res.getString("code"))&&"G00000".equals(res.getJSONObject("data").getString("code"))){
                return res.getJSONObject("data").getJSONObject("data").getString("token");
            }else throw new Exception(res.toJSONString());
        }catch (Exception e){
            throw e;
        }finally {
            httpServer.close();
        }
    }


    public Map<String,String> login(JSONObject dfp, String phone, String password, Proxy proxy) throws Exception {
        dfp.put("phone",phone);
        dfp.put("password",password);
        for (int i = 0; i < 3; i++) {
            try {
              Object obj=  passwordLogin(dfp, proxy);
                if(obj instanceof String){
                    CaptchaApi captchaApi=new CaptchaApi(dfp.getString("session"), (String) obj);
                    String token=captchaApi.execute(proxy);
                    dfp.put("env_token",token);
                }else if(obj instanceof Map){
                    return (Map<String, String>) obj;
                }
            }catch (Exception e){
                throw e;
            }
        }
        return null;
    }
    public void login(final JSONObject dfp,final String phone,final String password,final Proxy proxy,final ILoginCallback callback){
        dfp.put("phone",phone);
        dfp.put("password",password);
        for (int i = 0; i < 3; i++) {
            try {
                Object obj=  passwordLogin(dfp, proxy);
                if(obj instanceof String){
                    CaptchaApi captchaApi=new CaptchaApi(dfp.getString("session"), (String) obj);
                    String token=captchaApi.execute(proxy);
                    dfp.put("env_token",token);
                }else if(obj instanceof Map){
                    callback.onSuccess((Map<String, String>) obj);
                    return;
                }
            }catch (Exception e){
                callback.onError(e);
                return ;
            }
        }
        callback.onError(new Exception("登陆失败"));
    }



    public String decodeCaptcha(JSONObject dfp,final String token,final Proxy proxy) throws Exception {
        CaptchaApi api=new CaptchaApi(dfp.getString("session"),token);
        return api.execute(proxy);
    }


}

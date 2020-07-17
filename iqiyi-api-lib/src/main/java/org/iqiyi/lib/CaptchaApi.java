package org.iqiyi.lib;

import com.alibaba.fastjson.JSONObject;
import org.iqiyi.lib.service.HttpServer;

import java.net.Proxy;
import java.util.Base64;

public class CaptchaApi {

    private final String session,token;

    public CaptchaApi(String session, String token) {
        this.session = session;
        this.token = token;
    }

    private JSONObject postJson(JSONObject data, HttpServer.HttpProxy proxy) throws Exception {
        HttpServer httpServer=new HttpServer(proxy);
        try{
            HttpServer.HttpResponse response= httpServer.request(data).execute();
                return response.success().toJSON();
        }catch (Exception e){
            throw e;
        }finally {
            httpServer.close();
        }
    }
    private String postString(JSONObject data,HttpServer.HttpProxy proxy) throws Exception {
        HttpServer httpServer=new HttpServer(proxy);
        try{
            HttpServer.HttpResponse response= httpServer.request(data).execute();
            return response.success().toString();
        }catch (Exception e){
            throw e;
        }finally {
            httpServer.close();
        }
    }

    /**
     * 初始化验证码密钥
     * @return 返回sid,sr 密钥数据
     */
    private JSONObject captchaInitKey( HttpServer.HttpProxy proxy) throws Exception {
        JSONObject params=new JSONObject();
        params.put("session",this.session);
        params.put("token",this.token);
        HttpServer httpServer=new HttpServer();
        try {
            HttpServer.HttpResponse response = httpServer.request(Api.apiUrl+"/api/captcha/init_key", params).execute();
            JSONObject res= response.success().toJSON();
            if(res.getIntValue("code")==200){
                JSONObject iRes=  this.captchaInitKey(res.getJSONObject("data"),proxy);
                iRes.put("session",this.session);
                return iRes;
            }else throw new Exception(res.getString("message"));
        }finally {
            httpServer.close();
        }
    }

    /**
     * 请求爱奇艺接口
     * @param data 请求参数
     * @param proxy 代理参数
     * @return 成功返回 sid,sr
     * @throws
     */
    private JSONObject captchaInitKey(JSONObject data,HttpServer.HttpProxy proxy) throws Exception {
        JSONObject res=this.postJson(data,proxy);
        JSONObject result= res.getJSONObject("data");
        result.remove("i18n");
        result.remove("sign");
        return result;
    }

    /**
     * 获取验证码数据
     * @param params
     * @param proxy
     * @return
     * @throws Exception
     */
    private String captchaInitPage(JSONObject params,HttpServer.HttpProxy proxy) throws Exception {
        HttpServer httpServer=new HttpServer();
        try {
            HttpServer.HttpResponse response = httpServer.request(Api.apiUrl+"/api/captcha/init_page", params).execute();
            JSONObject res= response.toJSON();
            if(res.getIntValue("code")==200){
                return   this.postString(res.getJSONObject("data"),proxy);
            }else throw new Exception(res.getString("message"));
        }finally {
            httpServer.close();
        }
    }

    /**
     * 解密验证码数据,并返回 图片网址
     * @param body 需要解密的数据字符串
     * @return 返回验证码图片网址
     */
    private JSONObject putCaptchaData(String body,HttpServer.HttpProxy proxy) throws Exception {
        JSONObject params=new JSONObject();
        params.put("session",this.session);
        HttpServer httpServer=new HttpServer();
        try {
            HttpServer.HttpResponse response = httpServer.request(Api.apiUrl+"/api/captcha/put_data", params,body,null,null).execute();
            JSONObject res= response.toJSON();
            if(res.getIntValue("code")==200){
                JSONObject result=this.downloadImg(res.getJSONObject("data"),proxy);
                result.put("session",this.session);
                return result;
            }else throw new Exception(res.getString("message"));
        }finally {
            httpServer.close();
        }
    }

    private JSONObject downloadImg(JSONObject data,HttpServer.HttpProxy proxy){
        String captcha_url=data.getString("captcha_url");
        String slider_url=data.getString("slider_url");
        HttpServer httpServer=new HttpServer(proxy);
        byte[] bytes=  httpServer.download(captcha_url);
        JSONObject result=new JSONObject();
        result.put("captcha",Base64.getEncoder().encodeToString(bytes));
        result.put("slider",Base64.getEncoder().encodeToString(new HttpServer(proxy).download(slider_url)));
        return result;
    }

    /**
     * 提交验证码数据
     * @param data
     * @param proxy
     * @return
     * @throws Exception
     */
    private String captchaVerify(JSONObject data,HttpServer.HttpProxy proxy) throws Exception {
        JSONObject params=new JSONObject();
        params.put("session",session);
        HttpServer httpServer=new HttpServer();
        try {
            HttpServer.HttpResponse response = httpServer.request(Api.apiUrl+"/api/captcha/verify", params,data.toJSONString(),null,null).execute();
            JSONObject res= response.toJSON();
            if(res.getIntValue("code")==200){
                return this.postString(res.getJSONObject("data"),proxy);
            }else throw new Exception(res.getString("message"));
        }finally {
            httpServer.close();
        }
    }


    /**
     * 解密验证返回结果
     * @param body
     * @return
     * @throws Exception
     */
    private String captchaVerifyResult(String body) throws Exception {
        JSONObject params=new JSONObject();
        params.put("session",session);
        HttpServer httpServer=new HttpServer();
        try {
            HttpServer.HttpResponse response = httpServer.request(Api.apiUrl+"/api/captcha/verify_result", params,body,null,null).execute();
            JSONObject res= response.toJSON();
            if(res.getIntValue("code")==200){
                return res.getString("data");
            }else throw new Exception(res.getString("message"));
        }finally {
            httpServer.close();
        }
    }

    public String execute(HttpServer.HttpProxy proxy) throws Exception {
        JSONObject res=  this.captchaInitKey(proxy);
        String pageText= this.captchaInitPage(res,proxy);
        JSONObject resImg= this.putCaptchaData(pageText,proxy);
        String resVerify=  this.captchaVerify(resImg,proxy);
        return  this.captchaVerifyResult(resVerify);
    }


}

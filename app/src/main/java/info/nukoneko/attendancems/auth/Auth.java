package info.nukoneko.attendancems.auth;

import android.app.Activity;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import info.nukoneko.attendancems.common.network.Async;
import info.nukoneko.attendancems.common.network.AsyncCallback;
import info.nukoneko.attendancems.common.network.SendUtil;
import info.nukoneko.attendancems.common.Globals;
import info.nukoneko.attendancems.common.json.JsonGetItem;


/**
 * Created by Atsumi on 2014/11/17.
 */
public class Auth{
    Activity parentActivity;
    AuthCallback callback;

    Integer nonce = 0;

    public Auth(Activity my, String _url, AuthCallback callback){
        this.callback = callback;
        parentActivity = my;
        Globals.serverURI = Uri.parse(_url);
        firstAuth();
    }

    private void println(Object text){
        Toast.makeText(parentActivity, text.toString(), Toast.LENGTH_SHORT).show();
    }

    private void firstAuth() {
        this.nonce = new Random().nextInt(10000000);
        new Async<String>(new AsyncCallback<String>() {
            @Override
            public String doFunc(Object... params) {
                Map<String, Object> param = new HashMap<String, Object>();
                param.put("nonce", nonce);
                return SendUtil.send(Async.method.POST, Async.Protocol.HTTP, SendUtil.getBaseUri("auth"), param);
            }
            @Override
            public void onResult(String result) {
                if(result == null)return;
                String hash = generateFirstHash(result);
                if(!hash.equals("")) {
                    Globals.hash = hash;
                    println(result);
                    secondAuth();
                }else{
                    Globals.readingMode = false;
                    callback.onFailed();
                }
            }
        }).run();
    }

    private void secondAuth(){
        new Async<String>(new AsyncCallback<String>() {
            @Override
            public String doFunc(Object... params) {
                Map<String, Object> param = new HashMap<String, Object>();
                param.put("hash", Globals.hash);
                return SendUtil.send(Async.method.POST, Async.Protocol.HTTP, SendUtil.getBaseUri("auth"), param);
            }

            @Override
            public void onResult(String result) {
                try{
                    ObjectMapper object = new ObjectMapper();
                    JsonNode root = object.readValue(result, JsonNode.class);
                    Globals.isAuthEnable = root.get("auth").isBoolean();
                } catch (JsonMappingException e) {
                    e.printStackTrace();
                } catch (JsonParseException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(Globals.isAuthEnable) {
                    Globals.readingMode = true;
                    callback.onSuccess();
                }else{
                    Globals.readingMode = false;
                    callback.onFailed();
                }
            }
        }).run();
    }

    public interface AuthCallback{
        public void onSuccess();
        public void onFailed();
    }

    private String generateFirstHash(String json) {
        try {
            ObjectMapper object = new ObjectMapper();
            JsonGetItem jsonGetItem = new JsonGetItem( object.readValue(json, JsonNode.class));
            Globals.lectureID = jsonGetItem.getLong("lecture");
            String key = String.valueOf(this.nonce + jsonGetItem.getLong("timestamp") + Globals.lectureID);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(), "HmacSHA1"));
            String base64 = Base64.encodeToString(mac.doFinal(jsonGetItem.getString("hash").getBytes()), 0);
            System.out.println("BASE - " + base64);
            return base64.replace('+', '-').replace('/', '_');
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}

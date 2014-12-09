package info.nukoneko.attendancems.auth;

import android.app.Activity;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
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

    public Auth(Activity my, AuthCallback callback){
        this.callback = callback;
        this.parentActivity = my;
        if(Globals.serverURI != null) sessionAuth();
    }

    public Auth(Activity my, String _url, AuthCallback callback){
        this.callback = callback;
        parentActivity = my;
        Globals.serverURI = Uri.parse(_url);
        firstAuth();
    }

    private void firstAuth() {
        this.nonce = new Random().nextInt(10000000);
        new Async<>(new AsyncCallback<String>() {
            @Override
            public String doFunc(Object... params) {
                Map<String, Object> param = new HashMap<String, Object>();
                param.put("nonce", nonce);
                return SendUtil.send(Async.method.POST, Async.Protocol.HTTP, SendUtil.getBaseUri("auth"), param);
            }
            @Override
            public void onResult(String result) {
                String hash = generateFirstHash(result);
                if(!hash.equals("")) {
                    Globals.sessionToken = hash;
                    sessionAuth();
                }else{
                    Globals.readingMode = false;
                    callback.onFailed();
                }
            }
        }).run();
    }

    private void sessionAuth(){
        new Async<>(new AsyncCallback<String>() {
            @Override
            public String doFunc(Object... params) {
                Map<String, Object> param = new HashMap<String, Object>();
                param.put("hash", Globals.sessionToken);
                return SendUtil.send(Async.method.POST, Async.Protocol.HTTP, SendUtil.getBaseUri("auth"), param);
            }

            @Override
            public void onResult(String result) {
                try{
                    ObjectMapper object = new ObjectMapper();
                    JsonNode root = object.readValue(result, JsonNode.class);
                    Globals.isAuthEnable = root.get("auth").asBoolean(false);
                } catch (IOException e) {
                    e.printStackTrace();
                    Globals.isAuthEnable = false;
                }
                if(Globals.isAuthEnable) {
                    Globals.readingMode = true;
                    ((Globals)parentActivity.getApplication()).saveSettingPreference();
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
            return base64.replace('+', '-').replace('/', '_');
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}

package info.nukoneko.attendancems.auth;

import android.app.Activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    public Auth(Activity my, LoginObject object, AuthCallback callback){
        this.callback = callback;
        parentActivity = my;
        firstAuth(object);
    }

    private void firstAuth(final LoginObject object) {
        new Async<>(new AsyncCallback<String>() {
            @Override
            public String doFunc(Object... params) {
                Map<String, Object> param = new HashMap<String, Object>();
                param.put("userId", object.getUserID());
                param.put("passWd", object.getPassWD());
                return SendUtil.send(Async.method.POST, Async.Protocol.HTTP, SendUtil.getBaseUri("test"), param);
            }
            @Override
            public void onResult(String result) {
                JsonGetItem json = parseJson(result);
                if(json != null && json.getBoolean("auth")) {
                    Globals.sessionToken = json.getString("hash");
                    sessionAuth();
                }else{
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
                JsonGetItem json = parseJson(result);
                if(json != null && json.getBoolean("auth")) {
                    ((Globals)parentActivity.getApplication()).saveSettingPreference();
                    callback.onSuccess();
                }else{
                    callback.onFailed();
                }
            }
        }).run();
    }

    public interface AuthCallback{
        public void onSuccess();
        public void onFailed();
    }

    private JsonGetItem parseJson(String json) {
        JsonGetItem jsonGetItem = null;
        try {
            ObjectMapper object = new ObjectMapper();
            jsonGetItem = new JsonGetItem( object.readValue(json, JsonNode.class));
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return jsonGetItem;
    }

    public static class LoginObject{
        private String userID = "";
        private String passWD = "";
        public LoginObject(String u, String p){
            this.userID = u;
            this.passWD = p;
        }
        public String getUserID(){
            return this.userID;
        }
        public String getPassWD(){
            return this.passWD;
        }
    }
}

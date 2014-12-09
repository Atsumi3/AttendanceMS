package info.nukoneko.attendancems.common.network;

import android.os.Build;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import info.nukoneko.attendancems.common.Globals;
import info.nukoneko.attendancems.common.json.JsonGetItem;

/**
 * Created by Telneko on 2014/12/04.
 */
public class SendID {
    public static void sendID(final String id, final SendIDCallback callback){
        new Async<>(new AsyncCallback<String>() {
            @Override
            public String doFunc(Object... params) {
                Map <String ,Object> param = new HashMap<String, Object>();
                param.put("hash", Globals.sessionToken);
                param.put("sid", id);
                return SendUtil.send(Async.method.POST, Async.Protocol.HTTP, SendUtil.getBaseUri("id"), param);
            }

            @Override
            public void onResult(String result) {
                if(checkResult(result)){
                    callback.onSuccess();
                }else{
                    callback.onFailed(result);
                }
            }
        }).run();
    }
    public interface SendIDCallback{
        public void onSuccess();
        public void onFailed(String text);
    }

    private static boolean checkResult(String json){
        if(json == null) return  false;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
            JsonGetItem jsonGetItem = new JsonGetItem(jsonNode);
            return jsonGetItem.getBoolean("result");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}

package info.nukoneko.attendancems.common.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import info.nukoneko.attendancems.container.OnStartUpObject;

/**
 * Created by Telneko on 2014/12/04.
 */
public class SocketUtil {
    static Boolean isFirstLoad = true;

    public static WebSocketClient getClient(final onActionCallback callback){
        WebSocketClient ret = null;
        try{
            URI uri = new URI(SendUtil.createBaseUriWebSocket());
            ret = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    callback.onOpen(handshakedata);
                }
                @Override
                public void onMessage(final String message) {
                    callback.onMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    callback.onClose(code,reason,remote);
                }

                @Override
                public void onError(Exception ex) {
                    callback.onError(ex);
                }
            };
            ret.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static WebSocketClient getClient(final getMessageCallback callback){
        WebSocketClient ret = null;
        try{
            URI uri = new URI(SendUtil.createBaseUriWebSocket());
            ret = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    println("SOCKET OPEN" + handshakedata.getHttpStatusMessage());
                }
                @Override
                public void onMessage(final String message) {
                    if(!message.equals("")){
                        JsonNode jsonNode = null;
                        ObjectMapper objectMapper = new ObjectMapper();
                        try{
                            jsonNode = objectMapper.readValue(message, JsonNode.class);
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                        if(jsonNode != null && jsonNode.get("command") != null){
                            String command = jsonNode.get("command").asText();
                            if(command.equals("onStartUp")){
                                OnStartUpObject object = new OnStartUpObject(jsonNode);
                            }else if(command.equals("onReaderError")){
                                println("ReaderError : \n" + jsonNode.get("message").asText());
                            }else if(command.equals("onResume")){
                                callback.onUpdate(jsonNode);
                            }else if(command.equals("onRead")){
                                callback.onUpdate(jsonNode);
                            }else if(command.equals("onIdle")){
                                checkConnect();
                            }else if(command.equals("onHeartBeat")){
                                checkConnect();
                            }
                        }else{
                            println("パース失敗...");
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    println("CLOSE");
                }

                @Override
                public void onError(Exception ex) {
                    println("ERROR");
                }
            };
            ret.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private static void checkConnect(){
        if(!isFirstLoad) return;
        isFirstLoad = false;
        println("接続しました");
    }

    public interface getMessageCallback{
        public void onUpdate(JsonNode json);
    }

    public interface onActionCallback{
        public void onOpen(ServerHandshake handshake);
        public void onMessage(final String message);
        public void onClose(int code, String reason, boolean remote);
        public void onError(Exception ex);
    }

    private static void println(Object text){
        System.out.println(text.toString());
    }
}

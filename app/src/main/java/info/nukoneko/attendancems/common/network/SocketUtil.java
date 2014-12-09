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
    public static WebSocketClient getClient(final onActionCallback callback){
        WebSocketClient ret = null;
        try{
            URI uri = new URI(SendUtil.createBaseUriWebSocket());
            ret = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handShakeData) {
                    callback.onOpen(handShakeData);
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

    public interface onActionCallback{
        public void onOpen(ServerHandshake handshake);
        public void onMessage(final String message);
        public void onClose(int code, String reason, boolean remote);
        public void onError(Exception ex);
    }
}

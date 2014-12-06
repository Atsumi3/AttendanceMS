package info.nukoneko.attendancems.activity;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;

import info.nukoneko.attendancems.R;
import info.nukoneko.attendancems.adapter.AttendAdapter;
import info.nukoneko.attendancems.auth.Auth;
import info.nukoneko.attendancems.common.network.SendID;
import info.nukoneko.attendancems.common.network.SendUtil;
import info.nukoneko.attendancems.common.Globals;
import info.nukoneko.attendancems.common.network.SocketUtil;
import info.nukoneko.attendancems.container.EntryObject;

/**
 * Created by Telneko on 2014/12/04.
 */
public class MainActivity extends Activity {

    Handler mHandler;

    WebSocketClient mClient = null;

    ListView mListView;
    AttendAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Globals.targetIP = "192.168.0.7";
        Globals.sessionKey = 1234L;

        //init
        mHandler = new Handler();
        mAdapter = new AttendAdapter(this);
        mListView = (ListView)findViewById(R.id.user_list);
        mListView.setAdapter(mAdapter);
        if ("sdk".equals(Build.PRODUCT)) {
            java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
            java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        }

        System.out.println("READING MODE :" + Globals.readingMode);

        MainActivity.this.findViewById(R.id.b_send_data).setEnabled(false);
        if(Globals.readingMode){
            MainActivity.this.findViewById(R.id.b_auth).setEnabled(false);
            // socket client
            mClient = SocketUtil.getClient(new mOnSocketActionListener());
        }else{
            MainActivity.this.findViewById(R.id.b_socket_start).setEnabled(false);
        }

        // first auth
        findViewById(R.id.b_auth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Auth(MainActivity.this, SendUtil.createBaseUriHTTP(), new Auth.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        showToast("認証成功");
                        MainActivity.this.findViewById(R.id.b_auth).setEnabled(false);
                        MainActivity.this.findViewById(R.id.b_socket_start).setEnabled(true);
                        // socket client
                        mClient = SocketUtil.getClient(new mOnSocketActionListener());
                    }

                    @Override
                    public void onFailed() {
                        showToast("認証失敗");
                    }
                });
            }
        });

        // send id
        findViewById(R.id.b_send_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                println(Globals.hash);
                if (Globals.hash.equals("")) {
                    showToast("認証されていません");
                    MainActivity.this.findViewById(R.id.b_send_data).setEnabled(false);
                } else {
                    SendID.sendID("1140096", new mOnSendResult());
                }
            }
        });

        // start auth
        findViewById(R.id.b_socket_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    showToast("Session Start...");
                    mClient.send("{\"sessionKey\":1234}");
                    MainActivity.this.findViewById(R.id.b_send_data).setEnabled(true);
                    MainActivity.this.findViewById(R.id.b_socket_start).setEnabled(false);
                    MainActivity.this.findViewById(R.id.b_socket_stop).setEnabled(true);
                }
                catch (WebsocketNotConnectedException e){
                    e.printStackTrace();
                    mClient = SocketUtil.getClient(new mOnSocketActionListener());

                }
            }
        });

        // stop auth
        findViewById(R.id.b_socket_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClient.close();
                mAdapter.clear();
                mClient = SocketUtil.getClient(new mOnSocketActionListener());

                MainActivity.this.findViewById(R.id.b_socket_start).setEnabled(true);
                MainActivity.this.findViewById(R.id.b_socket_stop).setEnabled(false);
                MainActivity.this.findViewById(R.id.b_send_data).setEnabled(false);
            }
        });
    }


    public class mOnSocketActionListener implements SocketUtil.onActionCallback {
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            showToast("Socket Start");
        }

        @Override
        public void onMessage(String message) {
            if (!message.equals("")) {
                JsonNode jsonNode = null;
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    jsonNode = objectMapper.readValue(message, JsonNode.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if ((jsonNode != null ? jsonNode.get("command") : null) != null) {
                    String command = jsonNode.get("command").asText();
                    if (command.equals("onResume")) {
                        socketOnUpdate(jsonNode);
                    } else if (command.equals("onRead")) {
                        socketOnUpdate(jsonNode);
                    }
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            mClient.close();
            mAdapter.clear();
            mClient = SocketUtil.getClient(new mOnSocketActionListener());

            MainActivity.this.findViewById(R.id.b_socket_start).setEnabled(true);
            MainActivity.this.findViewById(R.id.b_socket_stop).setEnabled(false);
            MainActivity.this.findViewById(R.id.b_send_data).setEnabled(false);
        }

        @Override
        public void onError(Exception ex) {
            showToast("Socket Error...");
        }
    };
    public void socketOnUpdate(final JsonNode json){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EntryObject object = new EntryObject(json);
                mAdapter.add(object);
                mListView.setSelection(mAdapter.getCount()-1);

                if(object.getResult().equals("出席")){
                    mSoundChime.play(mSoundChimeID, 1.0F, 1.0F, 0, 0, 1.0F);
                }else if(object.getResult().equals("(処理済み)")){
                    mSoundStone.play(mSoundStoneID, 1.0F, 1.0F, 0, 0, 1.0F);
                }else{
                    mSoundBad.play(mSoundBadID, 1.0F, 1.0F, 0, 0, 1.0F);
                }
            }
        });
    }
    public class mOnSendResult implements SendID.SendIDCallback{
        @Override
        public void onSuccess() {
            Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(String text) {
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
        }
    }

    private SoundPool mSoundChime;
    private Integer mSoundChimeID;
    private SoundPool mSoundBad;
    private Integer mSoundBadID;
    private SoundPool mSoundStone;
    private Integer mSoundStoneID;
    @Override
    protected void onResume(){
        super.onResume();
        mSoundChime = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundChimeID = mSoundChime.load(getApplicationContext(), R.raw.tm2_chime002, 0);
        mSoundBad = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundBadID = mSoundBad.load(getApplicationContext(), R.raw.tm2_quiz003bad, 0);
        mSoundStone = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundStoneID = mSoundStone.load(getApplicationContext(), R.raw.tm2_stone001, 0);
    }
    @Override
    protected  void onPause(){
        super.onPause();
        mSoundChime.release();
        mSoundBad.release();
        mSoundStone.release();
    }

    private void println(Object text){
        System.out.println(text.toString());
    }

    private void showToast(final Object text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

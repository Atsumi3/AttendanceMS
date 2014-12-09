package info.nukoneko.attendancems.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;

import info.nukoneko.attendancems.R;
import info.nukoneko.attendancems.adapter.AttendAdapter;
import info.nukoneko.attendancems.auth.Auth;
import info.nukoneko.attendancems.common.AttendanceUtil;
import info.nukoneko.attendancems.common.ReadNFC;
import info.nukoneko.attendancems.common.network.SendID;
import info.nukoneko.attendancems.common.Globals;
import info.nukoneko.attendancems.common.network.SocketUtil;
import info.nukoneko.attendancems.container.EntryObject;
import info.nukoneko.attendancems.container.LectureObject;
import info.nukoneko.attendancems.container.OnStartUpObject;

import static java.lang.System.setProperty;

/**
 * Created by Telneko on 2014/12/04.
 */
public class MainActivity extends Activity {
    private Menu mainMenu;
    Handler mHandler;
    WebSocketClient mClient = null;

    ListView mListView;
    AttendAdapter mAdapter;

    NfcAdapter mNfcAdapter;
    PendingIntent mPendingIntent;

    /* Sound Pool */
    private SoundPool mSoundChime;
    private Integer mSoundChimeID;
    private SoundPool mSoundBad;
    private Integer mSoundBadID;
    private SoundPool mSoundStone;
    private Integer mSoundStoneID;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // getMacAddress
        Globals.macAddress = getMacAddress();

        // initialize components
        mHandler = new Handler();
        mAdapter = new AttendAdapter(this);
        mListView = (ListView)findViewById(R.id.user_list);
        mListView.setAdapter(mAdapter);

        // set auth status Text
        final TextView authStatusText = (TextView)findViewById(R.id.auth_status);
        if(!Globals.sessionToken.equals("")){
            new Auth(this, new Auth.AuthCallback() {
                @Override
                public void onSuccess() {
                    authStatusText.setText(getString(R.string.authenticated));
                }

                @Override
                public void onFailed() {
                    authStatusText.setText(getString(R.string.un_authenticated));
                }
            });
        }else{
            authStatusText.setText(getString(R.string.un_authenticated));
        }

            authStatusText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(Globals.isAuthEnable){

                    }else {
                        final TextView authStatusText = (TextView) findViewById(R.id.auth_status);
                        new Auth(MainActivity.this, Globals.serverURI.toString(), new Auth.AuthCallback() {
                            @Override
                            public void onSuccess() {
                                showToast(getString(R.string.authenticate_successfly));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        authStatusText.setText(getString(R.string.authenticated));
                                    }
                                });
                            }

                            @Override
                            public void onFailed() {
                                showToast(getString(R.string.authenticate_failed));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        authStatusText.setText(getString(R.string.un_authenticated));
                                    }
                                });
                            }
                        });
                    }
                }
            });

        // Socket
        if ("sdk".equals(Build.PRODUCT)) {
            setProperty("java.net.preferIPv6Addresses", "false");
            setProperty("java.net.preferIPv4Stack", "true");
        }
        mClient = SocketUtil.getClient(new mOnSocketActionListener());

        // initialize NfcAdapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(getApplicationContext(), getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    /***
     * MacAddressを取得して返す
     * @return MacAddress
     */
    public String getMacAddress() {
        WifiManager wm = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        return wm.getConnectionInfo().getMacAddress();
    }

    // Socket通信用のListener
    public class mOnSocketActionListener implements SocketUtil.onActionCallback {
        @Override
        public void onOpen(ServerHandshake handshake) {
            // Socketが開かれたらsessionKeyを送る
            mClient.send("{\"sessionKey\":" +  Globals.serverURI.getQueryParameter("key") + "}");
        }

        @Override
        public void onMessage(String message) {
            JsonNode jsonNode = null;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                jsonNode = objectMapper.readValue(message, JsonNode.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if ((jsonNode != null ? jsonNode.get("command") : null) != null) {
                AttendanceUtil.CommandKind command = AttendanceUtil.getCommand(jsonNode.get("command").asText());
                switch (command){
                    case onStartUp:
                        socketOnStartUp(jsonNode);
                        break;
                    case onReaderError:
                        break;
                    case onResume:
                        socketOnUpdate(jsonNode);
                        break;
                    case onRead:
                        socketOnUpdate(jsonNode);
                        break;
                    case onAdminCardReading:
                        break;
                    case onIdle:
                        break;
                    case onHeartBeat:
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            // 何らかの理由でSocketが閉じられたときに呼ばれる
            // クライアントの再接続を試みる
            mClient.close();
            mAdapter.clear();
            mClient = SocketUtil.getClient(new mOnSocketActionListener());
        }

        @Override
        public void onError(Exception ex) {
            // 鯖落ちした時などの処理
            showToast(getString(R.string.socket_close));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(MainActivity.this, QRReadActivity.class));
                    finish();
                }
            });
        }
    };

    // Socket通信を確立したときに送られてくるデータの処理
    // 講義の詳細、履修者データ、出席済みユーザが送られてくる
    public void socketOnStartUp(final JsonNode json){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                OnStartUpObject startUpObject = new OnStartUpObject(json);
                for(EntryObject object : startUpObject.getResumeEntryList()) {
                    mAdapter.add(object);
                }
                LectureObject object = startUpObject.getLecture();
                ((TextView) findViewById(R.id.lectureName)).setText(getString(R.string.lecture_name) + object.getName());
                ((TextView) findViewById(R.id.teacherName)).setText(getString(R.string.teacher_name) + object.getTeacherName());
                ((TextView) findViewById(R.id.entry)).setText(getString(R.string.entry_num) + startUpObject.getResumeEntryList().size() + "/ " + startUpObject.getEnrollmentTable().size());
                mListView.setSelection(mAdapter.getCount()-1);
            }
        });
    }

    // 誰かが出席した時などに送られてくる
    public void socketOnUpdate(final JsonNode json){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EntryObject object = new EntryObject(json);
                mAdapter.add(object);
                mListView.setSelection(mAdapter.getCount()-1);

                // 出席データの種類によって音変える
                if(object.getResult().equals(getString(R.string.entry))){
                    mSoundChime.play(mSoundChimeID, 1.0F, 1.0F, 0, 0, 1.0F);
                }else if(object.getResult().equals(getString(R.string.entered))){
                    mSoundStone.play(mSoundStoneID, 1.0F, 1.0F, 0, 0, 1.0F);
                }else{
                    mSoundBad.play(mSoundBadID, 1.0F, 1.0F, 0, 0, 1.0F);
                }
            }
        });
    }

    // Activityが再起動したときに行う
    @Override
    protected void onResume(){
        super.onResume();

        // 音声データをあらかじめセット
        mSoundChime = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundChimeID = mSoundChime.load(getApplicationContext(), R.raw.tm2_chime002, 0);
        mSoundBad = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundBadID = mSoundBad.load(getApplicationContext(), R.raw.tm2_quiz003bad, 0);
        mSoundStone = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundStoneID = mSoundStone.load(getApplicationContext(), R.raw.tm2_stone001, 0);

        // アプリケーションが起動している時に、ICカードがタッチされた際のイベントを受け取る
        // その他のICカードイベントを受けるアプリが存在していても、タッチイベントを独り占め
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }
    @Override
    protected  void onPause(){
        super.onPause();
        mSoundChime.release();
        mSoundBad.release();
        mSoundStone.release();

        // イベントの独り占めを無効に
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    private void showToast(final Object text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onNewIntent(Intent intent){
        // onResume のときに設定した foreground dispatch で
        // ICカードをタッチしたイベントを受け取り、そのIntentを処理する
        setIntent(intent);
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent){
        String action = intent.getAction();
        // Nfcにタッチした際のイベントかどうか
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)){
            sendID(ReadNFC.readNFC(getIntent()));
        }else{
            finish();
        }
    }

    //取得した番号の送信
    private void sendID(String studentID) {
        SendID.sendID(studentID, new SendID.SendIDCallback() {
            @Override
            public void onSuccess() {}

            @Override
            public void onFailed(String text) {}
        });
    }
}

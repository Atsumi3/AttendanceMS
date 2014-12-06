package info.nukoneko.attendancems.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);

        // DEBUG
        Globals.serverURI = Uri.parse("http://192.168.0.7:8888/?key=1234");

        Globals.setMacAddress(getApplication());

        //init components
        mHandler = new Handler();
        mAdapter = new AttendAdapter(this);
        mListView = (ListView)findViewById(R.id.user_list);
        mListView.setAdapter(mAdapter);

        if ("sdk".equals(Build.PRODUCT)) {
            setProperty("java.net.preferIPv6Addresses", "false");
            setProperty("java.net.preferIPv4Stack", "true");
        }
        mClient = SocketUtil.getClient(new mOnSocketActionListener());
    }


    public class mOnSocketActionListener implements SocketUtil.onActionCallback {
        @Override
        public void onOpen(ServerHandshake handshake) {
            mClient.send("{\"sessionKey\":1234}");
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
            mClient.close();
            mAdapter.clear();
            mClient = SocketUtil.getClient(new mOnSocketActionListener());

            MainActivity.this.findViewById(R.id.b_send_data).setEnabled(false);
            MainActivity.this.findViewById(R.id.b_auth).setEnabled(true);
        }

        @Override
        public void onError(Exception ex) {
            showToast("Socket Error...");
        }
    };

    public void socketOnStartUp(final JsonNode json){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                OnStartUpObject startUpObject = new OnStartUpObject(json);
                for(EntryObject object : startUpObject.getResumeEntryList()) {
                    mAdapter.add(object);
                }
                LectureObject object = startUpObject.getLecture();
                ((TextView) findViewById(R.id.lectureName)).setText("授業名: " + object.getName());
                ((TextView) findViewById(R.id.teacherName)).setText("教員名: " + object.getTeacherName());
                ((TextView) findViewById(R.id.entry)).setText("出席数: " + startUpObject.getResumeEntryList().size() + "/ " + startUpObject.getEnrollmentTable().size() + "人");
                mListView.setSelection(mAdapter.getCount()-1);
            }
        });
    }

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

    /*  サウンド制御 ここから  */

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

    /*  サウンド制御 ここまで  */

    private void println(Object text){
        System.out.println(text.toString());
    }
    private void showToast(final Object text){
        Toast.makeText(MainActivity.this, text.toString(), Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting, menu);
        mainMenu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            // メニュー表示
            if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
                if (mainMenu != null) {
                    mainMenu.performIdentifierAction(R.id.b_setting, 0);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Integer itemID = item.getItemId();
        switch (itemID){
            case R.id.menu_auth:
                new Auth(this, Globals.serverURI.toString(), new Auth.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        showToast("認証成功");
                    }
                    @Override
                    public void onFailed() {
                        showToast("認証失敗");
                    }
                });
                break;
            case R.id.menu_send:
                if (Globals.hash.equals("")) {
                    showToast("認証されていません");
                } else {
                    SendID.sendID("1140096", new SendID.SendIDCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("成功");
                        }

                        @Override
                        public void onFailed(String text) {
                            showToast("失敗\n" + text);
                        }
                    });
                }
                break;
            default:
                break;
        }
        return true;
    }
}

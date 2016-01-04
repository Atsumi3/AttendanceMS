package info.nukoneko.attendancems.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.Toast;


import java.net.ConnectException;

import info.nukoneko.attendancems.R;
import info.nukoneko.attendancems.common.Globals;
import info.nukoneko.attendancems.common.network.Async;
import info.nukoneko.attendancems.common.network.AsyncCallback;
import info.nukoneko.attendancems.common.network.SendID;
import info.nukoneko.attendancems.common.network.SendUtil;

/**
 * Created by Telneko on 2014/12/09.
 */
public class FirstActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isEnableNetWorkConnect(this)) {
            Toast.makeText(this, getString(R.string.not_connected_network), Toast.LENGTH_SHORT).show();
            finish();
        }
        // debug
        Globals.isDebug = false;
        Globals.serverURI = Uri.parse("http://192.168.0.7:8888/?key=1234");

        // エントリーの重複を無視するかどうか
        Globals.isIgnoreDuplicatedEntry = false;

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_first);

        Globals globals = (Globals) this.getApplication();
        // load success
        if (!globals.loadSettingPreference()) {
            intentQRReadActivity();
        }else{
            isExistServer();
        }
    }
    public boolean isEnableNetWorkConnect(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
    public void isExistServer(){
        new Async<>(new AsyncCallback<String>() {
            @Override
            public String doFunc(Object... params) {
                return SendUtil.send(Async.method.GET, Async.Protocol.HTTP, Globals.serverURI.toString(), null);
            }

            @Override
            public void onResult(String result) {
                if (result == null || result.contains("Error:")) {
                    // NotExist
                    intentQRReadActivity();
                } else {
                    //Exist
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            Intent intent = new Intent(FirstActivity.this, MainActivity.class);
                            startActivity(intent);
                            FirstActivity.this.finish();
                        }
                    }, 2000);
                }
            }
        }).run();
    }

    public void intentQRReadActivity(){
        new Handler().postDelayed(new Runnable() {
            public void run() {
                Intent intent;
                // Android のバージョンで カメラのビューを分岐
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    intent = new Intent(FirstActivity.this, QRReadActivity2.class);
                } else {
                    intent = new Intent(FirstActivity.this, QRReadActivity.class);
                }
                startActivity(intent);
                FirstActivity.this.finish();
            }
        }, 2000);
    }
}

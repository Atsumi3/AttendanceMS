package info.nukoneko.attendancems.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.util.List;

import info.nukoneko.attendancems.R;
import info.nukoneko.attendancems.common.Globals;
import info.nukoneko.attendancems.common.network.Async;
import info.nukoneko.attendancems.common.network.AsyncCallback;
import info.nukoneko.attendancems.common.network.SendUtil;

/**
 * Created by Telneko on 2014/12/09.
 */
public class QRReadActivity extends Activity {
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_qr_read);

        mSurfaceView = (SurfaceView)findViewById(R.id.surface);
        mSurfaceView.setOnClickListener(onSurfaceClickListener);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(callback);
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            mCamera = Camera.open();

            // ここで回転を決める
            if(isPortrait()) {
                mCamera.setDisplayOrientation(90);
            }else{
                mCamera.setDisplayOrientation(0);
            }

            try{
                mCamera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = getOptimalPreviewSize(sizes, i2, i3);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            mCamera.setParameters(parameters);

            mCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            mCamera.release();
            mCamera = null;
        }
    };

    protected boolean isPortrait() {
        return (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }
    private View.OnClickListener onSurfaceClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(mCamera != null){
                mCamera.autoFocus(autoFocusCallback);
            }
        }
    };
    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean b, Camera camera) {
            camera.setOneShotPreviewCallback(previewCallback);
        }
    };
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            int width = camera.getParameters().getPreviewSize().width;
            int height = camera.getParameters().getPreviewSize().height;
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(bytes,width,height,0,0,width,height,false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader multiFormatReader = new MultiFormatReader();
            try{
                Result result = multiFormatReader.decode(bitmap);
                isExistServer(result.getText());
            } catch (NotFoundException e) {
                e.printStackTrace();
                Toast.makeText(QRReadActivity.this, getString(R.string.read_failed), Toast.LENGTH_SHORT).show();
            }
        }
    };
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    public void isExistServer(final String url){
        Toast.makeText(QRReadActivity.this, getString(R.string.read_success), Toast.LENGTH_SHORT).show();
        try {
            System.out.println(Uri.parse(url).getHost());
        }
        catch (NullPointerException e){
            return;
        }
        new Async<>(new AsyncCallback<String>() {
            @Override
            public String doFunc(Object... params) {
                return SendUtil.send(Async.method.GET, Async.Protocol.HTTP, url, null);
            }

            @Override
            public void onResult(String result) {
                if(result == null || result.contains("Error:")){
                    Toast.makeText(QRReadActivity.this, getString(R.string.read_failed_qr), Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    Toast.makeText(QRReadActivity.this, getString(R.string.read_success), Toast.LENGTH_SHORT).show();
                    Globals.serverURI = Uri.parse(url);
                    ((Globals)QRReadActivity.this.getApplication()).saveSettingPreference();
                    Intent intent = new Intent(QRReadActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }).run();
    }
}

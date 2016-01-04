package info.nukoneko.attendancems.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import info.nukoneko.attendancems.R;
import info.nukoneko.attendancems.common.Globals;
import info.nukoneko.attendancems.common.network.Async;
import info.nukoneko.attendancems.common.network.AsyncCallback;
import info.nukoneko.attendancems.common.network.SendUtil;

/**
 * Created by Telneko on 2014/12/09.
 */
public class QRReadActivity2 extends Activity {
    private Size mPreviewSize;
    private TextureView mTextureView;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_qr_read2);

        mTextureView = (TextureView) findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(callback);

    }

    private final TextureView.SurfaceTextureListener callback = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            prepareCameraView();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @TargetApi(android.os.Build.VERSION_CODES.M)
    private void prepareCameraView() {
        // Camera機能にアクセスするためのCameraManagerの取得.
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // Back Cameraを取得してOpen.
            for (String strCameraId : manager.getCameraIdList()) {
                // Cameraから情報を取得するためのCharacteristics.
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(strCameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    // Front Cameraならスキップ.
                    continue;
                }
                // ストリームの設定を取得(出力サイズを取得する).
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

                // プレビュー画面のサイズ調整.
                this.configureTransform();

                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                manager.openCamera(strCameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        mCameraDevice = camera;
                        createCameraPreviewSession();
                    }

                    @Override
                    public void onDisconnected(CameraDevice cmdCamera) {
                        cmdCamera.close();
                        mCameraDevice = null;
                    }

                    @Override
                    public void onError(CameraDevice cmdCamera, int error) {
                        cmdCamera.close();
                        mCameraDevice = null;
                    }
                }, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(21)
    protected void createCameraPreviewSession() {
        if(null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if(null == texture) {
            return;
        }
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);
        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);
        try {
            mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                    Toast.makeText(QRReadActivity2.this, "onConfigureFailed", Toast.LENGTH_LONG).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    @TargetApi(21)
    protected void updatePreview() {
        if(null == mCameraDevice) {
            return;
        }
        // オートフォーカスモードに設定する.
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // 別スレッドで実行.
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try {
            // 画像を繰り返し取得してTextureViewに表示する.
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(21)
    public void onConfigurationChanged(Configuration newConfig)
    {
        // 画面の回転・サイズ変更でプレビュー画像の向きを変更する.
        super.onConfigurationChanged(newConfig);
        this.configureTransform();
    }
    @TargetApi(21)
    private void configureTransform()
    {
        // 画面の回転に合わせてmTextureViewの向き、サイズを変更する.
        if (null == mTextureView || null == mPreviewSize)
        {
            return;
        }
        Display display = getWindowManager().getDefaultDisplay();

        int rotation = display.getRotation();
        Matrix matrix = new Matrix();

        Point pntDisplay = new Point();
        display.getSize(pntDisplay);

        RectF rctView = new RectF(0, 0, pntDisplay.x, pntDisplay.y);
        RectF rctPreview = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = rctView.centerX();
        float centerY = rctView.centerY();

        rctPreview.offset(centerX - rctPreview.centerX(), centerY - rctPreview.centerY());
        matrix.setRectToRect(rctView, rctPreview, Matrix.ScaleToFit.FILL);
        float scale = Math.max(
                rctView.width() / mPreviewSize.getWidth(),
                rctView.height() / mPreviewSize.getHeight()
        );
        matrix.postScale(scale, scale, centerX, centerY);

        switch (rotation) {
            case Surface.ROTATION_0:
                matrix.postRotate(0, centerX, centerY);
                break;
            case Surface.ROTATION_90:
                matrix.postRotate(270, centerX, centerY);
                break;
            case Surface.ROTATION_180:
                matrix.postRotate(180, centerX, centerY);
                break;
            case Surface.ROTATION_270:
                matrix.postRotate(90, centerX, centerY);
                break;
        }
        mTextureView.setTransform(matrix);
    }

//    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(bytes,width,height,0,0,width,height,false);
//    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
//    MultiFormatReader multiFormatReader = new MultiFormatReader();
//    try{
//        Result result = multiFormatReader.decode(bitmap);
//        isExistServer(result.getText());
//    } catch (NotFoundException e) {
//        e.printStackTrace();
//        Toast.makeText(QRReadActivity2.this, getString(R.string.read_failed), Toast.LENGTH_SHORT).show();
//    }

    public void isExistServer(final String url){
        Toast.makeText(QRReadActivity2.this, getString(R.string.read_success), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(QRReadActivity2.this, getString(R.string.read_failed_qr), Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(QRReadActivity2.this, getString(R.string.read_success), Toast.LENGTH_SHORT).show();
                    Globals.serverURI = Uri.parse(url);
                    ((Globals)QRReadActivity2.this.getApplication()).saveSettingPreference();
                    Intent intent = new Intent(QRReadActivity2.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }).run();
    }
}

package com.bai.minicamera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by 201704012 on 2017/10/18.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CaptureActivity extends AppCompatActivity {
    // 从屏幕旋转到JPEG的转换。
    private final static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    /**
     * 在关闭相机前，防止应用退出.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    /**
     * 摄像头id
     */
    private String mCameraId;

    private final TextureView.SurfaceTextureListener mSufaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
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

    private CameraDevice mCameraDevice;
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };


    private AutoFitTextureView maftv_capture_preview;// TextureView
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (maftv_capture_preview.isAvailable()) {
            openCamera(maftv_capture_preview.getWidth(), maftv_capture_preview.getHeight());// 打开相机
        } else {
            maftv_capture_preview.setSurfaceTextureListener(mSufaceTextureListener);// 预览控件设置监听
        }
    }


    /**
     * 控件初始化
     */
    private void initUI() {
        maftv_capture_preview = (AutoFitTextureView) findViewById(R.id.aftv_capture_preview);// 预览控件
    }

    /**
     * 开启一个子线程
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");// 创建一个线程
        mBackgroundThread.start();// 线程启动
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    /**
     * 打开相机
     *
     * @param width
     * @param height
     */
    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissions();
            return;
        }

        setUpCameraOutputs(width, height);// 设置相机参数
       // configureTransform(width, height);// 设置相机

        // 相机管理
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);// 打开相机
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }

    }

    /**
     * @param width
     * @param height 相机参数设置
     */
    private void setUpCameraOutputs(int width, int height) {
        // 获取相机管理
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            setCameraId(0,cameraManager); // 后置
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(mCameraId);// 摄像头对应的特性对象



        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置摄像头
     * 1:前置
     * 0：后置
     */
    private void setCameraId(int id, CameraManager cameraManager){
        if(id==0){
            mCameraId = getRearFacingCameraId(cameraManager);// 后置
        }else if(id==1){
            mCameraId = getFrontFacingCameraId(cameraManager);// 前置
        }
    }

    /**
     * 获取前置摄像头id
     */
    private String getFrontFacingCameraId( CameraManager cameraManager) {
        // 获取相机管理
        String frontFacingCameraId = "";
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);// 摄像头对应的特性对象

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontFacingCameraId = cameraId;
                    return frontFacingCameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return frontFacingCameraId;
    }

    /**
     * 获取后置摄像头id
     */
    private String getRearFacingCameraId( CameraManager cameraManager) {
        String rearFacingCameraId = "";
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);// 摄像头对应的特性对象

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    rearFacingCameraId = cameraId;
                    return rearFacingCameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return rearFacingCameraId;
    }

    /**
     * 请求相机权限
     */
    private void requestCameraPermissions() {

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage("请求相机权限")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    1);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }
}

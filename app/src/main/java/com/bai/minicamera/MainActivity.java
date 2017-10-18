package com.bai.minicamera;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    private SurfaceView msv_capture_preview;
    private ImageView miv_photo_show;
    private SurfaceHolder mSurfaceHolder;

    private static final SparseIntArray CAMERA_ORIENTATIONS = new SparseIntArray();
    static {
        CAMERA_ORIENTATIONS.append(Surface.ROTATION_0,90);
        CAMERA_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        CAMERA_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        CAMERA_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Handler mChildHandler;
    private Handler mMainHandler;
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private Button mbtn_click;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_camera);
        initUi();
        initUiDate();
    }


    /**
     * 控件初始化
     */
    private void initUi() {
        msv_capture_preview = (SurfaceView) findViewById(R.id.sv_capture_preview);
        miv_photo_show = (ImageView) findViewById(R.id.iv_photo_show);
       // mbtn_click = (Button) findViewById(R.id.btn_click);
    }


    /**
     * 控件数据初始化
     */
    private void initUiDate() {

        // 获取surfaceholder
        mSurfaceHolder = msv_capture_preview.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback2() {
            @Override
            public void surfaceRedrawNeeded(SurfaceHolder holder) {

            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //初始化相机
                initCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (null != mCameraDevice){
                    mCameraDevice.close();
                    MainActivity.this.mCameraDevice =null;
                }
            }
        });

       /* mbtn_click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.basic:
                        takePicture();
                        break;
                    default:
                        break;
                }
            }
        });*/
    }

    /**
     * 相机初始化
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void initCamera() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mChildHandler = new Handler(handlerThread.getLooper());
        mMainHandler = new Handler(getMainLooper());
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int widthPixels = displayMetrics.widthPixels;
        int heightPixels = displayMetrics.heightPixels;
        mImageReader = ImageReader.newInstance(widthPixels,heightPixels, ImageFormat.JPEG,1);

        // 监听器
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mCameraDevice.close();
                msv_capture_preview.setVisibility(View.GONE);
                miv_photo_show.setVisibility(View.VISIBLE);
                // 拿到拍照照片数据
                Image image = reader.acquireNextImage();

                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);//由缓冲区存入字节数组
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    miv_photo_show.setImageBitmap(bitmap);
                }


            }
        },mMainHandler);

        getCameraManage();
    }

    /**
     * 获取相机管理
     */
    private void getCameraManage() {
        String CameraID ="" + CameraCharacteristics.LENS_FACING_FRONT;// 获取后置摄像头编号
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        //检查权限
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            return;
        }
        // 打开摄像头
        try {
            cameraManager.openCamera(CameraID,stateCallback,mMainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private CameraDevice mCameraDevice;
    /**
     * 摄像头监听
     */
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback(){

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            //开启预览
            takePreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice.close();
            MainActivity.this.mCameraDevice =null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Toast.makeText(MainActivity.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
        }

    };

    private ArrayList<Surface> mArrayListSurface = new ArrayList<>();

    /**
     * 开启预览
     */
    private void takePreview() {
        //创建预览需要的CaptureRequest.Builder
        try {
            final CaptureRequest.Builder previewRequsetBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //把surfaceview 的surface作为CaptureRequest.Builder的目标
            previewRequsetBuilder.addTarget(mSurfaceHolder.getSurface());
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mArrayListSurface.add(mSurfaceHolder.getSurface());
            mArrayListSurface.add(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(mArrayListSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (null == mCameraDevice) {
                        return;
                    }
                    // 当摄像头已经准备好时，开始显示预览
                    mCameraCaptureSession = session;
                    previewRequsetBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    // 打开闪光灯
                    previewRequsetBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    // 显示预览
                    CaptureRequest previewRequest = previewRequsetBuilder.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mChildHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍照
     */
    private void takePicture(){
        if (mCameraDevice == null){
                return;
        }
            // 创建拍照需要的CaptureRequest.Builder
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader.getSurface());
                // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                // 获取手机方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
                // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, CAMERA_ORIENTATIONS.get(rotation));
                //拍照
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mCameraCaptureSession.capture(mCaptureRequest, null, mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void getCameraPara( CaptureRequest.Builder captureRequestBuilder){

    }
}



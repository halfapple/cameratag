package com.app.cameratag;

import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class SelfCameraActivity extends AppCompatActivity {

    public static final int request_code = 100;
    public static final int result_code_ok = 101;

    private Camera.Parameters parameters = null;
    private Camera cameraInst = null;
    private Bundle bundle = null;

    private Handler handler = new Handler();

    private Bitmap mTempBitmap;
    private int PHOTO_HEIGHT = 0;
    private int PHOTO_WIDTH = 0;
    private int targetWidthPix = 0;
    private int targetHeightPix = 0;
    private float scaleWidth;
    private float scaleHeight;

    private Camera.Size adapterSize = null;
    private Camera.Size previewSize = null;

    private final Handler mHandlerTimer = new Handler();
    private boolean updateTimeFlag = true;

    //widgets
    private TextView mTimeTv;
    private Button mTakePhotoBtn;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initView();
        initEvent();

        updateTimeTv();
    }

    private void initView() {

        mTimeTv = (TextView)findViewById(R.id.time_tv);
        mTakePhotoBtn = (Button)findViewById(R.id.takepicture);
        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);

        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);
        mSurfaceView.setFocusable(true);
        mSurfaceView.setBackgroundColor(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
        mSurfaceView.getHolder().addCallback(new MySurfaceCallback());
    }

    private void initEvent() {
        mTakePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    updateTimeFlag = false;
                    cameraInst.takePicture(null, null, new MyPictureCallback());
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(getApplicationContext(), "error ", Toast.LENGTH_LONG).show();
                    try {
                        cameraInst.startPreview();
                    } catch (Throwable e) {

                    }
                }
            }
        });
    }

    private Runnable mUpdateTime = new Runnable() {
        @Override
        public void run() {
            updateTimeTv();
        }
    };

    private void updateTimeTv() {
        if (!updateTimeFlag) {
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mTimeTv.setText(df.format(new Date()));

        mHandlerTimer.postDelayed(mUpdateTime, 1000);
    }

    private final class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            bundle = new Bundle();
            bundle.putByteArray("bytes", data);
            new SavePicTask(data).execute();
            //camera.startPreview();
        }
    }

    private class SavePicTask extends AsyncTask<Void, Void, String> {
        private byte[] data;

        protected void onPreExecute() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }

        SavePicTask(byte[] data) {
            this.data = data;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return saveToSDCard(data);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        /*
         * result = /storage/sdcard0/DCIM/Camera/xxx.jpg
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (!TextUtils.isEmpty(result)) {
                Intent i = new Intent();
                i.putExtra("absPath", Uri.parse(result).toString());
                setResult(result_code_ok, i);
                finish();

            } else {
                Toast.makeText(getApplicationContext(), "error in store", Toast.LENGTH_LONG).show();
            }
        }
    }

    private final class MySurfaceCallback implements SurfaceHolder.Callback {

        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                if (cameraInst != null) {
                    cameraInst.stopPreview();
                    cameraInst.release();
                    cameraInst = null;
                }
            } catch (Exception e) {

            }

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (null == cameraInst) {
                try {
                    cameraInst = Camera.open();
                    cameraInst.setPreviewDisplay(holder);
                    initCamera();
                    cameraInst.startPreview();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            autoFocus();
        }
    }

    //实现自动对焦
    private void autoFocus() {
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (cameraInst == null) {
                    return;
                }
                cameraInst.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            initCamera();
                        }
                    }
                });
            }
        };
    }

    private void initCamera() {
        parameters = cameraInst.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);

        setUpPicSize(parameters);
        setUpPreviewSize(parameters);
        //}
        if (adapterSize != null) {
            parameters.setPictureSize(adapterSize.width, adapterSize.height);
        }
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        setDispaly(parameters, cameraInst);
        try {
            cameraInst.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cameraInst.startPreview();
        cameraInst.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }

    private void setUpPicSize(Camera.Parameters parameters) {

        if (adapterSize != null) {
            return;
        } else {
            adapterSize = findBestPictureResolution();
            return;
        }
    }

    private void setUpPreviewSize(Camera.Parameters parameters) {

        if (previewSize != null) {
            return;
        } else {
            previewSize = findBestPreviewResolution();
        }
    }

    /**
     * 最小预览界面的分辨率
     */
    private static final int MIN_PREVIEW_PIXELS = 480 * 320;

    /**
     * 最大宽高比差
     */
    private static final double MAX_ASPECT_DISTORTION = 0.15;
    private static final String TAG = "Camera";

    /**
     * 找出最适合的预览界面分辨率
     */
    private Camera.Size findBestPreviewResolution() {
        Camera.Parameters cameraParameters = cameraInst.getParameters();
        Camera.Size defaultPreviewResolution = cameraParameters.getPreviewSize();

        List<Camera.Size> rawSupportedSizes = cameraParameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            return defaultPreviewResolution;
        }

        // 按照分辨率从大到小排序
        List<Camera.Size> supportedPreviewResolutions = new ArrayList<Camera.Size>(rawSupportedSizes);
        Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        StringBuilder previewResolutionSb = new StringBuilder();
        for (Camera.Size supportedPreviewResolution : supportedPreviewResolutions) {
            previewResolutionSb.append(supportedPreviewResolution.width).append('x').append(supportedPreviewResolution.height)
                    .append(' ');
        }
        Log.v(TAG, "Supported preview resolutions: " + previewResolutionSb);


        // 移除不符合条件的分辨率
        double screenAspectRatio = (double) DensityUtil.getScreenWidth(getApplicationContext())
                / (double) DensityUtil.getScreenHeight(getApplicationContext());
        Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;

            // 移除低于下限的分辨率，尽可能取高分辨率
            if (width * height < MIN_PREVIEW_PIXELS) {
                it.remove();
                continue;
            }

            // 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
            // 由于camera的分辨率是width>height，我们设置的portrait模式中，width<height
            // 因此这里要先交换然preview宽高比后在比较
            boolean isCandidatePortrait = width > height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }

            // 找到与屏幕分辨率完全匹配的预览界面分辨率直接返回
            if (maybeFlippedWidth == DensityUtil.getScreenWidth(getApplicationContext())
                    && maybeFlippedHeight == DensityUtil.getScreenHeight(getApplicationContext())) {
                return supportedPreviewResolution;
            }
        }

        // 如果没有找到合适的，并且还有候选的像素，则设置其中最大比例的，对于配置比较低的机器不太合适
        if (!supportedPreviewResolutions.isEmpty()) {
            Camera.Size largestPreview = supportedPreviewResolutions.get(0);
            return largestPreview;
        }

        // 没有找到合适的，就返回默认的

        return defaultPreviewResolution;
    }

    private Camera.Size findBestPictureResolution() {
        Camera.Parameters cameraParameters = cameraInst.getParameters();
        List<Camera.Size> supportedPicResolutions = cameraParameters.getSupportedPictureSizes(); // 至少会返回一个值

        StringBuilder picResolutionSb = new StringBuilder();
        for (Camera.Size supportedPicResolution : supportedPicResolutions) {
            picResolutionSb.append(supportedPicResolution.width).append('x')
                    .append(supportedPicResolution.height).append(" ");
        }
        Log.d(TAG, "Supported picture resolutions: " + picResolutionSb);

        Camera.Size defaultPictureResolution = cameraParameters.getPictureSize();

        // 排序
        List<Camera.Size> sortedSupportedPicResolutions = new ArrayList<Camera.Size>(
                supportedPicResolutions);
        Collections.sort(sortedSupportedPicResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        // 移除不符合条件的分辨率
        double screenAspectRatio = (double) DensityUtil.getScreenWidth(getApplicationContext())
                / (double) DensityUtil.getScreenHeight(getApplicationContext());
        Iterator<Camera.Size> it = sortedSupportedPicResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;

            // 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
            // 由于camera的分辨率是width>height，我们设置的portrait模式中，width<height
            // 因此这里要先交换然后在比较宽高比
            boolean isCandidatePortrait = width > height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }
        }

        // 如果没有找到合适的，并且还有候选的像素，对于照片，则取其中最大比例的，而不是选择与屏幕分辨率相同的
        if (!sortedSupportedPicResolutions.isEmpty()) {
            return sortedSupportedPicResolutions.get(0);
        }

        // 没有找到合适的，就返回默认的
        return defaultPictureResolution;
    }

    //控制图像的正确显示方向
    private void setDispaly(Camera.Parameters parameters, Camera camera) {
        if (Build.VERSION.SDK_INT >= 8) {
            setDisplayOrientation(camera, 90);
        } else {
            parameters.setRotation(90);
        }
    }

    //实现的图像的正确显示
    private void setDisplayOrientation(Camera camera, int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation",
                    new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, new Object[]{i});
            }
        } catch (Exception e) {
            Log.e("Came_e", "图像出错");
        }
    }

    public String saveToSDCard(byte[] data) throws IOException {
        Bitmap croppedImage;

        //获得图片大小
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        PHOTO_WIDTH = options.outWidth;
        PHOTO_HEIGHT = options.outHeight;

        try {
            croppedImage = decodeRegionCrop(data);
        } catch (Exception e) {
            return null;
        }

        String imagePath = saveToFile(
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera",
                croppedImage);

        if (croppedImage != null) {
            croppedImage.recycle();
        }

        return imagePath;
    }

    private Runnable rr = new Runnable() {
        @Override
        public void run() {
            get_overlay();
        }
    };

    private synchronized Bitmap get_overlay() {
        if (mTempBitmap == null) {
            mTempBitmap = Bitmap.createBitmap(targetWidthPix, targetHeightPix, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mTempBitmap);

            LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
            RelativeLayout layout = (RelativeLayout)layoutInflater.inflate(R.layout.camera_marker, null);

            //top begin
            LinearLayout topRoot = (LinearLayout)layout.findViewById(R.id.marker_top_root);

            View topGap = layout.findViewById(R.id.top_gap);
            topGap.getLayoutParams().height = (int)(topGap.getLayoutParams().height * scaleHeight);

            TextView timeTv = (TextView)layout.findViewById(R.id.time_tv);
            timeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            timeTv.setText(mTimeTv.getText().toString());

            topRoot.requestLayout();
            //top end


            //bottom begin
            LinearLayout bottomRoot = (LinearLayout)layout.findViewById(R.id.marker_bottom_root);
            bottomRoot.getLayoutParams().height = (int)(bottomRoot.getLayoutParams().height * scaleHeight);
            bottomRoot.setPadding((int)(bottomRoot.getPaddingLeft() * scaleWidth),
                    bottomRoot.getPaddingTop(),
                    (int)(bottomRoot.getPaddingRight() * scaleWidth),
                    bottomRoot.getPaddingBottom());

            ImageView botIcon = (ImageView) bottomRoot.findViewById(R.id.bottom_icon);
            botIcon.getLayoutParams().width = (int)(botIcon.getLayoutParams().width * scaleWidth);
            botIcon.getLayoutParams().height = (int)(botIcon.getLayoutParams().height * scaleHeight);

            bottomRoot.requestLayout();
            //bottom end

            layout.setDrawingCacheEnabled(true);
            layout.measure(View.MeasureSpec.makeMeasureSpec(canvas.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(canvas.getHeight(), View.MeasureSpec.EXACTLY));
            layout.layout(0, 0, layout.getMeasuredWidth(), layout.getMeasuredHeight());
            layout.draw(canvas);
            canvas.drawColor(Color.TRANSPARENT);
            notify();
        }

        return mTempBitmap;
    }

    private synchronized Bitmap decodeRegionCrop(byte[] data) {
        System.gc();
        Bitmap croppedImage = null;
        try {

            croppedImage = BitmapFactory.decodeByteArray(data , 0, data.length);

        } catch (Throwable e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

        Matrix m = new Matrix();

        m.setRotate(90, PHOTO_WIDTH / 2, PHOTO_HEIGHT / 2);

        Bitmap rotatedImage = Bitmap.createBitmap(croppedImage, 0, 0,
                croppedImage.getWidth(), croppedImage.getHeight(), m, true);

        Paint photoPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        Bitmap icon = Bitmap.createBitmap(rotatedImage.getWidth(), rotatedImage.getHeight(), rotatedImage.getConfig());
        Canvas canvas = new Canvas(icon);
        Rect src = new Rect(0, 0, rotatedImage.getWidth(), rotatedImage.getHeight());
        canvas.drawBitmap(rotatedImage, src, src, photoPaint);
        if (targetWidthPix == 0 || targetHeightPix == 0) {
            try {
                targetWidthPix = rotatedImage.getWidth();
                targetHeightPix = rotatedImage.getHeight();
                scaleWidth = ((float) rotatedImage.getWidth()) / DensityUtil.getScreenWidth(getApplicationContext());
                scaleHeight = ((float)rotatedImage.getHeight()) / DensityUtil.getScreenHeight(getApplicationContext());
                handler.postDelayed(rr, 200);
                wait();
            } catch (InterruptedException e) {

            }
        }

        Bitmap temp = get_overlay();
        canvas.drawBitmap(temp, src, src, photoPaint);

        if (rotatedImage != croppedImage) {
            croppedImage.recycle();
        }
        
        return icon;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    private void releaseCamera() {
        if (cameraInst != null) {
            cameraInst.setPreviewCallback(null);
            cameraInst.release();
            cameraInst = null;
        }
        adapterSize = null;
        previewSize = null;
    }

    public static boolean mkdir(File file) {
        while (!file.getParentFile().exists()) {
            mkdir(file.getParentFile());
        }
        return file.mkdir();
    }

    //保存图片文件
    public static String saveToFile(String fileFolderStr, Bitmap croppedImage) throws FileNotFoundException, IOException {
        File jpgFile;

        File fileFolder = new File(fileFolderStr);
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        if (!fileFolder.exists()) {
            mkdir(fileFolder);
        }
        jpgFile = new File(fileFolder, filename);

        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
        croppedImage.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);

        try {
            outputStream.close();

        } catch (IOException e) {

        }

        return jpgFile.getPath();
    }

}

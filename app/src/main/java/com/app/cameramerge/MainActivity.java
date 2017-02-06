package com.app.cameramerge;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.app.cameramerge.deprecatedCamera.DeprecatedCameraActivity;
import com.app.cameramerge.overlayCamera.OverlayCameraActivity;
import com.app.cameramerge.screenshotCamera.ScreenShotCameraActivity;
import com.app.cameramerge.util.CrashLogHandler;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class MainActivity extends AppCompatActivity {

    private ImageView mImageApi;
    private String mFileUrlApi;

    private ImageView mImageView0;
    private String mFileUrl0;

    private ImageView mImageView;
    private String mFileUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CrashLogHandler crashLogHandler = CrashLogHandler.getInstance();
        crashLogHandler.init(getApplicationContext());

        setContentView(R.layout.activity_main);

        initImageLoader(getApplicationContext());

        findViewById(R.id.camera_api_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, DeprecatedCameraActivity.class);
                startActivityForResult(i, DeprecatedCameraActivity.request_code);

            }
        });

        findViewById(R.id.overlay_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, OverlayCameraActivity.class);
                startActivityForResult(i, OverlayCameraActivity.request_code);
            }
        });

        findViewById(R.id.screenshot_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ScreenShotCameraActivity.class);
                startActivityForResult(i, ScreenShotCameraActivity.request_code);
            }
        });

        mImageApi = (ImageView)findViewById(R.id.photo_api);
        mImageApi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, PhotoViewActivity.class);
                i.putExtra(PhotoViewActivity.URL_KEY, mFileUrlApi);
                startActivity(i);
            }
        });

        mImageView0 = (ImageView)findViewById(R.id.photo_iv0);
        mImageView0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, PhotoViewActivity.class);
                i.putExtra(PhotoViewActivity.URL_KEY, mFileUrl0);
                startActivity(i);
            }
        });

        mImageView = (ImageView) findViewById(R.id.photo_iv);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, PhotoViewActivity.class);
                i.putExtra(PhotoViewActivity.URL_KEY, mFileUrl);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == DeprecatedCameraActivity.request_code &&
                resultCode == DeprecatedCameraActivity.result_code_ok) {
            mFileUrlApi = "file://" + data.getStringExtra("absPath");
            ImageLoader.getInstance().displayImage(mFileUrlApi, mImageApi);

        } else if (requestCode == OverlayCameraActivity.request_code &&
                resultCode == OverlayCameraActivity.result_code_ok) {

            mFileUrl = "file://" + data.getStringExtra("absPath");
            ImageLoader.getInstance().displayImage(mFileUrl, mImageView);

        } else if (requestCode == ScreenShotCameraActivity.request_code &&
                resultCode == ScreenShotCameraActivity.result_code_ok) {

            mFileUrl0 = "file://" + data.getStringExtra("absPath");
            ImageLoader.getInstance().displayImage(mFileUrl0, mImageView0);
        }
    }

    public void initImageLoader(Context ctx) {
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(false)
                .imageScaleType(ImageScaleType.EXACTLY)
                .cacheOnDisk(true)
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(ctx)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .defaultDisplayImageOptions(defaultOptions)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .diskCache(new UnlimitedDiskCache(StorageUtils.getOwnCacheDirectory(ctx,
                        Environment.getExternalStorageDirectory() + "/cameraTag")))
                .diskCacheSize(100 * 1024 * 1024).tasksProcessingOrder(QueueProcessingType.LIFO)
                .memoryCache(new LruMemoryCache(2 * 1024 * 1024)).memoryCacheSize(2 * 1024 * 1024)
                .threadPoolSize(3)
                .build();
        ImageLoader.getInstance().init(config);
    }
}

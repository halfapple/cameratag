package com.app.cameramerge;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.nostra13.universalimageloader.core.ImageLoader;

import uk.co.senab.photoview.PhotoView;


public class PhotoViewActivity extends AppCompatActivity {

    public final static String TAG = PhotoViewActivity.class.getSimpleName();
    public final static String URL_KEY = TAG + "url_key";

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_photo_view);
        PhotoView iv = (PhotoView)findViewById(R.id.iv_photo);

        String picUrl = getIntent().getStringExtra(URL_KEY);
        if (!TextUtils.isEmpty(picUrl)) {
            ImageLoader.getInstance().displayImage(picUrl, iv);
        }
    }
}

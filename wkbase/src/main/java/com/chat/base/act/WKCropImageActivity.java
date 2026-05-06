package com.chat.base.act;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.chat.base.R;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.databinding.ActCutImgLayoutBinding;
import com.chat.base.utils.ImageUtils;

import java.io.File;

/**
 * 2020-11-27 18:14
 * 剪切图片
 */
public class WKCropImageActivity extends WKBaseActivity<ActCutImgLayoutBinding> {

    private int rotate = 0;

    @Override
    protected ActCutImgLayoutBinding getViewBinding() {
        return ActCutImgLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.crop);
    }


    @Override
    protected int getRightIvResourceId(ImageView imageView) {
        return R.mipmap.ic_ab_done;
    }

    @Override
    protected int getRightIvLeftResourceId(ImageView imageView) {
        return R.mipmap.bg_rotate_large;
    }

    private static final int MAX_AVATAR_SIZE = 400;

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();
        Bitmap bitmap = wkVBinding.cropImageView.getCroppedImage(MAX_AVATAR_SIZE, MAX_AVATAR_SIZE);
        if (bitmap == null) {
            showToast(R.string.avatar_process_fail);
            return;
        }
        ImageUtils.getInstance().saveAvatarForUpload(this, bitmap, path -> {
            Intent intent = new Intent();
            intent.putExtra("path", path);
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    @Override
    protected void rightLeftLayoutClick() {
        super.rightLeftLayoutClick();
        rotate = rotate + 90;
        wkVBinding.cropImageView.rotateImage(rotate);
    }

    @Override
    protected void initView() {
        String path = getIntent().getStringExtra("path");
        if (TextUtils.isEmpty(path)) {
            return;
        }
        Uri uri;
        if (path.startsWith("content:") || path.startsWith("file:")) {
            uri = Uri.parse(path);
        } else {
            File file = new File(path);
            uri = file.exists() ? Uri.fromFile(file) : Uri.parse(path);
        }
        wkVBinding.cropImageView.setImageUriAsync(uri);
    }
}

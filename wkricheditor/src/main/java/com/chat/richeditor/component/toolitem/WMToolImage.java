package com.chat.richeditor.component.toolitem;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.chat.base.config.WKApiConfig;
import com.chat.base.glide.ChooseMimeType;
import com.chat.base.glide.ChooseResult;
import com.chat.base.glide.GlideUtils;
import com.chat.base.net.ud.WKUploader;
import com.chat.base.ui.components.AlignImageSpan;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.WKReader;
import com.chat.richeditor.WKRichApplication;
import com.chat.richeditor.R;
import com.chat.richeditor.component.span.WMImageSpan;
import com.chat.richeditor.component.wmview.WMImageButton;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WMToolImage extends WMToolItem {

    private Context context;

    @Override
    public void applyStyle(int start, int end) {

    }

    @Override
    public List<View> getView(final Context context) {
        this.context = context;
        WMImageButton imageButton = new WMImageButton(context);

        imageButton.setImageResource(R.drawable.icon_text_picture);

        view = imageButton;
        view.setOnClickListener(v -> {
            SoftKeyboardUtils.getInstance().hideSoftKeyboard((Activity) context);
            GlideUtils.getInstance().chooseIMG((Activity) context, 9, false, ChooseMimeType.img, false, new GlideUtils.ISelectBack() {
                @Override
                public void onBack(List<ChooseResult> paths) {
                    String channelID = WKRichApplication.Companion.getInstance().iConversationContext.getChatChannelInfo().channelID;
                    byte channelType = WKRichApplication.Companion.getInstance().iConversationContext.getChatChannelInfo().channelType;
                    if (WKReader.isNotEmpty(paths)) {
                        String path = paths.get(0).path;
                        LoadingPopupView loadingPopup = new XPopup.Builder(context)
                                .asLoading(context.getString(R.string.loading));
                        loadingPopup.show();
                        GlideUtils.getInstance().compressImg(context, path, files -> {
                            if (WKReader.isNotEmpty(files)) {
                                File file = files.get(0);
                                WKUploader.getInstance().getUploadFileUrl(channelID, channelType, file.getAbsolutePath(), new WKUploader.IGetUploadFileUrl() {
                                    @Override
                                    public void onResult(String url, String fileUrl) {
                                        WKUploader.getInstance().upload(url, file.getAbsolutePath(), new WKUploader.IUploadBack() {
                                            @Override
                                            public void onSuccess(String url) {
                                                insertImage(WKApiConfig.getShowUrl(url), WMImageSpan.ImageType.URL);
                                                loadingPopup.dismiss();
                                            }

                                            @Override
                                            public void onError() {
                                                loadingPopup.dismiss();
                                            }
                                        });
                                    }
                                });
                            } else loadingPopup.dismiss();
                        });
                        //insertImage(path, WMImageSpan.ImageType.URL);
                    }
                }

                @Override
                public void onCancel() {

                }
            });

        });
        List<View> views = new ArrayList<>();
        views.add(view);
        return views;
    }

    @Override
    public void onSelectionChanged(int selStart, int selEnd) {

    }

    @Override
    public void onCheckStateUpdate() {

    }

    private void insertImage(final Object src, final WMImageSpan.ImageType type) {
        CustomTarget<Bitmap> customTarget = new CustomTarget<Bitmap>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                int width = bitmap.getWidth(), height = bitmap.getHeight();
                int[] ints = ImageUtils.getInstance().getImageWidthAndHeightToTalk(bitmap.getWidth(), bitmap.getHeight());

                bitmap = imageScale(bitmap, ints[0], ints[1]);
                ImageSpan imageSpan = null;
                if (type == WMImageSpan.ImageType.URI) {
                    imageSpan = new WMImageSpan(context, bitmap, ((Uri) src));
                } else if (type == WMImageSpan.ImageType.URL) {
                    imageSpan = new AlignImageSpan(context, ((String) src), width, height, bitmap, AlignImageSpan.ALIGN_BOTTOM) {
                        @Override
                        public void onClick(View view) {

                        }
                    };
                }
                if (imageSpan == null) {
                    return;
                }

                insertSpan(imageSpan);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };

        if (type == WMImageSpan.ImageType.URI) {
            Glide.with(context).asBitmap().load((Uri) src).centerCrop().into(customTarget);
        } else if (type == WMImageSpan.ImageType.URL) {
            Glide.with(context).asBitmap().load((String) src).centerCrop().into(customTarget);
        } else if (type == WMImageSpan.ImageType.RES) {
            ImageSpan imageSpan = new WMImageSpan(context, ((int) src));
            insertSpan(imageSpan);
        }
    }

    private void insertSpan(ImageSpan imageSpan) {
        Editable editable = this.getEditText().getEditableText();
        int start = this.getEditText().getSelectionStart();
        int end = this.getEditText().getSelectionEnd();

        SpannableStringBuilder text = new SpannableStringBuilder("\ufffc");

        text.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        editable.replace(start, end, text);

//        editable.setSpan(imageSpan,start,end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    }

    /**
     * 调整图片大小
     *
     * @param bitmap 源
     * @param dst_w  输出宽度
     * @param dst_h  输出高度
     * @return
     */
    public static Bitmap imageScale(Bitmap bitmap, int dst_w, int dst_h) {
        int src_w = bitmap.getWidth();
        int src_h = bitmap.getHeight();
        float scale_w = ((float) dst_w) / src_w;
        float scale_h = ((float) dst_h) / src_h;
        Matrix matrix = new Matrix();
        matrix.postScale(scale_w, scale_h);
        return Bitmap.createBitmap(bitmap, 0, 0, src_w, src_h, matrix,
                true);
    }

}
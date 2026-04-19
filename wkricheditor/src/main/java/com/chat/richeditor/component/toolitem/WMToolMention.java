package com.chat.richeditor.component.toolitem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.chat.base.endpoint.EndpointManager;
import com.chat.base.ui.Theme;
import com.chat.richeditor.R;
import com.chat.richeditor.component.span.WMMentionSpan;
import com.chat.richeditor.component.wmview.WMImageButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WMToolMention extends WMToolItem {
    AppCompatActivity context;

    @Override
    public void applyStyle(int start, int end) {

    }

    @Override
    public List<View> getView(Context context) {
        this.context = (AppCompatActivity) context;
        WMImageButton imageButton = new WMImageButton(context);

        imageButton.setImageResource(R.drawable.ic_mention);
        Theme.setColorFilter(context, imageButton, R.color.popupTextColor);
        view = imageButton;
        view.setOnClickListener(view1 -> EndpointManager.getInstance().invoke("choose_group_member", null));
        List<View> views = new ArrayList<>();
        views.add(view);
        return views;
    }

    public void addSpan(String showText, String uid) {
        int index = getEditText().getSelectionStart();
        Objects.requireNonNull(getEditText().getText()).insert(index, showText);
        SpannableString spannableString = new SpannableString(getEditText().getText());
        generateOneSpan(spannableString, new UnSpanText(index, index + showText.length(), showText, uid));
        getEditText().setText(spannableString);
        getEditText().setSelection(index + showText.length());
    }


    private void generateOneSpan(Spannable spannableString, UnSpanText unSpanText) {
        View spanView = getSpanView(context, unSpanText.showText.toString(), getEditText().getMeasuredWidth());
        BitmapDrawable bitmapDrawable = (BitmapDrawable) convertViewToDrawable(spanView);
        bitmapDrawable.setBounds(0, 0, bitmapDrawable.getIntrinsicWidth(), bitmapDrawable.getIntrinsicHeight());
        WMMentionSpan what = new WMMentionSpan(bitmapDrawable, unSpanText.showText.toString(), unSpanText.uid);
        final int start = unSpanText.start;
        final int end = unSpanText.end;
        spannableString.setSpan(what, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public Drawable convertViewToDrawable(View view) {
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), 30, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        view.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        cacheBmp.recycle();
        view.destroyDrawingCache();
        return new BitmapDrawable(context.getResources(), viewBmp);
    }

    public View getSpanView(Context context, String text, int maxWidth) {
        TextView view = new TextView(context);
        view.setMaxWidth(maxWidth);
        view.setText(text);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setSingleLine(true);
        //设置文字框背景色
        view.setBackgroundResource(com.chat.base.R.drawable.shape_corner_rectangle);
        view.setTextSize(14);
        //设置文字颜色
        getEditText().setGravity(Gravity.TOP);
        //view.setTextColor(getCurrentTextColor());
        view.setTextColor(ContextCompat.getColor(context, com.chat.base.R.color.colorDark));
        FrameLayout frameLayout = new FrameLayout(context);
        // frameLayout.setPadding(0, itemPadding, 0, itemPadding);
        frameLayout.addView(view);
        return frameLayout;
    }

    @Override
    public void onSelectionChanged(int selStart, int selEnd) {

    }

    @Override
    public void onCheckStateUpdate() {

    }

    private static class UnSpanText {
        int start;
        int end;
        CharSequence showText;
        String uid;

        UnSpanText(int start, int end, CharSequence showText, String uid) {
            this.start = start;
            this.end = end;
            this.showText = showText;
            this.uid = uid;
        }
    }

}

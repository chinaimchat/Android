package com.chat.richeditor.component.span;

import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

public class WMMentionSpan extends ImageSpan {

    private final String showText;
    public final String uid;

    public WMMentionSpan(Drawable d, String showText, String uid) {
        super(d);
        this.showText = showText;
        this.uid = uid;
    }

}

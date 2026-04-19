package com.chat.richeditor.component.wmview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;

import com.chat.base.ui.components.AlignImageSpan;
import com.chat.base.utils.StringUtils;
import com.chat.richeditor.WKRichApplication;
import com.chat.richeditor.component.RichStyles;
import com.chat.richeditor.component.span.WMMentionSpan;
import com.chat.richeditor.component.span.WMUnderlineSpan;
import com.chat.richeditor.component.toolitem.WMToolBold;
import com.chat.richeditor.component.toolitem.WMToolImage;
import com.chat.richeditor.component.toolitem.WMToolItalic;
import com.chat.richeditor.component.toolitem.WMToolItem;
import com.chat.richeditor.component.toolitem.WMToolMention;
import com.chat.richeditor.component.toolitem.WMToolStrikethrough;
import com.chat.richeditor.component.toolitem.WMToolTextColor;
import com.chat.richeditor.component.toolitem.WMToolTextSize;
import com.chat.richeditor.component.toolitem.WMToolUnderline;
import com.chat.richeditor.component.util.WMUtil;
import com.xinbida.wukongim.entity.WKChannelType;
import com.xinbida.wukongim.msgmodel.WKMsgEntity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class WMTextEditor extends LinearLayout {


    WMEditText editText;
    WMToolContainer toolContainer;

    private final WMToolItem toolMention = new WMToolMention();
    private final WMToolItem toolBold = new WMToolBold();
    private final WMToolItem toolItalic = new WMToolItalic();
    private final WMToolItem toolUnderline = new WMToolUnderline();
    private final WMToolItem toolStrikethrough = new WMToolStrikethrough();
    private final WMToolItem toolImage = new WMToolImage();
    private final WMToolItem toolTextColor = new WMToolTextColor();
    //    private final WMToolItem toolBackgroundColor = new WMToolBackgroundColor();
    private final WMToolItem toolTextSize = new WMToolTextSize();
//    private final WMToolItem toolListNumber = new WMToolListNumber();
//    private final WMToolItem toolListBullet = new WMToolListBullet();
//    private final WMToolItem toolAlignment = new WMToolAlignment();
//    private final WMToolItem toolQuote = new WMToolQuote();
//    private final WMToolItem toolListClickToSwitch = new WMToolListClickToSwitch();
//    private final WMToolItem toolSplitLine = new WMToolSplitLine();

    public WMTextEditor(Context context) {
        this(context, null);
    }

    public WMTextEditor(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WMTextEditor(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WMTextEditor(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    public void initView() {
        ScrollView scrollView = new ScrollView(getContext());
        editText = new WMEditText(getContext());
        scrollView.setFillViewport(true);
        editText.setPadding(50, 50, 50, 50);
        scrollView.addView(editText, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.setOrientation(VERTICAL);
        this.addView(scrollView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        toolContainer = new WMToolContainer(getContext());
        int h = WMUtil.getPixelByDp(getContext(), 45);
        this.addView(toolContainer, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, h));
        toolContainer.addToolItem(toolImage);
        toolContainer.addToolItem(toolTextColor);
//        toolContainer.addToolItem(toolBackgroundColor);
        toolContainer.addToolItem(toolTextSize);
        toolContainer.addToolItem(toolBold);
        toolContainer.addToolItem(toolItalic);
        toolContainer.addToolItem(toolUnderline);
        toolContainer.addToolItem(toolStrikethrough);
        if (WKRichApplication.Companion.getInstance().iConversationContext.getChatChannelInfo().channelType == WKChannelType.GROUP)
            toolContainer.addToolItem(toolMention);
//        toolContainer.addToolItem(toolListNumber);
//        toolContainer.addToolItem(toolListBullet);
//        toolContainer.addToolItem(toolAlignment);
//        toolContainer.addToolItem(toolQuote);
//        toolContainer.addToolItem(toolListClickToSwitch);
//        toolContainer.addToolItem(toolSplitLine);
        editText.setupWithToolContainer(toolContainer);
    }

    public WMTextEditor setEditable(boolean editable) {
        editText.setEditable(editable);
        if (editable) {
            toolContainer.setVisibility(VISIBLE);
        } else {
            toolContainer.setVisibility(GONE);
        }
        return this;
    }

    public WMEditText getEditText() {
        return editText;
    }

    public WMToolContainer getToolContainer() {
        return toolContainer;
    }

    public WMTextEditor setEditTextMaxLines(int maxLines) {
        editText.setMaxLines(maxLines);
        return this;
    }

    public WMTextEditor setEditTextPadding(int left, int top, int right, int bottom) {
        editText.setPadding(left, top, right, bottom);
        return this;
    }

    public WMTextEditor setEditTextLineSpacing(float add, float mult) {
        editText.setLineSpacing(add, mult);
        return this;
    }


    public void addMentionSpan(String showName, String uid) {
        ((WMToolMention) toolMention).addSpan(showName, uid);
    }

    public List<String> getMentions() {
        List<String> uids = new ArrayList<>();
        String spannedString = Objects.requireNonNull(editText.getText()).toString();
        Spannable spannableString = new SpannableString(editText.getEditableText());
        int next;
        for (int i = 0; i < spannableString.length(); i = next) {
            next = spannableString.nextSpanTransition(i, spannedString.length(), CharacterStyle.class);
            WMMentionSpan[] mentionSpans = spannableString.getSpans(i, next, WMMentionSpan.class);
            for (WMMentionSpan mentionSpan : mentionSpans) {
                uids.add(mentionSpan.uid);
            }
        }
        return uids;
    }

    public List<WKMsgEntity> getContentJsonArr() {
        List<WKMsgEntity> list = new ArrayList<>();
//        JSONArray jsonArray = new JSONArray();
        String spannedString = Objects.requireNonNull(editText.getText()).toString();
        Spannable spannableString = new SpannableString(editText.getEditableText());
        int next;
        for (int i = 0; i < spannableString.length(); i = next) {
            next = spannableString.nextSpanTransition(i, spannedString.length(), CharacterStyle.class);

            int numOfSpans = 0;
            CharacterStyle[] spans = spannableString.getSpans(i, next, CharacterStyle.class);
            for (int j = 0; j < spans.length; j++) {
                numOfSpans++;
            }
            StyleSpan[] styleSpans = spannableString.getSpans(i, next, StyleSpan.class);
            for (StyleSpan styleSpan : styleSpans) {
                WKMsgEntity mMsgEntity = new WKMsgEntity();
                if (styleSpan.getStyle() == Typeface.BOLD) {
                    // 加粗
                    mMsgEntity.length = next - i;
                    mMsgEntity.offset = i;
                    mMsgEntity.type = RichStyles.getBold();
                } else if (styleSpan.getStyle() == Typeface.ITALIC) {
                    // 斜体
                    mMsgEntity.type = RichStyles.getItalic();
                    mMsgEntity.offset = i;
                    mMsgEntity.length = next - i;
                }
                list.add(mMsgEntity);
            }
            // 下划线
            WMUnderlineSpan[] underlines = spannableString.getSpans(i, next, WMUnderlineSpan.class);
            for (WMUnderlineSpan ignored : underlines) {
                WKMsgEntity mMsgEntity = new WKMsgEntity();
                mMsgEntity.type = RichStyles.getUnderline();
                mMsgEntity.offset = i;
                mMsgEntity.length = next - i;
                list.add(mMsgEntity);
            }
            // 删除线
            StrikethroughSpan[] sts = spannableString.getSpans(i, next, StrikethroughSpan.class);
            for (StrikethroughSpan ignored : sts) {
                WKMsgEntity entity = new WKMsgEntity();
                entity.type = RichStyles.getStrikethrough();
                entity.offset = i;
                entity.length = next - i;
                list.add(entity);
            }
            // 字体大小
            AbsoluteSizeSpan[] sizeSpans = spannableString.getSpans(i, next, AbsoluteSizeSpan.class);
            for (AbsoluteSizeSpan sizeSpan : sizeSpans) {
                if (sizeSpan.getSize() == 16)
                    continue;
                WKMsgEntity entity = new WKMsgEntity();
                entity.type = RichStyles.getFont();
                entity.offset = i;
                entity.length = next - i;
                entity.value = String.valueOf(sizeSpan.getSize());
                list.add(entity);
            }
            // 字体颜色
            ForegroundColorSpan[] colorSpans = spannableString.getSpans(i, next, ForegroundColorSpan.class);
            for (ForegroundColorSpan colorSpan : colorSpans) {
                String color = getColor(colorSpan.getForegroundColor());
                if (color.equals("#333333"))
                    continue;
                WKMsgEntity entity = new WKMsgEntity();
                entity.type = RichStyles.getColor();
                entity.offset = i;
                entity.length = next - i;
                entity.value = color;
                list.add(entity);
            }
            // 提醒
            WMMentionSpan[] mentionSpans = spannableString.getSpans(i, next, WMMentionSpan.class);
            for (WMMentionSpan mentionSpan : mentionSpans) {
                WKMsgEntity entity = new WKMsgEntity();
                entity.type = RichStyles.getMention();
                entity.offset = i;
                entity.length = next - i;
                entity.value = mentionSpan.uid;
                list.add(entity);
            }
            AlignImageSpan[] imageSpans = spannableString.getSpans(i, next, AlignImageSpan.class);
            for (AlignImageSpan imageSpan : imageSpans) {
                WKMsgEntity entity = new WKMsgEntity();
                entity.type = RichStyles.getImg();
                entity.offset = i;
                entity.length = 1;

                try {
                    JSONObject valueJson = new JSONObject();
                    valueJson.put("url", imageSpan.url);
                    valueJson.put("width", imageSpan.width);
                    valueJson.put("height", imageSpan.height);
                    entity.value = valueJson.toString();
                    list.add(entity);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }


        List<String> urls = StringUtils.getStrUrls(spannedString);
        for (String url : urls) {
            int fromIndex = 0;
            while (fromIndex >= 0) {
                fromIndex = spannedString.indexOf(url, fromIndex);
                if (fromIndex >= 0) {
                    WKMsgEntity entity = new WKMsgEntity();
                    entity.type = RichStyles.getLink();
                    entity.offset = fromIndex;
                    entity.length = url.length();
                    entity.value = url;
                    list.add(entity);
                    fromIndex += url.length();
                }
            }
        }
        return list;
    }

    private String getColor(int color) {
        String R, G, B;

        StringBuilder sb = new StringBuilder();

        R = Integer.toHexString(Color.red(color));
        G = Integer.toHexString(Color.green(color));
        B = Integer.toHexString(Color.blue(color));
        R = R.length() == 1 ? "0" + R : R;
        G = G.length() == 1 ? "0" + G : G;
        B = B.length() == 1 ? "0" + B : B;
        sb.append("#");
        sb.append(R);
        sb.append(G);
        sb.append(B);

        return sb.toString();
    }
}
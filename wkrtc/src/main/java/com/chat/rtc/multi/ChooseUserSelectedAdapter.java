package com.chat.rtc.multi;

import android.app.Activity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.WKBaseApplication;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.ui.components.BackDrawable;
import com.chat.base.ui.components.RoundLayout;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.WKTimeUtils;
import com.chat.rtc.R;
import com.xinbida.wukongim.entity.WKChannelType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 2020-09-25 16:28
 * 已选中人列表
 */
class ChooseUserSelectedAdapter extends BaseMultiItemQuickAdapter<GroupMemberEntity, BaseViewHolder> {
    int[] colors;
    IGetEdit iGetEdit;

    public ChooseUserSelectedAdapter(IGetEdit iGetEdit) {
        this.iGetEdit = iGetEdit;
        addItemType(0, R.layout.item_user_selected);
        addItemType(1, R.layout.item_search_layout);
        colors = WKBaseApplication.getInstance().getContext().getResources().getIntArray(R.array.name_colors);

    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, GroupMemberEntity item, @NonNull List<?> payloads) {
        super.convert(holder, item, payloads);
        GroupMemberEntity uiEntity = (GroupMemberEntity) payloads.get(0);
        RoundLayout roundLayout = holder.getView(R.id.roundLayout);

        if (uiEntity.isSetDelete) {
            BackDrawable drawable = new BackDrawable(true);
            drawable.setColor(ContextCompat.getColor(getContext(), R.color.white));
            roundLayout.setBgColor(ContextCompat.getColor(getContext(), R.color.red));
            AvatarView avatarView = holder.getView(R.id.avatarView);
            RotateAnimation rotate = new RotateAnimation(0f, 90f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setInterpolator(new LinearInterpolator());
            rotate.setDuration(150);
            rotate.setRepeatCount(0);
            rotate.setFillAfter(true);
            rotate.setStartOffset(10);
            avatarView.imageView.setAnimation(rotate);
            rotate.start();
            avatarView.imageView.setImageDrawable(drawable);
        } else {
            int index = Math.abs(uiEntity.member.memberUID.hashCode()) % colors.length;
            roundLayout.setBgColor(colors[index]);
        }
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, GroupMemberEntity friendUIEntity) {
        if (friendUIEntity.itemType == 0) {
            int index = Math.abs(friendUIEntity.member.memberUID.hashCode()) % colors.length;
            RoundLayout roundLayout = baseViewHolder.getView(R.id.roundLayout);
            roundLayout.setBgColor(colors[index]);
//            baseViewHolder.setGone(R.id.endView, true);
            AvatarView avatarView = baseViewHolder.getView(R.id.avatarView);
            avatarView.showAvatar(friendUIEntity.member.memberUID, WKChannelType.PERSONAL, friendUIEntity.member.memberAvatarCacheKey);
            avatarView.setSize(25);
            avatarView.setStrokeWidth(0);
            String showName = TextUtils.isEmpty(friendUIEntity.member.memberRemark) ? friendUIEntity.member.memberName : friendUIEntity.member.memberRemark;
            baseViewHolder.setText(R.id.nameTv, showName);
        } else {
            EditText editText = baseViewHolder.getView(R.id.searchEt);
            addListener(editText);
        }
    }

    long lastTime = 0;

    private void addListener(EditText editText) {
        editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editText.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DEL && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                long nowTime = WKTimeUtils.getInstance().getCurrentMills();
                if (nowTime - lastTime < 300) return false;
                lastTime = WKTimeUtils.getInstance().getCurrentMills();
                String content = editText.getText().toString();
                if (TextUtils.isEmpty(content) && getData().size() > 1) {
                    GroupMemberEntity uiEntity = getData().get(getData().size() - 2);
                    iGetEdit.onDeleted(uiEntity.member.memberUID);
                    removeAt(getData().size() - 2);
                    return true;
                } else return false;

            }
            return false;
        });
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                SoftKeyboardUtils.getInstance().hideSoftKeyboard((Activity) getContext());
                return true;
            }
            return false;
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                iGetEdit.searchUser(editable.toString());
            }
        });
    }

    public interface IGetEdit {
        void onDeleted(String uid);

        void searchUser(String key);
    }
}

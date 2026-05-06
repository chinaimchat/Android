package com.chat.login.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.chat.base.WKBaseApplication;
import com.chat.base.act.WKCropImageActivity;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.LoginMenu;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.glide.ChooseMimeType;
import com.chat.base.glide.ChooseResult;
import com.chat.base.glide.GlideUtils;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.WKPermissions;
import com.chat.base.utils.WKReader;
import com.chat.login.R;
import com.chat.login.databinding.ActPerfectUserInfoLayoutBinding;
import com.chat.login.service.LoginModel;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.List;
import java.util.Objects;

/**
 * 2020-08-28 13:43
 * 完善个人资料（头像流程与 iOS WKRegisterNextVC 对齐：选图 → 裁剪 → 约 50KB JPEG → 上传）
 */
public class PerfectUserInfoActivity extends WKBaseActivity<ActPerfectUserInfoLayoutBinding> {

    String path;

    private final ActivityResultLauncher<Intent> cropAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                String cropped = result.getData().getStringExtra("path");
                if (TextUtils.isEmpty(cropped)) {
                    return;
                }
                path = cropped;
                LoginModel.getInstance().uploadAvatar(path, code -> {
                    if (code == HttpResponseCode.success) {
                        GlideUtils.getInstance().showAvatarImg(PerfectUserInfoActivity.this, WKConfig.getInstance().getUid(), WKChannelType.PERSONAL, "", wkVBinding.avatarView.imageView);
                        wkVBinding.coverIv.setVisibility(View.GONE);
                    } else {
                        showToast(com.chat.base.R.string.avatar_upload_fail);
                    }
                });
            });

    @Override
    protected ActPerfectUserInfoLayoutBinding getViewBinding() {
        return ActPerfectUserInfoLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.wklogin_perfect_userinfo);
    }

    @Override
    protected void initView() {
        wkVBinding.avatarView.setSize(120);
        wkVBinding.avatarView.setStrokeWidth(0);
        wkVBinding.avatarView.imageView.setImageResource(R.mipmap.icon_default_header);
    }

    @Override
    protected void initListener() {
        wkVBinding.sureBtn.getBackground().setTint(Theme.colorAccount);
        wkVBinding.avatarView.setOnClickListener(v -> chooseIMG());
        wkVBinding.sureBtn.setOnClickListener(v -> {

            if (TextUtils.isEmpty(path)) {
                showToast(R.string.wklogin_must_upload_header);
                return;
            }
            if (!checkEditInputIsEmpty(wkVBinding.nameEt, R.string.nickname_not_null)) {
                loadingPopup.show();
                LoginModel.getInstance().updateUserInfo("name", Objects.requireNonNull(wkVBinding.nameEt.getText()).toString(), (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        UserInfoEntity userInfoEntity = WKConfig.getInstance().getUserInfo();
                        userInfoEntity.name = wkVBinding.nameEt.getText().toString();
                        WKConfig.getInstance().saveUserInfo(userInfoEntity);
                        WKConfig.getInstance().setUserName(wkVBinding.nameEt.getText().toString());
                        List<LoginMenu> list = EndpointManager.getInstance().invokes(EndpointCategory.loginMenus, null);
                        if (WKReader.isNotEmpty(list)) {
                            for (LoginMenu menu : list) {
                                if (menu.iMenuClick != null)
                                    menu.iMenuClick.onClick();
                            }
                        }
                        loadingPopup.dismiss();
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }

        });
    }

    private void chooseIMG() {
        String desc = String.format(getString(com.chat.base.R.string.file_permissions_des), getString(com.chat.base.R.string.app_name));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            WKPermissions.getInstance().checkPermissions(new WKPermissions.IPermissionResult() {
                @Override
                public void onResult(boolean result) {
                    if (result) {
                        openImagePicker();
                    }
                }

                @Override
                public void clickResult(boolean isCancel) {
                }
            }, this, desc, Manifest.permission.CAMERA);
        } else {
            WKPermissions.getInstance().checkPermissions(new WKPermissions.IPermissionResult() {
                @Override
                public void onResult(boolean result) {
                    if (result) {
                        openImagePicker();
                    }
                }

                @Override
                public void clickResult(boolean isCancel) {
                }
            }, this, desc, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openImagePicker() {
        WKBaseApplication.getInstance().disconnect = false;
        GlideUtils.getInstance().chooseIMG(this, 1, true, ChooseMimeType.img, false, false, new GlideUtils.ISelectBack() {
            @Override
            public void onBack(List<ChooseResult> paths) {
                if (WKReader.isNotEmpty(paths)) {
                    String pickPath = paths.get(0).path;
                    if (!TextUtils.isEmpty(pickPath)) {
                        Intent intent = new Intent(PerfectUserInfoActivity.this, WKCropImageActivity.class);
                        intent.putExtra("path", pickPath);
                        cropAvatarLauncher.launch(intent);
                    }
                }
            }

            @Override
            public void onCancel() {

            }
        });
    }
}

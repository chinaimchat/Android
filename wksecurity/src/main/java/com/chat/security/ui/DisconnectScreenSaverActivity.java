package com.chat.security.ui;

import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKSystemAccount;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.ui.Theme;
import com.chat.security.R;
import com.chat.security.databinding.ActDisconnectSaverLayoutBinding;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannelType;
import com.xinbida.wukongim.message.type.WKConnectStatus;

/**
 * 2021/8/10 10:16
 */
public class DisconnectScreenSaverActivity extends WKBaseActivity<ActDisconnectSaverLayoutBinding> {
    @Override
    protected ActDisconnectSaverLayoutBinding getViewBinding() {
        overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent);
        return ActDisconnectSaverLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initPresenter() {
        EndpointManager.getInstance().setMethod("close_disconnect_screen", object -> {
            finish();
            return null;
        });
    }

    @Override
    protected void initView() {
        wkVBinding.appNameTv.setTextColor(Theme.colorAccount);
        wkVBinding.appNameTv.setText(R.string.app_name);
        wkVBinding.avatarView.showAvatar(WKSystemAccount.system_team, WKChannelType.PERSONAL);
        wkVBinding.avatarView.setSize(60);
        wkVBinding.chronometer.setBase(SystemClock.elapsedRealtime());
        wkVBinding.chronometer.setFormat(getString(R.string.disconnect_timing) + "%s");
        wkVBinding.chronometer.start();
        wkVBinding.saverTv.setText(String.format("%s%s", getString(R.string.app_name), getString(R.string.protect_account_security)));
    }

    @Override
    protected void initListener() {
        WKIM.getInstance().getConnectionManager().addOnConnectionStatusListener("disconnect_screen_saver", (i, reason) -> {
            if (i == WKConnectStatus.success || i == WKConnectStatus.syncMsg) {
                finish();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected boolean supportSlideBack() {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WKIM.getInstance().getConnectionManager().removeOnConnectionStatusListener("disconnect_screen_saver");
    }

    @Override
    public void finish() {
        super.finish();
        EndpointManager.getInstance().remove("close_disconnect_screen");
        overridePendingTransition(R.anim.bottom_silent, R.anim.bottom_out);
    }
}

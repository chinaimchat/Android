package com.chat.wallet.receive;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.ui.Theme;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.systembar.WKStatusBarUtils;
import com.chat.wallet.R;
import com.chat.wallet.databinding.ActivityWalletReceiveQrBinding;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.Objects;

/**
 * 钱包收款码：Material 样式与买币页 {@link com.chat.wallet.ui.BuyUsdtActivity} 对齐。
 */
public class WalletReceiveQrActivity extends WKBaseActivity<ActivityWalletReceiveQrBinding> {

    @Override
    protected ActivityWalletReceiveQrBinding getViewBinding() {
        return ActivityWalletReceiveQrBinding.inflate(getLayoutInflater());
    }

    @Override
    protected boolean supportSlideBack() {
        return false;
    }

    @Override
    protected void toggleStatusBarMode() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        int bar = ContextCompat.getColor(this, R.color.buy_usdt_appbar_bg);
        WKStatusBarUtils.setStatusBarColor(window, bar, 0);
        int nav = ContextCompat.getColor(this, R.color.buy_usdt_page_bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(nav);
        }
        boolean darkTheme = Theme.getDarkModeStatus(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = window.getDecorView();
            int vis = decor.getSystemUiVisibility();
            if (!darkTheme) {
                vis |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                vis &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(vis);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.wallet_buy_usdt_close_enter, R.anim.wallet_buy_usdt_close_exit);
    }

    @Override
    protected void setTitle(TextView titleTv) {
    }

    @Override
    protected void initView() {
        applyToolbarChrome();
        wkVBinding.walletReceiveToolbar.inflateMenu(R.menu.menu_wallet_receive_qr);
        wkVBinding.walletReceiveToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.wallet_receive_qr_menu_save) {
                saveQrCardToAlbum();
                return true;
            }
            return false;
        });
        wkVBinding.walletReceiveToolbar.setNavigationOnClickListener(v -> finish());
        tintToolbarNavIcon();

        String uid = WKConfig.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            showToast(R.string.wallet_receive_qr_no_uid);
            finish();
            return;
        }
        wkVBinding.walletReceiveNameTv.setText(WKConfig.getInstance().getUserName());
        wkVBinding.walletReceiveAvatarView.showAvatar(uid, WKChannelType.PERSONAL);

        String payload = WalletReceiveQrContract.buildReceiveUri(uid);
        Object bmp = EndpointManager.getInstance().invoke("create_qrcode", payload);
        if (bmp instanceof Bitmap) {
            wkVBinding.walletReceiveQrIv.setImageBitmap((Bitmap) bmp);
        }
    }

    private void applyToolbarChrome() {
        int tint = ContextCompat.getColor(this, R.color.buy_usdt_text_primary);
        wkVBinding.walletReceiveToolbar.setTitleTextColor(tint);
    }

    private void tintToolbarNavIcon() {
        Drawable nav = wkVBinding.walletReceiveToolbar.getNavigationIcon();
        if (nav == null) {
            return;
        }
        int tint = ContextCompat.getColor(this, R.color.buy_usdt_text_primary);
        DrawableCompat.setTint(Objects.requireNonNull(nav.mutate()), tint);
    }

    private void saveQrCardToAlbum() {
        Bitmap bitmap = ImageUtils.getInstance().loadBitmapFromView(wkVBinding.walletReceiveQrCard);
        ImageUtils.getInstance().saveBitmap(WalletReceiveQrActivity.this, bitmap, true,
                path -> showToast(com.chat.base.R.string.saved_album));
    }

    @Override
    protected void initListener() {
    }
}

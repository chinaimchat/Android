package com.chat.uikit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.chat.base.adapter.WKFragmentStateAdapter;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.common.WKCommonModel;
import com.chat.base.config.WKConfig;
import com.chat.base.config.WKConstants;
import com.chat.base.config.WKSharedPreferencesUtil;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.MailListDot;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.act.WKWebViewActivity;
import com.chat.base.act.WorkplaceWebViewActivity;
import com.chat.base.ui.components.CounterView;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.ActManagerUtils;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.WKDeviceUtils;
import com.chat.base.utils.WKDialogUtils;
import com.chat.base.utils.WKReader;
import com.chat.base.utils.WKTimeUtils;
import com.chat.base.utils.language.WKMultiLanguageUtil;
import com.chat.base.workplace.WorkplaceWebBubbleStore;
import com.chat.base.utils.rxpermissions.RxPermissions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.chat.uikit.contacts.service.FriendModel;
import com.chat.uikit.databinding.ActTabMainBinding;
import com.chat.uikit.fragment.ChatFragment;
import com.chat.uikit.fragment.ContactsFragment;
import com.chat.uikit.fragment.MyFragment;
import com.chat.uikit.fragment.WorkplaceFragment;
import com.chat.uikit.user.service.UserModel;

import org.telegram.ui.Components.RLottieImageView;

import java.util.ArrayList;
import java.util.List;


/**
 * 2019-11-12 13:57
 * tab导航栏
 */
public class TabActivity extends WKBaseActivity<ActTabMainBinding> {
    CounterView msgCounterView;
    CounterView contactsCounterView;
    CounterView workplaceCounterView;
    View contactsSpotView;
    RLottieImageView chatIV, contactsIV, workplaceIV, meIV;
    private TextView chatTV, contactsTV, workplaceTV, meTV;
    private long lastClickChatTabTime = 0L;
    private final boolean isShowTabText = true;

    // Workplace web bubble (after returning from WKWebViewActivity).
    private FrameLayout workplaceBubbleContainer;
    private FrameLayout workplaceBubbleOverlay;
    private ShapeableImageView workplaceBubbleAvatar;
    private TextView workplaceBubbleX;
    private int workplaceBubbleCloseSize;
    private int workplaceBubbleEdgeInset;
    private int workplaceBubbleCloseGap;
    private boolean workplaceBubbleHasManualPosition;
    private boolean workplaceBubbleDragging;
    private float workplaceBubbleDownRawX;
    private float workplaceBubbleDownRawY;
    private float workplaceBubbleDownX;
    private float workplaceBubbleDownY;
    private int workplaceBubbleTouchSlop;

    @Override
    protected ActTabMainBinding getViewBinding() {
        return ActTabMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initPresenter() {
        ActManagerUtils.getInstance().clearAllActivity();
    }

    @Override
    public boolean supportSlideBack() {
        return false;
    }

    @SuppressLint("CheckResult")
    @Override
    protected void initView() {
//        wkVBinding.vp.setUserInputEnabled(false);
        UserModel.getInstance().device();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String desc = String.format(getString(R.string.notification_permissions_desc), getString(R.string.app_name));
            RxPermissions rxPermissions = new RxPermissions(this);
            rxPermissions.request(Manifest.permission.POST_NOTIFICATIONS).subscribe(aBoolean -> {
                if (!aBoolean) {
                    WKDialogUtils.getInstance().showDialog(this, getString(com.chat.base.R.string.authorization_request), desc, true, getString(R.string.cancel), getString(R.string.to_set), 0, Theme.colorAccount, index -> {
                        if (index == 1) {
                            EndpointManager.getInstance().invoke("show_open_notification_dialog", this);
                        }
                    });
                }
            });
        } else {
            boolean isEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
            if (!isEnabled) {
                EndpointManager.getInstance().invoke("show_open_notification_dialog", this);
            }
        }

        chatIV = new RLottieImageView(this);
        contactsIV = new RLottieImageView(this);
        workplaceIV = new RLottieImageView(this);
        meIV = new RLottieImageView(this);
        chatTV = new TextView(this);
        contactsTV = new TextView(this);
        workplaceTV = new TextView(this);
        meTV = new TextView(this);
        Typeface face = Typeface.createFromAsset(getResources().getAssets(),
                "fonts/mw_bold.ttf");
        chatTV.setTypeface(face);
        contactsTV.setTypeface(face);
        workplaceTV.setTypeface(face);
        meTV.setTypeface(face);
        chatTV.setText(R.string.tab_text_chat);
        chatTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
        chatTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        contactsTV.setText(R.string.tab_text_contacts);
        contactsTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
        contactsTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        workplaceTV.setText(R.string.tab_text_workplace);
        workplaceTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
        workplaceTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        meTV.setText(R.string.tab_text_me);
        meTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
        meTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        List<Fragment> fragments = new ArrayList<>(4);
        fragments.add(new ChatFragment());
        fragments.add(new ContactsFragment());
        fragments.add(new WorkplaceFragment());
        fragments.add(new MyFragment());

        wkVBinding.vp.setAdapter(new WKFragmentStateAdapter(this, fragments));
        WKCommonModel.getInstance().getAppNewVersion(false, version -> {
            String v = WKDeviceUtils.getInstance().getVersionName(TabActivity.this);
            if (version != null && !TextUtils.isEmpty(version.download_url) && !version.app_version.equals(v)) {
                WKDialogUtils.getInstance().showNewVersionDialog(TabActivity.this, version);
            }
        });
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        WKCommonModel.getInstance().getAppConfig(null);
        wkVBinding.bottomNavigation.getOrCreateBadge(R.id.i_chat).setVisible(false);
        wkVBinding.bottomNavigation.getOrCreateBadge(R.id.i_contacts).setVisible(false);
        wkVBinding.bottomNavigation.getOrCreateBadge(R.id.i_workplace).setVisible(false);
        wkVBinding.bottomNavigation.getOrCreateBadge(R.id.i_my).setVisible(false);
        FrameLayout view = wkVBinding.bottomNavigation.findViewById(R.id.i_chat);
        msgCounterView = new CounterView(this);
        msgCounterView.setColors(R.color.white, R.color.reminderColor);
        if (view != null) {
            if (isShowTabText) {
                view.addView(chatIV, LayoutHelper.createFrame(35, 35, Gravity.CENTER | Gravity.TOP, 0, 5, 0, 0));
                view.addView(msgCounterView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 5, 0, 15));
                view.addView(chatTV, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 15, 0, 0));
            } else {
                view.addView(chatIV, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
                view.addView(msgCounterView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 5, 0, 15));
            }
        }
        FrameLayout contactsView = wkVBinding.bottomNavigation.findViewById(R.id.i_contacts);
        contactsCounterView = new CounterView(this);
        contactsCounterView.setColors(R.color.white, R.color.reminderColor);
        contactsSpotView = new View(this);
        contactsSpotView.setBackgroundResource(R.drawable.msg_bg);
        if (contactsView != null) {
            if (isShowTabText) {
                contactsView.addView(contactsIV, LayoutHelper.createFrame(35, 35, Gravity.CENTER | Gravity.TOP, 0, 5, 0, 0));
                contactsView.addView(contactsCounterView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 5, 0, 15));
                contactsView.addView(contactsTV, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 15, 0, 0));
            } else {
                contactsView.addView(contactsIV, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
                contactsView.addView(contactsCounterView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 5, 0, 15));
            }
            contactsView.addView(contactsSpotView, LayoutHelper.createFrame(10, 10, Gravity.CENTER_HORIZONTAL, 10, 10, 0, 0));
        }

        FrameLayout workplaceView = wkVBinding.bottomNavigation.findViewById(R.id.i_workplace);
        workplaceCounterView = new CounterView(this);
        workplaceCounterView.setColors(R.color.white, R.color.reminderColor);
        if (workplaceView != null) {
            if (isShowTabText) {
                workplaceView.addView(workplaceIV, LayoutHelper.createFrame(35, 35, Gravity.CENTER | Gravity.TOP, 0, 5, 0, 0));
                workplaceView.addView(workplaceCounterView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 5, 0, 15));
                workplaceView.addView(workplaceTV, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 15, 0, 0));
            } else {
                workplaceView.addView(workplaceIV, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
                workplaceView.addView(workplaceCounterView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 5, 0, 15));
            }
        }

        FrameLayout meView = wkVBinding.bottomNavigation.findViewById(R.id.i_my);
        if (meView != null) {
            if (isShowTabText) {
                meView.addView(meIV, LayoutHelper.createFrame(35, 35, Gravity.CENTER | Gravity.TOP, 0, 5, 0, 0));
                meView.addView(meTV, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 15, 0, 0));
            } else {
                meView.addView(meIV, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
            }
        }
        contactsSpotView.setVisibility(View.GONE);
        contactsCounterView.setVisibility(View.GONE);
        workplaceCounterView.setVisibility(View.GONE);
        msgCounterView.setVisibility(View.GONE);
        playAnimation(0);

        ensureWorkplaceBubbleOverlay();


    }

    @Override
    protected void initListener() {
        wkVBinding.vp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 0) {
                    playAnimation(0);
                    wkVBinding.bottomNavigation.setSelectedItemId(R.id.i_chat);
                } else if (position == 1) {
                    playAnimation(1);
                    wkVBinding.bottomNavigation.setSelectedItemId(R.id.i_contacts);
                } else if (position == 2) {
                    playAnimation(2);
                    wkVBinding.bottomNavigation.setSelectedItemId(R.id.i_workplace);
                } else {
                    playAnimation(3);
                    wkVBinding.bottomNavigation.setSelectedItemId(R.id.i_my);
                }
            }
        });
        wkVBinding.bottomNavigation.setItemIconTintList(null);
        wkVBinding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.i_chat) {
                long nowTime = WKTimeUtils.getInstance().getCurrentMills();
                if (wkVBinding.vp.getCurrentItem() == 0) {
                    if (nowTime - lastClickChatTabTime <= 300) {
                        EndpointManager.getInstance().invoke("scroll_to_unread_channel", null);
                    }
                    lastClickChatTabTime = nowTime;
                    return true;
                }
                wkVBinding.vp.setCurrentItem(0);
                playAnimation(0);
            } else if (item.getItemId() == R.id.i_contacts) {
                wkVBinding.vp.setCurrentItem(1);
                playAnimation(1);
            } else if (item.getItemId() == R.id.i_workplace) {
                wkVBinding.vp.setCurrentItem(2);
                playAnimation(2);
            } else {
                wkVBinding.vp.setCurrentItem(3);
                playAnimation(3);
            }
            return true;
        });
        EndpointManager.getInstance().setMethod("tab_activity", EndpointCategory.wkRefreshMailList, object -> {
            getAllRedDot();
            return null;
        });
    }

    private void ensureWorkplaceBubbleOverlay() {
        if (workplaceBubbleOverlay != null) return;

        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        workplaceBubbleOverlay = new FrameLayout(this);
        workplaceBubbleOverlay.setClickable(false);
        workplaceBubbleOverlay.setFocusable(false);
        workplaceBubbleOverlay.setOnTouchListener((v, event) -> false);

        FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        decorView.addView(workplaceBubbleOverlay, overlayLp);
        workplaceBubbleOverlay.bringToFront();

        int bubbleSize = AndroidUtilities.dp(56);
        workplaceBubbleCloseSize = AndroidUtilities.dp(18);
        workplaceBubbleEdgeInset = AndroidUtilities.dp(8);
        workplaceBubbleCloseGap = 0;
        workplaceBubbleContainer = new FrameLayout(this);

        FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(bubbleSize, bubbleSize);
        containerLp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        containerLp.rightMargin = AndroidUtilities.dp(16);
        workplaceBubbleContainer.setLayoutParams(containerLp);
        workplaceBubbleContainer.setBackgroundResource(R.drawable.bg_workplace_bubble);
        workplaceBubbleContainer.setClickable(true);
        workplaceBubbleContainer.setFocusable(true);
        workplaceBubbleContainer.setElevation(AndroidUtilities.dp(6));
        workplaceBubbleContainer.setClipToOutline(true);
        workplaceBubbleTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        workplaceBubbleAvatar = new ShapeableImageView(this);
        workplaceBubbleAvatar.setImageResource(R.mipmap.ic_discover_s);
        workplaceBubbleAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int defaultIconPadding = AndroidUtilities.dp(12);
        workplaceBubbleAvatar.setPadding(defaultIconPadding, defaultIconPadding, defaultIconPadding, defaultIconPadding);
        workplaceBubbleAvatar.setShapeAppearanceModel(
                workplaceBubbleAvatar.getShapeAppearanceModel()
                        .toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, bubbleSize / 2f)
                        .build()
        );
        FrameLayout.LayoutParams avatarLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        avatarLp.gravity = Gravity.CENTER;
        workplaceBubbleAvatar.setLayoutParams(avatarLp);
        workplaceBubbleAvatar.setOnTouchListener((v, event) -> handleWorkplaceBubbleDrag(event));

        workplaceBubbleX = new TextView(this);
        workplaceBubbleX.setText("×");
        workplaceBubbleX.setTextSize(13f);
        workplaceBubbleX.setTextColor(ContextCompat.getColor(this, R.color.chat_function_text));
        workplaceBubbleX.setTypeface(Typeface.DEFAULT_BOLD);
        workplaceBubbleX.setGravity(Gravity.CENTER);
        workplaceBubbleX.setIncludeFontPadding(false);
        workplaceBubbleX.setClickable(true);
        workplaceBubbleX.setBackgroundResource(R.drawable.bg_workplace_bubble_close);
        workplaceBubbleX.setElevation(AndroidUtilities.dp(8));
        workplaceBubbleX.setOnClickListener(v -> onWorkplaceBubbleXClick());

        workplaceBubbleContainer.addView(workplaceBubbleAvatar);
        workplaceBubbleOverlay.addView(workplaceBubbleContainer);
        FrameLayout.LayoutParams xLp = new FrameLayout.LayoutParams(workplaceBubbleCloseSize, workplaceBubbleCloseSize);
        workplaceBubbleOverlay.addView(workplaceBubbleX, xLp);

        hideWorkplaceBubble();
    }

    private void maybeShowWorkplaceBubble() {
        if (workplaceBubbleOverlay == null) return;
        WorkplaceWebBubbleStore store = WorkplaceWebBubbleStore.getInstance();
        Log.d("WorkplaceBubble", "maybeShow in TabActivity, hasPending=" + store.hasPending());
        if (store.hasPending()) {
            updateWorkplaceBubbleUI();
            ensureWorkplaceBubblePosition();
            workplaceBubbleOverlay.bringToFront();
            workplaceBubbleOverlay.setVisibility(View.VISIBLE);
            workplaceBubbleContainer.setVisibility(View.VISIBLE);
            updateWorkplaceBubbleXPosition();
            Log.d("WorkplaceBubble", "bubble visible");
        } else {
            hideWorkplaceBubble();
        }
    }

    private void hideWorkplaceBubble() {
        if (workplaceBubbleOverlay != null) {
            workplaceBubbleOverlay.setVisibility(View.GONE);
            if (workplaceBubbleX != null) workplaceBubbleX.setVisibility(View.GONE);
            Log.d("WorkplaceBubble", "bubble hidden");
        }
    }

    private void ensureWorkplaceBubblePosition() {
        if (workplaceBubbleOverlay == null || workplaceBubbleContainer == null) return;
        workplaceBubbleOverlay.post(() -> {
            if (workplaceBubbleOverlay == null || workplaceBubbleContainer == null) return;
            if (workplaceBubbleHasManualPosition) return;
            float defaultX = workplaceBubbleOverlay.getWidth() - workplaceBubbleContainer.getWidth() - AndroidUtilities.dp(16);
            float defaultY = (workplaceBubbleOverlay.getHeight() - workplaceBubbleContainer.getHeight()) / 2f;
            workplaceBubbleContainer.setX(Math.max(workplaceBubbleEdgeInset, defaultX));
            workplaceBubbleContainer.setY(Math.max(workplaceBubbleEdgeInset, defaultY));
            updateWorkplaceBubbleXPosition();
        });
    }

    private boolean handleWorkplaceBubbleDrag(MotionEvent event) {
        if (workplaceBubbleContainer == null || workplaceBubbleOverlay == null) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                workplaceBubbleDragging = false;
                workplaceBubbleDownRawX = event.getRawX();
                workplaceBubbleDownRawY = event.getRawY();
                workplaceBubbleDownX = workplaceBubbleContainer.getX();
                workplaceBubbleDownY = workplaceBubbleContainer.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - workplaceBubbleDownRawX;
                float dy = event.getRawY() - workplaceBubbleDownRawY;
                if (!workplaceBubbleDragging) {
                    workplaceBubbleDragging = Math.hypot(dx, dy) > workplaceBubbleTouchSlop;
                }
                if (workplaceBubbleDragging) {
                    workplaceBubbleHasManualPosition = true;
                    workplaceBubbleContainer.setX(clampWorkplaceBubbleX(workplaceBubbleDownX + dx));
                    workplaceBubbleContainer.setY(clampWorkplaceBubbleY(workplaceBubbleDownY + dy));
                    updateWorkplaceBubbleXPosition();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!workplaceBubbleDragging) {
                    onWorkplaceBubbleMainClick();
                } else {
                    snapWorkplaceBubbleToNearestEdge();
                }
                workplaceBubbleDragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (workplaceBubbleDragging) {
                    snapWorkplaceBubbleToNearestEdge();
                }
                workplaceBubbleDragging = false;
                return true;
            default:
                return false;
        }
    }

    private float clampWorkplaceBubbleX(float targetX) {
        float minX = workplaceBubbleEdgeInset;
        float maxX = Math.max(minX, workplaceBubbleOverlay.getWidth() - workplaceBubbleContainer.getWidth() - workplaceBubbleEdgeInset);
        return Math.max(minX, Math.min(targetX, maxX));
    }

    private float clampWorkplaceBubbleY(float targetY) {
        float minY = workplaceBubbleEdgeInset;
        float maxY = Math.max(minY, workplaceBubbleOverlay.getHeight() - workplaceBubbleContainer.getHeight() - workplaceBubbleEdgeInset);
        return Math.max(minY, Math.min(targetY, maxY));
    }

    private void snapWorkplaceBubbleToNearestEdge() {
        if (workplaceBubbleOverlay == null || workplaceBubbleContainer == null) return;
        float currentX = clampWorkplaceBubbleX(workplaceBubbleContainer.getX());
        float currentY = clampWorkplaceBubbleY(workplaceBubbleContainer.getY());
        float left = workplaceBubbleEdgeInset;
        float right = workplaceBubbleOverlay.getWidth() - workplaceBubbleContainer.getWidth() - workplaceBubbleEdgeInset;
        float top = workplaceBubbleEdgeInset;
        float bottom = workplaceBubbleOverlay.getHeight() - workplaceBubbleContainer.getHeight() - workplaceBubbleEdgeInset;

        float minDistance = Math.abs(currentX - left);
        float targetX = left;
        float targetY = currentY;

        float rightDistance = Math.abs(currentX - right);
        if (rightDistance < minDistance) {
            minDistance = rightDistance;
            targetX = right;
            targetY = currentY;
        }

        float topDistance = Math.abs(currentY - top);
        if (topDistance < minDistance) {
            minDistance = topDistance;
            targetX = currentX;
            targetY = top;
        }

        float bottomDistance = Math.abs(currentY - bottom);
        if (bottomDistance < minDistance) {
            targetX = currentX;
            targetY = bottom;
        }

        workplaceBubbleContainer.animate()
                .x(clampWorkplaceBubbleX(targetX))
                .y(clampWorkplaceBubbleY(targetY))
                .setDuration(180)
                .withEndAction(this::updateWorkplaceBubbleXPosition)
                .start();
        updateWorkplaceBubbleXPosition();
    }

    private void updateWorkplaceBubbleXPosition() {
        if (workplaceBubbleContainer == null || workplaceBubbleX == null) return;
        if (workplaceBubbleX.getVisibility() != View.VISIBLE) return;
        float x = workplaceBubbleContainer.getX() - workplaceBubbleCloseSize + AndroidUtilities.dp(12);
        float y = workplaceBubbleContainer.getY() + AndroidUtilities.dp(2);
        workplaceBubbleX.setX(Math.max(workplaceBubbleEdgeInset / 2f, x));
        workplaceBubbleX.setY(clampWorkplaceBubbleCloseY(y));
        workplaceBubbleX.bringToFront();
    }

    private float clampWorkplaceBubbleCloseY(float targetY) {
        if (workplaceBubbleOverlay == null) return targetY;
        float minY = workplaceBubbleEdgeInset;
        float maxY = Math.max(minY, workplaceBubbleOverlay.getHeight() - workplaceBubbleCloseSize - workplaceBubbleEdgeInset);
        return Math.max(minY, Math.min(targetY, maxY));
    }

    private void updateWorkplaceBubbleUI() {
        WorkplaceWebBubbleStore store = WorkplaceWebBubbleStore.getInstance();
        if (!store.hasPending()) return;

        // Load icon (if available) into bubble.
        String icon = store.getIcon();
        if (!TextUtils.isEmpty(icon)) {
            String iconUrl = com.chat.base.config.WKApiConfig.getShowUrl(icon);
            workplaceBubbleAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            workplaceBubbleAvatar.setPadding(0, 0, 0, 0);
            com.chat.base.glide.GlideUtils.getInstance().showImg(this, iconUrl, workplaceBubbleAvatar);
        } else {
            workplaceBubbleAvatar.setImageResource(R.mipmap.ic_discover_s);
            workplaceBubbleAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int defaultIconPadding = AndroidUtilities.dp(12);
            workplaceBubbleAvatar.setPadding(defaultIconPadding, defaultIconPadding, defaultIconPadding, defaultIconPadding);
        }

        // Step state: first click shows X, second click opens web.
        workplaceBubbleX.setVisibility(store.isXVisible() ? View.VISIBLE : View.GONE);
        updateWorkplaceBubbleXPosition();
    }

    private void onWorkplaceBubbleMainClick() {
        WorkplaceWebBubbleStore store = WorkplaceWebBubbleStore.getInstance();
        if (!store.hasPending()) return;
        if (!store.isXVisible()) {
            store.setXVisible(true);
            updateWorkplaceBubbleUI();
            return;
        }
        if (store.hasLiveSession()) {
            hideWorkplaceBubble();
            store.setXVisible(false);
            Intent intent = new Intent(this, WorkplaceWebViewActivity.class);
            intent.putExtra("url", store.getUrl());
            intent.putExtra("workplace_bubble_url", store.getUrl());
            intent.putExtra("workplace_bubble_icon", store.getIcon());
            intent.putExtra(WorkplaceWebViewActivity.EXTRA_BUBBLE_RESTORE, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return;
        }
        String url = store.getUrl();
        String icon = store.getIcon();
        hideWorkplaceBubble();
        if (TextUtils.isEmpty(url)) return;
        Intent intent = new Intent(this, WorkplaceWebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("workplace_bubble_url", url);
        intent.putExtra("workplace_bubble_icon", icon);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void onWorkplaceBubbleXClick() {
        WorkplaceWebBubbleStore store = WorkplaceWebBubbleStore.getInstance();
        WKWebViewActivity web = store.getBoundActivity();
        store.clear();
        hideWorkplaceBubble();
        if (web != null && !web.isFinishing()) {
            web.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeShowWorkplaceBubble();
        getAllRedDot();
        boolean sync_friend = WKSharedPreferencesUtil.getInstance().getBoolean("sync_friend");
        if (sync_friend) {
            FriendModel.getInstance().syncFriends((code, msg) -> {
                if (code != HttpResponseCode.success && !TextUtils.isEmpty(msg)) {
                    showToast(msg);
                }
                if (code == HttpResponseCode.success) {
                    WKSharedPreferencesUtil.getInstance().putBoolean("sync_friend", false);
                }
            });
        }
    }

    public void setMsgCount(int number) {
        WKUIKitApplication.getInstance().totalMsgCount = number;
        if (number > 0) {
            msgCounterView.setCount(number, true);
            msgCounterView.setVisibility(View.VISIBLE);
        } else {
            msgCounterView.setCount(0, true);
            msgCounterView.setVisibility(View.GONE);
        }
    }

    public void setContactCount(int number, boolean showDot) {
        if (number > 0 || showDot) {
            if (number > 0) {
                contactsCounterView.setCount(number, true);
                contactsCounterView.setVisibility(View.VISIBLE);
                contactsSpotView.setVisibility(View.GONE);
            } else {
                contactsCounterView.setVisibility(View.GONE);
                contactsSpotView.setVisibility(View.VISIBLE);
                contactsCounterView.setCount(0, true);
            }
        } else {
            contactsCounterView.setVisibility(View.GONE);
            contactsSpotView.setVisibility(View.GONE);
        }
    }

    private void getAllRedDot() {
        boolean showDot = false;
        int totalCount = 0;
        int newFriendCount = WKSharedPreferencesUtil.getInstance().getInt(WKConfig.getInstance().getUid() + "_new_friend_count");
        totalCount = totalCount + newFriendCount;
        List<MailListDot> list = EndpointManager.getInstance().invokes(EndpointCategory.wkGetMailListRedDot, null);
        if (WKReader.isNotEmpty(list)) {
            for (MailListDot MailListDot : list) {
                if (MailListDot != null) {
                    totalCount += MailListDot.numCount;
                    if (!showDot) showDot = MailListDot.showDot;
                }
            }
        }
        setContactCount(totalCount, showDot);
    }

    @Override
    public Resources getResources() {
        float fontScale = WKConstants.getFontScale();
        Resources res = super.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.fontScale = fontScale; //1 设置正常字体大小的倍数
        return createConfigurationContext(config).getResources();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        } else
            return super.onKeyDown(keyCode, event);
    }

    private void playAnimation(int index) {
        if (index == 0) {
            lastClickChatTabTime = 0;
            meIV.setImageResource(R.mipmap.ic_mine_n);
            contactsIV.setImageResource(R.mipmap.ic_contacts_n);
            chatIV.setImageResource(R.mipmap.ic_chat_s);
            workplaceIV.setImageResource(R.mipmap.ic_discover_n);
            if (isShowTabText) {
                chatTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_selected));
                contactsTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
                workplaceTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
                meTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
            }
        } else if (index == 1) {
            meIV.setImageResource(R.mipmap.ic_mine_n);
            chatIV.setImageResource(R.mipmap.ic_chat_n);
            contactsIV.setImageResource(R.mipmap.ic_contacts_s);
            workplaceIV.setImageResource(R.mipmap.ic_discover_n);
            if (isShowTabText) {
                chatTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
                contactsTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_selected));
                workplaceTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
                meTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
            }
        } else if (index == 2) {
            meIV.setImageResource(R.mipmap.ic_mine_n);
            chatIV.setImageResource(R.mipmap.ic_chat_n);
            contactsIV.setImageResource(R.mipmap.ic_contacts_n);
            workplaceIV.setImageResource(R.mipmap.ic_discover_s);
            if (isShowTabText) {
                chatTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
                contactsTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
                workplaceTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_selected));
                meTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
            }
        } else {
            chatIV.setImageResource(R.mipmap.ic_chat_n);
            contactsIV.setImageResource(R.mipmap.ic_contacts_n);
            meIV.setImageResource(R.mipmap.ic_mine_s);
            workplaceIV.setImageResource(R.mipmap.ic_discover_n);
            if (isShowTabText) {
                chatTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
                contactsTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
                workplaceTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_normal));
                meTV.setTextColor(ContextCompat.getColor(this, R.color.tab_text_selected));
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        WKMultiLanguageUtil.getInstance().setConfiguration();
        Theme.applyTheme();
    }

    @Override
    public void finish() {
        super.finish();
        EndpointManager.getInstance().remove("tab_activity");
    }
}

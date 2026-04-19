package com.chat.security.ui;

import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.security.R;
import com.chat.security.databinding.ActInputDestroyAccountCodeLayoutBinding;
import com.chat.security.service.SecurityModel;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.ResourceObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class InputDestroyAccountCodeActivity extends WKBaseActivity<ActInputDestroyAccountCodeLayoutBinding> {
    private Button titleRightBtn;
    private final int totalTime = 60;
    private Disposable timerDisposable;

    @Override
    protected ActInputDestroyAccountCodeLayoutBinding getViewBinding() {
        return ActInputDestroyAccountCodeLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.hint_verfi);
    }

    @Override
    protected void initView() {
        wkVBinding.getVCodeBtn.getBackground().setTint(Theme.colorAccount);
        startTimer();
    }

    @Override
    protected String getRightBtnText(Button titleRightBtn) {
        this.titleRightBtn = titleRightBtn;
        this.titleRightBtn.setEnabled(false);
        this.titleRightBtn.setAlpha(0.2f);
        return getString(R.string.sure);
    }

    @Override
    protected void rightButtonClick() {
        super.rightButtonClick();
        String code = Objects.requireNonNull(wkVBinding.codeEt.getText()).toString();
        if (!TextUtils.isEmpty(code)) {
            destroyAccount(code);
        }
    }

    @Override
    protected void initListener() {
        wkVBinding.codeEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s.toString())) {
                    titleRightBtn.setAlpha(0.2f);
                    titleRightBtn.setEnabled(false);
                } else {
                    titleRightBtn.setAlpha(1f);
                    titleRightBtn.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        wkVBinding.getVCodeBtn.setOnClickListener(v -> new SecurityModel().sendDestroyCode((code, msg) -> {
            if (code == HttpResponseCode.success) {
                startTimer();
            } else showToast(msg);
        }));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            maybeHideKeyboardOnOutsideTouch(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void maybeHideKeyboardOnOutsideTouch(MotionEvent ev) {
        View focus = getCurrentFocus();
        if (!(focus instanceof EditText)) {
            return;
        }
        Rect rect = new Rect();
        focus.getGlobalVisibleRect(rect);
        if (rect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
        focus.clearFocus();
    }

    private void destroyAccount(String code) {
        new SecurityModel().destroyAccount(code, (code1, msg) -> {
            if (code1 == HttpResponseCode.success) {
                EndpointManager.getInstance().invoke("exit_login",null);
            } else {
                showToast(msg);
            }
        });
    }


    public void startTimer() {
        disposeTimer();
        ResourceObserver<Long> observer = new ResourceObserver<>() {
            @Override
            public void onComplete() {
                wkVBinding.getVCodeBtn.setEnabled(true);
                wkVBinding.getVCodeBtn.setAlpha(1f);
                wkVBinding.getVCodeBtn.setText(R.string.get_verf_code);
                timerDisposable = null;
            }

            @Override
            public void onError(@NotNull Throwable e) {
                timerDisposable = null;
            }

            @Override
            public void onNext(@NotNull Long value) {
                wkVBinding.getVCodeBtn.setEnabled(false);
                wkVBinding.getVCodeBtn.setAlpha(0.2f);
                wkVBinding.getVCodeBtn.setText(String.format("%s%s s", getString(R.string.recapture), value));
            }
        };
        timerDisposable = observer;
        Observable.interval(0, 1, TimeUnit.SECONDS)
                .take(totalTime + 1)
                .map(takeValue -> totalTime - takeValue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    private void disposeTimer() {
        if (timerDisposable != null && !timerDisposable.isDisposed()) {
            timerDisposable.dispose();
        }
        timerDisposable = null;
    }

    @Override
    protected void onDestroy() {
        disposeTimer();
        super.onDestroy();
    }
}

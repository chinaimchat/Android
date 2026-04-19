package rtc.view;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;

/**
 * 5/6/21 3:04 PM
 */
public class MultiVideoChatLayout extends ViewGroup {

    private int mScreenWidth;

    //人数为2,3,4状态下的宽高度
    private int mSizeModel1;

    //人数为5,6,7,8，9状态下的宽高度
    private int mSizeModel2;

    public MultiVideoChatLayout(Context context) {
        this(context, null);
    }

    public MultiVideoChatLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiVideoChatLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MultiVideoChatLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
//        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        DisplayMetrics metrics = new DisplayMetrics();
//        wm.getDefaultDisplay().getMetrics(metrics);
//        mScreenWidth = metrics.widthPixels;

        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();

        mScreenWidth = dm.widthPixels;
        mSizeModel1 = mScreenWidth / 2;
        mSizeModel2 = mScreenWidth / 3;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //宽度默认给屏幕的宽度，高度直接取宽度，形成一个正方形
        final int width = MeasureSpec.makeMeasureSpec(mScreenWidth, MeasureSpec.EXACTLY);
        final int height = MeasureSpec.makeMeasureSpec(mScreenWidth, MeasureSpec.EXACTLY);
        setMeasuredDimension(width, height);


        final int childWidth = MeasureSpec.makeMeasureSpec(mScreenWidth / 3, MeasureSpec.EXACTLY);
        final int childHeight = MeasureSpec.makeMeasureSpec(mScreenWidth / 3, MeasureSpec.EXACTLY);

        final int childWidth2 = MeasureSpec.makeMeasureSpec(mScreenWidth / 2, MeasureSpec.EXACTLY);
        final int childHeight2 = MeasureSpec.makeMeasureSpec(mScreenWidth / 2, MeasureSpec.EXACTLY);

        if (getChildCount() > 4) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.measure(childWidth, childHeight);
            }
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.measure(childWidth2, childHeight2);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() <= 4) {
            layoutModel1();
        } else {
            layoutModel2();
        }
    }

    private void layoutModel2() {
        int currentWidth = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View item = getChildAt(i);
            if (i % 3 == 0) {
                currentWidth = 0;
                item.layout(0, i / 3 * mSizeModel2, mSizeModel2, i / 3 * mSizeModel2 + mSizeModel2);
            } else {
                item.layout(currentWidth + mSizeModel2, i / 3 * mSizeModel2, currentWidth + 2 * mSizeModel2, i / 3 * mSizeModel2 + mSizeModel2);
                currentWidth = currentWidth + mSizeModel2;
            }
            if (getChildCount() == 5) {
                if (i == 3) {
                    item.layout(mSizeModel2 / 2, i / 3 * mSizeModel2, currentWidth + 2 * mSizeModel2, i / 3 * mSizeModel2 + mSizeModel2);
                }
                if (i == 4) {
                    item.layout(mSizeModel2 / 2 + mSizeModel2, i / 3 * mSizeModel2, currentWidth + 2 * mSizeModel2, i / 3 * mSizeModel2 + mSizeModel2);
                }
            }
            if (getChildCount() == 7) {
                if (i == 6) {
                    item.layout(mSizeModel2, i / 3 * mSizeModel2, currentWidth + 2 * mSizeModel2, i / 3 * mSizeModel2 + mSizeModel2);
                }
            }
            if (getChildCount() == 8) {
                if (i == 6) {
                    item.layout(mSizeModel2 / 2, i / 3 * mSizeModel2, currentWidth + 2 * mSizeModel2, i / 3 * mSizeModel2 + mSizeModel2);
                }
                if (i == 7) {
                    item.layout(mSizeModel2 / 2 + mSizeModel2, i / 3 * mSizeModel2, currentWidth + 2 * mSizeModel2, i / 3 * mSizeModel2 + mSizeModel2);
                }
            }
        }
    }

    private void layoutModel1() {
        if (getChildCount() == 3) {
            for (int i = 0; i < getChildCount(); i++) {
                View item = getChildAt(i);
                if (i == 0) {
                    item.layout(0, 0, mSizeModel1, mSizeModel1);
                } else if (i == 1) {
                    item.layout(mSizeModel1, 0, mSizeModel1 * 2, mSizeModel1);
                } else if (i == 2) {
                    item.layout(mSizeModel1 / 2, mSizeModel1, mSizeModel1 + mSizeModel1 / 2, mSizeModel1 * 2);
                }
            }
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                View item = getChildAt(i);
                if (i % 2 == 0) {
                    item.layout(0, i / 2 * mSizeModel1, mSizeModel1, i / 2 * mSizeModel1 + mSizeModel1);
                } else {
                    item.layout(mSizeModel1, i / 2 * mSizeModel1, 2 * mSizeModel1, i / 2 * mSizeModel1 + mSizeModel1);
                }
            }
        }
    }

}

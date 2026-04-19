package rtc.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;


import com.chat.rtc.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 5/7/21 3:49 PM
 */
public class LoadingView extends View {

    private int mDefaultSize;
    private int mCircleColor = Color.WHITE;
    private Paint mPaint;
    private ValueAnimator mValueAnimator;
    private long mAnimTime = 1100;
    private long mDelayTime = 300;
    private float mPerCircleSpacingSize;
    private int mCircleCount = 3;
    private float mMaxPerCircleRadius;
    private List<DataBean> mDataBeanList = new ArrayList<>();

    public LoadingView(Context context) {
        this(context, null);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void init(Context context, AttributeSet attrs) {
        if (context != null && attrs != null) {
            mDefaultSize = dip2px(context, 50);
            mPerCircleSpacingSize = dip2px(context, 5);
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VLoadingView7);
            mCircleColor = typedArray.getColor(R.styleable.VLoadingView7_circle_color, mCircleColor);
            mCircleCount = typedArray.getInt(R.styleable.VLoadingView7_circle_count, mCircleCount);
            mPerCircleSpacingSize = typedArray.getDimension(R.styleable.VLoadingView7_per_circle_spacing_size, mPerCircleSpacingSize);
            typedArray.recycle();
        }


        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mCircleColor);

        long perDelayTime = mCircleCount <= 1 ? 0 : mDelayTime / (mCircleCount - 1);
        for (int i = 0; i < mCircleCount; i++) {
            long startTime = i * perDelayTime;
            mDataBeanList.add(new DataBean(startTime, startTime + mAnimTime));
        }

        long totalTime = mAnimTime + mDelayTime;
        mValueAnimator = ValueAnimator.ofFloat(0.0f, totalTime);
        mValueAnimator.setDuration(totalTime);
        mValueAnimator.setInterpolator(new LinearInterpolator());
        mValueAnimator.setRepeatMode(ValueAnimator.RESTART);
        mValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float time = (float) animation.getAnimatedValue();
                for (int i = 0; i < mDataBeanList.size(); i++) {
                    DataBean dataBean = mDataBeanList.get(i);
                    if (time < dataBean.startTime || time >= dataBean.endTime) {
                        dataBean.radius = 0;
                    } else {
                        long interval = (long) ((dataBean.endTime - dataBean.startTime) / 2.0f);
                        if (time >= dataBean.startTime && time < dataBean.startTime + interval) {
                            //zoom in
                            dataBean.radius = mMaxPerCircleRadius * (time - dataBean.startTime) / interval;
                        } else {
                            //zoom out
                            dataBean.radius = mMaxPerCircleRadius * (dataBean.endTime - time) / interval;
                        }
                    }
                }
                invalidate();
            }
        });
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float totalSpacing = mPerCircleSpacingSize * (mCircleCount - 1);
        if (totalSpacing > w) {
            return;
        }
        if (mCircleCount <= 1) {
            mMaxPerCircleRadius = w / 2.0f;
        } else {
            mMaxPerCircleRadius = (w - totalSpacing) / mCircleCount / 2.0f;
        }
        float y = h / 2.0f;
        for (int i = 0; i < mDataBeanList.size(); i++) {
            float x = mMaxPerCircleRadius;
            if (i > 0) {
                x = i * (mMaxPerCircleRadius * 2 + mPerCircleSpacingSize) + x;
            }
            mDataBeanList.get(i).centerPoint = new PointF(x, y);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int viewSize = measureMinSize(this, widthMeasureSpec, heightMeasureSpec, mDefaultSize);
        setMeasuredDimension(viewSize, viewSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < mDataBeanList.size(); i++) {
            DataBean dataBean = mDataBeanList.get(i);
            PointF centerPoint = dataBean.centerPoint;
            if (centerPoint != null) {
                canvas.drawCircle(centerPoint.x, centerPoint.y, dataBean.radius, mPaint);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mValueAnimator.cancel();
        mValueAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mValueAnimator.cancel();
    }

    class DataBean {
        long startTime;
        long endTime;
        float radius;
        PointF centerPoint;

        DataBean(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    /**
     * used in onMeasure,return suitable size (same width and height)
     */
    public static int measureMinSize(View view, int widthMeasureSpec, int heightMeasureSpec, int defaultSize) {
        if (view == null) {
            return defaultSize;
        }
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (wMode == MeasureSpec.AT_MOST || wMode == MeasureSpec.UNSPECIFIED) {
            width = defaultSize;
        }
        if (hMode == MeasureSpec.AT_MOST || hMode == MeasureSpec.UNSPECIFIED) {
            height = defaultSize;
        }
        return Math.min(width, height);
    }
}
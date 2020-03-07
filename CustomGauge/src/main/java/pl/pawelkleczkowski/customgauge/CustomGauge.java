package pl.pawelkleczkowski.customgauge;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

public class CustomGauge extends View {

    private static final int DEFAULT_LONG_POINTER_SIZE = 1;

    private Paint mPaint;
    private Paint mPaintInside;
    private float mStrokeWidth;
    private float mStrokeInsideWidth;
    private int mStrokeColor;
    private RectF mRect;
    private String mStrokeCap;
    private int mStartAngel;
    private int mSweepAngel;
    private int mStartValue;
    private int mEndValue;
    private int mValue;
    private double mPointAngel;
    private float mRectLeft;
    private float mRectTop;
    private float mRectRight;
    private float mRectBottom;
    private int mPoint;
    private int mPointColor;
    private int mPointSize;
    private int mPointStartColor;
    private int mPointEndColor;
    private AnimatedThread animatedThread;
    private AnimateGaugeListener animateGaugeListener;

    public CustomGauge(Context context) {
        super(context);
        init();
    }
    public CustomGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomGauge, 0, 0);

        // stroke style
        mStrokeWidth = a.getDimension(R.styleable.CustomGauge_strokeWidth, 10);
        mStrokeInsideWidth = a.getDimension(R.styleable.CustomGauge_strokeInsideWidth, 10);
        mStrokeColor = a.getColor(R.styleable.CustomGauge_strokeColor, ContextCompat.getColor(context,android.R.color.darker_gray));
        mStrokeCap = a.getString(R.styleable.CustomGauge_strokeCap);

        // angel start and sweep (opposite direction 0, 270, 180, 90)
        mStartAngel = a.getInt(R.styleable.CustomGauge_startAngel, 0);
        mSweepAngel = a.getInt(R.styleable.CustomGauge_sweepAngel, 360);

        // scale (from mStartValue to mEndValue)
        mStartValue = a.getInt(R.styleable.CustomGauge_startValue, 0);
        mEndValue = a.getInt(R.styleable.CustomGauge_endValue, 1000);

        // pointer size and color
        mPointSize = a.getColor(R.styleable.CustomGauge_pointSize, 0);
        mPointStartColor = a.getColor(R.styleable.CustomGauge_pointStartColor, ContextCompat.getColor(context,android.R.color.white));
        mPointEndColor = a.getColor(R.styleable.CustomGauge_pointEndColor, ContextCompat.getColor(context,android.R.color.white));

        // calculating one point sweep
        mPointAngel = ((double) Math.abs(mSweepAngel) / (mEndValue - mStartValue));
        a.recycle();
        init();
    }

    private void init() {
        //main Paint
        mPaint = new Paint();
        mPaintInside = new Paint();

        mPaintInside.setColor(mStrokeColor);
        mPaintInside.setStrokeWidth(mStrokeInsideWidth);
        mPaintInside.setAntiAlias(true);

        mPaint.setColor(mStrokeColor);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setAntiAlias(true);
        if (!TextUtils.isEmpty(mStrokeCap)) {
            if (mStrokeCap.equals("BUTT")){
                mPaint.setStrokeCap(Paint.Cap.BUTT);
                mPaintInside.setStrokeCap(Paint.Cap.BUTT);
            }else if (mStrokeCap.equals("ROUND")){
                mPaint.setStrokeCap(Paint.Cap.ROUND);
                mPaintInside.setStrokeCap(Paint.Cap.ROUND);
            }
        } else{
            mPaint.setStrokeCap(Paint.Cap.BUTT);
            mPaintInside.setStrokeCap(Paint.Cap.BUTT);
        }
        mPaint.setStyle(Paint.Style.STROKE);
        mPaintInside.setStyle(Paint.Style.STROKE);


        mRect = new RectF();

        mValue = mStartValue;
        mPoint = mStartAngel;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float paddingLeft = getPaddingLeft();
        float paddingRight= getPaddingRight();
        float paddingTop = getPaddingTop();
        float paddingBottom = getPaddingBottom();
        float width = getWidth() - (paddingLeft+paddingRight);
        float height = getHeight() - (paddingTop+paddingBottom);
        float radius = (width > height ? width/2 : height/2);

        mRectLeft = width/2 - radius + paddingLeft;
        mRectTop = height/2 - radius + paddingTop;
        mRectRight = width/2 - radius + paddingLeft + width;
        mRectBottom = height/2 - radius + paddingTop + height;

        mRect.set(mRectLeft, mRectTop, mRectRight, mRectBottom);

        mPaint.setColor(mStrokeColor);
        mPaint.setShader(null);
        canvas.drawArc(mRect, mStartAngel, mSweepAngel, true, mPaint);
        mPaint.setColor(mPointStartColor);
        mPaintInside.setColor(mPointStartColor);
        //mPaint.setShader(new LinearGradient(0, 0, 0, getHeight(), mPointEndColor, mPointStartColor, android.graphics.Shader.TileMode.MIRROR));

        if(mValue>0){
            if (mPointSize>0) {//if size of pointer is defined
                if (mPoint > mStartAngel + mPointSize/2) {
                    canvas.drawArc(mRect, mPoint - mPointSize/2, mPointSize, false, mPaintInside);
                }
                else { //to avoid excedding start/zero point
                    canvas.drawArc(mRect, mPoint, mPointSize, false, mPaintInside);
                }
            }
            else { //draw from start point to value point (long pointer)
                if (mValue==mStartValue) //use non-zero default value for start point (to avoid lack of pointer for start/zero value)
                    canvas.drawArc(mRect, mStartAngel, DEFAULT_LONG_POINTER_SIZE, false, mPaintInside);
                else
                    canvas.drawArc(mRect, mStartAngel, mPoint - mStartAngel, false, mPaintInside);
            }
        }


    }

    public void setValue(int value) {
        mValue = value;
        mPoint = (int) (mStartAngel + (mValue-mStartValue) * mPointAngel);
        invalidate();
    }

    public int getValue() {
        return mValue;
    }

    public void animateGauge(){
        animatedThread = new AnimatedThread(this.mValue);
        animatedThread.start();
    }

    /**
     * @version 1.0
     * @author fitra
     *
     * this class used for create animation in simple-gaguge-view
     */
    public class AnimatedThread extends Thread{
        Integer value = 0;

        public AnimatedThread(int value){
            this.value = value;
        }

        public void run() {
            for(int i=0;i<=value;i++){
                try{
                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setValue(value);
                        }
                    });
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ((Activity)getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(animateGaugeListener!=null){
                        animateGaugeListener.onFinishAnimation();
                    }
                }
            });

        }
    }

    public interface AnimateGaugeListener{
        void onFinishAnimation();
    }

    public AnimateGaugeListener getAnimateGaugeListener() {
        return animateGaugeListener;
    }
    public void setAnimateGaugeListener(AnimateGaugeListener animateGaugeListener) {
        this.animateGaugeListener = animateGaugeListener;
    }

    public int getPointStartColor() {
        return mPointStartColor;
    }

    public void setPointStartColor(int mPointStartColor) {
        this.mPointStartColor = mPointStartColor;
    }

    public int getPointEndColor() {
        return mPointEndColor;
    }

    public void setPointEndColor(int mPointEndColor) {
        this.mPointEndColor = mPointEndColor;
    }
}

package com.shuyu.waveview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;


/**
 * 声音波形的view
 * Created by shuyu on 2016/11/15.
 */

public class AudioWaveView extends View {


    public static final String MAX = "max_volume"; //map中的key
    public static final String MIN = "min_volume";//map中的key

    private Context mContext;

    private Bitmap mBitmap, mBackgroundBitmap;

    private Paint mPaint;

    private Paint mViewPaint;

    private Canvas mCanvas = new Canvas();

    private Canvas mBackCanVans = new Canvas();

    private ArrayList<Short> mRecDataList = new ArrayList<>();

    final protected Object mLock = new Object();

    private drawThread mInnerThread;

    private int mWidthSpecSize;
    private int mHeightSpecSize;
    private int mScale = 1;
    private int mBaseLine;
    private int mOffset = -11;//波形之间线与线的间隔

    private boolean mIsDraw = true;
    private int mWaveCount = 2;

    private int mWaveColor = Color.WHITE;


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AudioWaveView.this.invalidate();
        }
    };

    public AudioWaveView(Context context) {
        super(context);
        init(context, null);
    }

    public AudioWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AudioWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsDraw = false;
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        if (mBackgroundBitmap != null && !mBackgroundBitmap.isRecycled()) {
            mBackgroundBitmap.recycle();
        }
    }

    public void init(Context context, AttributeSet attrs) {
        mContext = context;
        if (isInEditMode())
            return;

        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.waveView);
            mOffset = ta.getInt(R.styleable.waveView_waveOffset, dip2px(context, -11));
            mWaveColor = ta.getColor(R.styleable.waveView_waveColor, Color.WHITE);
            mWaveCount = ta.getInt(R.styleable.waveView_waveCount, 2);
            ta.recycle();
        }

        if (mOffset == dip2px(context, -11)) {
            mOffset = dip2px(context, 1);
        }

        if (mWaveCount < 1) {
            mWaveCount = 1;
        } else if (mWaveCount > 2) {
            mWaveCount = 2;
        }

        mPaint = new Paint();
        mViewPaint = new Paint();
        mPaint.setColor(mWaveColor);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && mBackgroundBitmap == null) {
            ViewTreeObserver vto = getViewTreeObserver();
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (getWidth() > 0 && getHeight() > 0) {
                        mWidthSpecSize = getWidth();
                        mHeightSpecSize = getHeight();
                        mBaseLine = mHeightSpecSize / 2;
                        mBackgroundBitmap = Bitmap.createBitmap(mWidthSpecSize, mHeightSpecSize, Bitmap.Config.ARGB_8888);
                        mBitmap = Bitmap.createBitmap(mWidthSpecSize, mHeightSpecSize, Bitmap.Config.ARGB_8888);
                        mBackCanVans.setBitmap(mBackgroundBitmap);
                        mCanvas.setBitmap(mBitmap);
                        ViewTreeObserver vto = getViewTreeObserver();
                        vto.removeOnPreDrawListener(this);
                    }
                    return true;
                }
            });
        }
    }


    //内部类的线程
    class drawThread extends Thread {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            while (mIsDraw) {
                ArrayList<Short> dataList = new ArrayList<>();
                synchronized (mRecDataList) {
                    if (mRecDataList.size() != 0) {
                        dataList = (ArrayList<Short>) mRecDataList.clone();// 保存  接收数据
                    }
                }
                if (mBackgroundBitmap == null) {
                    continue;
                }
                resolveToWaveData(dataList);
                if (mBackCanVans != null) {
                    mBackCanVans.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    mBackCanVans.drawLine(0, mBaseLine, mWidthSpecSize, mBaseLine, mPaint);
                    int drawBufsize = dataList.size();
                    /*判断大小，是否改变显示的比例*/
                    for (int i = 0, j = 0; i < drawBufsize; i++, j += mOffset) {
                        Short sh = dataList.get(i);
                        short max = (short) (mBaseLine - sh / mScale);
                        short min;
                        if (mWaveCount == 2) {
                            min = (short) (sh / mScale + mBaseLine);
                        } else {
                            min = (short) (mBaseLine);
                        }
                        mBackCanVans.drawLine(j, mBaseLine, j, max, mPaint);
                        mBackCanVans.drawLine(j, min, j, mBaseLine, mPaint);

                    }
                    synchronized (mLock) {
                        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        mCanvas.drawBitmap(mBackgroundBitmap, 0, 0, mPaint);
                    }
                    Message msg = new Message();
                    msg.what = 0;
                    handler.sendMessage(msg);
                }
                //休眠暂停资源
                try {
                    Thread.sleep(45);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (mIsDraw && mBitmap != null) {
            synchronized (mLock) {
                c.drawBitmap(mBitmap, 0, 0, mViewPaint);
            }
        }
    }


    /**
     * 更具当前块数据来判断缩放音频显示的比例
     *
     * @param list 音频数据
     */
    private void resolveToWaveData(ArrayList<Short> list) {
        short allMax = 0;
        for (Short sh : list) {
            if (sh > allMax) {
                allMax = sh;
            }
        }
        int curScale = allMax / mBaseLine;
        if (curScale > mScale) {
            mScale = ((curScale == 0) ? 1 : curScale);
        }
    }

    /**
     * 开始绘制
     */
    public void startView() {
        if (mInnerThread != null && mInnerThread.isAlive()) {
            mIsDraw = false;
            while (mInnerThread.isAlive()) ;
            mBackCanVans.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        mIsDraw = true;
        mInnerThread = new drawThread();
        mInnerThread.start();
    }

    /**
     * 停止绘制
     */
    public void stopView() {
        mIsDraw = false;
        mRecDataList.clear();
        if (mInnerThread != null) {
            while (mInnerThread.isAlive()) ;
        }
        mBackCanVans.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }


    /**
     * 将这个list传到Record线程里，对其不断的填充
     * <p>
     * Map存有两个key，一个对应AudioWaveView的MAX这个key,一个对应AudioWaveView的MIN这个key
     *
     * @return 返回的是一个map的list
     */
    public ArrayList<Short> getRecList() {
        return mRecDataList;
    }

    /**
     * 设置线与线之间的偏移
     *
     * @param offset 偏移值 pix
     */
    public void setOffset(int offset) {
        this.mOffset = offset;
    }


    public int getWaveColor() {
        return mWaveColor;
    }

    /**
     * 设置波形颜色
     *
     * @param waveColor 音频颜色
     */
    public void setWaveColor(int waveColor) {
        this.mWaveColor = waveColor;
    }

    /**
     * 设置波形颜色
     *
     * @param waveCount 波形数量 1或者2
     */
    public void setWaveCount(int waveCount) {
        mWaveCount = waveCount;
        if (mWaveCount < 1) {
            mWaveCount = 1;
        } else if (mWaveCount > 2) {
            mWaveCount = 2;
        }
    }

    /**
     * dip转为PX
     */
    private int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }
}

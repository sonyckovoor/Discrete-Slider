/*
 * Copyright (c) Gustavo Claramunt (AnderWeb) 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.sc9.adw.library.widgets.discreteseekbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import in.sc9.adw.library.widgets.discreteseekbar.internal.PopupIndicator;
import in.sc9.adw.library.widgets.discreteseekbar.internal.compat.AnimatorCompat;
import in.sc9.adw.library.widgets.discreteseekbar.internal.compat.SeekBarCompat;
import in.sc9.adw.library.widgets.discreteseekbar.internal.drawable.MarkerDrawable;
import in.sc9.adw.library.widgets.discreteseekbar.internal.drawable.ThumbDrawable;
import in.sc9.adw.library.widgets.discreteseekbar.internal.drawable.TrackRectDrawable;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

public class DiscreteSeekBar extends View {

    /**
     * Interface to propagate seekbar change event
     */
    public interface OnProgressChangeListener {
        /**
         * When the {@link DiscreteSeekBar} value changes
         *
         * @param seekBar  The DiscreteSeekBar
         * @param value    the new value
         * @param fromUser if the change was made from the user or not (i.e. the developer calling {@link #setProgress(int)}
         */
        public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser);

        public void onStartTrackingTouch(DiscreteSeekBar seekBar);

        public void onStopTrackingTouch(DiscreteSeekBar seekBar);
    }

    /**
     * Interface to transform the current internal value of this DiscreteSeekBar to anther one for the visualization.
     * <p/>
     * This will be used on the floating bubble to display a different value if needed.
     * <p/>
     * Using this in conjunction with {@link #setIndicatorFormatter(String)} you will be able to manipulate the
     * value seen by the user
     *
     * @see #setIndicatorFormatter(String)
     * @see #setNumericTransformer(DiscreteSeekBar.NumericTransformer)
     */
    public static abstract class NumericTransformer {
        /**
         * Return the desired value to be shown to the user.
         * This value will be formatted using the format specified by {@link #setIndicatorFormatter} before displaying it
         *
         * @param value The value to be transformed
         * @return The transformed int
         */
        public abstract int transform(int value);

        /**
         * Return the desired value to be shown to the user.
         * This value will be displayed 'as is' without further formatting.
         *
         * @param value The value to be transformed
         * @return A formatted string
         */
        public String transformToString(int value) {
            return String.valueOf(value);
        }

        /**
         * Used to indicate which transform will be used. If this method returns true,
         * {@link #transformToString(int)} will be used, otherwise {@link #transform(int)}
         * will be used
         */
        public boolean useStringTransform() {
            return false;
        }
    }


    private static class DefaultNumericTransformer extends NumericTransformer {

        @Override
        public int transform(int value) {
            return value;
        }
    }


    private static final boolean isLollipopOrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    //We want to always use a formatter so the indicator numbers are "translated" to specific locales.
    private static final String DEFAULT_FORMATTER = "%d";

    private static final int PRESSED_STATE = android.R.attr.state_pressed;
    private static final int FOCUSED_STATE = android.R.attr.state_focused;
    private static final int PROGRESS_ANIMATION_DURATION = 250;
    private static final int INDICATOR_DELAY_FOR_TAPS = 150;
    private static final int DEFAULT_THUMB_COLOR = 0xff009688;
    private ThumbDrawable mThumb;
    private TrackRectDrawable mTrack;
    private TrackRectDrawable mScrubber;
    private Drawable mRipple;

    private int mTrackHeight;
    private int mScrubberHeight;
    private int mAddedTouchBounds;

    private int mMax;
    private int mMin;
    private int mValue;
    private int mKeyProgressIncrement = 1;
    private boolean mMirrorForRtl = false;
    private boolean mAllowTrackClick = true;
    private boolean mIndicatorPopupEnabled = true;
    private boolean mDiscretePointsEnabled = false;
    private boolean mDiscretePointsShowOnTouch = false;
    private boolean mDiscretePointsShowAlways = false;
    //We use our own Formatter to avoid creating new instances on every progress change
    Formatter mFormatter;
    private String mIndicatorFormatter;
    private boolean mIndicatorTextFromArray = false;
    private NumericTransformer mNumericTransformer;
    private StringBuilder mFormatBuilder;
    private OnProgressChangeListener mPublicChangeListener;
    private boolean mIsDragging;
    private int mDragOffset;

    private Rect mInvalidateRect = new Rect();
    private Rect mTempRect = new Rect();
    private PopupIndicator mIndicator;
    private AnimatorCompat mPositionAnimator;
    private float mAnimationPosition;
    private int mAnimationTarget;
    private float mDownX;
    private float mTouchSlop;

    private Paint mDiscretePoints, mDiscretePointsTran;
    private int mtextColor = Color.BLACK;
    int mTextSize = 9;
    int mTextPaddingTop = 3;
    int textStyle = 0;

    /**
     * Indicates that no text will be shown on the the view.
     *
     * @see #getTextStyle()
     * @see #setTextStyle(int)
     * @see #NONE
     * @see #ODD
     * @see #EVEN
     * @see #EXTREMES
     * @see #ALL
     */
    public static final int NONE = 0;
    /**
     * Indicates that odd text will be shown on the the view.
     *
     * @see #getTextStyle()
     * @see #setTextStyle(int)
     * @see #NONE
     * @see #ODD
     * @see #EVEN
     * @see #EXTREMES
     * @see #ALL
     */
    public static final int ODD = 1;
    /**
     * Indicates that even text will be shown on the the view.
     *
     * @see #getTextStyle()
     * @see #setTextStyle(int)
     * @see #NONE
     * @see #ODD
     * @see #EVEN
     * @see #EXTREMES
     * @see #ALL
     */
    public static final int EVEN = 2;
    /**
     * Indicates that extreme text will be shown on the the view.
     *
     * @see #getTextStyle()
     * @see #setTextStyle(int)
     * @see #NONE
     * @see #ODD
     * @see #EVEN
     * @see #EXTREMES
     * @see #ALL
     */
    public static final int EXTREMES = 3;
    /**
     * Indicates that all text will be shown on the the view.
     *
     * @see #getTextStyle()
     * @see #setTextStyle(int)
     * @see #NONE
     * @see #ODD
     * @see #EVEN
     * @see #EXTREMES
     * @see #ALL
     */
    public static final int ALL = 4;

    private int halfSize = 1;
    private RectF position;

    private String[] textArray;

    public DiscreteSeekBar(Context context) {
        this(context, null);
    }

    public DiscreteSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.discreteSeekBarStyle);
    }

    public DiscreteSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(true);
        setWillNotDraw(false);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        float density = context.getResources().getDisplayMetrics().density;
        mTrackHeight = (int) (1 * density);
        mScrubberHeight = (int) (4 * density);
        int thumbSize = (int) (density * ThumbDrawable.DEFAULT_SIZE_DP);

        //Extra pixels for a touch area of 48dp
        int touchBounds = (int) (density * 32);
        mAddedTouchBounds = (touchBounds - thumbSize) / 2;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DiscreteSeekBar,
                defStyleAttr, R.style.Widget_DiscreteSeekBar);

        int max = 100;
        int min = 0;
        int value = 0;
        mMirrorForRtl = a.getBoolean(R.styleable.DiscreteSeekBar_dsb_mirrorForRtl, mMirrorForRtl);
        mAllowTrackClick = a.getBoolean(R.styleable.DiscreteSeekBar_dsb_allowTrackClickToDrag, mAllowTrackClick);
        mIndicatorPopupEnabled = a.getBoolean(R.styleable.DiscreteSeekBar_dsb_indicatorPopupEnabled, mIndicatorPopupEnabled);
        mIndicatorTextFromArray = a.getBoolean(R.styleable.DiscreteSeekBar_dsb_indicatorTextFromArray, mIndicatorTextFromArray);
        int indexMax = R.styleable.DiscreteSeekBar_dsb_max;
        int indexMin = R.styleable.DiscreteSeekBar_dsb_min;
        int indexValue = R.styleable.DiscreteSeekBar_dsb_value;
        final TypedValue out = new TypedValue();
        //Not sure why, but we wanted to be able to use dimensions here...
        if (a.getValue(indexMax, out)) {
            if (out.type == TypedValue.TYPE_DIMENSION) {
                max = a.getDimensionPixelSize(indexMax, max);
            } else {
                max = a.getInteger(indexMax, max);
            }
        }
        if (a.getValue(indexMin, out)) {
            if (out.type == TypedValue.TYPE_DIMENSION) {
                min = a.getDimensionPixelSize(indexMin, min);
            } else {
                min = a.getInteger(indexMin, min);
            }
        }
        if (a.getValue(indexValue, out)) {
            if (out.type == TypedValue.TYPE_DIMENSION) {
                value = a.getDimensionPixelSize(indexValue, value);
            } else {
                value = a.getInteger(indexValue, value);
            }
        }

        mMin = min;
        mMax = Math.max(min + 1, max);
        mValue = Math.max(min, Math.min(max, value));
        updateKeyboardRange();

        mIndicatorFormatter = a.getString(R.styleable.DiscreteSeekBar_dsb_indicatorFormatter);

        mDiscretePointsEnabled = a.getBoolean(R.styleable.DiscreteSeekBar_dsb_discretePointsEnabled, false);
        mDiscretePointsShowAlways = a.getBoolean(R.styleable.DiscreteSeekBar_dsb_discretePointsShowAlways, false);

        ColorStateList trackColor = a.getColorStateList(R.styleable.DiscreteSeekBar_dsb_trackColor);
        ColorStateList progressColor = a.getColorStateList(R.styleable.DiscreteSeekBar_dsb_progressColor);
        ColorStateList rippleColor = a.getColorStateList(R.styleable.DiscreteSeekBar_dsb_rippleColor);
        int discretePointColor = a.getColor(R.styleable.DiscreteSeekBar_dsb_discretePointsColor, Color.RED);

        mtextColor = a.getColor(R.styleable.DiscreteSeekBar_dsb_textColor, Color.BLACK);
        mTextSize = a.getDimensionPixelSize(R.styleable.DiscreteSeekBar_dsb_TextSize, mTextSize);
        mTextPaddingTop = a.getDimensionPixelSize(R.styleable.DiscreteSeekBar_dsb_TextSize, mTextPaddingTop);
        textStyle = a.getInt(R.styleable.DiscreteSeekBar_dsb_TextStyle, textStyle);

        boolean editMode = isInEditMode();
        if (editMode || rippleColor == null) {
            rippleColor = new ColorStateList(new int[][]{new int[]{}}, new int[]{Color.DKGRAY});
        }
        if (editMode || trackColor == null) {
            trackColor = new ColorStateList(new int[][]{new int[]{}}, new int[]{Color.GRAY});
        }
        if (editMode || progressColor == null) {
            progressColor = new ColorStateList(new int[][]{new int[]{}}, new int[]{DEFAULT_THUMB_COLOR});
        }
        if (editMode) {
            discretePointColor = Color.RED;
            mtextColor = Color.BLACK;
        }
        mRipple = SeekBarCompat.getRipple(rippleColor);
        if (isLollipopOrGreater) {
            SeekBarCompat.setBackground(this, mRipple);
        } else {
            mRipple.setCallback(this);
        }


        TrackRectDrawable shapeDrawable = new TrackRectDrawable(trackColor);
        mTrack = shapeDrawable;
        mTrack.setCallback(this);

        shapeDrawable = new TrackRectDrawable(progressColor);
        mScrubber = shapeDrawable;
        mScrubber.setCallback(this);

        mThumb = new ThumbDrawable(progressColor, thumbSize);
        mThumb.setCallback(this);
        mThumb.setBounds(0, 0, mThumb.getIntrinsicWidth(), mThumb.getIntrinsicHeight());

        textArray = new String[((mMax - mMin) + 1)];

        int j = mMin;
        for (int i = 0; i < ((mMax - mMin) + 1); i++) {
            if (j <= mMax) {
                textArray[i] = j + "";
                j++;
            } else
                break;
        }

        if (!editMode) {
            mIndicator = new PopupIndicator(context, attrs, defStyleAttr, convertValueToMessage(mMax));
            mIndicator.setListener(mFloaterListener);
        }
        a.recycle();

        setNumericTransformer(new DefaultNumericTransformer());

        mDiscretePoints = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDiscretePoints.setColor(discretePointColor);
        mDiscretePoints.setStyle(Paint.Style.FILL);
        mDiscretePointsTran = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDiscretePointsTran.setColor(Color.TRANSPARENT);
        mDiscretePointsTran.setStyle(Paint.Style.FILL_AND_STROKE);
        position = new RectF();

    }

    public void setTextValues(String[] textArray) {
        if (textArray.length < 2)
            return;
        this.textArray = textArray;
        setMax(textArray.length - 1);
        setMin(0);
        invalidate();
    }

    /**
     * Sets the current Indicator formatter string
     *
     * @param formatter
     * @see String#format(String, Object...)
     * @see #setNumericTransformer(DiscreteSeekBar.NumericTransformer)
     */
    public void setIndicatorFormatter(@Nullable String formatter) {
        mIndicatorFormatter = formatter;
        updateProgressMessage(mValue);
    }

    /**
     * Sets the current {@link DiscreteSeekBar.NumericTransformer}
     *
     * @param transformer
     * @see #getNumericTransformer()
     */
    public void setNumericTransformer(@Nullable NumericTransformer transformer) {
        mNumericTransformer = transformer != null ? transformer : new DefaultNumericTransformer();
        //We need to refresh the PopupIndicator view
        if (!isInEditMode()) {
            if (mNumericTransformer.useStringTransform()) {
                mIndicator.updateSizes(mNumericTransformer.transformToString(mMax));
            } else {
                mIndicator.updateSizes(convertValueToMessage(mNumericTransformer.transform(mMax)));
            }
        }
        updateProgressMessage(mValue);
    }

    /**
     * Retrieves the current {@link DiscreteSeekBar.NumericTransformer}
     *
     * @return NumericTransformer
     * @see #setNumericTransformer
     */
    public NumericTransformer getNumericTransformer() {
        return mNumericTransformer;
    }

    /**
     * Sets the maximum value for this DiscreteSeekBar
     * if the supplied argument is smaller than the Current MIN value,
     * the MIN value will be set to MAX-1
     * <p/>
     * <p>
     * Also if the current progress is out of the new range, it will be set to MIN
     * </p>
     *
     * @param max
     * @see #setMin(int)
     * @see #setProgress(int)
     */
    public void setMax(int max) {
        mMax = max;
        if (mMax < mMin) {
            setMin(mMax - 1);
        }
        updateKeyboardRange();

        if (mValue < mMin || mValue > mMax) {
            setProgress(mMin);
        }
    }

    public int getMax() {
        return mMax;
    }

    /**
     * Sets the minimum value for this DiscreteSeekBar
     * if the supplied argument is bigger than the Current MAX value,
     * the MAX value will be set to MIN+1
     * <p>
     * Also if the current progress is out of the new range, it will be set to MIN
     * </p>
     *
     * @param min
     * @see #setMax(int)
     * @see #setProgress(int)
     */
    public void setMin(int min) {
        mMin = min;
        if (mMin > mMax) {
            setMax(mMin + 1);
        }
        updateKeyboardRange();

        if (mValue < mMin || mValue > mMax) {
            setProgress(mMin);
        }
    }

    public int getMin() {
        return mMin;
    }

    /**
     * Sets the current progress for this DiscreteSeekBar
     * The supplied argument will be capped to the current MIN-MAX range
     *
     * @param progress
     * @see #setMax(int)
     * @see #setMin(int)
     */
    public void setProgress(int progress) {
        setProgress(progress, false);
    }

    /**
     * Sets the current progress for this DiscreteSeekBar
     * The supplied argument should be present in the array of values
     *
     * @param value
     * @see #setTextValues(String[])
     */
    public void setValue(String value) {
/*        for (int i=0;i<textArray.length;i++) {
            if (textArray[i].equals(value)) {
                setProgress(i);
                break;
            }
        }*/
        setProgress(Arrays.asList(textArray).indexOf(value) + mMin);
    }

    private void setProgress(int value, boolean fromUser) {
        value = Math.max(mMin, Math.min(mMax, value));
        if (isAnimationRunning()) {
            mPositionAnimator.cancel();
        }

        if (mValue != value) {
            mValue = value;
            notifyProgress(value, fromUser);
            updateProgressMessage(value);
            updateThumbPosFromCurrentProgress();
        }
    }

    /**
     * Get the current progress
     *
     * @return the current progress :-P
     */
    public int getProgress() {
        return mValue;
    }
    /**
     * Get the value set on the progress bar
     *
     * @return the current value :-P
     */
    public String getValue() {
        return textArray[mValue];
    }

    /**
     * Sets a listener to receive notifications of changes to the DiscreteSeekBar's progress level. Also
     * provides notifications of when the DiscreteSeekBar shows/hides the bubble indicator.
     *
     * @param listener The seek bar notification listener
     * @see DiscreteSeekBar.OnProgressChangeListener
     */
    public void setOnProgressChangeListener(@Nullable OnProgressChangeListener listener) {
        mPublicChangeListener = listener;
    }

    /**
     * Sets the color of the seek thumb, as well as the color of the popup indicator.
     *
     * @param thumbColor     The color the seek thumb will be changed to
     * @param indicatorColor The color the popup indicator will be changed to
     *                       The indicator will animate from thumbColor to indicatorColor
     *                       when opening
     */
    public void setThumbColor(int thumbColor, int indicatorColor) {
        mThumb.setColorStateList(ColorStateList.valueOf(thumbColor));
        mIndicator.setColors(indicatorColor, thumbColor);
    }

    /**
     * Sets the color of the seek thumb, as well as the color of the popup indicator.
     *
     * @param thumbColorStateList The ColorStateList the seek thumb will be changed to
     * @param indicatorColor      The color the popup indicator will be changed to
     *                            The indicator will animate from thumbColorStateList(pressed state) to indicatorColor
     *                            when opening
     */
    public void setThumbColor(@NonNull ColorStateList thumbColorStateList, int indicatorColor) {
        mThumb.setColorStateList(thumbColorStateList);
        //we use the "pressed" color to morph the indicator from it to its own color
        int thumbColor = thumbColorStateList.getColorForState(new int[]{PRESSED_STATE}, thumbColorStateList.getDefaultColor());
        mIndicator.setColors(indicatorColor, thumbColor);
    }

    /**
     * Sets the color of the seekbar scrubber
     *
     * @param color The color the track  scrubber will be changed to
     */
    public void setScrubberColor(int color) {
        mScrubber.setColorStateList(ColorStateList.valueOf(color));
    }

    /**
     * Sets the color of the seekbar scrubber
     *
     * @param colorStateList The ColorStateList the track scrubber will be changed to
     */
    public void setScrubberColor(@NonNull ColorStateList colorStateList) {
        mScrubber.setColorStateList(colorStateList);
    }

    /**
     * Sets the color of the seekbar scrubber
     *
     * @param color The color the track will be changed to
     */
    public void setTrackColor(int color) {
        mTrack.setColorStateList(ColorStateList.valueOf(color));
    }

    /**
     * Sets the color of the seekbar scrubber
     *
     * @param colorStateList The ColorStateList the track will be changed to
     */
    public void setTrackColor(@NonNull ColorStateList colorStateList) {
        mTrack.setColorStateList(colorStateList);
    }

    public void setDiscretePointsEnabled(boolean discretePointsEnabled){
        this.mDiscretePointsEnabled=discretePointsEnabled;
    }

    public void setDiscretePointsShowAlways(boolean discretePointsShowAlways){
        this.mDiscretePointsShowAlways=discretePointsShowAlways;
    }
    public void setTextColor(int c){
        this.mtextColor=c;
    }

    public void setTextSize(int size){
        this.mTextSize=size;
    }

    public void setTextPaddingTop(int size){
        this.mTextPaddingTop=size;
    }

    public void setTextStyle(int type){
        this.textStyle=type;
    }

    public int getTextStyle(){
        return this.textStyle;
    }

    /**
     * If {@code enabled} is false the indicator won't appear. By default popup indicator is
     * enabled.
     */
    public void setIndicatorPopupEnabled(boolean enabled) {
        this.mIndicatorPopupEnabled = enabled;
    }

    private void notifyProgress(int value, boolean fromUser) {
        if (mPublicChangeListener != null) {
            mPublicChangeListener.onProgressChanged(DiscreteSeekBar.this, value, fromUser);
        }
        onValueChanged(value);
    }

    private void notifyBubble(boolean open) {
        if (open) {
            onShowBubble();
        } else {
            onHideBubble();
        }
    }

    /**
     * When the {@link DiscreteSeekBar} enters pressed or focused state
     * the bubble with the value will be shown, and this method called
     * <p>
     * Subclasses may override this to add functionality around this event
     * </p>
     */
    protected void onShowBubble() {
    }

    /**
     * When the {@link DiscreteSeekBar} exits pressed or focused state
     * the bubble with the value will be hidden, and this method called
     * <p>
     * Subclasses may override this to add functionality around this event
     * </p>
     */
    protected void onHideBubble() {
    }

    /**
     * When the {@link DiscreteSeekBar} value changes this method is called
     * <p>
     * Subclasses may override this to add functionality around this event
     * without having to specify a {@link DiscreteSeekBar.OnProgressChangeListener}
     * </p>
     */
    protected void onValueChanged(int value) {
    }

    private void updateKeyboardRange() {
        int range = mMax - mMin;
        if ((mKeyProgressIncrement == 0) || (range / mKeyProgressIncrement > 20)) {
            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            mKeyProgressIncrement = Math.max(1, Math.round((float) range / 20));
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int height = mThumb.getIntrinsicHeight() + getPaddingTop() + getPaddingBottom();
        height += (mAddedTouchBounds * 2);
        if (textStyle == NONE) {

        } else {
            height += mTextSize;
            height += mTextPaddingTop;
        }
        setMeasuredDimension(widthSize, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            removeCallbacks(mShowIndicatorRunnable);
            if (!isInEditMode()) {
                mIndicator.dismissComplete();
            }
            updateFromDrawableState();
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        super.scheduleDrawable(who, what, when);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int thumbWidth = mThumb.getIntrinsicWidth();
        int thumbHeight = mThumb.getIntrinsicHeight();
        int addedThumb = mAddedTouchBounds;
        int halfThumb = thumbWidth / 2;
        int paddingLeft = getPaddingLeft() + addedThumb;
        int paddingRight = getPaddingRight();
        int bottom = getHeight() - getPaddingBottom() - addedThumb;
        int top = getPaddingTop() + addedThumb;
        mThumb.setBounds(paddingLeft, top, paddingLeft + thumbWidth, top + thumbHeight);
        int trackHeight = Math.max(mTrackHeight / 2, 1);
        mTrack.setBounds(paddingLeft + halfThumb, top + halfThumb-trackHeight,
                getWidth() - halfThumb - paddingRight - addedThumb, top + halfThumb + trackHeight);
        int scrubberHeight = Math.max(mScrubberHeight / 2, 2);
        mScrubber.setBounds(paddingLeft + halfThumb, top + halfThumb - scrubberHeight,
                paddingLeft + halfThumb, top + halfThumb + scrubberHeight);

        //Update the thumb position after size changed
        updateThumbPosFromCurrentProgress();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {

        int thumbWidth = mThumb.getIntrinsicWidth();
        int thumbHeight = mThumb.getIntrinsicHeight();
        int addedThumb = mAddedTouchBounds;
        int halfThumb = thumbWidth / 2;
        int paddingLeft = getPaddingLeft() + addedThumb;
        int paddingRight = getPaddingRight();
        int bottom = getHeight() - getPaddingBottom() - addedThumb;
        int trackHeight = Math.max(mTrackHeight / 2, 1);
        int paddingTop = getPaddingTop();
        float margin = (canvas.getWidth() - (paddingLeft + getPaddingRight())) / 4;
        float halfHeight = (halfThumb + (trackHeight * .5f) + mAddedTouchBounds + paddingTop);
        float h = paddingTop+mAddedTouchBounds+halfThumb;
        int scrubberHeight = Math.max(mScrubberHeight / 2, 2);

        if (!isLollipopOrGreater) {
            mRipple.draw(canvas);
        }

        mTrack.draw(canvas);
        mScrubber.draw(canvas);

        //float interval = (((getWidth() - halfThumb - paddingRight) - (paddingLeft + halfThumb)) / (mMax - mMin));
        //float interval = ((getWidth() - halfThumb - paddingRight - addedThumb - paddingLeft - halfThumb) / (mMax - mMin));
        //float interval = ((getMeasuredWidth()-halfThumb-paddingLeft-paddingRight-addedThumb) / (mMax - mMin));
        float interval = (((float) mTrack.getBounds().right - (float) mTrack.getBounds().left) /
                (float) (mMax - mMin));
        for (int i = mMin; i <= mMax; i++) {
           /* position.set(paddingLeft+(i*margin)-halfSize,halfHeight-halfSize,
           paddingLeft+(i*margin)+halfSize, halfHeight+halfSize);  */

            position.set(mTrack.getBounds().left + ((i - mMin) * interval) - halfSize,
                    h - halfSize,
                    mTrack.getBounds().left + ((i - mMin) * interval) + halfSize,
                    h + halfSize);

            if (mDiscretePointsShowAlways) {
                canvas.drawOval(position, mDiscretePoints);
            } else if (mDiscretePointsShowOnTouch && mDiscretePointsEnabled) {
                canvas.drawOval(position, mDiscretePoints);
            } else {
                canvas.drawOval(position, mDiscretePointsTran);
            }
        }

        mThumb.draw(canvas);
        super.onDraw(canvas);
        if (textStyle == NONE) {

        } else {
            drawTextArray(canvas, thumbWidth, halfThumb, mThumb.getBounds().bottom + mTextPaddingTop + mTextSize,mTrack.getBounds().left,interval);
        }
    }

    private void drawTextArray(Canvas canvas, int thumbWidth, int halfThumb, float yPos,int left, float intervel) {
        int lenTextArray = textArray.length;
        if (lenTextArray == 0)
            return;

        float currentLeft = left;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(mtextColor);
        paint.setTextSize(mTextSize);
        //paint.setTypeface(); //TODO setTypeface in Discrete Seek Bar
        //elloWorldTextView2.setTextSize(activity.pixelsToSp(activity, textSize));
        //paint.m

        for (int i = 0; i < lenTextArray; i++) {
            /*if((int)getThumbValue(0)==i*(int)scaleStep)
                paint.setColor(Color.parseColor("#ffff0000"));
			else
				paint.setColor(Color.parseColor("#ff22445b"));*/

            float textLen = paint.measureText(textArray[i]);

            if (i == 0) {
                if (textStyle != NONE && (textStyle == ODD || textStyle == EXTREMES || textStyle == ALL))
                    canvas.drawText(textArray[i], currentLeft, yPos, paint);
            } else if (i == lenTextArray - 1) {
                if (textStyle != NONE)
                    if ((textStyle  == ALL) || (i % 2 == 0 && textStyle == ODD) || (i % 2 != 0 && textStyle == EVEN) || (textStyle == EXTREMES))
                        canvas.drawText(textArray[i], currentLeft - textLen, yPos, paint);
            } else {
                if (textStyle != 0 && textStyle != 3) {
                    if ((textStyle == ALL) || (i % 2 == 0 && textStyle == ODD) || (i % 2 != 0 && textStyle == EVEN))
                        canvas.drawText(textArray[i], currentLeft - (textLen / 2), yPos, paint);
                }
            }
            currentLeft += intervel;
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateFromDrawableState();
    }

    private void updateFromDrawableState() {
        int[] state = getDrawableState();
        boolean focused = false;
        boolean pressed = false;
        for (int i : state) {
            if (i == FOCUSED_STATE) {
                focused = true;
            } else if (i == PRESSED_STATE) {
                pressed = true;
            }
        }
        if (isEnabled() && (focused || pressed) && mIndicatorPopupEnabled) {
            //We want to add a small delay here to avoid
            //poping in/out on simple taps
            removeCallbacks(mShowIndicatorRunnable);
            postDelayed(mShowIndicatorRunnable, INDICATOR_DELAY_FOR_TAPS);
        } else {
            hideFloater();
        }
        mThumb.setState(state);
        mTrack.setState(state);
        mScrubber.setState(state);
        mRipple.setState(state);
    }

    private void updateProgressMessage(int value) {
/*        if(value == 10){
            mIndicator.setColors(Color.parseColor("#e54c65"),Color.parseColor("#e54c65"));
        }else {
            mIndicator.setColors(Color.parseColor("#12e500"),Color.parseColor("#12e500"));
        }*/
        if (!isInEditMode()) {
            if (mIndicatorTextFromArray) {
                mIndicator.setValue(textArray[value]);
            } else if (mNumericTransformer.useStringTransform()) {
                mIndicator.setValue(mNumericTransformer.transformToString(value));
            } else {
                mIndicator.setValue(convertValueToMessage(mNumericTransformer.transform(value)));
            }
        }
    }

    private String convertValueToMessage(int value) {
        String format = mIndicatorFormatter != null ? mIndicatorFormatter : DEFAULT_FORMATTER;
        //We're trying to re-use the Formatter here to avoid too much memory allocations
        //But I'm not completey sure if it's doing anything good... :(
        //Previously, this condition was wrong so the Formatter was always re-created
        //But as I fixed the condition, the formatter started outputting trash characters from previous
        //calls, so I mark the StringBuilder as empty before calling format again.

        //Anyways, I see the memory usage still go up on every call to this method
        //and I have no clue on how to fix that... damn Strings...
        if (mFormatter == null || !mFormatter.locale().equals(Locale.getDefault())) {
            int bufferSize = format.length() + String.valueOf(mMax).length();
            if (mFormatBuilder == null) {
                mFormatBuilder = new StringBuilder(bufferSize);
            } else {
                mFormatBuilder.ensureCapacity(bufferSize);
            }
            mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        } else {
            mFormatBuilder.setLength(0);
        }
        return mFormatter.format(format, value).toString();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        int actionMasked = MotionEventCompat.getActionMasked(event);
        Log.d("event.getX()", event.getX() + "");
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                Log.d("mDownX", mDownX + "");
                startDragging(event, isInScrollingContainer());
                mDiscretePointsShowOnTouch = true;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging()) {
                    updateDragging(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mDownX) > mTouchSlop) {
                        startDragging(event, false);
                    }
                }
                mDiscretePointsShowOnTouch = true;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopDragging();
                mDiscretePointsShowOnTouch = false;
                invalidate();
                break;
        }
        return true;
    }

    private boolean isInScrollingContainer() {
        return SeekBarCompat.isInScrollingContainer(getParent());
    }

    private boolean startDragging(MotionEvent ev, boolean ignoreTrackIfInScrollContainer) {
        final Rect bounds = mTempRect;
        mThumb.copyBounds(bounds);
        //Grow the current thumb rect for a bigger touch area
        bounds.inset(-mAddedTouchBounds, -mAddedTouchBounds);
        mIsDragging = (bounds.contains((int) ev.getX(), (int) ev.getY()));
        if (!mIsDragging && mAllowTrackClick && !ignoreTrackIfInScrollContainer) {
            //If the user clicked outside the thumb, we compute the current position
            //and force an immediate drag to it.
            mIsDragging = true;
            mDragOffset = (bounds.width() / 2) - mAddedTouchBounds;
            updateDragging(ev);
            Log.d("startDragging", ev.getX() + "");
            //As the thumb may have moved, get the bounds again
            mThumb.copyBounds(bounds);
            bounds.inset(-mAddedTouchBounds, -mAddedTouchBounds);
        }
        if (mIsDragging) {
            setPressed(true);
            attemptClaimDrag();
            setHotspot(ev.getX(), ev.getY());
            mDragOffset = (int) (ev.getX() - bounds.left - mAddedTouchBounds);
            if (mPublicChangeListener != null) {
                mPublicChangeListener.onStartTrackingTouch(this);
            }
        }
        return mIsDragging;
    }

    private boolean isDragging() {
        return mIsDragging;
    }

    private void stopDragging() {
        if (mPublicChangeListener != null) {
            mPublicChangeListener.onStopTrackingTouch(this);
        }
        mIsDragging = false;
        setPressed(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //TODO: Should we reverse the keys for RTL? The framework's SeekBar does NOT....
        boolean handled = false;
        if (isEnabled()) {
            int progress = getAnimatedProgress();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    handled = true;
                    if (progress <= mMin) break;
                    animateSetProgress(progress - mKeyProgressIncrement);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    handled = true;
                    if (progress >= mMax) break;
                    animateSetProgress(progress + mKeyProgressIncrement);
                    break;
            }
        }

        return handled || super.onKeyDown(keyCode, event);
    }

    private int getAnimatedProgress() {
        return isAnimationRunning() ? getAnimationTarget() : mValue;
    }


    boolean isAnimationRunning() {
        return mPositionAnimator != null && mPositionAnimator.isRunning();
    }

    void animateSetProgress(int progress) {
        final float curProgress = isAnimationRunning() ? getAnimationPosition() : getProgress();

        if (progress < mMin) {
            progress = mMin;
        } else if (progress > mMax) {
            progress = mMax;
        }
        //setProgressValueOnly(progress);

        if (mPositionAnimator != null) {
            mPositionAnimator.cancel();
        }

        mAnimationTarget = progress;
        mPositionAnimator = AnimatorCompat.create(curProgress,
                progress, new AnimatorCompat.AnimationFrameUpdateListener() {
                    @Override
                    public void onAnimationFrame(float currentValue) {
                        setAnimationPosition(currentValue);
                    }
                });
        mPositionAnimator.setDuration(PROGRESS_ANIMATION_DURATION);
        mPositionAnimator.start();
    }

    private int getAnimationTarget() {
        return mAnimationTarget;
    }

    void setAnimationPosition(float position) {
        mAnimationPosition = position;
        float currentScale = (position - mMin) / (float) (mMax - mMin);
        updateProgressFromAnimation(currentScale);
    }

    float getAnimationPosition() {
        return mAnimationPosition;
    }


    private void updateDragging(MotionEvent ev) {
        setHotspot(ev.getX(), ev.getY());
        int x = (int) ev.getX();
        Log.d("eaf", x + "");
        Rect oldBounds = mThumb.getBounds();
        int halfThumb = oldBounds.width() / 2;
        int addedThumb = mAddedTouchBounds;
        int newX = x - mDragOffset + halfThumb;
        int left = getPaddingLeft() + halfThumb + addedThumb;
        int right = getWidth() - (getPaddingRight() + halfThumb + addedThumb);
        if (newX < left) {
            newX = left;
        } else if (newX > right) {
            newX = right;
        }

        int available = right - left;
        float scale = (float) (newX - left) / (float) available;
        if (isRtl()) {
            scale = 1f - scale;
        }
        int progress = Math.round((scale * (mMax - mMin)) + mMin);
        setProgress(progress, true);
    }

    private void updateProgressFromAnimation(float scale) {
        Rect bounds = mThumb.getBounds();
        int halfThumb = bounds.width() / 2;
        int addedThumb = mAddedTouchBounds;
        int left = getPaddingLeft() + halfThumb + addedThumb;
        int right = getWidth() - (getPaddingRight() + halfThumb + addedThumb);
        int available = right - left;
        int progress = Math.round((scale * (mMax - mMin)) + mMin);
        //we don't want to just call setProgress here to avoid the animation being cancelled,
        //and this position is not bound to a real progress value but interpolated
        notifyProgress(mValue, true);
        if (progress != getProgress()) {
            mValue = progress;
            notifyProgress(mValue, true);
            updateProgressMessage(progress);
        }
        final int thumbPos = (int) (scale * available + 0.5f);
        updateThumbPos(thumbPos);
    }

    private void updateThumbPosFromCurrentProgress() {
        int thumbWidth = mThumb.getIntrinsicWidth();
        int addedThumb = mAddedTouchBounds;
        int halfThumb = thumbWidth / 2;
        float scaleDraw = (mValue - mMin) / (float) (mMax - mMin);

        //This doesn't matter if RTL, as we just need the "avaiable" area
        int left = getPaddingLeft() + halfThumb + addedThumb;
        int right = getWidth() - (getPaddingRight() + halfThumb + addedThumb);
        int available = right - left;

        final int thumbPos = (int) (scaleDraw * available + 0.5f);
        updateThumbPos(thumbPos);
    }

    private void updateThumbPos(int posX) {
        int thumbWidth = mThumb.getIntrinsicWidth();
        int halfThumb = thumbWidth / 2;
        int start;
        if (isRtl()) {
            start = getWidth() - getPaddingRight() - mAddedTouchBounds;
            posX = start - posX - thumbWidth;
        } else {
            start = getPaddingLeft() + mAddedTouchBounds;
            posX = start + posX;
        }
        mThumb.copyBounds(mInvalidateRect);
        mThumb.setBounds(posX, mInvalidateRect.top, posX + thumbWidth, mInvalidateRect.bottom);
        if (isRtl()) {
            mScrubber.getBounds().right = start - halfThumb;
            mScrubber.getBounds().left = posX + halfThumb;
        } else {
            mScrubber.getBounds().left = start + halfThumb;
            mScrubber.getBounds().right = posX + halfThumb;
        }
        final Rect finalBounds = mTempRect;
        mThumb.copyBounds(finalBounds);
        if (!isInEditMode()) {
            mIndicator.move(finalBounds.centerX());
        }

        mInvalidateRect.inset(-mAddedTouchBounds, -mAddedTouchBounds);
        finalBounds.inset(-mAddedTouchBounds, -mAddedTouchBounds);
        mInvalidateRect.union(finalBounds);
        SeekBarCompat.setHotspotBounds(mRipple, finalBounds.left, finalBounds.top, finalBounds.right, finalBounds.bottom);
        invalidate(mInvalidateRect);
    }


    private void setHotspot(float x, float y) {
        DrawableCompat.setHotspot(mRipple, x, y);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mThumb || who == mTrack || who == mScrubber || who == mRipple || super.verifyDrawable(who);
    }

    private void attemptClaimDrag() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private Runnable mShowIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            showFloater();
        }
    };

    private void showFloater() {
        if (!isInEditMode()) {
            mThumb.animateToPressed();
            mIndicator.showIndicator(this, mThumb.getBounds());
            notifyBubble(true);
        }
    }

    private void hideFloater() {
        removeCallbacks(mShowIndicatorRunnable);
        if (!isInEditMode()) {
            mIndicator.dismiss();
            notifyBubble(false);
        }
    }

    private MarkerDrawable.MarkerAnimationListener mFloaterListener = new MarkerDrawable.MarkerAnimationListener() {
        @Override
        public void onClosingComplete() {
            mThumb.animateToNormal();
        }

        @Override
        public void onOpeningComplete() {

        }

    };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mShowIndicatorRunnable);
        if (!isInEditMode()) {
            mIndicator.dismissComplete();
        }
    }

    public boolean isRtl() {
        return (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL) && mMirrorForRtl;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        CustomState state = new CustomState(superState);
        state.progress = getProgress();
        state.max = mMax;
        state.min = mMin;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(CustomState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        CustomState customState = (CustomState) state;
        setMin(customState.min);
        setMax(customState.max);
        setProgress(customState.progress, false);
        super.onRestoreInstanceState(customState.getSuperState());
    }

    static class CustomState extends BaseSavedState {
        private int progress;
        private int max;
        private int min;

        public CustomState(Parcel source) {
            super(source);
            progress = source.readInt();
            max = source.readInt();
            min = source.readInt();
        }

        public CustomState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel outcoming, int flags) {
            super.writeToParcel(outcoming, flags);
            outcoming.writeInt(progress);
            outcoming.writeInt(max);
            outcoming.writeInt(min);
        }

        public static final Creator<CustomState> CREATOR =
                new Creator<CustomState>() {

                    @Override
                    public CustomState[] newArray(int size) {
                        return new CustomState[size];
                    }

                    @Override
                    public CustomState createFromParcel(Parcel incoming) {
                        return new CustomState(incoming);
                    }
                };
    }
}

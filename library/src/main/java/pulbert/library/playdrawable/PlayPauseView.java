package pulbert.library.playdrawable;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

public class PlayPauseView extends FrameLayout {


    private static final long PLAY_PAUSE_ANIMATION_DURATION = 200;
    private final PlayPauseDrawable mDrawable;
    private final Paint mPaint = new Paint();
    private AnimatorSet mAnimatorSet;
    private AnimatorSet stopButtonSet;
    private int mBackgroundColor;
    private int mWidth;
    private int mHeight;
    private ObjectAnimator alphaAnimator;

    public PlayPauseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mBackgroundColor = getResources().getColor(android.R.color.transparent);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mDrawable = new PlayPauseDrawable(context);
        mDrawable.setCallback(this);

    }




    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mDrawable.setBounds(0, 0, w, h);
        mWidth = w;
        mHeight = h;
        Log.e("PlayPauseView", "onSizeChanged(width: " + w + "height: " + h + ")");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            setClipToOutline(true);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mDrawable || super.verifyDrawable(who);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(mBackgroundColor);
        final float radius = Math.min(mWidth, mHeight) / 2f;
        canvas.drawCircle(mWidth / 2f, mHeight / 2f, radius, mPaint);
        mDrawable.draw(canvas);
    }


    public void finishAnimation(final ImageView stopButton){

        if (stopButtonSet != null) {
            stopButtonSet.cancel();
        }
        alphaAnimator = ObjectAnimator.ofFloat(stopButton, View.ALPHA, 1, 0);

        alphaAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                stopButton.setVisibility(INVISIBLE);

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

        });
        stopButtonSet = new AnimatorSet();
        stopButtonSet.setDuration(200);
        stopButtonSet.play(alphaAnimator);
        stopButtonSet.start();

    }

    public void toggleStopButtonAnimation(final ImageButton stopButton){
        if (stopButtonSet != null) {
            stopButtonSet.cancel();
        }
        alphaAnimator = ObjectAnimator.ofFloat(stopButton, View.ALPHA, 0, 1);

        alphaAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                    stopButton.setVisibility(VISIBLE);

            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

        });
        stopButtonSet = new AnimatorSet();
        stopButtonSet.setDuration(200);
        stopButtonSet.play(alphaAnimator);
        stopButtonSet.start();

    }

    public void togglePlayPauseAnimation(){
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
        }
        mAnimatorSet = new AnimatorSet();
        final Animator pausePlayAnim = mDrawable.getPausePlayAnimator();
        mAnimatorSet.setInterpolator(new DecelerateInterpolator());
        mAnimatorSet.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
        mAnimatorSet.play(pausePlayAnim);
        mAnimatorSet.start();
    }



    public PlayPauseDrawable getPlayPauseDrawable() {
        return mDrawable;
    }
}

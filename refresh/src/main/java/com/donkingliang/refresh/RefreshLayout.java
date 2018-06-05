package com.donkingliang.refresh;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.ScrollingView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * 自定义的上下拉刷新控件
 */
public class RefreshLayout extends ViewGroup implements NestedScrollingParent {

    protected Context mContext;

    protected int mTouchSlop;

    /*
    触发下拉刷新的最小高度。
    一般来说，触发下拉刷新的高度就是头部View的高度
     */
    private int mHeaderTriggerMinHeight = 100;

    /*
    触发下拉刷新的最大高度。
    一般来说，触发下拉刷新的高度就是头部View的高度
     */
    private int mHeaderTriggerMaxHeight = 400;

    /*
     触发上拉加载的最小高度。
     一般来说，触发上拉加载的高度就是尾部View的高度
      */
    private int mFooterTriggerMinHeight = 100;

    /*
    触发上拉加载的最大高度。
    一般来说，触发上拉加载的高度就是尾部View的高度
     */
    private int mFooterTriggerMaxHeight = 400;

    //头部容器
    private LinearLayout mHeaderLayout;

    //头部View
    private View mHeaderView;

    //尾部容器
    private LinearLayout mFooterLayout;

    //尾部View
    private View mFooterView;

    //标记 无状态（既不是上拉 也 不是下拉）
    private final int STATE_NOT = -1;

    //标记 上拉状态
    private final int STATE_UP = 1;

    //标记 下拉状态
    private final int STATE_DOWN = 2;

    //当前状态
    private int mCurrentState = STATE_NOT;

    //是否处于正在下拉刷新状态
    private boolean mIsRefreshing = false;

    //是否处于正在上拉加载状态
    private boolean mIsLoadingMore = false;

    //是否启用下拉功能（默认开启）
    private boolean mIsRefresh = true;

    /*
    是否启用上拉功能（默认不开启）
    如果设置了上拉加载监听器OnLoadMoreListener，就会自动开启。
     */
    private boolean mIsLoadMore = false;

    //上拉、下拉的阻尼 设置上下拉时的拖动阻力效果
    private int mDamp = 4;

    //头部状态监听器
    private OnHeaderStateListener mOnHeaderStateListener;

    //尾部状态监听器
    private OnFooterStateListener mOnFooterStateListener;

    //下拉刷新监听器
    private OnRefreshListener mOnRefreshListener;

    //上拉加载监听器
    private OnLoadMoreListener mOnLoadMoreListener;

    //是否还有更多数据
    private boolean mHasMore = true;

    //是否显示空布局
    private boolean mIsEmpty = false;

    //----------------  用于监听Fling时的滚动状态  -------------------//
    // 在Fling的情况下，每隔50毫秒获取一下页面的滚动距离，如果跟上次没有变化，表示滚动停止。
    // 之所以用延时获取滚动距离方式获取滚动状态，是因为在sdk 23前，无法给View设置OnScrollChangeListener。
    private final int FLING_DELAY = 50;
    private Handler mFlingHandler = new Handler();
    private Runnable mFlingChangeListener = new Runnable() {
        @Override
        public void run() {
            if (computeFlingOffset()) {
                mFlingHandler.postDelayed(mFlingChangeListener, FLING_DELAY);
            }
        }
    };

    private int oldOffsetY;
    private int mFlingOrientation;
    private static final int ORIENTATION_FLING_NONE = 0;
    private static final int ORIENTATION_FLING_UP = 1;
    private static final int ORIENTATION_FLING_DOWN = 2;

    //手指触摸屏幕时的触摸点
    int mTouchY = 0;

    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setClipToPadding(false);
        initHeaderLayout();
        initFooterLayout();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    /**
     * 初始化头部
     */
    private void initHeaderLayout() {
        mHeaderLayout = new LinearLayout(mContext);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mHeaderLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mHeaderLayout.setLayoutParams(lp);
        addView(mHeaderLayout);
    }

    /**
     * 设置头部View
     *
     * @param headerView
     */
    public void setHeaderView(@NonNull View headerView) {
        if (headerView instanceof OnHeaderStateListener) {
            mHeaderView = headerView;
            mHeaderLayout.removeAllViews();
            mHeaderLayout.addView(mHeaderView);
            mOnHeaderStateListener = (OnHeaderStateListener) headerView;
        } else {
            // headerView必须实现OnHeaderStateListener接口，
            // 并通过OnHeaderStateListener的回调来更新headerView的状态。
            throw new IllegalArgumentException("headerView must implement the OnHeaderStateListener");
        }
    }

    /**
     * 初始化尾部
     */
    private void initFooterLayout() {
        mFooterLayout = new LinearLayout(mContext);
        LayoutParams lp = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mFooterLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mFooterLayout.setLayoutParams(lp);
        addView(mFooterLayout);
    }

    /**
     * 设置尾部View
     *
     * @param footerView
     */
    public void setFooterView(@NonNull View footerView) {
        if (footerView instanceof OnFooterStateListener) {
            mFooterView = footerView;
            mFooterLayout.removeAllViews();
            mFooterLayout.addView(mFooterView);
            mOnFooterStateListener = (OnFooterStateListener) footerView;
        } else {
            // footerView必须实现OnFooterStateListener接口，
            // 并通过OnFooterStateListener的回调来更新footerView的状态。
            throw new IllegalArgumentException("footerView must implement the OnFooterStateListener");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //测量头部高度
        View headerView = getChildAt(0);
        measureChild(headerView, widthMeasureSpec, heightMeasureSpec);

        //测量尾部高度
        View footerView = getChildAt(1);
        measureChild(footerView, widthMeasureSpec, heightMeasureSpec);

        //测量内容容器宽高
        int count = getChildCount();
        int contentHeight = 0;
        int contentWidth = 0;
        if (count > 2) {
            View content = getChildAt(2);
            measureChild(content, widthMeasureSpec, heightMeasureSpec);
            contentHeight = content.getMeasuredHeight();
            contentWidth = content.getMeasuredWidth();
        }

        //设置PullRefreshView的宽高
        setMeasuredDimension(measureWidth(widthMeasureSpec, contentWidth),
                measureHeight(heightMeasureSpec, contentHeight));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        //布局头部
        View headerView = getChildAt(0);
        headerView.layout(getPaddingLeft(), -headerView.getMeasuredHeight(), getPaddingLeft() + headerView.getMeasuredWidth(), 0);

        //布局尾部
        View footerView = getChildAt(1);
        footerView.layout(getPaddingLeft(), getMeasuredHeight(), getPaddingLeft()
                + footerView.getMeasuredWidth(), getMeasuredHeight() + footerView.getMeasuredHeight());

        int count = getChildCount();
        if (mIsEmpty) {
            //空布局容器
            if (count > 3) {
                View emptyView = getChildAt(3);
                emptyView.layout(getPaddingLeft(), getPaddingTop(), getPaddingLeft()
                        + emptyView.getMeasuredWidth(), getPaddingTop() + emptyView.getMeasuredHeight());
            }
        } else {
            //内容布局容器
            if (count > 2) {
                View content = getChildAt(2);
                content.layout(getPaddingLeft(), getPaddingTop(), getPaddingLeft()
                        + content.getMeasuredWidth(), getPaddingTop() + content.getMeasuredHeight());
            }
        }
    }

    private int measureWidth(int measureSpec, int contentWidth) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = contentWidth + getPaddingLeft() + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    private int measureHeight(int measureSpec, int contentHeight) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = contentHeight + getPaddingTop() + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    /**
     * 是否正在加载 (正在刷新或者正在加载更多)
     *
     * @return
     */
    private boolean isLoading() {
        return mIsRefreshing || mIsLoadingMore;
    }

    /**
     * @param isRefresh 是否开启下拉刷新功能 默认开启
     */
    public void setRefreshEnable(boolean isRefresh) {
        mIsRefresh = isRefresh;
    }

    /**
     * @param isLoadMore 是否开启上拉功能 默认不开启
     */
    public void setLoadMoreEnable(boolean isLoadMore) {
        mIsLoadMore = isLoadMore;
    }

    /**
     * 还原
     */
    private void restore(boolean isListener) {
        smoothScroll(getScrollY(), 0, 200, isListener, null);
    }

    /**
     * 通知刷新完成
     */
    public void refreshFinish() {
        if (isLoading()) {
            if (mIsLoadingMore) {
                mIsLoadingMore = false;
                if (mOnFooterStateListener != null) {
                    mOnFooterStateListener.onRetract(mFooterView);
                }
                if (getScrollY() > 0) {
                    smoothScroll(getScrollY(), 0, 200, false, null);
                    mCurrentState = STATE_NOT;
                }
            } else if (mIsRefreshing) {
                mIsRefreshing = false;
                if (mOnHeaderStateListener != null) {
                    mOnHeaderStateListener.onRetract(mHeaderView);
                }
                if (getScrollY() < 0) {
                    smoothScroll(getScrollY(), 0, 200, false, null);
                    mCurrentState = STATE_NOT;
                }
            }
        }
    }

    public void hasMore(boolean hasMore) {
        if (mHasMore != hasMore) {
            mHasMore = hasMore;
            if (mOnFooterStateListener != null) {
                mOnFooterStateListener.onHasMore(mFooterView, hasMore);
            }
        }
    }

    /**
     * 触发下拉刷新
     */
    public void autoRefresh() {
        if (!mIsRefresh || isLoading()) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                mCurrentState = STATE_DOWN;
                smoothScroll(getScrollY(), -getHeaderTriggerHeight(), 200, true, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        triggerRefresh();
                    }
                });
            }
        });
    }

    private void triggerRefresh() {
        if (!mIsRefresh || isLoading()) {
            return;
        }

        mIsRefreshing = true;
        mCurrentState = STATE_NOT;
        scroll(-getHeaderTriggerHeight(), false);
        if (mOnHeaderStateListener != null) {
            mOnHeaderStateListener.onRefresh(mHeaderView);
        }

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    /**
     * 触发上拉刷新
     */
    public void autoLoadMore() {
        if (isLoading() || !mHasMore || !mIsLoadMore) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                mCurrentState = STATE_UP;
                smoothScroll(getScrollY(), getFooterTriggerHeight(), 200, true, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        triggerLoadMore();
                    }
                });
            }
        });
    }

    private void triggerLoadMore() {
        if (isLoading() || !mHasMore || !mIsLoadMore) {
            return;
        }
        mIsLoadingMore = true;
        mCurrentState = STATE_NOT;
        scroll(getFooterTriggerHeight(), false);
        if (mOnFooterStateListener != null) {
            mOnFooterStateListener.onRefresh(mFooterView);
        }
        if (mOnLoadMoreListener != null) {
            mOnLoadMoreListener.onLoadMore();
        }
    }

    private void smoothScroll(int start, int end, int duration, final boolean isListening,
                              Animator.AnimatorListener listener) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end).setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scroll((int) animation.getAnimatedValue(), isListening);
            }
        });
        if (listener != null) {
            animator.addListener(listener);
        }
        animator.start();
    }

    /**
     * @param offset
     */
    private void scroll(int offset, boolean isListening) {

        scrollTo(0, offset);
        if (isListening) {
            int scrollOffset = Math.abs(offset);

            if (mCurrentState == STATE_DOWN && mOnHeaderStateListener != null) {
                int height = getHeaderTriggerHeight();
                mOnHeaderStateListener.onScrollChange(mHeaderView, scrollOffset,
                        scrollOffset >= height ? 100 : scrollOffset * 100 / height);
            }

            if (mCurrentState == STATE_UP && mOnFooterStateListener != null && mHasMore) {
                int height = getFooterTriggerHeight();
                mOnFooterStateListener.onScrollChange(mFooterView, scrollOffset,
                        scrollOffset >= height ? 100 : scrollOffset * 100 / height);
            }
        }
    }

    /**
     * 获取触发下拉刷新的下拉高度
     *
     * @return
     */
    public int getHeaderTriggerHeight() {
        int height = mHeaderLayout.getHeight();
        height = Math.max(height, mHeaderTriggerMinHeight);
        height = Math.min(height, mHeaderTriggerMaxHeight);
        return height;
    }

    /**
     * 获取触发上拉加载的上拉高度
     *
     * @return
     */
    public int getFooterTriggerHeight() {
        int height = mFooterLayout.getHeight();
        height = Math.max(height, mFooterTriggerMinHeight);
        height = Math.min(height, mFooterTriggerMaxHeight);
        return height;
    }

    public void setHeaderTriggerMinHeight(int headerTriggerMinHeight) {
        mHeaderTriggerMinHeight = headerTriggerMinHeight;
    }

    public void setHeaderTriggerMaxHeight(int headerTriggerMaxHeight) {
        mHeaderTriggerMaxHeight = headerTriggerMaxHeight;
    }

    public void setFooterTriggerMinHeight(int footerTriggerMinHeight) {
        mFooterTriggerMinHeight = footerTriggerMinHeight;
    }

    public void setFooterTriggerMaxHeight(int footerTriggerMaxHeight) {
        mFooterTriggerMaxHeight = footerTriggerMaxHeight;
    }

    /**
     * 设置拉动阻力 （1到10）
     *
     * @param damp
     */
    public void setDamp(int damp) {
        if (damp < 1) {
            mDamp = 1;
        } else if (damp > 10) {
            mDamp = 10;
        } else {
            mDamp = damp;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e("eee", "111");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e("eee", "222");
                break;
            case MotionEvent.ACTION_UP:
                Log.e("eee", "333");
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        int y = (int) ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchY = (int) ev.getY();
                mFlingHandler.removeCallbacksAndMessages(null);
                return false;
            case MotionEvent.ACTION_MOVE:
                if (isLoading()) {
                    return false;
                }

                if (pullDown() && y - mTouchY > mTouchSlop) {
                    mCurrentState = STATE_DOWN;
                    return true;
                }

                if (mHasMore && pullUp() && mTouchY - y > mTouchSlop) {
                    mCurrentState = STATE_UP;
                    return true;
                }

                return false;
            case MotionEvent.ACTION_UP:
                return false;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mTouchY > y) {
                    if (mCurrentState == STATE_UP) {
                        scroll((mTouchY - y) / mDamp, true);
                    }
                } else if (mCurrentState == STATE_DOWN) {
                    scroll((mTouchY - y) / mDamp, true);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!mIsRefreshing && !mIsLoadingMore) {
                    int scrollOffset = Math.abs(getScrollY());
                    if (mCurrentState == STATE_DOWN) {
                        if (scrollOffset < getHeaderTriggerHeight()) {
                            restore(true);
                        } else {
                            triggerRefresh();
                        }
                    } else if (mCurrentState == STATE_UP) {
                        if (scrollOffset < getFooterTriggerHeight()) {
                            restore(true);
                        } else {
                            triggerLoadMore();
                        }
                    } else {
                        restore(true);
                    }
                }
                mTouchY = 0;
                break;

            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    protected boolean pullDown() {
        return mIsRefresh && canDropDown();
    }

    protected boolean pullUp() {
        return mIsLoadMore && canDropUp();
    }

    /**
     * 可下拉的
     *
     * @return
     */
    protected boolean canDropDown() {
        if (getChildCount() >= 3 && getChildAt(2) instanceof ScrollingView) {
            ScrollingView view = (ScrollingView) getChildAt(2);
            return view.computeVerticalScrollOffset() <= 0;
        }
        return true;
    }

    /**
     * 可上拉的
     *
     * @return
     */
    protected boolean canDropUp() {
        if (getChildCount() >= 3 && getChildAt(2) instanceof ScrollingView) {
            ScrollingView view = (ScrollingView) getChildAt(2);
            return view.computeVerticalScrollOffset() + view.computeVerticalScrollExtent()
                    >= view.computeVerticalScrollRange();
        }
        return false;
    }

    /**
     * 显示空布局
     */
    public void showEmpty() {
        //显示空布局
        if (getChildCount() > 3) {
            getChildAt(3).setVisibility(VISIBLE);
        }
        //隐藏内容布局
        if (getChildCount() > 2) {
            getChildAt(2).setVisibility(GONE);
        }
    }

    /**
     * 隐藏空布局
     */
    public void hideEmpty() {
        //隐藏空布局
        if (getChildCount() > 3) {
            getChildAt(3).setVisibility(GONE);
        }
        //显示内容布局
        if (getChildCount() > 2) {
            getChildAt(2).setVisibility(VISIBLE);
        }
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isLoading() || !mHasMore;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        super.onNestedScrollAccepted(child, target, axes);
    }

    @Override
    public void onStopNestedScroll(View child) {
        super.onStopNestedScroll(child);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (mIsRefreshing) {
            int height = getHeaderTriggerHeight();
            int scrollY = getScrollY();
            if (dyUnconsumed < 0 && scrollY > -height) {
                int offset = -getScrollY() - height;
                if (offset < 0) {
                    scrollBy(0, Math.max(dyUnconsumed, offset));
                }
            }
        }

        if (mIsLoadingMore || !mHasMore) {
            int height = getFooterTriggerHeight();
            int scrollY = getScrollY();
            if (dyUnconsumed > 0 && scrollY < height) {
                scrollBy(0, Math.min(dyUnconsumed, height - scrollY));
            }
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (mIsRefreshing) {
            int scrollY = getScrollY();
            if (dy > 0 && scrollY < 0) {
                scrollBy(0, Math.min(dy, -scrollY));
                consumed[1] = dy;
            }
        }

        if (mIsLoadingMore || !mHasMore) {
            int scrollY = getScrollY();
            if (dy < 0 && scrollY > 0) {
                scrollBy(0, Math.max(dy, -scrollY));
                consumed[1] = dy;
            }
        }
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        if (getChildCount() >= 3 && getChildAt(2) instanceof ScrollingView) {
            initFling();
            mFlingHandler.postDelayed(mFlingChangeListener, FLING_DELAY);
        }
        return super.onNestedFling(target, velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (mIsRefreshing && velocityY > 0 && getScrollY() < 0) {
            restore(false);
        }

        if ((mIsLoadingMore || !mHasMore) && velocityY < 0 && getScrollY() > 0) {
            restore(false);
        }
        return false;
    }

    @Override
    public int getNestedScrollAxes() {
        return super.getNestedScrollAxes();
    }

    private void initFling() {
        if (getChildCount() >= 3 && getChildAt(2) instanceof ScrollingView) {
            ScrollingView view = (ScrollingView) getChildAt(2);
            oldOffsetY = view.computeVerticalScrollOffset();
            mFlingOrientation = ORIENTATION_FLING_NONE;
        }
    }

    private boolean computeFlingOffset() {
        if (getChildCount() >= 3 && getChildAt(2) instanceof ScrollingView) {
            ScrollingView view = (ScrollingView) getChildAt(2);
            int offsetY = view.computeVerticalScrollOffset();
            int interval = Math.abs(offsetY - oldOffsetY);
            int offset = 0;
            if (interval > 0) {
                if (offsetY > oldOffsetY) {
                    mFlingOrientation = ORIENTATION_FLING_UP;
                    offset = getScrollBottomOffset();
                } else if (offsetY < oldOffsetY) {
                    mFlingOrientation = ORIENTATION_FLING_DOWN;
                    offset = getScrollTopOffset();
                }
                if (interval > 30 && offset < interval) {
                    if (mIsRefreshing && mFlingOrientation == ORIENTATION_FLING_DOWN) {
                        int height = getHeaderTriggerHeight();
                        smoothScroll(getScrollY(), -height, (int) (1.0f * height * FLING_DELAY / interval), false, null);
                    } else if ((mIsLoadingMore || !mHasMore) && mFlingOrientation == ORIENTATION_FLING_UP) {
                        int height = getFooterTriggerHeight();
                        smoothScroll(getScrollY(), height, (int) (1.0f * height * FLING_DELAY / interval), false, null);
                    }
                    return false; // 停止fling监听
                }

                oldOffsetY = offsetY;
                return true;
            } else {
                // 滚动停止
                return false;
            }
        }
        return false;
    }

    private int getScrollTopOffset() {
        if (getChildCount() >= 3 && getChildAt(2) instanceof ScrollingView) {
            ScrollingView view = (ScrollingView) getChildAt(2);
            return view.computeVerticalScrollOffset();
        }
        return 0;
    }

    private int getScrollBottomOffset() {
        if (getChildCount() >= 3 && getChildAt(2) instanceof ScrollingView) {
            ScrollingView view = (ScrollingView) getChildAt(2);
            return view.computeVerticalScrollRange() - view.computeVerticalScrollOffset()
                    - view.computeVerticalScrollExtent();
        }
        return 0;
    }

    /**
     * 设置上拉监听器
     *
     * @param listener
     */
    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
        if (listener != null) {
            setLoadMoreEnable(true);
        }
    }

    /**
     * 设置下拉监听器
     *
     * @param listener
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }


    //----------------  监听接口  -------------------//

    /**
     * 头部状态监听器
     */
    public interface OnHeaderStateListener {

        /**
         * 头部滑动变化
         *
         * @param headerView   头部View
         * @param scrollOffset 滑动距离
         * @param scrollRatio  从开始到触发阀值的滑动比率（0到100）如果滑动到达了阀值，就算再滑动，这个值也是100
         */
        void onScrollChange(View headerView, int scrollOffset, int scrollRatio);

        /**
         * 头部处于刷新状态 （触发下拉刷新的时候调用）
         *
         * @param headerView 头部View
         */
        void onRefresh(View headerView);

        /**
         * 头部收起
         *
         * @param headerView 头部View
         */
        void onRetract(View headerView);

    }

    /**
     * 头部状态监听器
     */
    public interface OnFooterStateListener {

        /**
         * 尾部滑动变化
         *
         * @param footerView   尾部View
         * @param scrollOffset 滑动距离
         * @param scrollRatio  从开始到触发阀值的滑动比率（0到100）如果滑动到达了阀值，就算在滑动，这个值也是100
         */
        void onScrollChange(View footerView, int scrollOffset, int scrollRatio);

        /**
         * 尾部处于加载状态 （触发上拉加载的时候调用）
         *
         * @param footerView 尾部View
         */
        void onRefresh(View footerView);

        /**
         * 尾部收起
         *
         * @param footerView 尾部View
         */
        void onRetract(View footerView);

        /**
         * 是否还有更多(是否可以加载下一页)
         *
         * @param footerView
         * @param hasMore
         */
        void onHasMore(View footerView, boolean hasMore);
    }

    /**
     * 上拉加载监听器
     */
    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    /**
     * 下拉更新监听器
     */
    public interface OnRefreshListener {
        void onRefresh();
    }
}

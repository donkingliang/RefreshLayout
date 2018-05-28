package com.donkingliang.refresh;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * 自定义的上下拉刷新控件
 */
public abstract class RefreshLayout extends ViewGroup implements NestedScrollingParent {

    private static final String TAG = RefreshLayout.class.getSimpleName();

    protected Context mContext;

    NestedScrollingParentHelper mParentHelper;
//
//    //头部下拉的最小高度
//    private static final int HEAD_DEFAULT_HEIGHT = 100;
//
//    //尾部上拉的最小高度
//    private static final int TAIL_DEFAULT_HEIGHT = 100;

    /**
     * 头部容器
     */
    private LinearLayout mHeaderLayout;

    /**
     * 头部View
     */
    private View mHeaderView;

    /**
     * 头部的高度
     */
    private int mHeaderHeight = 100;

    /**
     * 尾部容器
     */
    private LinearLayout mFooterLayout;

    /**
     * 尾部View
     */
    private View mFooterView;

    /**
     * 尾部的高度
     */
    private int mFooterHeight = 100;

//    /**
//     * 滑动的偏移量
//     */
//    private int mScrollOffset = 0;

    /**
     * 标记 无状态（既不是上拉 也 不是下拉）
     */
    private final int STATE_NOT = -1;

    /**
     * 标记 上拉状态
     */
    private final int STATE_UP = 1;

    /**
     * 标记 下拉状态
     */
    private final int STATE_DOWN = 2;

    /**
     * 当前状态
     */
    private int mCurrentState = STATE_NOT;

    /**
     * 是否处于下拉 正在更新状态
     */
    private boolean mIsRefreshing = false;

    /**
     * 是否处于上拉 正在加载状态
     */
    private boolean mIsLoadingMore = false;

    /**
     * 是否启用下拉功能（默认开启）
     */
    private boolean mIsRefresh = true;

    /**
     * 是否启用上拉功能（默认不开启）
     */
    private boolean mIsLoadMore = false;

    /**
     * 上拉、下拉的阻尼 设置上下拉时的拖动阻力效果
     */
    private int mDamp = 4;

    /**
     * 头部状态监听器
     */
    private OnHeaderStateListener mOnHeaderStateListener;

    /**
     * 尾部状态监听器
     */
    private OnFooterStateListener mOnFooterStateListener;

    /**
     * 上拉监听器
     */
    private OnLoadMoreListener mOnLoadMoreListener;

    /**
     * 下拉监听器
     */
    private OnRefreshListener mOnRefreshListener;

    /**
     * 是否还有更多数据。
     */
    private boolean isMore = true;

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
        mParentHelper = new NestedScrollingParentHelper(this);
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
            //获取头部高度
            mHeaderHeight = measureViewHeight(mHeaderLayout);
            Log.i(TAG, "mHeaderHeight = " + mHeaderHeight);
            if (mIsRefreshing) {
                scroll(-mHeaderHeight);
            }
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
            //获取尾部高度
            mFooterHeight = measureViewHeight(mFooterLayout);
            Log.i(TAG, "mFooterHeight = " + mFooterHeight);
            if (mIsLoadingMore) {
                scroll(mFooterHeight);
            }
        } else {
            // footerView必须实现OnFooterStateListener接口，
            // 并通过OnFooterStateListener的回调来更新footerView的状态。
            throw new IllegalArgumentException("footerView must implement the OnFooterStateListener");
        }
    }

    private int measureViewHeight(View view) {
        int width = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        view.measure(width, height);
        return view.getMeasuredHeight();
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
        headerView.layout(getPaddingLeft(), -mHeaderHeight, getPaddingLeft() + headerView.getMeasuredWidth(), 0);

        //布局尾部
        View footerView = getChildAt(1);
        footerView.layout(getPaddingLeft(), getMeasuredHeight(), getPaddingLeft()
                + footerView.getMeasuredWidth(), getMeasuredHeight() + mFooterHeight);

        //布局内容容器
        int count = getChildCount();
        if (count > 2) {
            View content = getChildAt(2);
            content.layout(getPaddingLeft(), getPaddingTop(), getPaddingLeft()
                    + content.getMeasuredWidth(), getPaddingTop() + content.getMeasuredHeight());
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
    private void restore() {
        mCurrentState = STATE_NOT;
        smoothScroll(getScrollY(), 0);
    }

    /**
     * 通知刷新完成
     */
    public void refreshFinish() {
        restore();
        if (isLoading()) {
            if (mIsLoadingMore) {
                mIsLoadingMore = false;
                if (mOnFooterStateListener != null && isMore) {
                    mOnFooterStateListener.onRetract(mFooterView);
                }
            } else if (mIsRefreshing) {
                mIsRefreshing = false;
                if (mOnHeaderStateListener != null) {
                    mOnHeaderStateListener.onRetract(mHeaderView);
                }
            }
        }
    }

    public void isMore(boolean isMore) {
        this.isMore = isMore;
        if (mOnFooterStateListener != null) {
            if (isMore) {
                mOnFooterStateListener.onHasMore(mFooterView);
            } else {
                mOnFooterStateListener.onNotMore(mFooterView);
            }
        }
    }

    /**
     * 触发下拉刷新
     */
    public void autoRefresh() {

        if (!mIsRefresh) {
            return;
        }

        if (!isLoading()) {
            mIsRefreshing = true;
            mCurrentState = STATE_NOT;
            scroll(-mHeaderHeight);
            if (mOnHeaderStateListener != null) {
                mOnHeaderStateListener.onRefresh(mHeaderView);
            }

            if (mOnRefreshListener != null) {
                mOnRefreshListener.onRefresh();
            }
            scrollTop();
        }
    }

    /**
     * 触发上拉刷新
     */
    public void autoLoadMore() {

        if (!mIsLoadMore) {
            return;
        }

        if (!isLoading()) {
            mIsLoadingMore = true;
            mCurrentState = STATE_NOT;
            if (isMore) {
                scroll(mFooterHeight);
                if (mOnFooterStateListener != null) {
                    mOnFooterStateListener.onRefresh(mFooterView);
                }
                if (mOnLoadMoreListener != null) {
                    mOnLoadMoreListener.onLoadMore();
                }
            } else {
                refreshFinish();
            }
        }
    }

    /**
     * 滚动到顶部
     */
    protected void scrollTop() {
        if (getChildCount() > 2) {
            View view = getChildAt(2);
            view.scrollTo(view.getScrollX(), 0);
        }
    }

    private void smoothScroll(int start, int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end).setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scroll((int) animation.getAnimatedValue());
            }
        });
        animator.start();
    }

    /**
     * @param offset
     */
    private void scroll(int offset) {

        if (offset < 0 && !mIsRefresh) {
            return;
        }

        if (offset > 0 && !mIsLoadMore) {
            return;
        }

        scrollTo(0, offset);
        int scrollOffset = Math.abs(offset);

        if (mCurrentState == STATE_DOWN && mOnHeaderStateListener != null) {
            mOnHeaderStateListener.onScrollChange(mHeaderView, scrollOffset,
                    scrollOffset >= mHeaderHeight ? 100 : scrollOffset * 100 / mHeaderHeight);
        }

        if (mCurrentState == STATE_UP && mOnFooterStateListener != null && isMore) {
            mOnFooterStateListener.onScrollChange(mFooterView, scrollOffset,
                    scrollOffset >= mFooterHeight ? 100 : scrollOffset * 100 / mFooterHeight);
        }
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

//    public boolean isPullDown() {
//        return mIsRefreshing;
//    }
//
//    public boolean isPullUp() {
//        return mIsLoadingMore;
//    }

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
    protected abstract boolean canDropDown();

    /**
     * 可上拉的
     *
     * @return
     */
    protected abstract boolean canDropUp();

//    protected boolean isTop() {
//
//        if (getChildCount() < 2) {
//            return true;
//        }
//
//        View view = getChildAt(2);
//
//        if (view instanceof ViewGroup) {
//
//            if (view instanceof ScrollView) {
//                ScrollView scrollView = (ScrollView) view;
//                return scrollView.getScrollY() <= 0;
//            } else {
//                return isChildTop((ViewGroup) view);
//            }
//        } else {
//            return true;
//        }
//    }
//
//    protected boolean isChildTop(ViewGroup viewGroup) {
//        int minY = 0;
//        int count = viewGroup.getChildCount();
//        for (int i = 0; i < count; i++) {
//            View view = viewGroup.getChildAt(i);
//            int topMargin = 0;
//            LayoutParams lp = view.getLayoutParams();
//            if (lp instanceof MarginLayoutParams) {
//                topMargin = ((MarginLayoutParams) lp).topMargin;
//            }
//            int top = view.getTop() - topMargin;
//            minY = Math.min(minY, top);
//        }
//        return minY >= 0;
//    }

//    protected boolean isBottom() {
//
//        if (getChildCount() < 2) {
//            return false;
//        }
//
//        View view = getChildAt(2);
//
//        if (view instanceof ViewGroup) {
//            if (view instanceof ScrollView) {
//                ScrollView scrollView = (ScrollView) view;
//                if (scrollView.getChildCount() > 0) {
//                    return scrollView.getScrollY() >= scrollView.getChildAt(0).getHeight() - scrollView.getHeight();
//                } else {
//                    return true;
//                }
//            } else {
//                return isChildBottom((ViewGroup) view);
//            }
//        } else {
//            return true;
//        }
//    }
//
//    protected boolean isChildBottom(ViewGroup viewGroup) {
//        int maxY = 0;
//        int count = viewGroup.getChildCount();
//
//        if (count == 0) {
//            return false;
//        }
//
//        for (int i = 0; i < count; i++) {
//            View view = viewGroup.getChildAt(i);
//            int bottomMargin = 0;
//            LayoutParams lp = view.getLayoutParams();
//            if (lp instanceof MarginLayoutParams) {
//                bottomMargin = ((MarginLayoutParams) lp).bottomMargin;
//            }
//            int bottom = view.getBottom() + bottomMargin;
//            maxY = Math.max(maxY, bottom);
//        }
//
//        int h = viewGroup.getMeasuredHeight() - viewGroup.getPaddingBottom();
//
//        return maxY <= h;
//    }

    /**
     * 设置上拉监听器
     *
     * @param listener
     */
    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
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
         * 没有更多
         *
         * @param footerView
         */
        void onNotMore(View footerView);

        /**
         * 有更多
         *
         * @param footerView
         */
        void onHasMore(View footerView);
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


    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        Log.e("eee", "onStartNestedScroll = " + child + " + " + target + " + " + nestedScrollAxes);
        return true;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        Log.e("eee", "onNestedScrollAccepted = " + child + " + " + target + " + " + axes);
        super.onNestedScrollAccepted(child, target, axes);
    }

    @Override
    public void onStopNestedScroll(View child) {
        Log.e("eee", "onStopNestedScroll = " + child);
        restore();
        super.onStopNestedScroll(child);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        Log.e("eee", "onNestedScroll = " + target + " + " + dxConsumed + " + " + dyConsumed + "  + " + dxUnconsumed);
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Log.e("****", "onNestedPreScroll = dx:" + dx + " + dy:" + dy);

        if (pullDown()) {
            if (dy < 0) {
                if (getScrollY() > -mHeaderHeight) {
                    scroll(getScrollY() + dy);
                }
                consumed[1] += dy;
            } else if (getScrollY() < 0) {
                int offset = getScrollY() + dy;
                if (offset < 0) {
                    scroll(offset);
                } else {
                    scroll(0);
                }
                consumed[1] += dy;
            }
        }

        // pullDown() && y - mY > 20
        if (dy > 0) {

        }
        if (mIsRefreshing) {
            int scrollY = getScrollY();
            if (scrollY > 0) {

            }
        }

//        super.onNestedPreScroll(target, dx, dy, consumed);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        Log.e("eee", "onNestedFling = " + target + " + " + velocityX + " + " + velocityY + "  + " + consumed);
        return super.onNestedFling(target, velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Log.e("eee", "onNestedPreFling = " + target + " + " + velocityX + " + " + velocityY);
        return super.onNestedPreFling(target, velocityX, velocityY);
    }

    @Override
    public int getNestedScrollAxes() {
        Log.e("eee", "getNestedScrollAxes");
        return super.getNestedScrollAxes();
    }

    //    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//
//        int y = (int) event.getY();
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                return true;
//            case MotionEvent.ACTION_MOVE:
//                if (mY > y) {
//                    if (mCurrentState == STATE_UP) {
//                        scroll((mY - y) / mDamp);
//                    }
//                } else if (mCurrentState == STATE_DOWN) {
//                    scroll((mY - y) / mDamp);
//                }
//                break;
//
//            case MotionEvent.ACTION_UP:
//                if (!mIsRefreshing && !mIsLoadingMore) {
//                    int scrollOffset = Math.abs(getScrollY());
//                    if (mCurrentState == STATE_DOWN) {
//                        if (scrollOffset < mFooterHeight) {
//                            restore();
//                        } else {
//                            autoRefresh();
//                        }
//                    } else if (mCurrentState == STATE_UP) {
//                        if (scrollOffset < mFooterHeight) {
//                            restore();
//                        } else {
//                            autoLoadMore();
//                        }
//                    } else {
//                        restore();
//                    }
//                }
//                mY = 0;
//                break;
//
//            default:
//                break;
//        }
//        return super.onTouchEvent(event);
//    }
//
    int mY = 0;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

//        int y = (int) ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mY = (int) ev.getY();
//                return false;
            case MotionEvent.ACTION_MOVE:
//                if (isLoading()) {
//                    return false;
//                }
//
//                if (pullDown() && y - mY > 20) {
//                    mCurrentState = STATE_DOWN;
//                    return true;
//                }
//
//                if (pullUp() && mY - y > 20) {
//                    mCurrentState = STATE_UP;
//                    return true;
//                }
//
//                return false;
            case MotionEvent.ACTION_UP:

//                return false;
        }

        return super.onInterceptTouchEvent(ev);
    }
}

# RefreshLayout
Android 下拉刷新和上拉加载更多框架。RefreshLayout可以用于嵌套任意的View。比如RecyclerView、ScrollView、WebView等，实现下拉刷新和上拉加载功能。可以设置空布局和自定义头部尾部View等。

**效果图**

![main](https://github.com/donkingliang/RefreshLayout/blob/master/%E6%95%88%E6%9E%9C%E5%9B%BE/main.png) ![recyclerview](https://github.com/donkingliang/RefreshLayout/blob/master/%E6%95%88%E6%9E%9C%E5%9B%BE/recyclerview.png) ![webview](https://github.com/donkingliang/RefreshLayout/blob/master/%E6%95%88%E6%9E%9C%E5%9B%BE/webview.png) 

**1、引入依赖** 

在Project的build.gradle在添加以下代码

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
在Module的build.gradle在添加以下代码

```
  implementation 'com.github.donkingliang:RefreshLayout:1.1.0'
```

**2、编写布局**
```xml
    <com.donkingliang.refresh.RefreshLayout
        android:id="@+id/refresh_layout"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <!-- RefreshLayout嵌套的第一个View是内容布局。必须 -->
        <android.support.v7.widget.RecyclerView
            android:id="@+id/recycler_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:overScrollMode="never" />

        <!-- RefreshLayout嵌套的第二个View表示空布局。可选 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="空布局" />

        </LinearLayout>

    </com.donkingliang.refresh.RefreshLayout>
```

**3、常用设置**
```java
//设置头部(刷新)
mRefreshLayout.setHeaderView(new HeaderView(this));

//设置尾部(加载更多)
mRefreshLayout.setFooterView(new FooterView(this));

//设置刷新监听，触发刷新时回调
mRefreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
    @Override
    public void onRefresh() {
        // 刷新回调
        }
    });

//设置上拉加载更多的监听，触发加载时回调。
//RefreshLayout默认没有启用上拉加载更多的功能，如果设置了OnLoadMoreListener，则自动启用。
mRefreshLayout.setOnLoadMoreListener(new RefreshLayout.OnLoadMoreListener() {
    @Override
    public void onLoadMore() {
        // 加载回调 
        }
    });

// 启用下拉刷新功能。默认启用
mRefreshLayout.setRefreshEnable(true);

// 启用上拉加载更多功能。默认不启用，如果设置了OnLoadMoreListener，则自动启用。
mRefreshLayout.setLoadMoreEnable(true);

// 是否还有更多数据，只有为true是才能上拉加载更多，它会回调FooterView的onHasMore()方法。默认为true。
mRefreshLayout.hasMore(true);

//自动触发下拉刷新。只有启用了下拉刷新功能时起作用。
mRefreshLayout.autoRefresh();

//自动触发上拉加载更多，在滑动到底部的时候，自动加载更多。只有在启用了上拉加载更多功能并且有更多数据时起作用。
mRefreshLayout.autoLoadMore();

//通知刷新完成，isSuccess是否刷新成功
mRefreshLayout.finishRefresh(boolean isSuccess);

//通知加载完成，isSuccess是否加载成功，hasMore是否还有更多数据
mRefreshLayout.finishLoadMore(boolean isSuccess,boolean hasMore);

// 是否自动触发加载更多。只有在启用了上拉加载更多功能时起作用。
mRefreshLayout.setAutoLoadMore(true);

// 隐藏内容布局，显示空布局。
mRefreshLayout.showEmpty();

// 隐藏空布局，显示内容布局。
mRefreshLayout.hideEmpty();
```

**4、自定义头部和尾部**

一般来说，RefreshLayout的头部和尾部的View需要开发者自己实现，因为在实际的开发中往往需要我们自己定义头部、尾部的样式和控制状态。框架中提供了一个简单的头部(HeaderView)和尾部(FooterView)，这两个View的实现比较简单，样式也不是很好看，可以作为开发者自定义头部和尾部的参考吧。

框架中提供了自定义头部和尾部View的接口，使用这些接口可以很简单的实现和控制自己的头部和尾部。

自定义头部必须实现RefreshLayout.OnHeaderStateListener接口，并通过这个接口的回调来控件头部的状态。
```java
public class MyHeaderView extends LinearLayout implements RefreshLayout.OnHeaderStateListener{

    public MyHeaderView(Context context) {
        super(context);
    }

    public MyHeaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 头部滑动变化
     *
     * @param headerView   头部View
     * @param scrollOffset 滑动距离
     * @param scrollRatio  从开始到触发阀值的滑动比率（0到100）如果滑动到达了阀值，就算再滑动，这个值也是100
     */
    @Override
    public void onScrollChange(View headerView, int scrollOffset, int scrollRatio) {
    }

    /**
     * 头部处于刷新状态 （触发下拉刷新的时候调用）
     *
     * @param headerView 头部View
     */
    @Override
    public void onRefresh(View headerView) {
    }

    /**
     * 刷新完成，头部收起
     *
     * @param headerView 头部View
     @param isSuccess  是否刷新成功
     */
    @Override
    public void onRetract(View headerView, boolean isSuccess) {
    }
}
```

自定义尾部必须实现RefreshLayout.OnFooterStateListener接口，并通过这个接口的回调来控件尾部的状态。
```java
public class MyFooterView extends LinearLayout implements RefreshLayout.OnFooterStateListener{

    public MyFooterView(Context context) {
        super(context);
    }

    public MyFooterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyFooterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 尾部滑动变化
     *
     * @param footerView   尾部View
     * @param scrollOffset 滑动距离
     * @param scrollRatio  从开始到触发阀值的滑动比率（0到100）如果滑动到达了阀值，就算在滑动，这个值也是100
     */
    @Override
    public void onScrollChange(View footerView, int scrollOffset, int scrollRatio) {
    }

    /**
     * 尾部处于加载状态 （触发上拉加载的时候调用）
     *
     * @param footerView 尾部View
     */
    @Override
    public void onRefresh(View footerView) {
    }

    /**
     * 加载完成，尾部收起
     *
     * @param footerView 尾部View
     @param isSuccess  是否加载成功
     */
    @Override
    public void onRetract(View footerView, boolean isSuccess) {
    }

    /**
     * 是否还有更多(是否可以加载下一页)
     *
     * @param footerView
     * @param hasMore
     */
    @Override
    public void onHasMore(View footerView, boolean hasMore) {
    }
}
```

package com.donkingliang.refreshlayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.donkingliang.refresh.FooterView;
import com.donkingliang.refresh.HeaderView;
import com.donkingliang.refresh.RefreshLayout;
import com.donkingliang.refreshlayout.adapter.RecyclerAdapter;

/**
 * Depiction:
 * Author:lry
 * Date:2018/6/6
 */
public class RecyclerViewActivity extends AppCompatActivity {

    private RefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        mRefreshLayout = (RefreshLayout) findViewById(R.id.refresh_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RecyclerAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        //设置头部(刷新)
        mRefreshLayout.setHeaderView(new HeaderView(this));

        //设置尾部(加载更新)
        mRefreshLayout.setFooterView(new FooterView(this));

        //设置刷新监听，触发刷新时回调
        mRefreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //延时3秒刷新完成，模拟网络加载的情况
                mRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //通知刷新完成
                        mRefreshLayout.finishRefresh();
                        //是否还有更多数据
                        mRefreshLayout.hasMore(true);
                        mAdapter.setCount(20);
                    }
                }, 3000);
            }
        });

        //设置上拉加载更多的监听，触发加载时回调。
        //RefreshLayout默认没有启用上拉加载更多的功能，如果设置了OnLoadMoreListener，则自动启用。
        mRefreshLayout.setOnLoadMoreListener(new RefreshLayout.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                mRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //通知加载完成
                        if (mAdapter.getItemCount() < 50) {
                            mAdapter.setCount(mAdapter.getItemCount() + 10);
                            mRefreshLayout.finishLoadMore(true);
                        } else {
                            mRefreshLayout.finishLoadMore(false);
                        }
                    }
                }, 3000);
            }
        });

//        // 启用下拉刷新功能。默认启用
//        mRefreshLayout.setRefreshEnable(true);
//
//        // 启用上拉加载更多功能。默认不启用，如果设置了OnLoadMoreListener，则自动启用。
//        mRefreshLayout.setLoadMoreEnable(true);
//
//        // 是否还有更多数据，只有为true是才能上拉加载更多，它会回调FooterView的onHasMore()方法。默认为true。
//        mRefreshLayout.hasMore(true);

        //自动触发下拉刷新。只有启用了下拉刷新功能时起作用。
        mRefreshLayout.autoRefresh();

//        //自动触发上拉加载更多。只有在启用了上拉加载更多功能并且有更多数据时起作用。
//        mRefreshLayout.autoLoadMore();
//
//        // 是否自动触发加载更多，在滑动到底部的时候，自动加载更多。只有在启用了上拉加载更多功能时起作用。
//        mRefreshLayout.setAutoLoadMore(true);
//
//        // 隐藏内容布局，显示空布局。
//        mRefreshLayout.showEmpty();
//
//        // 隐藏空布局，显示内容布局。
//        mRefreshLayout.hideEmpty();
    }
}

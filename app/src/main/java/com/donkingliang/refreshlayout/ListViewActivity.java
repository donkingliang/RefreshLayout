package com.donkingliang.refreshlayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;

import com.donkingliang.refresh.FooterView;
import com.donkingliang.refresh.HeaderView;
import com.donkingliang.refresh.RefreshLayout;
import com.donkingliang.refreshlayout.adapter.RecyclerAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Depiction:
 * Author:lry
 * Date:2018/6/6
 */
public class ListViewActivity extends AppCompatActivity {

    private RefreshLayout mRefreshLayout;
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;

    private static final String LV_REFRESH_TIME = "LV_Refresh_Time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        mRefreshLayout = (RefreshLayout) findViewById(R.id.refresh_layout);
        mListView = (ListView) findViewById(R.id.list_view);
        mAdapter = new ArrayAdapter<String>(this, R.layout.adapter_item, R.id.tv_name);
        mListView.setAdapter(mAdapter);

        //设置头部(刷新)
        HeaderView headerView = new HeaderView(this);
        long refreshTime = SPUtil.getRefreshTime(LV_REFRESH_TIME);
        if (refreshTime > 0) {
            headerView.setRefreshTime(new Date(refreshTime));
        }
        mRefreshLayout.setHeaderView(headerView);

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
                        SPUtil.writeRefreshTime(LV_REFRESH_TIME,new Date().getTime());
                        //通知刷新完成
                        mRefreshLayout.finishRefresh();
                        //是否还有更多数据
                        mRefreshLayout.hasMore(true);

                        mAdapter.clear();
                        mAdapter.addAll(getData(20, 0));
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
                        if (mAdapter.getCount() < 50) {
                            mAdapter.addAll(getData(10, mAdapter.getCount()));
                            //通知加载完成
                            mRefreshLayout.finishLoadMore(true);
                        } else {
                            //通知加载完成
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
//        // 是否自动触发加载更多。只有在启用了上拉加载更多功能时起作用。
//        mRefreshLayout.setAutoLoadMore(true);
//
//        // 隐藏内容布局，显示空布局
//        mRefreshLayout.showEmpty();
//
//        // 隐藏空布局，显示内容布局
//        mRefreshLayout.hideEmpty();
    }

    private List<String> getData(int count, int position) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add("ListView item:" + (i + position));
        }

        return list;
    }
}

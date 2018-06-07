package com.donkingliang.refreshlayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.donkingliang.refresh.FooterView;
import com.donkingliang.refresh.HeaderView;
import com.donkingliang.refresh.RefreshLayout;

/**
 * Depiction:
 * Author:lry
 * Date:2018/6/6
 */
public class ScrollViewActivity extends AppCompatActivity {

    private RefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scroll_view);

        mRefreshLayout = (RefreshLayout) findViewById(R.id.refresh_layout);

        //设置头部(刷新)
        mRefreshLayout.setHeaderView(new HeaderView(this));

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
                    }
                }, 3000);
            }
        });

//        // 启用下拉刷新功能。默认启用
//        mRefreshLayout.setRefreshEnable(true);
//
//        //自动触发下拉刷新。只有启用了下拉刷新功能时起作用。
//        mRefreshLayout.autoRefresh();
//
//        // 隐藏内容布局，显示空布局
//        mRefreshLayout.showEmpty();
//
//        // 隐藏空布局，显示内容布局
//        mRefreshLayout.hideEmpty();
    }
}

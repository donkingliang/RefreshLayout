package com.donkingliang.refreshlayout;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.donkingliang.refresh.FooterView;
import com.donkingliang.refresh.HeaderView;
import com.donkingliang.refresh.RefreshLayout;
import com.donkingliang.refreshlayout.adapter.ItemAdapter;

public class MainActivity extends AppCompatActivity {

    private RefreshLayout mRefresh;
    private RecyclerView mRvRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRefresh = (RefreshLayout) findViewById(R.id.refresh);
        mRvRefresh = (RecyclerView) findViewById(R.id.rv_refresh);

        mRvRefresh.setLayoutManager(new LinearLayoutManager(this));
        mRvRefresh.setAdapter(new ItemAdapter(this));
        mRefresh.setHeaderView(new HeaderView(this));
        mRefresh.setFooterView(new FooterView(this));
        mRefresh.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefresh.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefresh.refreshFinish();
                    }
                }, 3000);
            }
        });

        mRefresh.setOnLoadMoreListener(new RefreshLayout.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                mRefresh.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefresh.refreshFinish();
                    }
                }, 3000);
            }
        });

        mRefresh.autoRefresh();

    }
}

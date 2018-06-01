package com.donkingliang.refreshlayout;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import com.donkingliang.refresh.RefreshLayout;
import com.donkingliang.refresh.RefreshRecyclerView;
import com.donkingliang.refreshlayout.adapter.ItemAdapter;

public class MainActivity extends AppCompatActivity {

    private RefreshRecyclerView mRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRefresh = (RefreshRecyclerView) findViewById(R.id.rv_refresh);

        mRefresh.setLayoutManager(new LinearLayoutManager(this));
        mRefresh.setAdapter(new ItemAdapter(this));

        mRefresh.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefresh.postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        mRefresh.refreshFinish();
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
//                        mRefresh.refreshFinish();
                    }
                }, 3000);
            }
        });
//        mRefresh.isMore(false);
    }
}

package com.donkingliang.refresh;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FooterView extends RelativeLayout implements RefreshLayout.OnFooterStateListener {

    private ImageView ivLoading;
    private TextView tvState;

    private AnimationDrawable animationDrawable;

    private boolean hasMore = true;

    private String footerPulling;
    private String footerRelease;
    private String footerLoading;
    private String footerFinish;
    private String footerNothing;

    public FooterView(Context context) {
        super(context);
        animationDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.progress_round);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.footer_view_layout, this, false);
        this.addView(layout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        initView(layout);

        Resources resources = context.getResources();
        footerPulling = resources.getString(R.string.footer_pulling);
        footerRelease = resources.getString(R.string.footer_release);
        footerLoading = resources.getString(R.string.footer_loading);
        footerFinish = resources.getString(R.string.footer_finish);
        footerNothing = resources.getString(R.string.footer_nothing);
    }

    private void initView(View view) {
        ivLoading = (ImageView) view.findViewById(R.id.iv_loading);
        tvState = (TextView) view.findViewById(R.id.tv_state);
    }

    @Override
    public void onScrollChange(View tail, int scrollOffset, int scrollRatio) {
        if (hasMore) {
            if (scrollRatio < 100) {
                tvState.setText(footerPulling);
                ivLoading.setImageResource(R.drawable.icon_down_arrow);
                ivLoading.setRotation(180);
            } else {
                tvState.setText(footerRelease);
                ivLoading.setImageResource(R.drawable.icon_down_arrow);
                ivLoading.setRotation(0);
            }
        }
    }

    @Override
    public void onRefresh(View footerView) {
        if (hasMore) {
            tvState.setText(footerLoading);
            ivLoading.setImageDrawable(animationDrawable);
            animationDrawable.start();
        }
    }

    @Override
    public void onRetract(View footerView) {
        if (hasMore) {
            tvState.setText(footerFinish);
            ivLoading.setImageBitmap(null);
        }
    }

    @Override
    public void onHasMore(View tail, boolean hasMore) {
        this.hasMore = hasMore;
        if (!hasMore) {
            tvState.setText(footerNothing);
            ivLoading.setImageBitmap(null);
        }
    }
}

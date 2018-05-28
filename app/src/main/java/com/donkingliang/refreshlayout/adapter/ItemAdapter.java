package com.donkingliang.refreshlayout.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.donkingliang.refreshlayout.R;

/**
 * Depiction:
 * Author:lry
 * Date:2018/5/25
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemHolder> {

    private Context mContext;

    public ItemAdapter(Context context) {
        mContext = context;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.adapter_item, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return 20;
    }

    static class ItemHolder extends RecyclerView.ViewHolder {

        TextView tvName;

        public ItemHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
        }
    }
}

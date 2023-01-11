package com.cloudcoin2.wallet.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.cloudcoin2.wallet.Model.EchoStatus;
import com.cloudcoin2.wallet.R;

import java.util.ArrayList;

/**
 * Created by Arka Chakraborty on 15/02/22
 */
public class IndicatorAdapter extends RecyclerView.Adapter<IndicatorAdapter.MyViewHolder> {

    private ArrayList<EchoStatus> mList=new ArrayList<>(25);
    private Context mContext;
    private int itemWidth;

    public IndicatorAdapter(Context mContext, int itemWidth, ArrayList<EchoStatus> mList) {
        this.mList = mList;
        this.mContext = mContext;
     if(itemWidth>15)
      this.itemWidth = itemWidth-15;
        this.itemWidth = itemWidth;


    }

    public IndicatorAdapter(Context mContext, int itemWidth) {
        this.mContext = mContext;
        this.itemWidth = itemWidth;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addData(EchoStatus status) {
        mList.add(status);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void replaceData(EchoStatus status,int position) {
        mList.get(position).setRaidaId(status.getRaidaId());
        mList.get(position).setStatus(status.getStatus());

        //notifyItemChanged(position);

    }

    @SuppressLint("NotifyDataSetChanged")
    public boolean findData(int raidaID) {
        boolean isexists=false;
        for (int i=0;i<mList.size();i++){
            if(mList.get(i).getRaidaId()==raidaID) {
                if (mList.get(i).getStatus().equals("success"))
                    isexists=true;
            }
        }

        //notifyItemChanged(position);
      return isexists;
    }
    @SuppressLint("NotifyDataSetChanged")
    public void addList(ArrayList<EchoStatus> mList) {
        this.mList = mList;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_indicator, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        if (mList.get(position).getStatus().equals("Test")) {
            holder.llItem.setBackground(mContext.getDrawable(R.drawable.grey_round_all));
        } else if (mList.get(position).getStatus().equals("success"))
            holder.llItem.setBackground(mContext.getDrawable(R.drawable.green_round_all));
        else if (mList.get(position).getStatus().equals("failure"))
            holder.llItem.setBackground(mContext.getDrawable(R.drawable.red_round_all));
        else
            holder.llItem.setBackground(mContext.getDrawable(R.drawable.yellow_round_all));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout llItem;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            llItem = itemView.findViewById(R.id.item_indicator_llItem);
            itemView.getLayoutParams().width = itemWidth;

        }

    }
}

package com.cloudcoin2.wallet.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;


import com.cloudcoin2.wallet.R;
import com.cloudcoin2.wallet.db.Transactions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by SIRSHA BANERJEE on 15/12/20
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TasksViewHolder> {

    private Context mCtx;
    private List<Transactions> taskList;

    public TransactionAdapter(Context mCtx, List<Transactions> taskList) {
        this.mCtx = mCtx;
        this.taskList = taskList;
    }

    @Override
    public TasksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mCtx).inflate(R.layout.item_transaction, parent, false);
        return new TasksViewHolder(view);
    }


    @Override
    public void onBindViewHolder(TasksViewHolder holder, int position) {
        Transactions t = taskList.get(position);
        if(t.type.equalsIgnoreCase("0")) {
            if(t.memo.equals(""))
                holder.tvHeader.setText("No Memo");
            else
            holder.tvHeader.setText(t.memo);
            holder.ivPointer.setImageDrawable(ContextCompat.getDrawable(mCtx, R.drawable.ic_green_up));
            holder.tvAmount.setText("+"+String.valueOf(t.amount));
            holder.tvAmount.setTextColor(ContextCompat.getColor(mCtx, R.color.colorGreen));
            holder.ivPriceSymbol.setImageDrawable(ContextCompat.getDrawable(mCtx, R.drawable.ic_price_symbol_green));
        }
        else {
            if(t.memo.equals(""))
                holder.tvHeader.setText("No Memo");
            else
                holder.tvHeader.setText(t.memo);
            holder.ivPointer.setImageDrawable(ContextCompat.getDrawable(mCtx, R.drawable.ic_red_down));
            holder.tvAmount.setText("-"+String.valueOf(t.amount));
            holder.tvAmount.setTextColor(ContextCompat.getColor(mCtx, R.color.colorDarkRed));
            holder.ivPriceSymbol.setImageDrawable(ContextCompat.getDrawable(mCtx, R.drawable.ic_price_symbol_red));
        }
        if(t.date!=null) {
            long sysTime = Long.parseLong(t.date);

            Date date = new Date((long) sysTime);
            DateFormat formattedDate = new SimpleDateFormat("MMM dd, YY hh:mm a");
            holder.tvDate.setText(formattedDate.format(date));

        }
        else
        {
            holder.tvDate.setText("");
        }

    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    class TasksViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView tvHeader, tvDate, tvAmount;
        ImageView ivPointer, ivPriceSymbol;

        public TasksViewHolder(View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.item_transaction_tvHeader);
            tvAmount = itemView.findViewById(R.id.item_transaction_tvAmount);
            tvDate = itemView.findViewById(R.id.item_transaction_tvDate);
            ivPointer = itemView.findViewById(R.id.item_transaction_ivPointer);
            ivPriceSymbol = itemView.findViewById(R.id.item_transaction_ivPriceSymbol);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

        }
    }
}

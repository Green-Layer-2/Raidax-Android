package com.cloudcoin2.wallet.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudcoin2.wallet.Model.Transaction;
import com.cloudcoin2.wallet.R;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactions;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        // Bind transaction data to the views here, e.g.:
        // holder.tvAmount.setText(String.valueOf(transaction.getAmount()));
        holder.tvDescription.setText((String.valueOf(transaction.getDescription())));
        holder.tvDated.setText((String.valueOf(transaction.getDated())));

    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // Declare your item views here, e.g.:
        // TextView tvAmount;
        TextView tvAmount;
        TextView tvDescription;
        TextView tvDated;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize your item views here, e.g.:
            tvDescription = itemView.findViewById(R.id.item_transaction_tvHeader);
            tvDated = itemView.findViewById(R.id.item_transaction_tvDate);
        }
    }
}

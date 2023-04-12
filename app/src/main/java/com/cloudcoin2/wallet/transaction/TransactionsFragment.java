package com.cloudcoin2.wallet.transaction;

import android.view.View;
import android.widget.TextView;
import com.cloudcoin2.wallet.Adapter.TransactionAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudcoin2.wallet.Model.Transaction;
import com.cloudcoin2.wallet.R;
import com.cloudcoin2.wallet.Utils.DatabaseHelper;
import com.cloudcoin2.wallet.base.BaseFragment2;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by NAVRAJ SINGH on 10/04/23
 */
public class TransactionsFragment extends BaseFragment2 {

    private  List<Transaction> transactions = new ArrayList<>();
    private DatabaseHelper databaseHelper ;
    private TransactionAdapter transactionAdapter;
    private RecyclerView recyclerView;
    private TextView tvClear;

    @Override
    protected int defineLayoutResource() {
        return R.layout.fragment_transaction;
    }

    @Override
    protected void initializeComponent(@NotNull View view) {
        recyclerView = view.findViewById(R.id.fragment_transaction_rvTransaction);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        tvClear = view.findViewById(R.id.fragment_transaction_tvClear);
        databaseHelper = new DatabaseHelper(getActivity());

        transactions = Transaction.getTransactions(databaseHelper);

        transactionAdapter = new TransactionAdapter(transactions);
        recyclerView.setAdapter(transactionAdapter);

    }

    @Override
    protected void initializeBehavior() {

    }


 }


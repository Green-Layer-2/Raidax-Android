package com.cloudcoin2.wallet.transaction;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudcoin2.wallet.R;
import com.cloudcoin2.wallet.base.BaseFragment2;
import com.cloudcoin2.wallet.db.DatabaseClient;
import com.cloudcoin2.wallet.db.Transactions;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SIRSHA BANERJEE on 15/12/20
 */
public class TransactionFragment extends BaseFragment2 {

    private  List<Transactions> transactions = new ArrayList<>();

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
        tvClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle("Clear History");
                alert.setMessage("Are you sure you want to clear transaction history?");
                alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearHistory();
                    }
                });

                alert.setNegativeButton("No", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                alert.show();

            }
        });

        fetchTask();
    }

    @Override
    protected void initializeBehavior() {

    }


    private void fetchTask() {




        class FetchTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {


                 transactions = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                        .transactionDao()
                        .getAll();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                //Toast.makeText(getActivity().getApplicationContext(), "Fetched", Toast.LENGTH_LONG).show();
                TransactionAdapter adapter = new TransactionAdapter(getActivity(), transactions);
                recyclerView.setAdapter(adapter);
            }
        }

        FetchTask st = new FetchTask();
        st.execute();
    }

    private void clearHistory(){
        class DeleteTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {

                 DatabaseClient.getInstance(getActivity()).getAppDatabase()
                        .transactionDao()
                        .nukeTable();
                 transactions = new ArrayList<>();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                Toast.makeText(getActivity(), "Transaction History Cleared", Toast.LENGTH_LONG).show();
              TransactionAdapter adapter = new TransactionAdapter(getActivity(), transactions);
                recyclerView.setAdapter(adapter);
            }
        }

        DeleteTask st = new DeleteTask();
        st.execute();
    }
}

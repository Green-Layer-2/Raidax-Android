package com.cloudcoin2.wallet.Model;

import android.database.Cursor;
import android.util.Log;

import com.cloudcoin2.wallet.Utils.DatabaseHelper;
import com.cloudcoin2.wallet.Utils.RAIDAX;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Transaction {

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDated() {
        return dated;
    }

    public void setDated(String dated) {
        this.dated = dated;
    }

    private int id;
    private String description;
    private long timestamp;
    private String dated;

    public Transaction() {

    }

    public Transaction(int id, String description, long timestamp, String dated) {
        this.id = id;
        this.description = description;
        this.timestamp = timestamp;
        this.dated = dated;
    }

    public static ArrayList<Transaction> getTransactions(DatabaseHelper databaseHelper) {
        ArrayList<Transaction> transactions = new ArrayList<>();

        Cursor cursor = databaseHelper.getStatements(); // Assuming you have a method called getStatements() that returns a Cursor

        int i =0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Retrieve the values from the Cursor
                String description = cursor.getString(0);
                long timestamp = cursor.getLong(1);

                // Perform any required operation on the retrieved values
                // For example, you can print the values
                Date date = new Date(timestamp);

                // Create a SimpleDateFormat object with the desired date format
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                // Convert the Date object to a formatted date string
                String dateString = simpleDateFormat.format(date);
                Log.d(RAIDAX.TAG, "Description: " + description + ", Timestamp: " + dateString);
                Transaction transaction = new Transaction(i++, description, timestamp, dateString);
                transactions.add(transaction);

            } while (cursor.moveToNext());

            // Close the Cursor after use
            cursor.close();

        }
        return transactions;
    }

}

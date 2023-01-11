package com.cloudcoin2.wallet.db;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

/**
 * Created by SIRSHA BANERJEE on 15/12/20
 */
@Database(entities = {SavedCoin.class, Transactions.class,Settings.class},version = 3, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CoinDao coinDao();
    public abstract TransactionDao transactionDao();
    public abstract SettingsDao settingsDao();

    @NonNull
    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config) {
        return null;
    }

    @NonNull
    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return null;
    }

    @Override
    public void clearAllTables() {

    }
}

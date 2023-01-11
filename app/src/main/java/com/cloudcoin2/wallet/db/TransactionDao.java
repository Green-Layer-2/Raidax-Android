package com.cloudcoin2.wallet.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM Transactions order by id desc")
    List<Transactions> getAll();

    @Insert
    void insertAll(Transactions... users);


    @Query("DELETE FROM Transactions")
    public void nukeTable();
}

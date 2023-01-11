package com.cloudcoin2.wallet.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * Created by SIRSHA BANERJEE on 15/12/20
 */
@Dao
public interface CoinDao {

    @Query("SELECT * FROM SavedCoin")
    List<SavedCoin> getAll();

    @Query("SELECT * FROM SavedCoin WHERE status=:bank OR status=:fracked")
    List<SavedCoin> getAllBank(String bank ,String fracked);

    @Query("Select * FROM SavedCoin WHERE serialNumber=:serial AND status='Bank' OR status='Fracked'")
    List<SavedCoin> getValidCoin(int serial);

    @Query("Select * FROM SavedCoin WHERE serialNumber=:serial")
    List<SavedCoin> getCoin(int serial);


    @Transaction @Insert
    void insertAll(List<SavedCoin> coins);

    @Query("DELETE FROM SavedCoin")
    public void nukeTable();

    @Query("Select status FROM SavedCoin WHERE serialNumber=:serial")
    public String getCoinStatus(int serial);

    @Query("DELETE FROM SavedCoin WHERE serialNumber=:serial")
    public void deleteCoin(int serial);

    @Query("UPDATE SavedCoin SET status=:status WHERE serialNumber=:serial")
    public void upDateCoinStatus(int serial,String status);

}

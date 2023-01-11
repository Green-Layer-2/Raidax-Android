package com.cloudcoin2.wallet.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface SettingsDao {

        @Query("SELECT * FROM Settings")
        List<Settings> getAllStatus();

        @Insert
        void insertSettings(Settings settings);

        @Query("UPDATE Settings SET value=:value WHERE description=:desc ")
        public void upDateSettings(String value,String desc);



}

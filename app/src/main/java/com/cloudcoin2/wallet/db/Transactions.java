package com.cloudcoin2.wallet.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Transactions {


    @PrimaryKey(autoGenerate = true)
    public int id;

     @ColumnInfo
    public String date;

   @ColumnInfo(name = "memo")
    public String memo;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "amount")
    public String amount;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

}

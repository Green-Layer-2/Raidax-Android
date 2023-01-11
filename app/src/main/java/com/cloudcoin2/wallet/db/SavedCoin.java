package com.cloudcoin2.wallet.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Created by SIRSHA BANERJEE on 15/12/20
 */
@Entity
public class SavedCoin {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "amount")
    public String amount;

    @ColumnInfo(name = "denomination")
    public String denomination;

    @ColumnInfo(name="status")
    public String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ColumnInfo(name = "serialNumber")
    public int serialNumber;
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getDenomination() {
        return denomination;
    }

    public void setDenomination(String denomination) {
        this.denomination = denomination;
    }
}

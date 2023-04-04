package com.cloudcoin2.wallet.Utils;

// DatabaseHelper.java
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "my_database.db";
    private static final int DATABASE_VERSION = 1;

    // Table and column names
    public static final String TABLE_STATEMENTS = "statements";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_STATEMENTS + " (" +
                COLUMN_DESCRIPTION + " TEXT," +
                COLUMN_TIMESTAMP + " INTEGER" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database schema upgrades here
    }

    public Cursor getStatements() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_STATEMENTS + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        return db.rawQuery(query, null);
    }

    // DatabaseHelper.java (continued)
    public boolean insertStatement(String description, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_DESCRIPTION, description);
        contentValues.put(COLUMN_TIMESTAMP, timestamp);

        long result = db.insert(TABLE_STATEMENTS, null, contentValues);

        // Check if the insertion was successful
        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }


}

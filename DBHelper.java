package com.example.tfai;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {
    private static final String MYDATABASE_TABLE = "CONTACTS";

    public DBHelper(@Nullable Context context, @Nullable String name) {
        super(context, name, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table CONTACTS " +
                        "(id INTEGER primary key AUTOINCREMENT, contact VARCHAR(40))"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    public boolean insertValue(String contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("contact", contact.trim());
        long result = db.insert("CONTACTS", null, contentValues);
        if (result == -1) {
            Log.e("DBHelper", "Failed to insert contact: " + contact);
            return false;
        } else {
            Log.d("DBHelper", "Inserted contact: " + contact + " at row: " + result);
            return true;
        }
    }


    public void logAllContacts() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("CONTACTS", new String[]{"contact"}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String dbContact = cursor.getString(cursor.getColumnIndex("contact"));
                Log.d("DBHelper", "Contact in DB: [" + dbContact + "] Length: " + dbContact.length());
            } while (cursor.moveToNext());
        } else {
            Log.d("DBHelper", "No contacts found in the database.");
        }
        cursor.close();
        db.close();
    }




    public boolean deleteContact(String contactToDelete) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + MYDATABASE_TABLE + " WHERE contact = ?", new String[]{contactToDelete});

        if (cursor != null && cursor.getCount() > 0) {
            int rowsDeleted = db.delete(MYDATABASE_TABLE, "contact = ?", new String[]{contactToDelete});
            Log.d("Sqliteacti", "row deleted"+rowsDeleted + contactToDelete);
            cursor.close();
            db.close();
            return rowsDeleted > 0;

        } else {
            Log.d("Sqliteacti", "No contact found to delete: " + contactToDelete);
            return false;
        }
    }


    public void logAllRows(SQLiteDatabase db, String message) {
        Cursor cursor = db.rawQuery("SELECT * FROM " + MYDATABASE_TABLE, null);

        Log.d("DBHelper", message + ": Table contents:");
        if (cursor.moveToFirst()) {
            do {
                StringBuilder row = new StringBuilder();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    row.append(cursor.getColumnName(i)).append(": ").append(cursor.getString(i)).append(" | ");
                }
                Log.d("DBHelper", row.toString());
            } while (cursor.moveToNext());
        } else {
            Log.d("DBHelper", message + ": No rows found in the table.");
        }
        cursor.close();
    }







    public ArrayList<String> getAllContacts() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * from " + MYDATABASE_TABLE, null);

        ArrayList<String> contacts = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String contact = cursor.getString(cursor.getColumnIndex("contact"));
                contacts.add(contact);
                Log.d("Sqliteacti", "Contact found: " + contact);
            } while (cursor.moveToNext());
        } else {
            Log.d("Sqliteacti", "No contacts found in the table.");
        }
        cursor.close();
        db.close();
        Log.d("Sqliteacti", "All contacts: " + contacts);
        return contacts;
    }

}

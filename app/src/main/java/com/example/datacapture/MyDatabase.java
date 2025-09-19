package com.example.datacapture;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {TableModel.class}, version = 1)
public abstract class MyDatabase extends RoomDatabase {
    public abstract TableModelDao tableModelDao();
}

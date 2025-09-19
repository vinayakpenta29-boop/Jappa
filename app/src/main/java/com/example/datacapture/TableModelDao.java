package com.example.datacapture;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TableModelDao {
    @Insert
    void insert(TableModel row);

    @Query("SELECT * FROM TableModel ORDER BY id ASC")
    List<TableModel> getAll();

    @Query("DELETE FROM TableModel")
    void deleteAll();
}

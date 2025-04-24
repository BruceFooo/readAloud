package com.frunoob.readaloud;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.frunoob.readaloud.dao.BookDao;
import com.frunoob.readaloud.entity.Book;

@Database(entities = {Book.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract BookDao bookDao();
}

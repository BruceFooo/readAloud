package com.frunoob.readaloud.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.frunoob.readaloud.entity.Book;

import java.util.List;

@Dao
public interface BookDao {
    @Query("SELECT * FROM book")
    List<Book> getAll();

    @Insert
    void insertAll(Book... books);

    @Delete
    void delete(Book book);

    @Query("SELECT * FROM book WHERE uri IN (:uri)")
    Book findByUri(String uri);

    @Update
    void update(Book book);
}

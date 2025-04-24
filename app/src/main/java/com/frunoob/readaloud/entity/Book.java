package com.frunoob.readaloud.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.sql.Date;

@Entity
public class Book {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    public String name;

    public String location;

    public int paperNumber;

    public int readLine;

    public String uri;

    public String lastReadTime;

    public void setUri(String string) {
        // TODO Auto-generated method stub
         this.uri = string;
    }
}

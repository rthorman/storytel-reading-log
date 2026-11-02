package com.readinglog.storytel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class LogDatabase extends SQLiteOpenHelper {
    static final class Book {
        long id;
        String title = "", author = "", series = "", format = "Audiobook", started = "", finished = "";
    }
    static final class Note {
        long id;
        String body, created;
    }
    LogDatabase(Context context) { super(context, "reading-log.db", null, 1); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE series (id INTEGER PRIMARY KEY, name TEXT NOT NULL COLLATE NOCASE UNIQUE)");
        db.execSQL("CREATE TABLE books (id INTEGER PRIMARY KEY, title TEXT NOT NULL, author TEXT NOT NULL, series_id INTEGER REFERENCES series(id), format TEXT NOT NULL CHECK(format IN ('Audiobook','Ebook')), started TEXT NOT NULL, finished TEXT NOT NULL)");
        db.execSQL("CREATE TABLE comments (id INTEGER PRIMARY KEY, book_id INTEGER REFERENCES books(id) ON DELETE CASCADE, series_id INTEGER REFERENCES series(id) ON DELETE CASCADE, body TEXT NOT NULL, created TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M','now')), CHECK ((book_id IS NULL) != (series_id IS NULL)))");
        db.execSQL("CREATE INDEX books_series ON books(series_id)");
        db.execSQL("CREATE INDEX comments_book ON comments(book_id)");
        db.execSQL("CREATE INDEX comments_series ON comments(series_id)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("A migration is required; never discard the reading history.");
    }
    List<Book> books(String query, int sort, int filter) {
        String order = switch(sort) {
            case 1 -> "b.author COLLATE NOCASE, b.title COLLATE NOCASE";
            case 2 -> "CASE WHEN s.name IS NULL THEN 1 ELSE 0 END, s.name COLLATE NOCASE, b.title COLLATE NOCASE";
            case 3 -> "b.started DESC, b.title COLLATE NOCASE";
            default -> "b.title COLLATE NOCASE, b.author COLLATE NOCASE";
        };
        String status = switch(filter) {
            case 1 -> " AND b.started != '' AND b.finished = ''";
            case 2 -> " AND b.finished != ''";
            case 3 -> " AND b.started = ''";
            default -> "";
        };
        String pattern = "%" + query.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        List<Book> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT b.id,b.title,b.author,COALESCE(s.name,''),b.format,b.started,b.finished FROM books b LEFT JOIN series s ON s.id=b.series_id WHERE (b.title LIKE ? ESCAPE '\\' OR b.author LIKE ? ESCAPE '\\' OR s.name LIKE ? ESCAPE '\\')" + status + " ORDER BY " + order + ",b.id", new String[]{pattern,pattern,pattern})) {
            while(c.moveToNext()) {
                Book b = new Book(); b.id=c.getLong(0); b.title=c.getString(1); b.author=c.getString(2); b.series=c.getString(3);
                b.format=c.getString(4); b.started=c.getString(5); b.finished=c.getString(6); result.add(b);
            }
        }
        return result;
    }
    long seriesId(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(); values.put("name", name.trim());
        db.insertWithOnConflict("series", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        try(Cursor c=db.rawQuery("SELECT id FROM series WHERE name=? COLLATE NOCASE",new String[]{name.trim()})) {
            if (!c.moveToFirst()) throw new IllegalStateException("Series could not be saved.");
            return c.getLong(0);
        }
    }
    List<String> series() {
        List<String> result = new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT name FROM series ORDER BY name COLLATE NOCASE",null)) {
            while(c.moveToNext()) result.add(c.getString(0));
        }
        return result;
    }
    void save(Book b) {
        LogRules.validate(b.title,b.author,b.started,b.finished);
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            ContentValues v=new ContentValues(); v.put("title",b.title.trim()); v.put("author",b.author.trim());
            if(b.series.trim().isEmpty()) v.putNull("series_id"); else v.put("series_id",seriesId(b.series));
            v.put("format",b.format); v.put("started",b.started); v.put("finished",b.finished);
            if(b.id==0) b.id=db.insertOrThrow("books",null,v); else db.update("books",v,"id=?",new String[]{Long.toString(b.id)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    List<Note> notes(long id, boolean series) {
        List<Note> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,body,created FROM comments WHERE " + (series?"series_id":"book_id") + "=? ORDER BY id DESC",new String[]{Long.toString(id)})) {
            while(c.moveToNext()) { Note n=new Note(); n.id=c.getLong(0); n.body=c.getString(1); n.created=c.getString(2); result.add(n); }
        }
        return result;
    }
    void addNote(long id, boolean series, String body) {
        if(body.trim().isEmpty()) throw new IllegalArgumentException("Write a comment first.");
        ContentValues v=new ContentValues(); v.put(series?"series_id":"book_id",id); v.put("body",body.trim());
        getWritableDatabase().insertOrThrow("comments",null,v);
    }
    void deleteNote(long id) { getWritableDatabase().delete("comments","id=?",new String[]{Long.toString(id)}); }
    void deleteBook(long id) { getWritableDatabase().delete("books","id=?",new String[]{Long.toString(id)}); }
}

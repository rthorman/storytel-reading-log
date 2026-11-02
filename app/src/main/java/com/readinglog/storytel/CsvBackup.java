// SPDX-License-Identifier: AGPL-3.0-or-later
package com.readinglog.storytel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.*;
import java.time.LocalDateTime;
import java.time.format.*;
import java.util.*;

/** Versioned, single-file CSV backups. Validate before offering transactional replacement. */
final class CsvBackup {
    static final List<String> HEADER=Collections.unmodifiableList(Arrays.asList("record_type","id","parent_id","title","author","series","format","started","finished","comment","created_utc"));
    static final class Snapshot {
        final List<List<String>> series=new ArrayList<>(), books=new ArrayList<>(), comments=new ArrayList<>();
        int bookCount() { return books.size(); }
        int seriesCount() { return series.size(); }
        int commentCount() { return comments.size(); }
    }
    private static List<String> record(String... fields) { String[] row=new String[11];Arrays.fill(row,"");System.arraycopy(fields,0,row,0,fields.length);return Arrays.asList(row); }
    static void write(LogDatabase helper,OutputStream stream) throws IOException {
        // Android 8 provides java.time; avoid Java 9 collection factories on that API.
        Writer out=new BufferedWriter(new OutputStreamWriter(stream,StandardCharsets.UTF_8));
        SQLiteDatabase db=helper.getReadableDatabase(); db.beginTransactionNonExclusive();
        try {
            Csv.write(out,HEADER); Csv.write(out,record("meta","1"));
            try(Cursor c=db.rawQuery("SELECT id,name FROM series ORDER BY id",null)) {while(c.moveToNext()) Csv.write(out,record("series",c.getString(0),"","","",c.getString(1)));}
            try(Cursor c=db.rawQuery("SELECT b.id,COALESCE(b.series_id,''),b.title,b.author,COALESCE(s.name,''),b.format,b.started,b.finished FROM books b LEFT JOIN series s ON b.series_id=s.id ORDER BY b.id",null)) {
                while(c.moveToNext()) Csv.write(out,record("book",c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7)));
            }
            try(Cursor c=db.rawQuery("SELECT id,book_id,series_id,body,created FROM comments ORDER BY id",null)) {
                while(c.moveToNext()) Csv.write(out,record(c.isNull(1)?"series_comment":"book_comment",c.getString(0),c.isNull(1)?c.getString(2):c.getString(1),"","","","","","",c.getString(3),c.getString(4)));
            }
            out.flush(); db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    static Snapshot read(InputStream stream) throws IOException {
        InputStream limited=new FilterInputStream(stream) {
            long bytes;
            private void count(long n) throws IOException {bytes+=n;if(bytes>25L*1024*1024)throw new IOException("Backup exceeds 25 MiB.");}
            @Override public int read() throws IOException {int n=super.read();if(n!=-1)count(1);return n;}
            @Override public int read(byte[] b,int off,int len) throws IOException {int n=in.read(b,off,len);if(n>0)count(n);return n;}
        };
        CharsetDecoder utf8=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        List<List<String>> rows=Csv.read(new InputStreamReader(limited,utf8));
        if(rows.size()<2 || !rows.get(0).equals(HEADER) || !rows.get(1).equals(record("meta","1"))) throw new IOException("Not a supported Storytel Reading Log CSV backup (version 1).");
        Snapshot snapshot=new Snapshot(); Set<Long> seriesIds=new HashSet<>(),bookIds=new HashSet<>(),commentIds=new HashSet<>(); Set<String> names=new HashSet<>();Map<Long,String> seriesNames=new HashMap<>();
        for(int index=2;index<rows.size();index++) {
            List<String> row=rows.get(index);
            if(row.size()==1 && row.get(0).isEmpty())continue;
            if(row.size()!=11)throw new IOException("Wrong number of columns at CSV record "+(index+1)+".");
            long id=id(row.get(1));
            switch(row.get(0)) {
                case "series":
                    only(row,0,1,5);
                    if(!seriesIds.add(id) || row.get(5).trim().isEmpty() || !names.add(sqliteCase(row.get(5))))throw new IOException("Duplicate or empty series in backup.");
                    seriesNames.put(id,row.get(5));snapshot.series.add(row);break;
                case "book":
                    only(row,0,1,2,3,4,5,6,7,8);
                    if(!bookIds.add(id))throw new IOException("Duplicate book ID in backup.");
                    if(!row.get(6).equals("Audiobook")&&!row.get(6).equals("Ebook"))throw new IOException("Unsupported book format in backup.");
                    try {LogRules.validate(row.get(3),row.get(4),row.get(7),row.get(8));} catch(IllegalArgumentException e) {throw new IOException("Invalid book at CSV record "+(index+1)+": "+e.getMessage(),e);}
                    snapshot.books.add(row);break;
                case "book_comment": case "series_comment":
                    only(row,0,1,2,9,10);
                    if(!commentIds.add(id)||row.get(9).trim().isEmpty())throw new IOException("Duplicate or empty comment in backup.");
                    try {LocalDateTime.parse(row.get(10),DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT));}catch(DateTimeParseException e){throw new IOException("Invalid comment timestamp.",e);}
                    snapshot.comments.add(row);break;
                default: throw new IOException("Unknown record type: "+row.get(0));
            }
        }
        for(List<String> b:snapshot.books) {
            if(b.get(2).isEmpty()) {if(!b.get(5).isEmpty())throw new IOException("Book series has no parent ID.");}
            else if(!b.get(5).equals(seriesNames.get(id(b.get(2)))))throw new IOException("Book refers to a missing or mismatched series.");
        }
        for(List<String> note:snapshot.comments) if(!(note.get(0).equals("book_comment")?bookIds:seriesIds).contains(id(note.get(2))))throw new IOException("Comment refers to a missing book or series.");
        return snapshot;
    }
    private static long id(String value) throws IOException {
        try {long n=Long.parseLong(value);if(n<=0||n==Long.MAX_VALUE||!Long.toString(n).equals(value))throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IOException("Backup contains an invalid record ID.");}
    }
    private static String sqliteCase(String value) {
        // SQLite's built-in NOCASE folds ASCII only; match its uniqueness rule exactly.
        StringBuilder folded=new StringBuilder(value.length());
        for(int i=0;i<value.length();i++) {char c=value.charAt(i);folded.append(c>='A'&&c<='Z'?(char)(c+32):c);}
        return folded.toString();
    }
    private static void only(List<String> row,int... fields) throws IOException {
        boolean[] allowed=new boolean[11];for(int field:fields)allowed[field]=true;
        for(int i=0;i<11;i++)if(!allowed[i]&&!row.get(i).isEmpty())throw new IOException("Unexpected data in column "+HEADER.get(i)+".");
    }
    static void restore(LogDatabase helper,Snapshot snapshot) {
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try {
            db.delete("comments",null,null);db.delete("books",null,null);db.delete("series",null,null);
            for(List<String> row:snapshot.series) {ContentValues v=new ContentValues();v.put("id",Long.parseLong(row.get(1)));v.put("name",row.get(5));db.insertOrThrow("series",null,v);}
            for(List<String> row:snapshot.books) {
                ContentValues v=new ContentValues();v.put("id",Long.parseLong(row.get(1)));if(!row.get(2).isEmpty())v.put("series_id",Long.parseLong(row.get(2)));
                v.put("title",row.get(3));v.put("author",row.get(4));v.put("format",row.get(6));v.put("started",row.get(7));v.put("finished",row.get(8));db.insertOrThrow("books",null,v);
            }
            for(List<String> row:snapshot.comments) {
                ContentValues v=new ContentValues();v.put("id",Long.parseLong(row.get(1)));v.put(row.get(0).equals("book_comment")?"book_id":"series_id",Long.parseLong(row.get(2)));v.put("body",row.get(9));v.put("created",row.get(10));db.insertOrThrow("comments",null,v);
            }
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
}

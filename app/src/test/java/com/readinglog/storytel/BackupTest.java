// SPDX-License-Identifier: AGPL-3.0-or-later
package com.readinglog.storytel;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class BackupTest {
    private LogDatabase db;
    @Before public void setup() {RuntimeEnvironment.getApplication().deleteDatabase("reading-log.db");db=new LogDatabase(RuntimeEnvironment.getApplication());}
    @After public void close() {db.close();}
    private byte[] dump() throws Exception {ByteArrayOutputStream out=new ByteArrayOutputStream();CsvBackup.write(db,out);return out.toByteArray();}
    private CsvBackup.Snapshot read(byte[] data) throws Exception {return CsvBackup.read(new ByteArrayInputStream(data));}
    @Test public void restoresAllRelationsCommentsAndExactValues() throws Exception {
        LogDatabase.Book b=new LogDatabase.Book();b.title="The \"Book\", Åsa";b.author="Ö Author";b.series="Series, One";b.started="2025-01-01";b.finished="2025-02-01";b.format="Ebook";db.save(b);
        db.addNote(b.id,false,"A comma, a \"quote\"\nand another line 日本語");db.addNote(db.seriesId(b.series),true,"Series comment");db.seriesId("Empty series");
        byte[] original=dump();CsvBackup.Snapshot snapshot=read(original);assertEquals(1,snapshot.bookCount());assertEquals(2,snapshot.seriesCount());assertEquals(2,snapshot.commentCount());
        db.deleteBook(b.id);CsvBackup.restore(db,snapshot);assertArrayEquals(original,dump());
        assertEquals(b.title,db.books("",0,0).get(0).title);assertEquals(1,db.notes(b.id,false).size());assertEquals(1,db.notes(db.seriesId(b.series),true).size());
    }
    @Test public void invalidBackupNeverMutatesDatabase() throws Exception {
        LogDatabase.Book b=new LogDatabase.Book();b.title="Kept";b.author="Author";db.save(b);byte[] original=dump();
        for(String invalid:Arrays.asList("garbage",new String(original,StandardCharsets.UTF_8).replace("\"meta\",\"1\"","\"meta\",\"999\""),new String(original,StandardCharsets.UTF_8).replace("\"Audiobook\"","\"bad format\""))) {
            try{read(invalid.getBytes(StandardCharsets.UTF_8));fail("Invalid backup accepted");}catch(IOException expected){}assertArrayEquals(original,dump());
        }
    }
    @Test public void failedRestoreRollsBackEverything() throws Exception {
        LogDatabase.Book b=new LogDatabase.Book();b.title="Kept";b.author="Author";db.save(b);db.addNote(b.id,false,"Keep me");byte[] original=dump();
        CsvBackup.Snapshot snapshot=read(original);snapshot.books.add(snapshot.books.get(0));
        try {CsvBackup.restore(db,snapshot);fail("Duplicate ID should fail insertion");}catch(android.database.SQLException expected){}assertArrayEquals(original,dump());
    }
    @Test public void emptyBackupReplacesLibraryAndDanglingParentsAreRejected() throws Exception {
        byte[] empty=dump();LogDatabase.Book b=new LogDatabase.Book();b.title="Book";b.author="Author";b.series="Series";db.save(b);db.addNote(b.id,false,"Comment");
        String text=new String(dump(),StandardCharsets.UTF_8).replace("\"book_comment\",\"1\",\"1\"","\"book_comment\",\"1\",\"999\"");
        try {read(text.getBytes(StandardCharsets.UTF_8));fail("Dangling comment accepted");}catch(IOException expected){}
        CsvBackup.restore(db,read(empty));assertTrue(db.books("",0,0).isEmpty());assertTrue(db.series().isEmpty());
    }
}

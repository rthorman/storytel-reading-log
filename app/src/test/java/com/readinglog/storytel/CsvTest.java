// SPDX-License-Identifier: AGPL-3.0-or-later
package com.readinglog.storytel;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

public class CsvTest {
    @Test public void roundTripQuotesNewlinesUnicodeAndEmptyCells() throws Exception {
        List<String> input=Arrays.asList("A, B","He said \"hello\"","line one\r\nline two","Åsa Ö\n日本語","", "=a1");
        StringWriter writer=new StringWriter();Csv.write(writer,input);
        assertEquals(input,Csv.read(new StringReader("\ufeff"+writer)).get(0));
    }
    @Test public void acceptsCrLfAndFinalRecordWithoutNewline() throws Exception {
        assertEquals(3,Csv.read(new StringReader("a,b\rc,d\ne,f")).size());
        assertEquals(Arrays.asList("a",""),Csv.read(new StringReader("a,")).get(0));
    }
    @Test public void rejectsMalformedQuotesAndOversizedFields() throws Exception {
        for(String input:Arrays.asList("\"unfinished","a\"b,c","\"closed\"junk",String.join("",Collections.nCopies(65537,"x")))) {
            try {Csv.read(new StringReader(input));fail("Expected rejected CSV");}catch(IOException expected) {}
        }
    }
}

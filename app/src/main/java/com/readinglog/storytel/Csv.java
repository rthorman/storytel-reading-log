// SPDX-License-Identifier: AGPL-3.0-or-later
package com.readinglog.storytel;

import java.io.*;
import java.util.*;

/** Strict RFC 4180-style UTF-8 CSV codec. Supports quoted commas, quotes and multiline comments. */
final class Csv {
    private Csv() {}
    static List<List<String>> read(Reader reader) throws IOException {
        PushbackReader in=new PushbackReader(reader,1);
        List<List<String>> rows=new ArrayList<>(); List<String> row=new ArrayList<>(); StringBuilder cell=new StringBuilder();
        boolean quoted=false,closed=false,started=false,first=true; int raw;
        while((raw=in.read())!=-1) {
            char c=(char)raw;
            if(first) { first=false; if(c=='\ufeff') continue; }
            if(quoted) {
                if(c=='"') { int next=in.read(); if(next=='"') cell.append('"'); else {quoted=false;closed=true;if(next!=-1)in.unread(next);} }
                else cell.append(c);
            } else if(c==',' || c=='\n' || c=='\r') {
                row.add(cell.toString()); cell.setLength(0); started=false; closed=false;
                if(row.size()>11) throw new IOException("Too many CSV columns.");
                if(c!=',') { if(c=='\r') {int next=in.read();if(next!='\n'&&next!=-1)in.unread(next);} rows.add(row);row=new ArrayList<>(); }
            } else {
                if(closed) throw new IOException("Unexpected text after a quoted CSV field.");
                if(c=='"') { if(started)throw new IOException("Unexpected quote inside a CSV field.");quoted=true;started=true; }
                else {cell.append(c);started=true;}
            }
            if(cell.length()>65536) throw new IOException("A CSV field exceeds 64 KiB.");
            if(rows.size()>100000) throw new IOException("Backup exceeds 100,000 records.");
        }
        if(quoted) throw new IOException("Unclosed quoted CSV field.");
        if(started||closed||!row.isEmpty()||cell.length()>0) {row.add(cell.toString());rows.add(row);}
        return rows;
    }
    static void write(Writer out,List<String> row) throws IOException {
        for(int i=0;i<row.size();i++) { if(row.get(i).length()>65536)throw new IOException("A CSV field exceeds 64 KiB.");if(i>0)out.write(',');out.write('"');out.write(row.get(i).replace("\"","\"\""));out.write('"'); }
        out.write("\r\n");
    }
}

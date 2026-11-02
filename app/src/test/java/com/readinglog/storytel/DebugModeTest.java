// SPDX-License-Identifier: AGPL-3.0-or-later
package com.readinglog.storytel;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import android.content.Context;
import java.io.*;
import java.nio.file.Files;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class DebugModeTest {
    private Context context;
    private File log;
    @Before public void setup() {context=RuntimeEnvironment.getApplication();context.getSharedPreferences("diagnostics",0).edit().clear().commit();DebugLog.clear(context);log=new File(context.getFilesDir(),"storytel-debug.jsonl");}
    @Test public void loggingIsOffByDefaultAndOnlyRecordsWhileEnabled() throws Exception {
        assertFalse(DebugLog.enabled(context));DebugLog.event(context,"must_not_exist",DebugLog.data("title","Private book"));assertFalse(log.exists());
        DebugLog.setEnabled(context,true);DebugLog.event(context,"test_event",DebugLog.data("value","A book"));assertTrue(log.exists());
        String enabled=new String(Files.readAllBytes(log.toPath()),java.nio.charset.StandardCharsets.UTF_8);assertTrue(enabled.contains("debug_mode_enabled"));assertTrue(enabled.contains("test_event"));
        DebugLog.setEnabled(context,false);long length=log.length();DebugLog.event(context,"must_not_exist",DebugLog.data());assertEquals(length,log.length());assertFalse(DebugLog.enabled(context));
        ByteArrayOutputStream export=new ByteArrayOutputStream();DebugLog.export(context,export);assertTrue(export.toString("UTF-8").contains("test_event"));
        DebugLog.clear(context);assertFalse(log.exists());
    }
}

// SPDX-License-Identifier: AGPL-3.0-or-later
package com.readinglog.storytel;

import android.content.Context;
import android.os.Bundle;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Two rotating private files, at most about 2 MiB total. Contains Storytel reading metadata. */
final class DebugLog {
    static boolean enabled(Context context) {return context.getSharedPreferences("diagnostics",Context.MODE_PRIVATE).getBoolean("enabled",false);}
    static void setEnabled(Context context,boolean enabled) {
        if(enabled) {
            context.getSharedPreferences("diagnostics",Context.MODE_PRIVATE).edit().putBoolean("enabled",true).apply();
            event(context,"debug_mode_enabled",data());
        } else {
            event(context,"debug_mode_disabled",data());
            context.getSharedPreferences("diagnostics",Context.MODE_PRIVATE).edit().putBoolean("enabled",false).apply();
        }
    }
    static String version(Context context) {
        try {return context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;}
        catch(Exception ignored) {return "unknown";}
    }
    private static final long LIMIT=1024*1024;
    static synchronized void event(Context context,String event,JSONObject details) {
        if(!enabled(context)) return;
        try {
            File file=new File(context.getFilesDir(),"storytel-debug.jsonl");
            if(file.length()>LIMIT) {
                File old=new File(context.getFilesDir(),"storytel-debug.previous.jsonl");
                if(old.exists() && !old.delete()) return;
                if(!file.renameTo(old)) return;
            }
            JSONObject row=new JSONObject(); row.put("time",Instant.now().toString()); row.put("event",event); row.put("details",details);
            try(FileOutputStream out=new FileOutputStream(file,true)) { out.write((row+"\n").getBytes(StandardCharsets.UTF_8)); }
        } catch(Exception e) { android.util.Log.e("StorytelReadingLog","Could not write diagnostic event",e); }
    }
    static JSONObject data(Object... pairs) {
        JSONObject o=new JSONObject();
        try { for(int i=0;i<pairs.length;i+=2) o.put(String.valueOf(pairs[i]),pairs[i+1]); } catch(Exception ignored) {}
        return o;
    }
    static JSONObject extras(Bundle bundle) {
        JSONObject o=new JSONObject();
        for(String key:bundle.keySet()) {
            try {
                Object value=bundle.get(key);
                // Do not serialize tokens, images, pending intents or arbitrary parcelables.
                String text=value instanceof CharSequence || value instanceof Number || value instanceof Boolean ? String.valueOf(value) : value==null?"null":"["+value.getClass().getSimpleName()+"]";
                if(text.length()>4096) text=text.substring(0,4096)+" [truncated]";
                o.put(key,text);
            } catch(Exception ignored) {}
        }
        return o;
    }
    static synchronized void export(Context context,OutputStream out) throws IOException {
        out.write((data("event","export_header","exported_at",Instant.now(),"app_version",version(context),"android_api",android.os.Build.VERSION.SDK_INT,"device",android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL)+"\n").getBytes(StandardCharsets.UTF_8));
        for(String name:new String[]{"storytel-debug.previous.jsonl","storytel-debug.jsonl"}) {
            File f=new File(context.getFilesDir(),name);
            if(f.exists()) try(InputStream in=new FileInputStream(f)) { byte[] buffer=new byte[8192]; int n; while((n=in.read(buffer))!=-1) out.write(buffer,0,n); }
        }
    }
    static synchronized void clear(Context context) {
        for(String name:new String[]{"storytel-debug.previous.jsonl","storytel-debug.jsonl"}) new File(context.getFilesDir(),name).delete();
    }
}

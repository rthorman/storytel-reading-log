package com.readinglog.storytel;

import android.app.Application;

public final class LogApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        DebugLog.event(this,"app_process_started",DebugLog.data("version","0.1.0","android_api",android.os.Build.VERSION.SDK_INT));
        Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)-> {
            DebugLog.event(this,"uncaught_exception",DebugLog.data("thread",thread.getName(),"error",android.util.Log.getStackTraceString(error)));
            if(previous!=null) previous.uncaughtException(thread,error);
            else android.os.Process.killProcess(android.os.Process.myPid());
        });
    }
}

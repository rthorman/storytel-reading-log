package com.readinglog.storytel;

import android.app.Notification;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.time.LocalDate;

/** Only Storytel notifications are inspected. Notification contents never leave this device. */
public final class StorytelListener extends NotificationListenerService {
    @Override public void onListenerConnected() {
        DebugLog.event(this,"listener_connected",DebugLog.data());
        StatusBarNotification[] active = getActiveNotifications();
        if(active != null) for(StatusBarNotification n:active) capture(n);
    }
    @Override public void onNotificationPosted(StatusBarNotification n) { capture(n); }
    @Override public void onListenerDisconnected() { DebugLog.event(this,"listener_disconnected",DebugLog.data()); }
    @Override public void onNotificationRemoved(StatusBarNotification n) {
        if(isStorytel(n.getPackageName())) DebugLog.event(this,"notification_removed",DebugLog.data("package",n.getPackageName(),"id",n.getId()));
    }
    private boolean isStorytel(String pkg) { return pkg.equals("grit.storytel.app") || pkg.equals("com.storytel.app"); }
    private void capture(StatusBarNotification item) {
        String pkg=item.getPackageName();
        if(!isStorytel(pkg)) return;
        try { inspect(item); } catch(Exception e) {
            DebugLog.event(this,"capture_error",DebugLog.data("package",pkg,"error",android.util.Log.getStackTraceString(e)));
        }
    }
    private void inspect(StatusBarNotification item) {
        Notification n=item.getNotification();
        DebugLog.event(this,"notification_posted",DebugLog.data("package",item.getPackageName(),"id",item.getId(),"posted_at_ms",item.getPostTime(),"ongoing",item.isOngoing(),"extras",DebugLog.extras(n.extras)));
        String title="", author="";
        MediaSession.Token token=n.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
        if(token!=null) {
            MediaController controller=new MediaController(this,token);
            PlaybackState state=controller.getPlaybackState();
            MediaMetadata meta=controller.getMetadata();
            DebugLog.event(this,"media_metadata",DebugLog.data("state",state==null?"unavailable":state.getState(),"position_ms",state==null?-1:state.getPosition(),"title",meta==null?null:meta.getString(MediaMetadata.METADATA_KEY_TITLE),"author",meta==null?null:meta.getString(MediaMetadata.METADATA_KEY_AUTHOR),"artist",meta==null?null:meta.getString(MediaMetadata.METADATA_KEY_ARTIST),"album",meta==null?null:meta.getString(MediaMetadata.METADATA_KEY_ALBUM),"duration_ms",meta==null?-1:meta.getLong(MediaMetadata.METADATA_KEY_DURATION)));
            if(state!=null && state.getState()!=PlaybackState.STATE_PLAYING) { DebugLog.event(this,"capture_skipped",DebugLog.data("reason","not playing")); return; }
            if(meta!=null) {
                title=clean(meta.getString(MediaMetadata.METADATA_KEY_TITLE));
                author=clean(meta.getString(MediaMetadata.METADATA_KEY_AUTHOR));
                if(author.isEmpty()) author=clean(meta.getString(MediaMetadata.METADATA_KEY_ARTIST));
            }
        } else if(!item.isOngoing()) { DebugLog.event(this,"capture_skipped",DebugLog.data("reason","no media token and not ongoing")); return; }
        if(title.isEmpty()) title=clean(n.extras.getCharSequence(Notification.EXTRA_TITLE));
        if(title.isEmpty() || title.equalsIgnoreCase("Storytel")) { DebugLog.event(this,"capture_skipped",DebugLog.data("reason","missing book title")); return; }
        if(author.isEmpty()) author=clean(n.extras.getCharSequence(Notification.EXTRA_TEXT));
        if(author.isEmpty()) author="Unknown author";
        try(LogDatabase db=new LogDatabase(this)) {
            LogDatabase.Book found=null;
            for(LogDatabase.Book b:db.books("",0,0)) {
                if(b.title.equalsIgnoreCase(title) && (b.author.equalsIgnoreCase(author) || b.author.equals("Unknown author"))) { found=b; break; }
            }
            boolean created=found==null;
            if(found==null) { found=new LogDatabase.Book(); found.title=title; found.author=author; }
            boolean started=found.started.isEmpty();
            if(started) { found.started=LocalDate.now().toString(); db.save(found); }
            DebugLog.event(this,"capture_result",DebugLog.data("book_id",found.id,"title",title,"author",author,"created",created,"start_date_recorded",started));
            getSharedPreferences("automation",MODE_PRIVATE).edit().putString("last_capture",title + " · " + LocalDate.now()).apply();
        }
    }
    private static String clean(CharSequence value) { return value==null?"":value.toString().trim(); }
}

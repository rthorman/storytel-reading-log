// SPDX-License-Identifier: AGPL-3.0-or-later
package com.readinglog.storytel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.net.Uri;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private LogDatabase db;
    private Ui ui;
    private static final ExecutorService backupExecutor=Executors.newSingleThreadExecutor();
    private static final java.util.concurrent.atomic.AtomicBoolean TRANSFER_GATE=new java.util.concurrent.atomic.AtomicBoolean();
    private boolean ownsTransferGate, backupWorkerRunning;
    private final android.os.Handler backupHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable backupAvailability=this::updateBackupAvailability;
    private boolean backupPending;
    private Button backupExportButton, backupImportButton;
    private ProgressDialog backupProgress;
    private AlertDialog backupPreview;
    private LinearLayout root, body, rows, chips, statusCard;
    private TextView count;
    private ScrollView listScroll;
    private String query="", seriesQuery="", page="main";
    private int tab, sort, filter, libraryScroll;
    private LogDatabase.Book selected, draft;
    private EditText titleInput, authorInput, seriesInput, startInput, finishInput, commentInput;
    private TextView formError;
    private boolean ebook, restoredDirty;
    private long noteId;
    private boolean seriesNotes;
    private String noteName="", notesReturn="main", editorReturn="main";
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener captureChanged=(prefs,key)-> { if("last_capture".equals(key)&&"main".equals(page)&&tab==0&&rows!=null) refreshLibrary(); };
    private static final String[] SORTS={"Title A–Z","Author A–Z","Series A–Z","Recently started"};
    private static final String[] FILTERS={"All books","In progress","Finished","To read"};
    private int dp(int n) { return ui.dp(n); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); backupHandler.postDelayed(backupAvailability,250); db=new LogDatabase(this); ui=new Ui(this); getSharedPreferences("automation",MODE_PRIVATE).registerOnSharedPreferenceChangeListener(captureChanged);
        if(state!=null) {
            tab=state.getInt("tab"); sort=state.getInt("sort"); filter=state.getInt("filter"); query=state.getString("query",""); seriesQuery=state.getString("series_query",""); libraryScroll=state.getInt("library_scroll");
            selected=bookById(state.getLong("selected"));
            String restorePage=state.getString("page","main");
            if(restorePage.equals("edit")) {
                draft=new LogDatabase.Book(); draft.id=state.getLong("draft_id"); draft.title=state.getString("draft_title",""); draft.author=state.getString("draft_author",""); draft.series=state.getString("draft_series",""); draft.started=state.getString("draft_start",""); draft.finished=state.getString("draft_finish",""); draft.format=state.getString("draft_format","Audiobook");
                editorReturn=state.getString("editor_return","main"); restoredDirty=state.getBoolean("draft_changed"); showEditor(draft); return;
            }
            if(restorePage.equals("seriesDetail")) { showSeries(state.getString("current_series","")); return; }
            if(restorePage.equals("details") && selected!=null) { showDetails(selected); return; }
            if(restorePage.equals("notes")) {
                noteId=state.getLong("note_id"); seriesNotes=state.getBoolean("series_notes"); noteName=state.getString("note_name",""); notesReturn=state.getString("notes_return","main");
                showNotes(noteId,seriesNotes,noteName,notesReturn); commentInput.setText(state.getString("comment_draft","")); return;
            }
        }
        showMain();
    }
    @Override protected void onResume() { super.onResume(); if(page.equals("main")) { if(tab==2) showMain(); else if(rows!=null) refreshLibrary(); } }
    @Override protected void onDestroy() { if(backupProgress!=null) backupProgress.dismiss(); if(backupPreview!=null) backupPreview.dismiss(); backupHandler.removeCallbacks(backupAvailability); if(!backupWorkerRunning) setBackupPending(false); db.close(); getSharedPreferences("automation",MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(captureChanged); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putInt("tab",tab); out.putInt("sort",sort); out.putInt("filter",filter); out.putString("query",query); out.putString("series_query",seriesQuery); out.putString("page",page); out.putInt("library_scroll",listScroll!=null&&page.equals("main")&&tab==0?listScroll.getScrollY():libraryScroll); out.putLong("selected",selected==null?0:selected.id); out.putString("current_series",currentSeries);
        if(page.equals("edit")) { LogDatabase.Book b=readDraft(); out.putLong("draft_id",b.id); out.putString("draft_title",b.title); out.putString("draft_author",b.author); out.putString("draft_series",b.series); out.putString("draft_start",b.started); out.putString("draft_finish",b.finished); out.putString("draft_format",b.format); out.putString("editor_return",editorReturn); out.putBoolean("draft_changed",draftChanged()); }
        if(page.equals("notes")) { out.putLong("note_id",noteId); out.putBoolean("series_notes",seriesNotes); out.putString("note_name",noteName); out.putString("notes_return",notesReturn); out.putString("comment_draft",commentInput.getText().toString()); }
    }
    private LogDatabase.Book bookById(long id) { if(id==0) return null; for(LogDatabase.Book b:db.books("",0,0)) if(b.id==id) return b; return null; }
    private void scaffold(String name,boolean back) {
        root=ui.column(); root.setBackgroundColor(Ui.PAPER);
        root.setOnApplyWindowInsetsListener((v,i)-> { v.setPadding(0,i.getSystemWindowInsetTop(),0,i.getSystemWindowInsetBottom()); return i; });
        setContentView(root);
        LinearLayout top=ui.row(); top.setPadding(dp(16),dp(4),dp(16),dp(4));
        if(back) top.addView(ui.textButton("‹  Back",this::navigateBack));
        else { TextView mark=ui.heading("S",25); mark.setTextColor(Ui.BRASS); mark.setGravity(Gravity.CENTER); top.addView(mark,new LinearLayout.LayoutParams(dp(40),dp(48))); }
        TextView title=ui.eyebrow(name); if(back) title.setGravity(Gravity.END); ui.weight(top,title); root.addView(top); root.addView(ui.rule());
        body=ui.column(); root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    }
    private LinearLayout scrollPage() {
        ScrollView scroll=new ScrollView(this); scroll.setClipToPadding(false); scroll.setFillViewport(true);
        LinearLayout content=ui.column(); content.setPadding(dp(22),dp(24),dp(22),dp(28)); scroll.addView(content); body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); return content;
    }
    private void footer(String label,Runnable action) {
        LinearLayout footer=ui.column(); footer.setPadding(dp(22),dp(12),dp(22),dp(12)); footer.setBackgroundColor(Ui.PAPER); footer.addView(ui.button(label,true,action)); root.addView(ui.rule()); root.addView(footer);
    }
    private void showMain() {
        page="main"; rows=null; scaffold("Storytel Reading Log",false);
        if(tab==2) renderSettings(); else renderLibrary();
        LinearLayout nav=ui.row(); nav.setPadding(dp(10),dp(6),dp(10),dp(6)); nav.setBackgroundColor(Ui.SURFACE);
        String[] names={"Library","Series","Settings"};
        for(int i=0;i<names.length;i++) { final int destination=i; Button b=ui.textButton(names[i],()-> { hideKeyboard(); if(listScroll!=null&&tab==0) libraryScroll=listScroll.getScrollY(); tab=destination; showMain(); }); b.setTextColor(tab==i?Ui.INK:Ui.MUTED); if(tab==i) b.setBackground(ui.shape(Ui.TINT,0,9)); b.setSelected(tab==i); nav.addView(b,new LinearLayout.LayoutParams(0,-2,1)); }
        root.addView(ui.rule()); root.addView(nav);
    }
    private void renderLibrary() {
        listScroll=new ScrollView(this); listScroll.setFillViewport(true); listScroll.setClipToPadding(false);
        LinearLayout libraryContent=ui.column(); listScroll.addView(libraryContent);
        body.addView(listScroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout heading=ui.row(); heading.setPadding(dp(22),dp(20),dp(22),dp(14));
        LinearLayout title=ui.column(); title.addView(ui.eyebrow("A personal collection")); ui.gap(title,7); title.addView(ui.heading(tab==0?"Your library":"Collected series",32)); ui.weight(heading,title);
        heading.addView(ui.textButton("+ Add",()->beginEdit(new LogDatabase.Book(),"main"))); libraryContent.addView(heading);
        LinearLayout controls=ui.column(); controls.setPadding(dp(22),0,dp(22),0);
        if(tab==0) { statusCard=ui.column(); controls.addView(statusCard); updateStatus(); ui.gap(controls,14); }
        EditText search=ui.input(tab==0?"Search title, author or series":"Search series",tab==0?query:seriesQuery,false); search.setContentDescription(tab==0?"Search title, author or series":"Search series"); search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH); controls.addView(search);
        if(tab==0) {
            HorizontalScrollView chipScroll=new HorizontalScrollView(this); chipScroll.setHorizontalScrollBarEnabled(false); chips=ui.row(); chips.setPadding(0,dp(12),0,dp(12)); chipScroll.addView(chips); controls.addView(chipScroll); renderChips();
        } else ui.gap(controls,12);
        LinearLayout order=ui.row(); count=ui.text("",12,Ui.MUTED); ui.weight(order,count);
        if(tab==0) order.addView(ui.textButton("Sort: "+SORTS[sort]+" ▾",()->new AlertDialog.Builder(this).setTitle("Sort your library").setSingleChoiceItems(SORTS,sort,(d,which)-> { sort=which; d.dismiss(); ((Button)order.getChildAt(1)).setText("Sort: "+SORTS[sort]+" ▾"); refreshLibrary(); }).setNegativeButton("Cancel",null).show()));
        controls.addView(order); libraryContent.addView(controls);
        rows=ui.column(); rows.setPadding(dp(22),dp(4),dp(22),dp(20)); libraryContent.addView(rows);
        search.addTextChangedListener(watcher(()-> { if(tab==0) query=search.getText().toString(); else seriesQuery=search.getText().toString(); refreshLibrary(); }));
        refreshLibrary(); if(tab==0) listScroll.post(()->listScroll.scrollTo(0,libraryScroll));
    }
    private void updateStatus() {
        if(statusCard==null) return; statusCard.removeAllViews();
        boolean enabled=notificationAccess(); String last=getSharedPreferences("automation",MODE_PRIVATE).getString("last_capture","");
        LinearLayout row=ui.row(); row.setPadding(dp(12),dp(10),dp(12),dp(10));
        ui.clickable(row,Ui.TINT,0,9,()-> { tab=2; showMain(); });
        LinearLayout copy=ui.column(); TextView state=ui.text(enabled?"Notification access enabled":"Automatic logging is off",13,Ui.INK); state.setTypeface(null,Typeface.BOLD); copy.addView(state); ui.gap(copy,4);
        copy.addView(ui.text(last.isEmpty()?(enabled?"Play a book in Storytel to begin.":"Enable listening capture in Settings."):"Last detected: "+last,12,Ui.MUTED));
        ui.weight(row,copy); TextView arrow=ui.text("›",24,Ui.INK); arrow.setPadding(dp(10),0,0,0); row.addView(arrow); row.setMinimumHeight(dp(56)); statusCard.addView(row);
    }
    private void renderChips() {
        chips.removeAllViews();
        for(int i=0;i<FILTERS.length;i++) { final int choice=i; Button chip=ui.button(FILTERS[i],false,()-> { filter=choice; renderChips(); refreshLibrary(); }); chip.setSelected(filter==i); chip.setTextColor(filter==i?Ui.SURFACE:Ui.INK); ui.clickable(chip,filter==i?Ui.INK:Ui.SURFACE,filter==i?0:Ui.LINE,24,()->{filter=choice;renderChips();refreshLibrary();}); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2); p.setMarginEnd(dp(8)); chips.addView(chip,p); }
    }
    private void refreshLibrary() {
        if(!page.equals("main")||rows==null||tab==2) return;
        rows.removeAllViews(); if(tab==0) updateStatus();
        if(tab==1) {
            int visible=0; List<LogDatabase.Book> books=db.books("",0,0);
            for(String name:db.series()) if(name.toLowerCase(Locale.ROOT).contains(seriesQuery.toLowerCase(Locale.ROOT))) {
                visible++; int total=0,finished=0; for(LogDatabase.Book b:books) if(b.series.equalsIgnoreCase(name)) { total++; if(!b.finished.isEmpty()) finished++; }
                LinearLayout card=ui.card(); card.addView(ui.eyebrow("Series")); ui.gap(card,8); card.addView(ui.heading(name,24)); ui.gap(card,8); card.addView(ui.text(total+" books · "+finished+" finished",13,Ui.MUTED)); ui.gap(card,8); card.addView(ui.text("View books & series comments  ›",13,Ui.INK));
                ui.clickable(card,Ui.SURFACE,Ui.LINE,12,()->showSeries(name)); ui.add(rows,card,12);
            }
            count.setText(visible+(visible==1?" series":" series"));
            if(visible==0) empty(!seriesQuery.isEmpty()?"No matching series":"Stories belong together",!seriesQuery.isEmpty()?"Try another series name.":"Assign a series when editing a book. Its books and shared comments will live here.",false);
            return;
        }
        List<LogDatabase.Book> books=db.books(query,sort,filter); count.setText(books.size()+(books.size()==1?" book":" books"));
        if(books.isEmpty()) {
            boolean constrained=!query.trim().isEmpty()||filter!=0;
            empty(constrained?"No books found":"A library of your own",constrained?"Try a different search or reading status.":"A place for every story you hear, every book you finish, and every thought you keep.",!constrained);
            if(constrained) rows.addView(ui.button("Clear search & filters",false,()-> { query="";filter=0;libraryScroll=0;showMain(); }));
        }
        for(LogDatabase.Book b:books) ui.add(rows,bookCard(b),12);
    }
    private View bookCard(LogDatabase.Book b) {
        LinearLayout card=ui.row(); card.setPadding(dp(14),dp(14),dp(14),dp(14)); card.setGravity(Gravity.TOP);
        card.addView(ui.cover(b.title,false),new LinearLayout.LayoutParams(dp(66),dp(96)));
        LinearLayout info=ui.column(); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.setMarginStart(dp(16)); card.addView(info,p);
        info.addView(ui.heading(b.title,21)); ui.gap(info,5); info.addView(ui.text(b.author,14,Ui.MUTED));
        if(!b.series.isEmpty()) { ui.gap(info,7); info.addView(ui.text(b.series,12,Ui.BRASS_TEXT)); }
        ui.gap(info,9); TextView badge=ui.badge(LogRules.status(b.started,b.finished)+" · "+b.format); info.addView(badge,new LinearLayout.LayoutParams(-2,-2));
        if(!b.started.isEmpty()) { ui.gap(info,7); info.addView(ui.text(!b.finished.isEmpty()?"Finished "+date(b.finished):"Started "+date(b.started),12,Ui.MUTED)); }
        ui.clickable(card,Ui.SURFACE,Ui.LINE,12,()-> { libraryScroll=listScroll.getScrollY(); hideKeyboard(); showDetails(b); });
        card.setContentDescription(b.title+", "+b.author+", "+LogRules.status(b.started,b.finished)+". Open book details"); return card;
    }
    private void empty(String title,String description,boolean illustration) {
        LinearLayout card=ui.column(); card.setGravity(Gravity.CENTER_HORIZONTAL); card.setPadding(dp(12),dp(20),dp(12),dp(22));
        if(illustration) { ImageView art=new ImageView(this); art.setImageResource(R.drawable.library_engraving); art.setScaleType(ImageView.ScaleType.FIT_CENTER); art.setAdjustViewBounds(true); art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); card.addView(art,new LinearLayout.LayoutParams(-1,dp(150))); ui.gap(card,18); }
        TextView heading=ui.heading(title,28); heading.setGravity(Gravity.CENTER); card.addView(heading); ui.gap(card,10); TextView caption=ui.text(description,15,Ui.MUTED); caption.setGravity(Gravity.CENTER); card.addView(caption); ui.gap(card,20);
        if(illustration) { ui.add(card,ui.button("Set up automatic logging",true,()->{tab=2;showMain();}),8); card.addView(ui.textButton("Add a book yourself",()->beginEdit(new LogDatabase.Book(),"main"))); }
        rows.addView(card);
    }
    private void showDetails(LogDatabase.Book b) {
        selected=bookById(b.id); if(selected==null) {showMain();return;} b=selected; final LogDatabase.Book book=b;
        page="details"; scaffold("The book record",true); LinearLayout content=scrollPage();
        LinearLayout hero=ui.row(); hero.setGravity(Gravity.TOP); hero.addView(ui.cover(b.title,true),new LinearLayout.LayoutParams(dp(98),dp(142)));
        LinearLayout words=ui.column(); LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.setMarginStart(dp(18));hero.addView(words,wp);
        words.addView(ui.eyebrow(b.format));ui.gap(words,9);words.addView(ui.heading(b.title,30));ui.gap(words,10);words.addView(ui.text(b.author,16,Ui.MUTED));ui.add(content,hero,24);
        content.addView(ui.badge(LogRules.status(b.started,b.finished)),new LinearLayout.LayoutParams(-2,-2));ui.gap(content,20);
        LinearLayout dates=ui.card(); metadata(dates,"Started",date(b.started));metadata(dates,"Finished",date(b.finished));ui.add(content,dates,18);
        if(!b.series.isEmpty()) { ui.add(content,ui.button("Series: "+b.series+"  ›",false,()->showSeries(book.series)),12); }
        ui.add(content,ui.button("Edit book & dates",true,()->beginEdit(book,"details")),10);
        ui.add(content,ui.button("Book comments · "+db.notes(b.id,false).size(),false,()->showNotes(book.id,false,book.title,"details")),20);
        TextView hint=ui.text("Start dates can be captured from Storytel. Finish dates and series are yours to complete.",13,Ui.MUTED);ui.add(content,hint,28);
        Button delete=ui.textButton("Delete book",()->new AlertDialog.Builder(this).setTitle("Delete this book?").setMessage("The book and its comments will be deleted. Series comments are kept.").setPositiveButton("Delete",(d,w)->{db.deleteBook(book.id);selected=null;showMain();}).setNegativeButton("Cancel",null).show());delete.setTextColor(Ui.ERROR);content.addView(delete);
    }
    private void metadata(LinearLayout parent,String label,String value) {
        LinearLayout row=ui.row();row.setPadding(0,dp(6),0,dp(6));TextView name=ui.text(label,14,Ui.MUTED);ui.weight(row,name);TextView text=ui.text(value,14,Ui.INK);text.setGravity(Gravity.END);ui.weight(row,text);parent.addView(row);
    }
    private String currentSeries="";
    private void showSeries(String name) {
        currentSeries=name; page="seriesDetail"; scaffold("The series shelf",true);LinearLayout content=scrollPage();content.addView(ui.eyebrow("Collected stories"));ui.gap(content,10);ui.add(content,ui.heading(name,34),18);
        long id=db.seriesId(name);ui.add(content,ui.button("Series comments · "+db.notes(id,true).size(),true,()->showNotes(id,true,name,"seriesDetail")),22);
        List<LogDatabase.Book> books=db.books("",0,0);int total=0;
        for(LogDatabase.Book b:books) if(b.series.equalsIgnoreCase(name)) { total++;LinearLayout card=ui.card();card.addView(ui.heading(b.title,22));ui.gap(card,6);card.addView(ui.text(b.author+" · "+LogRules.status(b.started,b.finished),14,Ui.MUTED));ui.clickable(card,Ui.SURFACE,Ui.LINE,12,()->showDetails(b));ui.add(content,card,12); }
        if(total==0) content.addView(ui.text("No books currently belong to this series. Its comments are kept here.",15,Ui.MUTED));
    }
    private void beginEdit(LogDatabase.Book b,String from) { editorReturn=from;restoredDirty=false;showEditor(b); }
    private void showEditor(LogDatabase.Book b) {
        draft=b; ebook=b.format.equals("Ebook"); page="edit";scaffold(b.id==0?"A new book":"Edit the record",true);LinearLayout content=scrollPage();ui.add(content,ui.heading(b.id==0?"Add to your library":"A few particulars",32),8);ui.add(content,ui.text("Keep the details of this story together.",15,Ui.MUTED),24);
        titleInput=field(content,"Title","Book title",b.title);authorInput=field(content,"Author","Author name",b.author);
        TextView seriesLabel=ui.text("Series · optional",13,Ui.INK);ui.add(content,seriesLabel,7);AutoCompleteTextView series=new AutoCompleteTextView(this);ui.inputStyle(series);series.setSingleLine(true);series.setHint("A series or collection");series.setText(b.series);series.setThreshold(1);series.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,db.series()));series.setId(View.generateViewId());seriesLabel.setLabelFor(series.getId());seriesInput=series;ui.add(content,series,20);
        ui.add(content,ui.text("Format",13,Ui.INK),7);LinearLayout formats=ui.row();Button audio=ui.button("Audiobook",false,()->{}),read=ui.button("Ebook",false,()->{});
        Runnable updateFormat=()->{audio.setBackground(ui.shape(ebook?Ui.SURFACE:Ui.INK,ebook?Ui.LINE:0,9));audio.setTextColor(ebook?Ui.INK:Ui.SURFACE);audio.setSelected(!ebook);read.setBackground(ui.shape(ebook?Ui.INK:Ui.SURFACE,ebook?0:Ui.LINE,9));read.setTextColor(ebook?Ui.SURFACE:Ui.INK);read.setSelected(ebook);};
        audio.setOnClickListener(v->{ebook=false;updateFormat.run();});read.setOnClickListener(v->{ebook=true;updateFormat.run();});formats.addView(audio,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,-2,1);fp.setMarginStart(dp(10));formats.addView(read,fp);updateFormat.run();ui.add(content,formats,24);
        ui.add(content,ui.eyebrow("The reading journey"),12);startInput=dateField(content,"Started",b.started);finishInput=dateField(content,"Finished",b.finished);ui.add(content,ui.text("Leave a date blank if you have not started or finished. Dates use YYYY-MM-DD.",13,Ui.MUTED),12);
        formError=ui.text("",14,Ui.ERROR);formError.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);content.addView(formError);footer("Save book",()->{LogDatabase.Book edited=readDraft();try {db.save(edited);selected=edited;hideKeyboard();if(editorReturn.equals("details"))showDetails(edited);else showMain();}catch(IllegalArgumentException e){formError.setText(e.getMessage());}catch(Exception e){logError("save_book_error",e);formError.setText("The book could not be saved. Please try again.");}});
    }
    private EditText field(LinearLayout parent,String name,String hint,String value) {
        TextView label=ui.text(name,13,Ui.INK);ui.add(parent,label,7);EditText input=ui.input(hint,value,false);input.setId(View.generateViewId());label.setLabelFor(input.getId());ui.add(parent,input,20);return input;
    }
    private EditText dateField(LinearLayout parent,String name,String value) {
        TextView label=ui.text(name,13,Ui.INK);ui.add(parent,label,7);LinearLayout row=ui.row();EditText input=ui.input("YYYY-MM-DD",value,false);input.setId(View.generateViewId());label.setLabelFor(input.getId());input.setInputType(android.text.InputType.TYPE_CLASS_DATETIME|android.text.InputType.TYPE_DATETIME_VARIATION_DATE);row.addView(input,new LinearLayout.LayoutParams(0,-2,1));
        Button choose=ui.textButton("Choose",()-> {LocalDate date=LocalDate.now();try{date=LocalDate.parse(input.getText().toString());}catch(Exception ignored){}DatePickerDialog picker=new DatePickerDialog(this,(v,y,m,d)->input.setText(LocalDate.of(y,m+1,d).toString()),date.getYear(),date.getMonthValue()-1,date.getDayOfMonth());picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();});choose.setContentDescription("Choose "+name.toLowerCase(Locale.ROOT)+" date");row.addView(choose);ui.add(parent,row,18);return input;
    }
    private LogDatabase.Book readDraft() {LogDatabase.Book b=new LogDatabase.Book();b.id=draft.id;b.title=titleInput.getText().toString();b.author=authorInput.getText().toString();b.series=seriesInput.getText().toString();b.format=ebook?"Ebook":"Audiobook";b.started=startInput.getText().toString().trim();b.finished=finishInput.getText().toString().trim();return b;}
    private boolean draftChanged() {LogDatabase.Book b=readDraft();return restoredDirty||!b.title.equals(draft.title)||!b.author.equals(draft.author)||!b.series.equals(draft.series)||!b.format.equals(draft.format)||!b.started.equals(draft.started)||!b.finished.equals(draft.finished);}
    private void showNotes(long id,boolean forSeries,String name,String from) {
        noteId=id;seriesNotes=forSeries;noteName=name;notesReturn=from;page="notes";scaffold(forSeries?"Series marginalia":"Book marginalia",true);LinearLayout content=scrollPage();content.addView(ui.eyebrow(forSeries?"Notes on the collection":"Thoughts worth keeping"));ui.gap(content,10);ui.add(content,ui.heading(name,30),22);
        TextView label=ui.text("A new comment",13,Ui.INK);ui.add(content,label,7);commentInput=ui.input("What would you like to remember?","",true);commentInput.setId(View.generateViewId());label.setLabelFor(commentInput.getId());ui.add(content,commentInput,12);
        LinearLayout comments=ui.column();ui.add(content,ui.button("Add comment",true,()->{try{db.addNote(id,forSeries,commentInput.getText().toString());commentInput.setText("");commentInput.setError(null);hideKeyboard();renderNotes(comments,id,forSeries);}catch(IllegalArgumentException e){commentInput.setError(e.getMessage());}catch(Exception e){logError("save_comment_error",e);toast("Comment could not be saved.");}}),26);content.addView(comments);renderNotes(comments,id,forSeries);
    }
    private void renderNotes(LinearLayout list,long id,boolean forSeries) {
        list.removeAllViews();List<LogDatabase.Note> notes=db.notes(id,forSeries);ui.add(list,ui.eyebrow(notes.size()+" saved "+(notes.size()==1?"comment":"comments")),14);
        if(notes.isEmpty())list.addView(ui.text("This page is waiting for your first thought.",16,Ui.MUTED));
        for(LogDatabase.Note n:notes){LinearLayout card=ui.card();ui.add(card,ui.text(n.created+" UTC",11,Ui.MUTED),10);TextView text=ui.text(n.body,16,Ui.INK);text.setTextIsSelectable(true);card.addView(text);ui.gap(card,8);Button delete=ui.textButton("Delete comment",()->new AlertDialog.Builder(this).setTitle("Delete this comment?").setPositiveButton("Delete",(d,w)->{db.deleteNote(n.id);renderNotes(list,id,forSeries);}).setNegativeButton("Cancel",null).show());delete.setTextColor(Ui.ERROR);card.addView(delete);ui.add(list,card,12);}
    }
    private void renderSettings() {
        LinearLayout content=scrollPage();content.addView(ui.eyebrow("The keeper’s desk"));ui.gap(content,9);ui.add(content,ui.heading("Settings",34),24);
        LinearLayout capture=ui.card();capture.addView(ui.eyebrow("Automatic listening log"));ui.gap(capture,10);capture.addView(ui.heading(notificationAccess()?"Ready to listen":"Connect your listening",26));ui.gap(capture,10);capture.addView(ui.text(notificationAccess()?"Notification access is enabled. Playback detection depends on Storytel’s notification metadata.":"Give this app notification access, then play an audiobook in Storytel.",15,Ui.MUTED));ui.gap(capture,16);capture.addView(ui.button("Open notification access",true,()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));ui.gap(capture,14);
        capture.addView(ui.text("Last detected",12,Ui.BRASS_TEXT));ui.gap(capture,5);capture.addView(ui.text(getSharedPreferences("automation",MODE_PRIVATE).getString("last_capture","No listening detected yet"),14,Ui.INK));ui.add(content,capture,20);
        ui.add(content,ui.text("Android grants broad notification access. This app processes only Storytel notifications and stores its reading log on your device.",13,Ui.MUTED),22);
        ui.add(content,ui.eyebrow("Carry your library with you"),12);
        LinearLayout backup=ui.card(); backup.addView(ui.heading("Backup & transfer",26)); ui.gap(backup,9);
        ui.add(backup,ui.text("Export your books, series and comments as a CSV file. Keep that file somewhere safe before changing or updating phones, then import it on the other device.",14,Ui.MUTED),16);
        backupExportButton=ui.button("Export reading data",true,()->chooseBackupDocument(false));
        backupImportButton=ui.button("Import reading data",false,()->chooseBackupDocument(true));
        backupExportButton.setEnabled(!backupPending&&!TRANSFER_GATE.get()); backupImportButton.setEnabled(!backupPending&&!TRANSFER_GATE.get());
        ui.add(backup,backupExportButton,9); ui.add(backup,backupImportButton,10);
        backup.addView(ui.text("Import previews the file first. Restoring replaces the current books, series and comments. Debug logs are separate.",12,Ui.MUTED));
        ui.add(content,backup,22);
        ui.add(content,ui.eyebrow("When something goes astray"),12);LinearLayout diagnostics=ui.card();diagnostics.addView(ui.heading("Diagnostics",26));ui.gap(diagnostics,9);
        Switch debugMode=new Switch(this);debugMode.setText("Debug mode");debugMode.setTextSize(16);debugMode.setTextColor(Ui.INK);debugMode.setMinHeight(dp(48));debugMode.setChecked(DebugLog.enabled(this));debugMode.setOnCheckedChangeListener((button,checked)->DebugLog.setEnabled(this,checked));diagnostics.addView(debugMode);
        ui.add(diagnostics,ui.text("Off by default. Enable, reproduce a problem, then export the log. Previously recorded logs remain until you clear them.",13,Ui.MUTED),14);ui.add(diagnostics,ui.text("Export notification details, capture decisions and errors to help investigate a problem. Logs include personal reading information.",14,Ui.MUTED),16);ui.add(diagnostics,ui.button("Export debug log",false,this::exportDebug),8);diagnostics.addView(ui.textButton("Clear debug history",()->new AlertDialog.Builder(this).setTitle("Clear debug history?").setMessage("Books and comments will be kept.").setPositiveButton("Clear",(d,w)->{DebugLog.clear(this);toast("Debug history cleared");}).setNegativeButton("Cancel",null).show()));ui.add(content,diagnostics,22);
        ui.add(content,ui.eyebrow("A note on the prototype"),10);ui.add(content,ui.text("Book starts can be captured automatically. Series, ebook activity and finish dates need your input. A pause or chapter ending never marks a book finished.",14,Ui.MUTED),18);content.addView(ui.rule());ui.gap(content,16);content.addView(ui.text("Storytel Reading Log · "+appVersion()+"\nA private reading journal. Independent companion app; not affiliated with Storytel.",12,Ui.MUTED));
    }
    private boolean notificationAccess() {
        String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");if(enabled==null)return false;ComponentName own=new ComponentName(this,StorytelListener.class);for(String item:enabled.split(":"))if(own.equals(ComponentName.unflattenFromString(item)))return true;return false;
    }
    private void exportDebug() {
        new AlertDialog.Builder(this).setTitle("Export debug log?").setMessage("The file contains Storytel notification text, book details, playback metadata and errors. Review it before sharing.").setPositiveButton("Choose file",(d,w)->{Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/octet-stream");intent.putExtra(Intent.EXTRA_TITLE,"storytel-debug-"+LocalDate.now()+".jsonl");startActivityForResult(intent,42);}).setNegativeButton("Cancel",null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==43||request==44) {
            if(result!=RESULT_OK||data==null||data.getData()==null) { setBackupPending(false); return; }
            Uri file=data.getData(); if(!ownsTransferGate&&!acquireBackupSlot()) return; setBackupPending(true);
            if(request==43) writeReadingData(file); else readReadingData(file);
            return;
        }
        if(request==42&&result==RESULT_OK&&data!=null&&data.getData()!=null) {
            try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())) {
                if(out==null) throw new java.io.IOException("No output stream available");
                DebugLog.export(this,out); toast("Debug log exported");
            } catch(Exception e) { logError("export_error",e); toast("Export failed. Please try another location."); }
        }
    }
    private void chooseBackupDocument(boolean importing) {
        if(!acquireBackupSlot()) return;
        Intent intent=new Intent(importing?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(importing?"*/*":"text/csv");
        if(!importing) intent.putExtra(Intent.EXTRA_TITLE,"storytel-reading-log-"+LocalDate.now()+".csv");
        setBackupPending(true);
        try { startActivityForResult(intent,importing?44:43); }
        catch(Exception e) { setBackupPending(false); logError("backup_picker_error",e); toast("A file picker could not be opened on this device."); }
    }
    private boolean acquireBackupSlot() {
        if(backupPending) { toast("Please finish the current backup or import first."); return false; }
        if(!ownsTransferGate&&!TRANSFER_GATE.compareAndSet(false,true)) { toast("A backup or import is still running. Please wait."); return false; }
        ownsTransferGate=true; setBackupPending(true); return true;
    }
    private void setBackupPending(boolean pending) {
        backupPending=pending;
        if(!pending&&ownsTransferGate&&!backupWorkerRunning) { ownsTransferGate=false;TRANSFER_GATE.set(false); }
        updateBackupAvailability();
    }
    private void updateBackupAvailability() {
        boolean available=!backupPending&&!TRANSFER_GATE.get();
        if(backupExportButton!=null) backupExportButton.setEnabled(available);
        if(backupImportButton!=null) backupImportButton.setEnabled(available);
        backupHandler.removeCallbacks(backupAvailability);
        if(TRANSFER_GATE.get()&&!ownsTransferGate&&!isDestroyed()&&!isFinishing()) backupHandler.postDelayed(backupAvailability,250);
    }
    private void showBackupProgress(String message) {
        if(backupProgress!=null) backupProgress.dismiss();
        backupProgress=new ProgressDialog(this); backupProgress.setMessage(message);
        backupProgress.setIndeterminate(true); backupProgress.setCancelable(false); backupProgress.show();
    }
    private void dismissBackupProgress() {
        if(backupProgress!=null) { backupProgress.dismiss(); backupProgress=null; }
    }
    private void backupUi(Runnable action) {
        runOnUiThread(()-> {
            backupWorkerRunning=false;
            if(!isFinishing()&&!isDestroyed()) action.run();
            else setBackupPending(false);
        });
    }
    private void writeReadingData(Uri file) {
        showBackupProgress("Exporting your reading data…");
        backupWorkerRunning=true;
        backupExecutor.execute(()-> {
            try {
                try(LogDatabase worker=new LogDatabase(getApplicationContext());
                    java.io.OutputStream out=getApplicationContext().getContentResolver().openOutputStream(file,"wt")) {
                    if(out==null) throw new java.io.IOException("The chosen file could not be opened.");
                    CsvBackup.write(worker,out);
                }
                backupUi(()-> { dismissBackupProgress(); setBackupPending(false); toast("Reading data exported. Keep the CSV file safe for transfer or backup."); });
            } catch(Exception e) { backupFailure("Export failed",e,false); }
        });
    }
    private void readReadingData(Uri file) {
        showBackupProgress("Checking the CSV file…");
        backupWorkerRunning=true;
        backupExecutor.execute(()-> {
            try {
                final CsvBackup.Snapshot snapshot;
                try(java.io.InputStream in=getApplicationContext().getContentResolver().openInputStream(file)) {
                    if(in==null) throw new java.io.IOException("The chosen file could not be opened.");
                    snapshot=CsvBackup.read(in);
                }
                backupUi(()-> {
                    dismissBackupProgress();
                    String summary=snapshot.bookCount()+" books · "+snapshot.seriesCount()+" series · "+snapshot.commentCount()+" comments";
                    backupPreview=new AlertDialog.Builder(this).setTitle("Restore reading data?")
                        .setMessage(summary+"\n\nRestoring replaces all current books, series and comments on this device. Export the current library first if you want to keep it.\n\nDebug logs are not included.")
                        .setPositiveButton("Replace & restore",(dialog,which)->restoreReadingData(snapshot))
                        .setNegativeButton("Cancel",(dialog,which)->setBackupPending(false)).create();
                    backupPreview.setOnCancelListener(dialog->setBackupPending(false)); backupPreview.show();
                });
            } catch(Exception e) { backupFailure("Import failed",e,true); }
        });
    }
    private void restoreReadingData(CsvBackup.Snapshot snapshot) {
        showBackupProgress("Restoring your reading data…");
        backupWorkerRunning=true;
        backupExecutor.execute(()-> {
            boolean restored=false;
            try(LogDatabase worker=new LogDatabase(getApplicationContext())) {
                CsvBackup.restore(worker,snapshot); restored=true;
            } catch(Exception e) {
                if(!restored) { backupFailure("Restore failed",e,true); return; }
                logError("backup_close_error",e);
            }
            backupUi(()-> { dismissBackupProgress(); setBackupPending(false); selected=null; query="";seriesQuery="";filter=0;libraryScroll=0;tab=2;showMain();toast("Reading data restored"); });
        });
    }
    private void backupFailure(String title,Exception error,boolean importing) {
        logError(importing?"csv_import_error":"csv_export_error",error);
        backupUi(()-> {
            dismissBackupProgress(); setBackupPending(false);
            String message=error.getMessage(); if(message==null||message.trim().isEmpty())message="The file could not be processed. Please try again.";
            new AlertDialog.Builder(this).setTitle(title).setMessage("No reading data was changed.\n\n"+message).setPositiveButton("Close",null).show();
        });
    }
    private String appVersion() {
        try { return getPackageManager().getPackageInfo(getPackageName(),0).versionName; }
        catch(Exception ignored) { return "Prototype"; }
    }
    @Override public void onBackPressed() { navigateBack(); }
    private void navigateBack() {
        hideKeyboard();if(page.equals("edit")){Runnable leave=()->{if(editorReturn.equals("details")&&selected!=null)showDetails(selected);else showMain();};if(draftChanged())new AlertDialog.Builder(this).setTitle("Discard changes?").setMessage("Your unsaved book details will be lost.").setPositiveButton("Discard",(d,w)->leave.run()).setNegativeButton("Keep editing",null).show();else leave.run();return;}
        if(page.equals("notes")){Runnable leave=()->{if(notesReturn.equals("details")&&selected!=null)showDetails(selected);else if(notesReturn.equals("seriesDetail"))showSeries(noteName);else showMain();};if(!commentInput.getText().toString().trim().isEmpty())new AlertDialog.Builder(this).setTitle("Discard this comment?").setPositiveButton("Discard",(d,w)->leave.run()).setNegativeButton("Keep writing",null).show();else leave.run();return;}
        if(!page.equals("main")){showMain();return;}if(tab!=0){tab=0;showMain();return;}super.onBackPressed();
    }
    private TextWatcher watcher(Runnable action) {return new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){action.run();}public void afterTextChanged(Editable e){}};}
    private void hideKeyboard(){View v=getCurrentFocus();if(v!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    private void logError(String event,Exception e){DebugLog.event(this,event,DebugLog.data("error",android.util.Log.getStackTraceString(e)));}
    private String date(String value){if(value.isEmpty())return "Not recorded";try{return LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy",Locale.getDefault()));}catch(Exception e){return value;}}
}

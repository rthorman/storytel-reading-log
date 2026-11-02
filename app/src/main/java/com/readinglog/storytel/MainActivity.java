package com.readinglog.storytel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import java.time.LocalDate;
import java.util.List;

public final class MainActivity extends Activity {
    private LogDatabase db;
    private LinearLayout root, rows;
    private EditText search;
    private Spinner sort, filter;
    private boolean seriesTab;
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) { super.onCreate(state); db=new LogDatabase(this); home(); }
    @Override protected void onResume() { super.onResume(); if(rows!=null) refresh(); }
    @Override protected void onDestroy() { db.close(); super.onDestroy(); }
    private LinearLayout column() {
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(20),dp(10),dp(20),dp(10)); return l;
    }
    private TextView label(String text, int size) {
        TextView t=new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(Color.rgb(38,48,44)); t.setPadding(0,dp(7),0,dp(7)); return t;
    }
    private Button button(String text,Runnable action) { Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setOnClickListener(v->action.run()); return b; }
    private EditText field(LinearLayout parent,String hint,String value) {
        parent.addView(label(hint,14)); EditText e=new EditText(this); e.setText(value); e.setSingleLine(true); parent.addView(e); return e;
    }
    private Spinner spinner(String[] values) {
        Spinner s=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values); s.setAdapter(adapter); return s;
    }
    private void home() {
        root=column(); root.setBackgroundColor(Color.rgb(250,247,241));
        // Respect status/navigation bars including Android 15 edge-to-edge enforcement.
        root.setOnApplyWindowInsetsListener((v,i)-> { v.setPadding(dp(20),i.getSystemWindowInsetTop()+dp(10),dp(20),i.getSystemWindowInsetBottom()+dp(10)); return i; });
        setContentView(root);
        root.addView(label("Your reading life",30));
        root.addView(label("Storytel · books, listening & notes",14));
        root.addView(button("Set up automatic listening log",this::automation));
        root.addView(button("Export debug log",this::exportDebug));
        LinearLayout tabs=new LinearLayout(this);
        tabs.addView(button("Books",()->{ seriesTab=false; refresh(); })); tabs.addView(button("Series",()->{ seriesTab=true; refresh(); }));
        tabs.addView(button("+ Book",()->edit(new LogDatabase.Book()))); root.addView(tabs);
        search=new EditText(this); search.setSingleLine(true); search.setHint("Search title, author or series"); root.addView(search);
        LinearLayout options=new LinearLayout(this);
        sort=spinner(new String[]{"Title A–Z","Author A–Z","Series A–Z","Recently started"});
        filter=spinner(new String[]{"All books","In progress","Finished","To read"});
        options.addView(sort,new LinearLayout.LayoutParams(0,dp(48),1)); options.addView(filter,new LinearLayout.LayoutParams(0,dp(48),1)); root.addView(options);
        ScrollView scroll=new ScrollView(this); rows=column(); scroll.addView(rows); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { refresh(); }
            public void afterTextChanged(Editable e) {}
        });
        AdapterView.OnItemSelectedListener selected=new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id) { refresh(); }
            public void onNothingSelected(AdapterView<?> p) {}
        };
        sort.setOnItemSelectedListener(selected); filter.setOnItemSelectedListener(selected); refresh();
    }
    private void refresh() {
        rows.removeAllViews(); String query=search.getText().toString();
        sort.setVisibility(seriesTab?View.GONE:View.VISIBLE); filter.setVisibility(seriesTab?View.GONE:View.VISIBLE);
        if(seriesTab) {
            for(String name:db.series()) if(name.toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT))) rows.addView(button(name,()->notes(db.seriesId(name),true,name)));
            if(rows.getChildCount()==0) rows.addView(label("No series found. Add a series to a book to get started.",16));
            return;
        }
        List<LogDatabase.Book> books=db.books(query,sort.getSelectedItemPosition(),filter.getSelectedItemPosition());
        rows.addView(label(books.size()+" books",14));
        if(books.isEmpty()) rows.addView(label("Your next chapter starts here. Enable automatic logging or add a book.",20));
        for(LogDatabase.Book b:books) {
            LinearLayout card=column(); card.setBackgroundColor(Color.WHITE);
            card.addView(label(b.title,22)); card.addView(label(b.author,16));
            if(!b.series.isEmpty()) card.addView(label(b.series,14));
            card.addView(label(b.format+" · "+LogRules.status(b.started,b.finished),14));
            card.addView(label("Started: "+(b.started.isEmpty()?"—":b.started)+"   Finished: "+(b.finished.isEmpty()?"—":b.finished),13));
            card.addView(button("Details & comments",()->details(b)));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(12); rows.addView(card,p);
        }
    }
    private void automation() {
        String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        boolean active=enabled!=null && enabled.contains(getPackageName()+"/");
        String last=getSharedPreferences("automation",MODE_PRIVATE).getString("last_capture","No listening detected yet");
        new AlertDialog.Builder(this).setTitle("Automatic Storytel logging")
            .setMessage((active?"Access enabled":"Access not enabled")+"\n\n"+last+"\n\nEnable this app in Android notification access, then play a book in Storytel. We inspect only Storytel notifications and save the first observed listening date locally. Android grants broad notification access, but other apps' notifications are ignored.\n\nReview detected titles and authors: Storytel metadata can vary. Series, ebook activity, and completion dates need manual entry. Pausing or ending a chapter never marks a book finished.")
            .setPositiveButton("Open notification access",(d,w)->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
            .setNeutralButton("Clear debug log",(d,w)->new AlertDialog.Builder(this).setMessage("Delete diagnostic history from this device?").setPositiveButton("Clear",(a,b)->DebugLog.clear(this)).setNegativeButton("Cancel",null).show())
            .setNegativeButton("Close",null).show();
    }
    private void exportDebug() {
        new AlertDialog.Builder(this).setTitle("Export Storytel diagnostics")
            .setMessage("The log contains book titles, authors, Storytel notification text, playback metadata and diagnostic errors. Choose where to save it and review it before sharing.")
            .setPositiveButton("Choose file",(d,w)-> {
                Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE,"storytel-debug-"+LocalDate.now()+".jsonl"); startActivityForResult(intent,42);
            }).setNegativeButton("Cancel",null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==42 && result==RESULT_OK && data!=null && data.getData()!=null) {
            try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())) {
                if(out==null) throw new java.io.IOException("No output stream available");
                DebugLog.export(this,out); Toast.makeText(this,"Debug log exported",Toast.LENGTH_LONG).show();
            } catch(Exception e) { DebugLog.event(this,"export_error",DebugLog.data("error",e.toString())); Toast.makeText(this,"Export failed: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
        }
    }
    private void details(LogDatabase.Book b) {
        String[] actions={"Edit book & dates","Book comments",b.series.isEmpty()?"No series assigned":"Series comments","Delete book"};
        new AlertDialog.Builder(this).setTitle(b.title).setItems(actions,(d,w)-> {
            if(w==0) edit(b);
            if(w==1) notes(b.id,false,b.title);
            if(w==2 && !b.series.isEmpty()) notes(db.seriesId(b.series),true,b.series);
            if(w==3) new AlertDialog.Builder(this).setTitle("Delete this book?").setMessage("Its book comments will also be deleted. Series comments are kept.").setPositiveButton("Delete",(x,y)->{db.deleteBook(b.id); refresh();}).setNegativeButton("Cancel",null).show();
        }).show();
    }
    private void dateField(LinearLayout form,EditText input,String name) {
        input.setHint("YYYY-MM-DD or leave empty");
        form.addView(button("Choose "+name,()-> {
            LocalDate initial=LocalDate.now(); try { initial=LocalDate.parse(input.getText().toString()); } catch(Exception ignored) {}
            DatePickerDialog picker=new DatePickerDialog(this,(v,y,m,d)->input.setText(LocalDate.of(y,m+1,d).toString()),initial.getYear(),initial.getMonthValue()-1,initial.getDayOfMonth());
            picker.getDatePicker().setMaxDate(System.currentTimeMillis()); picker.show();
        }));
    }
    private void edit(LogDatabase.Book original) {
        LinearLayout form=column();
        EditText title=field(form,"Title",original.title), author=field(form,"Author",original.author);
        form.addView(label("Series (optional)",14)); AutoCompleteTextView series=new AutoCompleteTextView(this); series.setSingleLine(true); series.setText(original.series);
        series.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,db.series())); series.setThreshold(1); form.addView(series);
        form.addView(label("Format",14)); Spinner format=spinner(new String[]{"Audiobook","Ebook"}); format.setSelection(original.format.equals("Ebook")?1:0); form.addView(format);
        EditText start=field(form,"Started",original.started); dateField(form,start,"start date");
        EditText finish=field(form,"Finished",original.finished); dateField(form,finish,"finish date");
        TextView error=label("",14); error.setTextColor(Color.rgb(160,30,30)); form.addView(error);
        ScrollView scroll=new ScrollView(this); scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(original.id==0?"Add a book":"Edit book").setView(scroll).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{
            LogDatabase.Book b=new LogDatabase.Book(); b.id=original.id; b.title=title.getText().toString(); b.author=author.getText().toString(); b.series=series.getText().toString(); b.format=format.getSelectedItem().toString(); b.started=start.getText().toString().trim(); b.finished=finish.getText().toString().trim();
            try { db.save(b); dialog.dismiss(); refresh(); } catch(IllegalArgumentException e) { error.setText(e.getMessage()); }
        })); dialog.show();
    }
    private void notes(long id,boolean series,String name) {
        LinearLayout form=column(), list=column();
        EditText input=new EditText(this); input.setHint("Write a comment"); input.setMinLines(2); form.addView(input);
        Runnable render=()-> {
            list.removeAllViews(); List<LogDatabase.Note> comments=db.notes(id,series);
            if(comments.isEmpty()) list.addView(label("No comments yet",16));
            for(LogDatabase.Note n:comments) { list.addView(label(n.created+" UTC\n"+n.body,16)); list.addView(button("Delete comment",()->new AlertDialog.Builder(this).setMessage("Delete this comment?").setPositiveButton("Delete",(d,w)->{db.deleteNote(n.id); list.removeAllViews(); for(LogDatabase.Note remaining:db.notes(id,series)) list.addView(label(remaining.created+" UTC\n"+remaining.body,16));}).setNegativeButton("Cancel",null).show())); }
        };
        form.addView(button("Add comment",()-> { try { db.addNote(id,series,input.getText().toString()); input.setText(""); render.run(); } catch(IllegalArgumentException e) { input.setError(e.getMessage()); } }));
        form.addView(list); render.run(); ScrollView scroll=new ScrollView(this); scroll.addView(form);
        new AlertDialog.Builder(this).setTitle((series?"Series: ":"")+name).setView(scroll).setPositiveButton("Close",null).show();
    }
}

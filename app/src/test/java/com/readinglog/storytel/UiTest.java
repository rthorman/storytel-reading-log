// SPDX-License-Identifier: AGPL-3.0-or-later
package com.readinglog.storytel;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class UiTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    @Before public void setup() {RuntimeEnvironment.getApplication().deleteDatabase("reading-log.db");}
    @After public void close() {if(controller!=null)controller.pause().stop().destroy();}
    private void start(boolean seed) {
        if(seed) try(LogDatabase db=new LogDatabase(RuntimeEnvironment.getApplication())) {
            book(db,"The Secret Garden","Frances Hodgson Burnett","", "2025-09-10","");
            book(db,"A Study in Scarlet","Arthur Conan Doyle","Sherlock Holmes","2025-08-02","2025-08-12");
            book(db,"The Hound of the Baskervilles","Arthur Conan Doyle","Sherlock Holmes","","");
        }
        controller=Robolectric.buildActivity(MainActivity.class).setup().visible();activity=controller.get();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }
    private static void book(LogDatabase db,String title,String author,String series,String start,String finish) {LogDatabase.Book b=new LogDatabase.Book();b.title=title;b.author=author;b.series=series;b.started=start;b.finished=finish;db.save(b);}
    private View root() {return activity.getWindow().getDecorView();}
    private TextView find(View view,String value) {
        if(view instanceof TextView && value.contentEquals(((TextView)view).getText()))return (TextView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){TextView result=find(((ViewGroup)view).getChildAt(i),value);if(result!=null)return result;}
        return null;
    }
    private EditText input(View view,String hint) {
        if(view instanceof EditText && hint.contentEquals(((EditText)view).getHint()))return (EditText)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){EditText result=input(((ViewGroup)view).getChildAt(i),hint);if(result!=null)return result;}
        return null;
    }
    private void click(String value) {
        TextView view=find(root(),value);assertNotNull("Missing action: "+value,view);
        View target=view;while(target!=null&&!target.isClickable()) {android.view.ViewParent p=target.getParent();target=p instanceof View?(View)p:null;}
        assertNotNull("No clickable parent for "+value,target);target.performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }
    private void capture(String name) throws Exception {
        View decor=root();int w=activity.getResources().getDisplayMetrics().widthPixels,h=activity.getResources().getDisplayMetrics().heightPixels;
        decor.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));decor.layout(0,0,w,h);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);decor.draw(new Canvas(bitmap));
        File directory=new File("build/ui-screenshots");assertTrue(directory.isDirectory()||directory.mkdirs());
        try(OutputStream out=new FileOutputStream(new File(directory,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
    }
    @Test public void rendersLibraryDetailsSeriesAndSettings() throws Exception {
        start(true);assertNotNull(find(root(),"The Secret Garden"));capture("library");
        click("The Secret Garden");assertNotNull(find(root(),"Edit book & dates"));capture("book-details");
        activity.onBackPressed();click("Series");assertNotNull(find(root(),"Sherlock Holmes"));capture("series");
        click("Settings");assertNotNull(find(root(),"Export debug log"));assertNotNull(find(root(),"Export reading data"));
        TextView toggle=find(root(),"Debug mode");assertTrue(toggle instanceof Switch);assertFalse(((Switch)toggle).isChecked());capture("settings");
    }
    @Test public void searchKeepsFocusAndNoResultsCanBeReset() throws Exception {
        start(true);EditText search=input(root(),"Search title, author or series");assertNotNull(search);search.requestFocus();search.setText("Doyle");
        assertNotNull(find(root(),"A Study in Scarlet"));assertNull(find(root(),"The Secret Garden"));assertTrue(search.hasFocus());
        search.setText("not a book");assertNotNull(find(root(),"No books found"));capture("no-results");click("Clear search & filters");assertNotNull(find(root(),"The Secret Garden"));
    }
    @Test public void emptyLibraryAndLargeTextStayScrollable() throws Exception {
        RuntimeEnvironment.setQualifiers("w320dp-h640dp-xhdpi");start(false);assertNotNull(find(root(),"A library of your own"));capture("empty-library");
        RuntimeEnvironment.setFontScale(1.5f);controller.recreate();activity=controller.get();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();capture("large-text");
        TextView add=find(root(),"Add a book yourself");assertNotNull(add);boolean scrollAncestor=false;for(android.view.ViewParent p=add.getParent();p!=null;p=p.getParent())if(p instanceof ScrollView)scrollAncestor=true;assertTrue("Empty actions must be reachable by scrolling",scrollAncestor);
    }
    @Test public void editingAndRotationPreserveDraft() throws Exception {
        start(false);click("+ Add");EditText title=input(root(),"Book title");assertNotNull(title);title.setText("A new book");input(root(),"Author name").setText("An author");
        controller.recreate();activity=controller.get();assertEquals("A new book",input(root(),"Book title").getText().toString());click("Save book");assertNotNull(find(root(),"A new book"));
        try(LogDatabase db=new LogDatabase(activity)){assertEquals(1,db.books("",0,0).size());}
    }
}

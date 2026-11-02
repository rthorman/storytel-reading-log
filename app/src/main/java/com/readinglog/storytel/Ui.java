// SPDX-License-Identifier: AGPL-3.0-or-later
package com.readinglog.storytel;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Locale;

/** Small native view vocabulary: ink, paper, brass, readable type and generous targets. */
final class Ui {
    static final int PAPER=Color.rgb(246,241,229), SURFACE=Color.rgb(255,252,245), INK=Color.rgb(24,60,49),
        MUTED=Color.rgb(99,104,92), BRASS=Color.rgb(152,115,65), BRASS_TEXT=Color.rgb(132,98,54), LINE=Color.rgb(217,207,187),
        TINT=Color.rgb(228,234,222), ERROR=Color.rgb(155,48,39);
    final Context context;
    Ui(Context context) { this.context=context; }
    int dp(float value) { return Math.round(value*context.getResources().getDisplayMetrics().density); }
    LinearLayout column() { LinearLayout l=new LinearLayout(context); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout row() { LinearLayout l=new LinearLayout(context); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    TextView text(String text,int size,int color) {
        TextView t=new TextView(context); t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("kern"); t.setIncludeFontPadding(false); t.setLineSpacing(dp(3),1f); return t;
    }
    TextView heading(String value,int size) { TextView t=text(value,size,INK); t.setTypeface(Typeface.create("serif",Typeface.NORMAL)); return t; }
    TextView eyebrow(String value) { TextView t=text(value.toUpperCase(Locale.ROOT),11,BRASS_TEXT); t.setLetterSpacing(.16f); t.setTypeface(null,Typeface.BOLD); return t; }
    GradientDrawable shape(int color,int border,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if(border!=0) d.setStroke(dp(1),border); return d;
    }
    void clickable(View v,int color,int border,int radius,Runnable action) {
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x18183C31),shape(color,border,radius),null));
        v.setOnClickListener(x->action.run()); v.setFocusable(true);
    }
    Button button(String text,boolean primary,Runnable action) {
        Button b=new Button(context); b.setText(text); b.setAllCaps(false); b.setTextSize(14); b.setTypeface(null,Typeface.BOLD);
        b.setTextColor(primary?SURFACE:INK); b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48)); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(dp(16),dp(12),dp(16),dp(12)); b.setStateListAnimator(null);
        clickable(b,primary?INK:SURFACE,primary?0:LINE,10,action); return b;
    }
    Button textButton(String text,Runnable action) {
        Button b=button(text,false,action); b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x18183C31),shape(Color.TRANSPARENT,0,8),null)); return b;
    }
    void inputStyle(EditText e) {
        e.setTextSize(16); e.setTextColor(INK); e.setHintTextColor(MUTED); e.setMinHeight(dp(52));
        e.setPadding(dp(14),dp(12),dp(14),dp(12)); e.setBackground(shape(SURFACE,LINE,9));
        e.setSelectAllOnFocus(false);
    }
    EditText input(String hint,String value,boolean multiline) {
        EditText e=new EditText(context); inputStyle(e); e.setHint(hint); e.setText(value); e.setSingleLine(!multiline);
        if(multiline) { e.setMinLines(3); e.setGravity(Gravity.TOP|Gravity.START); }
        return e;
    }
    LinearLayout card() {
        LinearLayout l=column(); l.setPadding(dp(16),dp(16),dp(16),dp(16)); l.setBackground(shape(SURFACE,LINE,12)); return l;
    }
    View rule() { View v=new View(context); v.setBackgroundColor(LINE); v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1))); return v; }
    void gap(LinearLayout l,int size) { View v=new View(context); l.addView(v,new LinearLayout.LayoutParams(1,dp(size))); }
    void add(LinearLayout parent,View child,int bottom) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(bottom); parent.addView(child,p);
    }
    void weight(LinearLayout parent,View child) { parent.addView(child,new LinearLayout.LayoutParams(0,-2,1)); }
    View cover(String title,boolean large) {
        int[] colors={INK,Color.rgb(78,55,45),Color.rgb(48,66,79),Color.rgb(102,73,49)};
        int index=Math.floorMod(title.hashCode(),colors.length);
        LinearLayout outer=column(); outer.setGravity(Gravity.CENTER); outer.setPadding(dp(6),dp(8),dp(6),dp(8)); outer.setBackground(shape(colors[index],0,4));
        LinearLayout inner=column(); inner.setGravity(Gravity.CENTER); inner.setBackground(shape(Color.TRANSPARENT,0xffb89a68,1));
        // Decorative covers are hidden from accessibility; the adjacent title is the real label.
        inner.setPadding(dp(5),dp(8),dp(5),dp(8)); GradientDrawable frame=shape(Color.TRANSPARENT,0xffb89a68,1); inner.setBackground(frame);
        TextView ornament=text("✦",large?18:12,0xffd5bb88); ornament.setGravity(Gravity.CENTER); inner.addView(ornament);
        String[] words=title.trim().split("\\s+"); String initials=words.length==0||words[0].isEmpty()?"S":words[0].substring(0,1)+(words.length>1?words[words.length-1].substring(0,1):"");
        TextView letters=heading(initials.toUpperCase(Locale.ROOT),large?38:23); letters.setTextColor(SURFACE); letters.setGravity(Gravity.CENTER); inner.addView(letters,new LinearLayout.LayoutParams(-1,0,1));
        TextView line=text("—",12,0xffd5bb88); line.setGravity(Gravity.CENTER); inner.addView(line);
        outer.addView(inner,new LinearLayout.LayoutParams(-1,-1)); outer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        return outer;
    }
    TextView badge(String text) {
        TextView t=text(text,12,INK); t.setPadding(dp(9),dp(5),dp(9),dp(5)); t.setBackground(shape(TINT,0,6)); return t;
    }
}

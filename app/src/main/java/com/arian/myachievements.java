package com.arian.myachievements;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout list;
    TextView counts;
    ArrayList<Achievement> data = new ArrayList<>();
    SharedPreferences prefs;
    View dragged;
    int draggedFrom = -1;

    final int BG = Color.rgb(32,33,36);
    final int CARD = Color.rgb(61,65,70);
    final int PANEL = Color.rgb(43,46,50);
    final int CYAN = Color.rgb(100,199,240);

    static class Achievement {
        String title, desc, medal;
        boolean done;
        Achievement(String t, String d, String m, boolean c) {
            title=t; desc=d; medal=m; done=c;
        }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(32,33,36));
        getWindow().setNavigationBarColor(Color.rgb(23,24,26));
        prefs = getSharedPreferences("achievements", MODE_PRIVATE);
        load();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        // Top bar
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(14), dp(8), dp(10), dp(5));

        TextView menu = tv("☰", 30, Color.WHITE);
        top.addView(menu, new LinearLayout.LayoutParams(dp(48), dp(52)));

        TextView title = tv("All Achievements", 28, Color.LTGRAY);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));

        TextView more = tv("⋮", 32, Color.WHITE);
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(v -> showMenu(more));
        top.addView(more, new LinearLayout.LayoutParams(dp(45), dp(52)));
        root.addView(top);

        counts = tv("", 18, Color.WHITE);
        counts.setGravity(Gravity.CENTER);
        counts.setPadding(0, dp(2), 0, dp(10));
        root.addView(counts);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), 0, dp(8), dp(90));
        list.setOnDragListener((v,e) -> handleDrag(e));

        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        // Add button
        TextView add = tv("+", 34, Color.WHITE);
        add.setGravity(Gravity.CENTER);
        GradientDrawable fab = new GradientDrawable();
        fab.setColor(CYAN);
        fab.setShape(GradientDrawable.OVAL);
        add.setBackground(fab);
        add.setElevation(dp(8));
        add.setOnClickListener(v -> showEditor(-1));

        FrameLayout frame = new FrameLayout(this);
        frame.addView(root, new FrameLayout.LayoutParams(-1,-1));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(62),dp(62),Gravity.RIGHT|Gravity.BOTTOM);
        fp.setMargins(0,0,dp(18),dp(18));
        frame.addView(add, fp);

        setContentView(frame);
        render();
    }

    TextView tv(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    boolean handleDrag(DragEvent e) {
        if (e.getAction() == DragEvent.ACTION_DRAG_STARTED) return true;

        if (e.getAction() == DragEvent.ACTION_DRAG_LOCATION && dragged != null) {
            float y = e.getY();
            int target = list.getChildCount();
            for (int i=0;i<list.getChildCount();i++) {
                View c=list.getChildAt(i);
                if (c==dragged) continue;
                if (y < c.getTop()+c.getHeight()/2f) { target=i; break; }
            }
            int current = list.indexOfChild(dragged);
            if (target > list.getChildCount()-1) target=list.getChildCount()-1;
            if (target >= 0 && target != current && target != current+1) {
                list.removeView(dragged);
                if (target > list.getChildCount()) target=list.getChildCount();
                list.addView(dragged, target);
            }
            return true;
        }

        if (e.getAction() == DragEvent.ACTION_DROP) {
            saveOrderFromViews();
            if (dragged != null) dragged.setAlpha(1f);
            dragged=null;
            render();
            return true;
        }

        if (e.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            if (dragged != null) dragged.setAlpha(1f);
            dragged=null;
            return true;
        }
        return true;
    }

    View makeCard(Achievement a, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(0,0,dp(5),0);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        card.setElevation(dp(2));

        TextView medal = tv(medalEmoji(a.medal), 48, medalColor(a.medal));
        medal.setGravity(Gravity.CENTER);
        GradientDrawable side = new GradientDrawable();
        side.setColor(PANEL);
        side.setCornerRadius(dp(16));
        medal.setBackground(side);
        card.addView(medal, new LinearLayout.LayoutParams(dp(112),dp(112)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(12),dp(9),dp(4),dp(8));

        TextView title = tv(a.title, 21, Color.WHITE);
        title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        TextView desc = tv(a.desc, 16, Color.LTGRAY);
        desc.setGravity(Gravity.RIGHT);
        desc.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        TextView status = tv(a.done ? "Status: Completed" : "Status: Not completed", 13,
                a.done ? CYAN : Color.LTGRAY);

        text.addView(title, new LinearLayout.LayoutParams(-1,0,1));
        text.addView(desc, new LinearLayout.LayoutParams(-1,0,1));
        text.addView(status, new LinearLayout.LayoutParams(-1,dp(24)));
        card.addView(text, new LinearLayout.LayoutParams(0,dp(112),1));

        card.setTag(a);
        card.setOnClickListener(v -> showEditor(index));

        card.setOnLongClickListener(v -> {
            dragged = card;
            draggedFrom = list.indexOfChild(card);
            card.setAlpha(.45f);
            ClipData cd = ClipData.newPlainText("position", String.valueOf(draggedFrom));
            if (android.os.Build.VERSION.SDK_INT >= 24)
                card.startDragAndDrop(null, new View.DragShadowBuilder(card), null, View.DRAG_FLAG_GLOBAL);
            else
                card.startDrag(null, new View.DragShadowBuilder(card), null, 0);
            return true;
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(112));
        lp.setMargins(0,dp(6),0,dp(6));
        card.setLayoutParams(lp);
        return card;
    }

    String medalEmoji(String m) {
        if ("Bronze".equals(m)) return "🥉";
        if ("Silver".equals(m)) return "🥈";
        if ("Gold".equals(m)) return "🥇";
        return "🏆";
    }

    int medalColor(String m) {
        if ("Bronze".equals(m)) return Color.rgb(205,127,50);
        if ("Silver".equals(m)) return Color.LTGRAY;
        if ("Gold".equals(m)) return Color.rgb(255,193,7);
        return CYAN;
    }

    void render() {
        list.removeAllViews();
        for (int i=0;i<data.size();i++) list.addView(makeCard(data.get(i), i));
        int b=0,s=0,g=0,p=0;
        for(Achievement a:data) {
            if(a.done) {
                if(a.medal.equals("Bronze")) b++;
                else if(a.medal.equals("Silver")) s++;
                else if(a.medal.equals("Gold")) g++;
                else p++;
            }
        }
        counts.setText("🥉 "+b+"    🥈 "+s+"    🥇 "+g+"    🏆 "+p);
    }

    void showEditor(final int index) {
        boolean edit=index>=0;
        Achievement old=edit?data.get(index):new Achievement("","","Platinum",false);

        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18),dp(5),dp(18),0);

        EditText title=new EditText(this);
        title.setHint("Achievement title");
        title.setText(old.title);
        title.setTextColor(Color.WHITE);
        title.setHintTextColor(Color.GRAY);

        EditText desc=new EditText(this);
        desc.setHint("Description");
        desc.setText(old.desc);
        desc.setTextColor(Color.WHITE);
        desc.setHintTextColor(Color.GRAY);
        desc.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        desc.setMinLines(2);

        Spinner spinner=new Spinner(this);
        String[] medals={"Bronze","Silver","Gold","Platinum"};
        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, medals));
        for(int i=0;i<medals.length;i++) if(medals[i].equals(old.medal)) spinner.setSelection(i);

        box.addView(title);
        box.addView(desc);
        box.addView(spinner);

        AlertDialog d=new AlertDialog.Builder(this)
                .setTitle(edit ? "Edit Achievement" : "New Achievement")
                .setView(box)
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Save",null)
                .create();

        if(edit) {
            d.setOnShowListener(x -> {
                d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    old.title=title.getText().toString().trim();
                    old.desc=desc.getText().toString().trim();
                    old.medal=medals[spinner.getSelectedItemPosition()];
                    save(); render(); d.dismiss();
                });
                d.setButton(AlertDialog.BUTTON_NEUTRAL,"Delete",(di,w)->{});
                d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    data.remove(index); save(); render(); d.dismiss();
                });
            });
        } else {
            d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String t=title.getText().toString().trim();
                if(t.length()==0){ title.setError("Enter a title"); return; }
                data.add(new Achievement(t,desc.getText().toString().trim(),
                        medals[spinner.getSelectedItemPosition()],false));
                save(); render(); d.dismiss();
            }));
        }
        d.show();
    }

    void showMenu(View anchor) {
        PopupMenu pm=new PopupMenu(this,anchor);
        pm.getMenu().add("Add Achievement");
        pm.getMenu().add("Reset all completion");
        pm.getMenu().add("Delete all");
        pm.setOnMenuItemClickListener(item -> {
            String s=item.getTitle().toString();
            if(s.equals("Add Achievement")) showEditor(-1);
            else if(s.equals("Reset all completion")) {
                for(Achievement a:data)a.done=false; save(); render();
            } else if(s.equals("Delete all")) {
                new AlertDialog.Builder(this).setTitle("Delete all?")
                        .setMessage("This cannot be undone.")
                        .setNegativeButton("Cancel",null)
                        .setPositiveButton("Delete",(d,w)->{data.clear();save();render();}).show();
            }
            return true;
        });
        pm.show();
    }

    void saveOrderFromViews() {
        ArrayList<Achievement> n=new ArrayList<>();
        // Cards carry their Achievement object as a tag.
        n.clear();
        for(int i=0;i<list.getChildCount();i++){
            View v=list.getChildAt(i);
            Object tag=v.getTag();
            if(tag instanceof Achievement) n.add((Achievement)tag);
        }
        if(n.size()==data.size()) data=n;
        save();
    }

    void save() {
        JSONArray arr=new JSONArray();
        try{
            for(Achievement a:data){
                JSONObject o=new JSONObject();
                o.put("title",a.title); o.put("desc",a.desc);
                o.put("medal",a.medal); o.put("done",a.done);
                arr.put(o);
            }
        }catch(Exception ignored){}
        prefs.edit().putString("items",arr.toString()).apply();
    }

    void load() {
        String raw=prefs.getString("items","");
        if(raw.length()==0)return;
        try{
            JSONArray a=new JSONArray(raw);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.getJSONObject(i);
                data.add(new Achievement(o.optString("title"),o.optString("desc"),
                        o.optString("medal","Platinum"),o.optBoolean("done",false)));
            }
        }catch(Exception ignored){}
    }

    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
}

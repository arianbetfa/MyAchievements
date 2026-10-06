package com.arian.myachievements;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.InputType;
import java.io.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    LinearLayout list;
    TextView counts;
    ArrayList<Achievement> data = new ArrayList<>();
    SharedPreferences prefs;
    View dragged;
    boolean unlocked = false;

    final int BG_DARK = Color.rgb(32,33,36);
    final int CARD_DARK = Color.rgb(61,65,70);
    final int PANEL_DARK = Color.rgb(43,46,50);
    final int CYAN = Color.rgb(100,199,240);
    final int BG_LIGHT = Color.rgb(245,246,248);
    final int CARD_LIGHT = Color.WHITE;
    final int PANEL_LIGHT = Color.rgb(232,234,238);

    static class Achievement {
        String title, desc, medal, imagePath;
        boolean done;
        Achievement(String t, String d, String m, boolean c) { this(t,d,m,c,""); }
        Achievement(String t, String d, String m, boolean c, String img) {
            title=t; desc=d; medal=m; done=c; imagePath=img==null?"":img;
        }
    }

    @Override public void onCreate(Bundle b) {
        prefs = getSharedPreferences("achievements", MODE_PRIVATE);
        setTheme(prefs.getBoolean("lightMode", false) ? R.style.AppTheme_Light : R.style.AppTheme);
        super.onCreate(b);
        getWindow().setStatusBarColor(isLight()?BG_LIGHT:BG_DARK);
        getWindow().setNavigationBarColor(isLight()?Color.rgb(230,232,235):Color.rgb(23,24,26));
        load();
        if (hasPin()) showPinGate(); else unlocked=true;
        buildUi();
    }

    boolean isLight(){ return prefs.getBoolean("lightMode", false); }
    int bg(){return isLight()?BG_LIGHT:BG_DARK;}
    int cardColor(){return isLight()?CARD_LIGHT:CARD_DARK;}
    int panelColor(){return isLight()?PANEL_LIGHT:PANEL_DARK;}
    int primaryText(){return isLight()?Color.rgb(30,31,34):Color.WHITE;}
    int secondaryText(){return isLight()?Color.rgb(85,87,92):Color.LTGRAY;}

    void buildUi(){
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg());
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(14),dp(8),dp(10),dp(5));

        TextView menu = tv("☰",30,primaryText());
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v->showSideMenu());
        top.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(52)));

        TextView title = tv("All Achievements",28,primaryText());
        top.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));

        TextView more = tv("⋮",32,primaryText());
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(v->showMenu(more));
        top.addView(more,new LinearLayout.LayoutParams(dp(45),dp(52)));
        root.addView(top);

        counts=tv("",18,primaryText());
        counts.setGravity(Gravity.CENTER);
        counts.setPadding(0,dp(2),0,dp(10));
        root.addView(counts);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(bg());
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8),0,dp(8),dp(90));
        list.setOnDragListener((v,e)->handleDrag(e));
        scroll.addView(list);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        TextView add=tv("+",34,Color.WHITE); add.setGravity(Gravity.CENTER);
        GradientDrawable fab=new GradientDrawable(); fab.setColor(CYAN); fab.setShape(GradientDrawable.OVAL); add.setBackground(fab); add.setElevation(dp(8));
        add.setOnClickListener(v->showEditor(-1));

        FrameLayout frame=new FrameLayout(this); frame.addView(root,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(62),dp(62),Gravity.RIGHT|Gravity.BOTTOM); fp.setMargins(0,0,dp(18),dp(18)); frame.addView(add,fp);
        setContentView(frame); render();
    }

    TextView tv(String s,float size,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t; }

    boolean handleDrag(DragEvent e){
        if(e.getAction()==DragEvent.ACTION_DRAG_STARTED)return true;
        if(e.getAction()==DragEvent.ACTION_DRAG_LOCATION && dragged!=null){
            float y=e.getY(); int target=list.getChildCount();
            for(int i=0;i<list.getChildCount();i++){ View c=list.getChildAt(i); if(c==dragged)continue; if(y<c.getTop()+c.getHeight()/2f){target=i;break;} }
            int current=list.indexOfChild(dragged); if(target>list.getChildCount()-1)target=list.getChildCount()-1;
            if(target>=0 && target!=current && target!=current+1){ list.removeView(dragged); if(target>list.getChildCount())target=list.getChildCount(); list.addView(dragged,target); }
            return true;
        }
        if(e.getAction()==DragEvent.ACTION_DROP){saveOrderFromViews(); if(dragged!=null)dragged.setAlpha(1f); dragged=null; render(); return true;}
        if(e.getAction()==DragEvent.ACTION_DRAG_ENDED){if(dragged!=null)dragged.setAlpha(1f); dragged=null; return true;}
        return true;
    }

    View makeCard(Achievement a,int index){
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(0,0,dp(5),0);
        GradientDrawable bgd=new GradientDrawable(); bgd.setColor(cardColor()); bgd.setCornerRadius(dp(16)); card.setBackground(bgd); card.setElevation(dp(3));

        FrameLayout visual=new FrameLayout(this);
        GradientDrawable side=new GradientDrawable(); side.setColor(panelColor()); side.setCornerRadius(dp(16)); visual.setBackground(side);
        if(a.imagePath!=null && !a.imagePath.isEmpty()){
            try{ ImageView im=new ImageView(this); im.setImageBitmap(BitmapFactory.decodeFile(a.imagePath)); im.setScaleType(ImageView.ScaleType.CENTER_CROP); visual.addView(im,new FrameLayout.LayoutParams(-1,-1)); }
            catch(Exception ignored){ addMedal(visual,a); }
        }else addMedal(visual,a);
        TextView grip=tv("☷",22,secondaryText()); grip.setGravity(Gravity.CENTER); visual.addView(grip,new FrameLayout.LayoutParams(dp(34),dp(34),Gravity.LEFT|Gravity.BOTTOM));
        card.addView(visual,new LinearLayout.LayoutParams(dp(112),dp(112)));

        LinearLayout text=new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL); text.setPadding(dp(12),dp(9),dp(4),dp(8));
        TextView title=tv(a.title,21,primaryText()); title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); title.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        TextView desc=tv(a.desc,16,secondaryText()); desc.setGravity(Gravity.RIGHT); desc.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        TextView status=tv(a.done?"Status: Completed":"Status: Not completed",13,a.done?CYAN:secondaryText()); status.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        text.addView(title,new LinearLayout.LayoutParams(-1,0,1)); text.addView(desc,new LinearLayout.LayoutParams(-1,0,1)); text.addView(status,new LinearLayout.LayoutParams(-1,dp(24)));
        card.addView(text,new LinearLayout.LayoutParams(0,dp(112),1));

        TextView done=tv(a.done?"✓":"○",24,a.done?CYAN:secondaryText()); done.setGravity(Gravity.CENTER); done.setContentDescription("Toggle completed");
        done.setOnClickListener(v->{a.done=!a.done;save();render();}); card.addView(done,new LinearLayout.LayoutParams(dp(44),dp(112)));

        card.setTag(a); card.setOnClickListener(v->showEditor(index));
        card.setOnLongClickListener(v->{ dragged=card; card.setAlpha(.45f); if(android.os.Build.VERSION.SDK_INT>=24)card.startDragAndDrop(null,new View.DragShadowBuilder(card),null,View.DRAG_FLAG_GLOBAL); else card.startDrag(null,new View.DragShadowBuilder(card),null,0); return true; });
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(112)); lp.setMargins(0,dp(7),0,dp(7)); card.setLayoutParams(lp); return card;
    }

    void addMedal(FrameLayout f,Achievement a){ TextView medal=tv(medalEmoji(a.medal),48,medalColor(a.medal)); medal.setGravity(Gravity.CENTER); f.addView(medal,new FrameLayout.LayoutParams(-1,-1)); }
    String medalEmoji(String m){ if("Bronze".equals(m))return "🥉"; if("Silver".equals(m))return "🥈"; if("Gold".equals(m))return "🥇"; return "🏆"; }
    int medalColor(String m){ if("Bronze".equals(m))return Color.rgb(205,127,50); if("Silver".equals(m))return Color.LTGRAY; if("Gold".equals(m))return Color.rgb(255,193,7); return CYAN; }

    void render(){
        if(list==null)return; list.removeAllViews(); for(int i=0;i<data.size();i++)list.addView(makeCard(data.get(i),i));
        int b=0,s=0,g=0,p=0; for(Achievement a:data)if(a.done){if(a.medal.equals("Bronze"))b++;else if(a.medal.equals("Silver"))s++;else if(a.medal.equals("Gold"))g++;else p++;}
        counts.setText("🥉 "+b+"    🥈 "+s+"    🥇 "+g+"    🏆 "+p);
    }

    void showEditor(final int index){
        boolean edit=index>=0; Achievement old=edit?data.get(index):new Achievement("","","Platinum",false);
        final String[] chosenImage={old.imagePath};
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(5),dp(18),0);
        EditText title=new EditText(this); title.setHint("Achievement title"); title.setText(old.title); title.setTextColor(primaryText()); title.setHintTextColor(Color.GRAY);
        EditText desc=new EditText(this); desc.setHint("Description"); desc.setText(old.desc); desc.setTextColor(primaryText()); desc.setHintTextColor(Color.GRAY); desc.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE); desc.setMinLines(2);
        Spinner spinner=new Spinner(this); String[] medals={"Bronze","Silver","Gold","Platinum"}; spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,medals)); for(int i=0;i<medals.length;i++)if(medals[i].equals(old.medal))spinner.setSelection(i);
        ImageView preview=new ImageView(this); preview.setScaleType(ImageView.ScaleType.CENTER_CROP); preview.setBackgroundColor(panelColor()); preview.setAdjustViewBounds(true); preview.setMinimumHeight(dp(100));
        if(chosenImage[0]!=null&&!chosenImage[0].isEmpty())preview.setImageBitmap(BitmapFactory.decodeFile(chosenImage[0])); else preview.setImageResource(android.R.drawable.ic_menu_gallery);
        Button imageBtn=new Button(this); imageBtn.setText(chosenImage[0].isEmpty()?"Add image":"Change image"); imageBtn.setOnClickListener(v->pickImage(chosenImage,preview,imageBtn));
        Button removeImage=new Button(this); removeImage.setText("Remove image"); removeImage.setOnClickListener(v->{chosenImage[0]=""; preview.setImageResource(android.R.drawable.ic_menu_gallery); imageBtn.setText("Add image");});
        box.addView(title); box.addView(desc); box.addView(spinner); box.addView(preview,new LinearLayout.LayoutParams(-1,dp(130))); box.addView(imageBtn); if(edit)box.addView(removeImage);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(edit?"Edit Achievement":"New Achievement").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        d.setOnShowListener(x->{d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String t=title.getText().toString().trim(); if(t.length()==0){title.setError("Enter a title");return;} old.title=t; old.desc=desc.getText().toString().trim(); old.medal=medals[spinner.getSelectedItemPosition()]; old.imagePath=chosenImage[0]; if(!edit)data.add(old); save();render();d.dismiss();}); if(edit){d.setButton(AlertDialog.BUTTON_NEUTRAL,"Delete",(di,w)->{}); d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{data.remove(index);save();render();d.dismiss();});}}); d.show();
    }

    void pickImage(String[] chosen,ImageView preview,Button btn){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,77);
        pendingImageHolder=chosen; pendingPreview=preview; pendingImageButton=btn;
    }
    String[] pendingImageHolder; ImageView pendingPreview; Button pendingImageButton;
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent dataIntent){super.onActivityResult(requestCode,resultCode,dataIntent); if(requestCode!=77||resultCode!=RESULT_OK||dataIntent==null)return; Uri uri=dataIntent.getData(); if(uri==null)return; try{Bitmap b=BitmapFactory.decodeStream(getContentResolver().openInputStream(uri)); if(b!=null)showCropDialog(b);}catch(Exception e){Toast.makeText(this,"Could not open image",Toast.LENGTH_SHORT).show();}}

    void showCropDialog(Bitmap source){
        final CropView crop=new CropView(this,source); LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(10),0,dp(10),0); box.addView(crop,new LinearLayout.LayoutParams(-1,dp(280)));
        TextView hint=tv("Crop amount",15,secondaryText()); hint.setGravity(Gravity.CENTER); box.addView(hint,new LinearLayout.LayoutParams(-1,dp(36)));
        SeekBar seek=new SeekBar(this); seek.setMax(70); seek.setProgress(35); seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){crop.setCropFraction(.25f+.7f*(p/70f));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}}); box.addView(seek);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Crop image").setMessage("Move the slider to choose how much of the image stays visible.").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Use image",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{Bitmap out=crop.getCroppedBitmap(); try{File f=new File(getFilesDir(),"ach_"+System.currentTimeMillis()+".jpg"); FileOutputStream os=new FileOutputStream(f); out.compress(Bitmap.CompressFormat.JPEG,90,os); os.close(); pendingImageHolder[0]=f.getAbsolutePath(); pendingPreview.setImageBitmap(out); pendingImageButton.setText("Change image"); d.dismiss();}catch(Exception e){Toast.makeText(this,"Could not save image",Toast.LENGTH_SHORT).show();}})); d.show();
    }

    class CropView extends View {
        Bitmap bmp; Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); float fraction=.6f; float downX,downY,offsetX,offsetY;
        CropView(Context c,Bitmap b){super(c);bmp=b;p.setFilterBitmap(true);}
        void setCropFraction(float f){fraction=f;invalidate();}
        protected void onDraw(Canvas c){super.onDraw(c); float scale=Math.max(getWidth()/(float)bmp.getWidth(),getHeight()/(float)bmp.getHeight()); float w=bmp.getWidth()*scale,h=bmp.getHeight()*scale; float left=(getWidth()-w)/2+offsetX,top=(getHeight()-h)/2+offsetY; c.drawBitmap(bmp,null,new RectF(left,top,left+w,top+h),p); float size=Math.min(getWidth(),getHeight())*fraction; float cx=getWidth()/2f+offsetX,cy=getHeight()/2f+offsetY; p.setColor(0x99000000);p.setStyle(Paint.Style.FILL); c.drawRect(0,0,getWidth(),Math.max(0,cy-size/2),p);c.drawRect(0,cy+size/2,getWidth(),getHeight(),p);c.drawRect(0,cy-size/2,Math.max(0,cx-size/2),cy+size/2,p);c.drawRect(cx+size/2,cy-size/2,getWidth(),cy+size/2,p);p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));c.drawRect(cx-size/2,cy-size/2,cx+size/2,cy+size/2,p);p.setStyle(Paint.Style.FILL); }
        public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){offsetX+=e.getX()-downX;offsetY+=e.getY()-downY;downX=e.getX();downY=e.getY();invalidate();return true;}return true;}
        Bitmap getCroppedBitmap(){float scale=Math.max(getWidth()/(float)bmp.getWidth(),getHeight()/(float)bmp.getHeight());float w=bmp.getWidth()*scale,h=bmp.getHeight()*scale;float left=(getWidth()-w)/2+offsetX,top=(getHeight()-h)/2+offsetY;float size=Math.min(getWidth(),getHeight())*fraction;float cx=getWidth()/2f+offsetX,cy=getHeight()/2f+offsetY;float sx=(cx-size/2-left)/scale,sy=(cy-size/2-top)/scale,ss=size/scale; sx=Math.max(0,Math.min(bmp.getWidth()-ss,sx));sy=Math.max(0,Math.min(bmp.getHeight()-ss,sy));Bitmap out=Bitmap.createBitmap(bmp,(int)sx,(int)sy,Math.max(1,(int)ss),Math.max(1,(int)ss));return out;}
    }

    void showMenu(View anchor){PopupMenu pm=new PopupMenu(this,anchor);pm.getMenu().add("Add Achievement");pm.getMenu().add("Reset all completion");pm.getMenu().add("Delete all");pm.setOnMenuItemClickListener(item->{String s=item.getTitle().toString();if(s.equals("Add Achievement"))showEditor(-1);else if(s.equals("Reset all completion")){for(Achievement a:data)a.done=false;save();render();}else if(s.equals("Delete all")){new AlertDialog.Builder(this).setTitle("Delete all?").setMessage("This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{data.clear();save();render();}).show();}return true;});pm.show();}

    void showSideMenu(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(18),dp(22),dp(12));box.setBackgroundColor(panelColor());
        TextView h=tv("My Achievements",24,primaryText());box.addView(h,new LinearLayout.LayoutParams(-1,dp(58)));
        Button settings=new Button(this);settings.setText("Settings");settings.setOnClickListener(v->{showSettings();});box.addView(settings);
        Button add=new Button(this);add.setText("Add Achievement");add.setOnClickListener(v->{showEditor(-1);});box.addView(add);
        Button close=new Button(this);close.setText("Close");close.setOnClickListener(v->((Dialog)v.getTag()).dismiss());box.addView(close);
        Dialog dialog=new Dialog(this);dialog.setContentView(box);Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(dp(310),-1);w.setGravity(Gravity.LEFT|Gravity.TOP);WindowManager.LayoutParams lp=w.getAttributes();lp.width=dp(310);lp.height=WindowManager.LayoutParams.MATCH_PARENT;w.setAttributes(lp);}close.setTag(dialog);dialog.show();
    }

    void showSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(5),dp(18),0);
        Button theme=new Button(this);theme.setText(isLight()?"Switch to Dark Mode":"Switch to Day Mode");theme.setOnClickListener(v->{prefs.edit().putBoolean("lightMode",!isLight()).apply();recreate();});box.addView(theme);
        Button pin=new Button(this);pin.setText(hasPin()?"Change / Remove PIN":"Set PIN");pin.setOnClickListener(v->showPinSettings());box.addView(pin);
        new AlertDialog.Builder(this).setTitle("Settings").setView(box).setNegativeButton("Close",null).show();
    }

    boolean hasPin(){String p=prefs.getString("pin","");return p.length()>0;}
    void showPinSettings(){
        if(hasPin()){
            new AlertDialog.Builder(this).setTitle("PIN").setItems(new String[]{"Change PIN","Remove PIN"},(d,w)->{if(w==0)askNewPin();else {prefs.edit().remove("pin").apply();Toast.makeText(this,"PIN removed",Toast.LENGTH_SHORT).show();}}).show();
        }else askNewPin();
    }
    void askNewPin(){
        final EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);input.setHint("4-8 digit PIN");
        new AlertDialog.Builder(this).setTitle("Set PIN").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String p=input.getText().toString();if(p.length()<4||p.length()>8){Toast.makeText(this,"PIN must be 4 to 8 digits",Toast.LENGTH_SHORT).show();return;}prefs.edit().putString("pin",p).apply();Toast.makeText(this,"PIN saved",Toast.LENGTH_SHORT).show();}).show();
    }
    void showPinGate(){
        final EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);input.setHint("PIN");
        final AlertDialog d=new AlertDialog.Builder(this).setTitle("Enter PIN").setMessage("Enter your PIN to open the app.").setView(input).setCancelable(false).setPositiveButton("Unlock",null).create();
        d.setOnShowListener(x->{d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(input.getText().toString().equals(prefs.getString("pin",""))){unlocked=true;d.dismiss();}else{input.setError("Wrong PIN");}});});d.show();
    }

    void saveOrderFromViews(){ArrayList<Achievement> n=new ArrayList<>();for(int i=0;i<list.getChildCount();i++){Object tag=list.getChildAt(i).getTag();if(tag instanceof Achievement)n.add((Achievement)tag);}if(n.size()==data.size())data=n;save();}
    void save(){JSONArray arr=new JSONArray();try{for(Achievement a:data){JSONObject o=new JSONObject();o.put("title",a.title);o.put("desc",a.desc);o.put("medal",a.medal);o.put("done",a.done);o.put("imagePath",a.imagePath);arr.put(o);}}catch(Exception ignored){}prefs.edit().putString("items",arr.toString()).apply();}
    void load(){String raw=prefs.getString("items","");if(raw.length()==0)return;try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);data.add(new Achievement(o.optString("title"),o.optString("desc"),o.optString("medal","Platinum"),o.optBoolean("done",false),o.optString("imagePath","")));}}catch(Exception ignored){}}
    int dp(int x){return(int)(x*getResources().getDisplayMetrics().density+.5f);}
}

package com.arian.myachievements;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import android.text.InputType;

import java.io.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    LinearLayout list;
    LinearLayout categoryBar;
    TextView counts, screenTitle;

    String currentFilter = "All";

    ArrayList<Achievement> data = new ArrayList<>();
    SharedPreferences prefs;

    View dragged;

    final int BG_DARK = Color.rgb(32,33,36);
    final int CARD_DARK = Color.rgb(61,65,70);
    final int DONE_DARK = Color.rgb(38,91,135);
    final int PANEL_DARK = Color.rgb(43,46,50);
    final int DONE_PANEL_DARK = Color.rgb(31,76,112);
    final int CYAN = Color.rgb(100,199,240);

    final int BG_LIGHT = Color.rgb(245,246,248);
    final int CARD_LIGHT = Color.WHITE;
    final int DONE_LIGHT = Color.rgb(211,239,216);
    final int PANEL_LIGHT = Color.rgb(232,234,238);
    final int DONE_PANEL_LIGHT = Color.rgb(191,226,198);
    final int GREEN = Color.rgb(45,145,75);

    // ---------------------------------------------------------
    // ACHIEVEMENT
    // ---------------------------------------------------------

    static class Achievement {
        String title;
        String desc;
        String medal;
        String imagePath;
        String imageShape;
        boolean done;
        boolean pinned;

        // New customization
        int customColor;
        String backgroundMode; // default / color / image

        Achievement(String t, String d, String m, boolean c) {
            this(t,d,m,c,null,"circle",false);
        }

        Achievement(String t, String d, String m, boolean c, String img) {
            this(t,d,m,c,img,"circle",false);
        }

        Achievement(String t, String d, String m, boolean c, String img, boolean pin) {
            this(t,d,m,c,img,"circle",pin);
        }

        Achievement(String t, String d, String m, boolean c,
                    String img, String shape, boolean pin) {

            title = t;
            desc = d;
            medal = m;
            done = c;
            imagePath = img == null ? "" : img;
            imageShape = shape == null ? "circle" : shape;
            pinned = pin;

            customColor = 0;
            backgroundMode = "default";
        }
    }

    // ---------------------------------------------------------
    // LIFECYCLE
    // ---------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("achievements", MODE_PRIVATE);

        if (prefs.getBoolean("lightMode", false)) {
            setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        } else {
            setTheme(android.R.style.Theme_Material_NoActionBar);
        }

        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(
                prefs.getBoolean("lightMode", false)
                        ? BG_LIGHT : BG_DARK
        );

        getWindow().setNavigationBarColor(
                prefs.getBoolean("lightMode", false)
                        ? BG_LIGHT : BG_DARK
        );

        load();
        buildUi();

        if (hasPin()) {
            showPinGate();
        }
    }

    // ---------------------------------------------------------
    // COLORS
    // ---------------------------------------------------------

    boolean isLight() {
        return prefs.getBoolean("lightMode", false);
    }

    int bg() {
        return isLight() ? BG_LIGHT : BG_DARK;
    }

    int cardColor(Achievement a) {

        if ("color".equals(a.backgroundMode) && a.customColor != 0) {
            return a.customColor;
        }

        if ("image".equals(a.backgroundMode) && a.imagePath != null
                && !a.imagePath.isEmpty()) {
            return Color.TRANSPARENT;
        }

        if (a.done) {
            return isLight() ? DONE_LIGHT : DONE_DARK;
        }

        return isLight() ? CARD_LIGHT : CARD_DARK;
    }

    int panelColor() {
        return isLight() ? PANEL_LIGHT : PANEL_DARK;
    }

    int visualColor(Achievement a) {

        if ("Platinum".equals(a.medal))
            return Color.rgb(55,85,130);

        if ("Gold".equals(a.medal))
            return Color.rgb(170,135,25);

        if ("Silver".equals(a.medal))
            return Color.rgb(145,150,158);

        return Color.rgb(135,85,55);
    }

    int primaryText() {
        return isLight() ? Color.rgb(25,25,28) : Color.WHITE;
    }

    int secondaryText() {
        return isLight() ? Color.rgb(85,88,95) : Color.rgb(210,212,216);
    }

    boolean isDarkColor(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        double brightness =
                (0.299 * r) +
                (0.587 * g) +
                (0.114 * b);

        return brightness < 145;
    }

    int achievementTextColor(Achievement a) {

        if ("color".equals(a.backgroundMode)
                && a.customColor != 0) {

            return isDarkColor(a.customColor)
                    ? Color.WHITE
                    : Color.rgb(25,25,25);
        }

        if ("image".equals(a.backgroundMode)) {
            return Color.WHITE;
        }

        return primaryText();
    }

    int achievementSecondaryColor(Achievement a) {

        if ("color".equals(a.backgroundMode)
                && a.customColor != 0) {

            return isDarkColor(a.customColor)
                    ? Color.rgb(235,235,235)
                    : Color.rgb(70,70,70);
        }

        if ("image".equals(a.backgroundMode)) {
            return Color.WHITE;
        }

        return secondaryText();
    }

    // ---------------------------------------------------------
    // UI
    // ---------------------------------------------------------

    void buildUi() {

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(bg());

        frame.setFitsSystemWindows(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8),dp(5),dp(8),0);

        frame.addView(root,
                new FrameLayout.LayoutParams(
                        -1,-1
                ));

        // TOP BAR
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView menu = tv("☰",24,true);
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v -> showSideMenu());
        addPressAnimation(menu);

        top.addView(menu,
                new LinearLayout.LayoutParams(dp(48),dp(48)));

        screenTitle = tv("My Achievements",21,true);
        screenTitle.setTextColor(primaryText());

        top.addView(screenTitle,
                new LinearLayout.LayoutParams(
                        0,dp(48),1
                ));

        TextView more = tv("⋮",27,true);
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(v -> showMenu());
        addPressAnimation(more);

        top.addView(more,
                new LinearLayout.LayoutParams(dp(48),dp(48)));

        root.addView(top);

        // COUNTS
        counts = tv("",14,true);
        counts.setGravity(Gravity.CENTER);
        counts.setPadding(0,0,0,dp(5));

        root.addView(counts,
                new LinearLayout.LayoutParams(
                        -1,dp(32)
                ));

        // CATEGORIES
        categoryBar = new LinearLayout(this);
        categoryBar.setGravity(Gravity.CENTER_VERTICAL);
        categoryBar.setPadding(0,0,0,dp(3));

        root.addView(categoryBar,
                new LinearLayout.LayoutParams(
                        -1,dp(48)
                ));

        buildCategoryBar();

        // LIST
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8),dp(2),dp(8),dp(78));

        scroll.addView(list);

        root.addView(scroll,
                new LinearLayout.LayoutParams(
                        -1,0,1
                ));

        // FAB
        TextView add = tv("+",30,true);
        add.setGravity(Gravity.CENTER);
        add.setTextColor(Color.WHITE);

        GradientDrawable addBg =
                rounded(GREEN,18);

        add.setBackground(addBg);
        add.setElevation(dp(8));

        add.setOnClickListener(v -> showEditor(-1));
        addPressAnimation(add);

        FrameLayout.LayoutParams fp =
                new FrameLayout.LayoutParams(
                        dp(62),dp(62),
                        Gravity.BOTTOM | Gravity.RIGHT
                );

        fp.setMargins(0,0,dp(18),dp(18));

        frame.addView(add,fp);

        setContentView(frame);

        render();
    }

    TextView tv(String text, float size, boolean bold) {

        TextView t = new TextView(this);

        t.setText(text);
        t.setTextSize(size);

        t.setTypeface(
                Typeface.create(
                        "sans-serif",
                        bold
                                ? Typeface.BOLD
                                : Typeface.NORMAL
                )
        );

        t.setTextColor(primaryText());

        return t;
    }

    GradientDrawable rounded(int color, float radius) {

        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp((int)radius));

        return g;
    }

    void addPressAnimation(View v) {

        v.setOnTouchListener((view,event) -> {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                view.animate()
                        .scaleX(.94f)
                        .scaleY(.94f)
                        .setDuration(80)
                        .start();

            } else if (
                    event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL
            ) {

                view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start();
            }

            return false;
        });
    }

    // ---------------------------------------------------------
    // CATEGORY BAR
    // ---------------------------------------------------------

    void buildCategoryBar() {

        categoryBar.removeAllViews();

        String[] names = {
                "All",
                "Platinum",
                "Gold",
                "Silver",
                "Bronze"
        };

        for (String name : names) {

            TextView b = tv(name,12,true);
            b.setGravity(Gravity.CENTER);

            b.setOnClickListener(v -> {

                currentFilter = name;
                render();

            });

            addPressAnimation(b);

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            0,dp(38),1
                    );

            lp.setMargins(dp(3),0,dp(3),0);

            categoryBar.addView(b,lp);
        }

        updateCategoryButtons();
    }

    void updateCategoryButtons() {

        if (categoryBar == null)
            return;

        String[] names = {
                "All",
                "Platinum",
                "Gold",
                "Silver",
                "Bronze"
        };

        for (int i=0;i<categoryBar.getChildCount();i++) {

            TextView b =
                    (TextView)categoryBar.getChildAt(i);

            String name = names[i];

            boolean selected =
                    name.equals(currentFilter);

            b.setTextColor(categoryTextColor(name));

            GradientDrawable g =
                    rounded(
                            selected
                                    ? (isLight()
                                    ? Color.rgb(215,218,224)
                                    : Color.rgb(78,82,88))
                                    : Color.TRANSPARENT,
                            11
                    );

            b.setBackground(g);
        }
    }

    int categoryTextColor(String name) {

        if ("Platinum".equals(name))
            return isLight()
                    ? Color.rgb(35,65,110)
                    : Color.WHITE;

        if ("Gold".equals(name))
            return Color.rgb(215,175,40);

        if ("Silver".equals(name))
            return Color.rgb(100,145,205);

        if ("Bronze".equals(name))
            return Color.rgb(165,100,65);

        return primaryText();
    }

    // ---------------------------------------------------------
    // CARD
    // ---------------------------------------------------------

    View makeCard(Achievement a, int index) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        card.setPadding(
                dp(3),dp(2),dp(3),dp(2)
        );

        // Custom / image background
        if ("image".equals(a.backgroundMode)
                && a.imagePath != null
                && !a.imagePath.isEmpty()) {

            Bitmap bm = decodeImage(a.imagePath);

            if (bm != null) {

                card.setBackground(
                        new AchievementBackgroundDrawable(
                                bm,
                                isLight()
                        )
                );

            } else {

                card.setBackground(
                        rounded(cardColor(a),14)
                );
            }

        } else {

            card.setBackground(
                    rounded(cardColor(a),14)
            );
        }

        card.setElevation(dp(2));

        // VISUAL
        FrameLayout visual =
                new FrameLayout(this);

        visual.setPadding(0,0,0,0);

        Bitmap image = null;

        if (a.imagePath != null
                && !a.imagePath.isEmpty()) {

            image = decodeImage(a.imagePath);
        }

        if (image != null) {

            ImageView iv =
                    new ImageView(this);

            iv.setImageBitmap(image);
            iv.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

            GradientDrawable imageBg =
                    new GradientDrawable();

            imageBg.setColor(Color.TRANSPARENT);

            int borderColor =
                    medalBorderColor(a.medal);

            imageBg.setStroke(dp(1),borderColor);

            if ("square".equals(a.imageShape)) {

                imageBg.setCornerRadius(dp(4));

            } else {

                imageBg.setShape(
                        GradientDrawable.OVAL
                );
            }

            iv.setBackground(imageBg);
            iv.setClipToOutline(true);

            FrameLayout.LayoutParams ip =
                    new FrameLayout.LayoutParams(
                            dp(64),dp(64),
                            Gravity.CENTER
                    );

            visual.addView(iv,ip);

        } else {

            TextView medal =
                    tv(medalEmoji(a.medal),31,false);

            medal.setGravity(Gravity.CENTER);

            visual.addView(
                    medal,
                    new FrameLayout.LayoutParams(
                            dp(64),dp(64),
                            Gravity.CENTER
                    )
            );
        }

        LinearLayout.LayoutParams visualLp =
                new LinearLayout.LayoutParams(
                        dp(68),dp(68)
                );

        card.addView(visual,visualLp);

        // TEXT
        LinearLayout texts =
                new LinearLayout(this);

        texts.setOrientation(
                LinearLayout.VERTICAL
        );

        texts.setGravity(Gravity.CENTER_VERTICAL);
        texts.setPadding(dp(5),0,dp(3),0);

        int textColor =
                achievementTextColor(a);

        int secondColor =
                achievementSecondaryColor(a);

        TextView title =
                tv(a.title,17,true);

        title.setTextColor(textColor);
        title.setMaxLines(2);
        title.setGravity(Gravity.RIGHT);
        title.setTextDirection(View.TEXT_DIRECTION_RTL);

        TextView desc =
                tv(a.desc,12.5f,false);

        desc.setTextColor(secondColor);
        desc.setMaxLines(2);
        desc.setGravity(Gravity.RIGHT);
        desc.setTextDirection(View.TEXT_DIRECTION_RTL);

        texts.addView(title,
                new LinearLayout.LayoutParams(
                        -1,0,1
                ));

        texts.addView(desc,
                new LinearLayout.LayoutParams(
                        -1,0,1
                ));

        card.addView(
                texts,
                new LinearLayout.LayoutParams(
                        0,dp(72),1
                )
        );

        // ACTIONS
        LinearLayout actions =
                new LinearLayout(this);

        actions.setOrientation(
                LinearLayout.VERTICAL
        );

        actions.setGravity(Gravity.CENTER);

        TextView done =
                tv(a.done ? "✓" : "○",21,true);

        done.setGravity(Gravity.CENTER);

        done.setTextColor(
                a.done
                        ? (isLight()
                        ? Color.rgb(25,125,65)
                        : Color.rgb(105,205,245))
                        : secondColor
        );

        done.setOnClickListener(v -> {

            a.done = !a.done;

            save();
            render();
        });

        addPressAnimation(done);

        TextView pin =
                tv(a.pinned ? "★" : "☆",20,true);

        pin.setGravity(Gravity.CENTER);

        pin.setTextColor(
                a.pinned
                        ? Color.rgb(255,205,50)
                        : secondColor
        );

        pin.setOnClickListener(v -> {

            a.pinned = !a.pinned;

            save();
            render();
        });

        addPressAnimation(pin);

        TextView delete =
                tv("🗑",16,false);

        delete.setGravity(Gravity.CENTER);

        delete.setOnClickListener(
                v -> confirmDelete(index)
        );

        addPressAnimation(delete);

        actions.addView(
                done,
                new LinearLayout.LayoutParams(
                        dp(30),0,1
                )
        );

        actions.addView(
                pin,
                new LinearLayout.LayoutParams(
                        dp(30),0,1
                )
        );

        actions.addView(
                delete,
                new LinearLayout.LayoutParams(
                        dp(30),0,1
                )
        );

        card.addView(actions,
                new LinearLayout.LayoutParams(
                        dp(32),dp(78)
                ));

        // DRAG GRIP
        TextView grip =
                tv("☷",18,true);

        grip.setGravity(Gravity.CENTER);

        grip.setTextColor(
                achievementSecondaryColor(a)
        );

        FrameLayout wrapper =
                new FrameLayout(this);

        wrapper.setBackgroundColor(
                Color.TRANSPARENT
        );

        // Instead of changing the structure of the card,
        // use a transparent overlay only for the grip.
        // The grip remains at the left edge.

        grip.setOnTouchListener((v,event) -> {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                dragged = card;

                ClipData data =
                        ClipData.newPlainText(
                                "achievement",
                                ""
                        );

                View.DragShadowBuilder shadow =
                        new View.DragShadowBuilder(card);

                card.startDragAndDrop(
                        data,
                        shadow,
                        card,
                        0
                );

                return true;
            }

            return false;
        });

        card.addView(
                grip,
                0,
                new LinearLayout.LayoutParams(
                        dp(24),dp(30)
                )
        );

        card.setOnDragListener(
                (v,event) -> handleDrag(v,event)
        );

        card.setOnClickListener(
                v -> showEditor(index)
        );

        card.setOnLongClickListener(v -> {

            dragged = card;

            ClipData data =
                    ClipData.newPlainText(
                            "achievement",
                            ""
                    );

            View.DragShadowBuilder shadow =
                    new View.DragShadowBuilder(card);

            card.startDragAndDrop(
                    data,
                    shadow,
                    card,
                    0
            );

            return true;
        });

        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(
                        -1,dp(94)
                );

        cp.setMargins(0,dp(2),0,dp(2));

        card.setLayoutParams(cp);

        return card;
    }

    int medalBorderColor(String medal) {

        if ("Platinum".equals(medal))
            return Color.rgb(35,70,125);

        if ("Gold".equals(medal))
            return Color.rgb(235,195,35);

        if ("Silver".equals(medal))
            return Color.WHITE;

        return Color.rgb(145,85,50);
    }

    // ---------------------------------------------------------
    // CUSTOM CARD BACKGROUND
    // ---------------------------------------------------------

    static class AchievementBackgroundDrawable
            extends Drawable {

        Bitmap bitmap;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boolean light;

        AchievementBackgroundDrawable(
                Bitmap b,
                boolean l
        ) {
            bitmap = b;
            light = l;

            paint.setFilterBitmap(true);
        }

        @Override
        public void draw(Canvas canvas) {

            Rect bounds = getBounds();

            if (bitmap == null)
                return;

            Path path = new Path();

            float radius = 14;

            path.addRoundRect(
                    new RectF(bounds),
                    radius,
                    radius,
                    Path.Direction.CW
            );

            canvas.save();
            canvas.clipPath(path);

            // CENTER CROP
            float scale =
                    Math.max(
                            bounds.width() /
                                    (float)bitmap.getWidth(),
                            bounds.height() /
                                    (float)bitmap.getHeight()
                    );

            float w =
                    bitmap.getWidth() * scale;

            float h =
                    bitmap.getHeight() * scale;

            float left =
                    bounds.left +
                            (bounds.width()-w)/2f;

            float top =
                    bounds.top +
                            (bounds.height()-h)/2f;

            RectF dst =
                    new RectF(
                            left,
                            top,
                            left+w,
                            top+h
                    );

            paint.setAlpha(255);

            canvas.drawBitmap(
                    bitmap,
                    null,
                    dst,
                    paint
            );

            // READABILITY OVERLAY
            paint.setColor(
                    Color.argb(
                            light ? 80 : 105,
                            0,0,0
                    )
            );

            canvas.drawRect(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    paint
            );

            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(
                ColorFilter filter
        ) {
            paint.setColorFilter(filter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    // ---------------------------------------------------------
    // DRAG / DROP
    // ---------------------------------------------------------

    boolean handleDrag(
            View v,
            DragEvent event
    ) {

        if (event.getAction()
                == DragEvent.ACTION_DROP) {

            finishDrag((View)v);

            return true;
        }

        if (event.getAction()
                == DragEvent.ACTION_DRAG_ENDED) {

            return true;
        }

        return true;
    }

    void finishDrag(View target) {

        if (dragged == null ||
                dragged == target)
            return;

        int from =
                list.indexOfChild(dragged);

        int to =
                list.indexOfChild(target);

        if (from < 0 || to < 0)
            return;

        View item = dragged;

        list.removeViewAt(from);

        if (from < to)
            to--;

        list.addView(item,to);

        saveOrderFromViews();

        dragged = null;
    }

    void saveOrderFromViews() {

        ArrayList<Achievement> reordered =
                new ArrayList<>();

        for (int i=0;i<list.getChildCount();i++) {

            View v =
                    list.getChildAt(i);

            Object tag = v.getTag();

            if (tag instanceof Achievement) {
                reordered.add(
                        (Achievement)tag
                );
            }
        }

        if (reordered.size() ==
                visibleAchievements().size()) {

            ArrayList<Achievement> result =
                    new ArrayList<>();

            HashSet<Achievement> visible =
                    new HashSet<>(
                            visibleAchievements()
                    );

            int visibleIndex = 0;

            for (Achievement a : data) {

                if (visible.contains(a)) {

                    result.add(
                            reordered.get(
                                    visibleIndex++
                            )
                    );

                } else {

                    result.add(a);
                }
            }

            data = result;

            save();
            render();
        }
    }

    // ---------------------------------------------------------
    // RENDER
    // ---------------------------------------------------------

    void render() {

        if (list == null)
            return;

        list.removeAllViews();

        ArrayList<Achievement> visible =
                visibleAchievements();

        for (int i=0;i<visible.size();i++) {

            Achievement a =
                    visible.get(i);

            int realIndex =
                    data.indexOf(a);

            View card =
                    makeCard(a,realIndex);

            card.setTag(a);

            list.addView(card);
        }

        int bronzeTotal=0;
        int bronzeDone=0;

        int silverTotal=0;
        int silverDone=0;

        int goldTotal=0;
        int goldDone=0;

        int platinumTotal=0;
        int platinumDone=0;

        for (Achievement a : data) {

            if ("Bronze".equals(a.medal)) {
                bronzeTotal++;
                if (a.done)
                    bronzeDone++;
            }

            else if ("Silver".equals(a.medal)) {
                silverTotal++;
                if (a.done)
                    silverDone++;
            }

            else if ("Gold".equals(a.medal)) {
                goldTotal++;
                if (a.done)
                    goldDone++;
            }

            else if ("Platinum".equals(a.medal)) {
                platinumTotal++;
                if (a.done)
                    platinumDone++;
            }
        }

        counts.setText(
                "🥉 " + bronzeTotal + " - " + bronzeDone +
                "    🥈 " + silverTotal + " - " + silverDone +
                "    🥇 " + goldTotal + " - " + goldDone +
                "    🏆 " + platinumTotal + " - " + platinumDone
        );

        updateCategoryButtons();
    }

    ArrayList<Achievement> visibleAchievements() {

        ArrayList<Achievement> result =
                new ArrayList<>();

        for (Achievement a : data) {

            if (!currentFilter.equals("All")
                    && !currentFilter.equals(a.medal))
                continue;

            result.add(a);
        }

        Collections.sort(
                result,
                (a,b) -> {

                    if (a.pinned && !b.pinned)
                        return -1;

                    if (!a.pinned && b.pinned)
                        return 1;

                    return Integer.compare(
                            data.indexOf(a),
                            data.indexOf(b)
                    );
                }
        );

        return result;
    }

    // ---------------------------------------------------------
    // EDITOR
    // ---------------------------------------------------------

    void showEditor(int index) {

        boolean editing = index >= 0;

        Achievement old =
                editing
                        ? data.get(index)
                        : new Achievement(
                                "",
                                "",
                                "Platinum",
                                false
                        );

        final String[] chosenImage = {
                old.imagePath
        };

        final String[] chosenShape = {
                old.imageShape
        };

        final int[] chosenColor = {
                old.customColor
        };

        final String[] chosenMode = {
                old.backgroundMode
        };

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(18),dp(5),dp(18),0
        );

        // CUSTOMIZATION SECTION
        TextView customizationTitle =
                tv("Customization",16,true);

        customizationTitle.setTextColor(
                primaryText()
        );

        customizationTitle.setPadding(
                0,dp(8),0,dp(6)
        );

        box.addView(customizationTitle);

        TextView modeText =
                tv(
                        customizationText(
                                chosenMode[0]
                        ),
                        13,
                        true
                );

        modeText.setTextColor(
                secondaryText()
        );

        box.addView(modeText);

        LinearLayout customButtons =
                new LinearLayout(this);

        customButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button defaultBtn =
                editorButton("Default");

        Button colorBtn =
                editorButton("Custom Color");

        Button imageBtn =
                editorButton("Image Background");

        customButtons.addView(
                defaultBtn,
                new LinearLayout.LayoutParams(
                        0,dp(44),1
                )
        );

        customButtons.addView(
                colorBtn,
                new LinearLayout.LayoutParams(
                        0,dp(44),1
                )
        );

        customButtons.addView(
                imageBtn,
                new LinearLayout.LayoutParams(
                        0,dp(44),1
                )
        );

        box.addView(customButtons);

        defaultBtn.setOnClickListener(v -> {

            chosenMode[0] = "default";

            modeText.setText(
                    customizationText(
                            chosenMode[0]
                    )
            );
        });

        colorBtn.setOnClickListener(v -> {

            showColorPickerDialog(
                    old,
                    chosenColor,
                    chosenMode,
                    modeText
            );
        });

        imageBtn.setOnClickListener(v -> {

            if (chosenImage[0] == null ||
                    chosenImage[0].isEmpty()) {

                Toast.makeText(
                        this,
                        "First add an image to this achievement.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            chosenMode[0] = "image";

            modeText.setText(
                    customizationText(
                            chosenMode[0]
                    )
            );
        });

        // TITLE
        EditText title =
                new EditText(this);

        title.setHint("Achievement title");
        title.setText(old.title);
        title.setTextSize(17);
        title.setTypeface(
                Typeface.create(
                        "sans-serif-medium",
                        Typeface.NORMAL
                )
        );

        title.setTextColor(primaryText());
        title.setHintTextColor(
                isLight()
                        ? Color.rgb(130,132,138)
                        : Color.rgb(160,163,168)
        );

        box.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,dp(54)
                )
        );

        // DESCRIPTION
        EditText desc =
                new EditText(this);

        desc.setHint("Description");
        desc.setText(old.desc);
        desc.setTextSize(13);
        desc.setGravity(
                Gravity.TOP | Gravity.RIGHT
        );

        desc.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        desc.setMinLines(2);

        desc.setTextColor(primaryText());
        desc.setHintTextColor(
                isLight()
                        ? Color.rgb(130,132,138)
                        : Color.rgb(160,163,168)
        );

        box.addView(
                desc,
                new LinearLayout.LayoutParams(
                        -1,dp(72)
                )
        );

        // MEDAL
        Spinner medalSpinner =
                new Spinner(this);

        String[] medals = {
                "Bronze",
                "Silver",
                "Gold",
                "Platinum"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        medals
                );

        medalSpinner.setAdapter(adapter);

        int selectedMedal = 0;

        for (int i=0;i<medals.length;i++) {

            if (medals[i].equals(old.medal)) {
                selectedMedal = i;
                break;
            }
        }

        medalSpinner.setSelection(selectedMedal);

        box.addView(
                medalSpinner,
                new LinearLayout.LayoutParams(
                        -1,dp(52)
                )
        );

        // SHAPE
        TextView shapeTitle =
                tv("Image Shape",14,true);

        shapeTitle.setPadding(
                0,dp(6),0,0
        );

        box.addView(shapeTitle);

        RadioGroup shapeGroup =
                new RadioGroup(this);

        shapeGroup.setOrientation(
                RadioGroup.HORIZONTAL
        );

        RadioButton circle =
                new RadioButton(this);

        circle.setText("Circle");
        circle.setTextColor(primaryText());

        RadioButton square =
                new RadioButton(this);

        square.setText("Square");
        square.setTextColor(primaryText());

        shapeGroup.addView(circle);
        shapeGroup.addView(square);

        if ("square".equals(old.imageShape))
            square.setChecked(true);
        else
            circle.setChecked(true);

        shapeGroup.setOnCheckedChangeListener(
                (group,checkedId) -> {

                    if (checkedId == square.getId())
                        chosenShape[0] = "square";
                    else
                        chosenShape[0] = "circle";
                }
        );

        box.addView(
                shapeGroup,
                new LinearLayout.LayoutParams(
                        -1,dp(48)
                )
        );

        // PIN
        CheckBox pin =
                new CheckBox(this);

        pin.setText("Pin this achievement");
        pin.setChecked(old.pinned);
        pin.setTextColor(primaryText());

        box.addView(pin);

        // PREVIEW
        ImageView preview =
                new ImageView(this);

        preview.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        preview.setBackgroundColor(
                panelColor()
        );

        LinearLayout.LayoutParams pp =
                new LinearLayout.LayoutParams(
                        -1,dp(120)
                );

        pp.setMargins(
                0,dp(5),0,dp(5)
        );

        box.addView(preview,pp);

        if (chosenImage[0] != null
                && !chosenImage[0].isEmpty()) {

            Bitmap bm =
                    decodeImage(chosenImage[0]);

            if (bm != null)
                preview.setImageBitmap(bm);
        }

        // IMAGE BUTTON
        Button imageButton =
                editorButton(
                        editing
                                ? "Change Image"
                                : "Add Image"
                );

        box.addView(
                imageButton,
                new LinearLayout.LayoutParams(
                        -1,dp(48)
                )
        );

        // REMOVE IMAGE
        Button removeImage =
                editorButton("Remove Image");

        box.addView(
                removeImage,
                new LinearLayout.LayoutParams(
                        -1,dp(44)
                )
        );

        imageButton.setOnClickListener(v -> {

            pendingImageHolder = chosenImage;
            pendingPreview = preview;
            pendingImageButton = imageButton;

            pickImage();
        });

        removeImage.setOnClickListener(v -> {

            chosenImage[0] = "";

            preview.setImageDrawable(null);

            Toast.makeText(
                    this,
                    "Image removed",
                    Toast.LENGTH_SHORT
            ).show();
        });

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                editing
                                        ? "Edit Achievement"
                                        : "New Achievement"
                        )
                        .setView(box)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Save",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            Button save =
                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    );

            save.setOnClickListener(v -> {

                String t =
                        title.getText()
                                .toString()
                                .trim();

                if (t.isEmpty()) {

                    title.setError(
                            "Enter a title"
                    );

                    return;
                }

                String selected =
                        medals[
                                medalSpinner
                                        .getSelectedItemPosition()
                        ];

                if (editing) {

                    old.title = t;
                    old.desc =
                            desc.getText()
                                    .toString();

                    old.medal = selected;
                    old.imagePath =
                            chosenImage[0];

                    old.imageShape =
                            chosenShape[0];

                    old.pinned =
                            pin.isChecked();

                    old.customColor =
                            chosenColor[0];

                    old.backgroundMode =
                            chosenMode[0];

                } else {

                    Achievement a =
                            new Achievement(
                                    t,
                                    desc.getText()
                                            .toString(),
                                    selected,
                                    false,
                                    chosenImage[0],
                                    chosenShape[0],
                                    pin.isChecked()
                            );

                    a.customColor =
                            chosenColor[0];

                    a.backgroundMode =
                            chosenMode[0];

                    data.add(a);
                }

                save();
                render();

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    Button editorButton(String text) {

        Button b = new Button(this);

        b.setText(text);
        b.setTextSize(12);
        b.setAllCaps(false);

        b.setTypeface(
                Typeface.create(
                        "sans-serif-medium",
                        Typeface.NORMAL
                )
        );

        b.setTextColor(primaryText());

        addPressAnimation(b);

        return b;
    }

    String customizationText(String mode) {

        if ("color".equals(mode))
            return "Custom color selected";

        if ("image".equals(mode))
            return "Achievement image selected";

        return "Default appearance";
    }

    // ---------------------------------------------------------
    // COLOR PICKER
    // ---------------------------------------------------------

    void showColorPickerDialog(
            Achievement old,
            int[] chosenColor,
            String[] chosenMode,
            TextView modeText
    ) {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(18),dp(10),dp(18),dp(8)
        );

        final View colorPreview =
                new View(this);

        int starting =
                chosenColor[0] != 0
                        ? chosenColor[0]
                        : Color.rgb(70,120,180);

        colorPreview.setBackground(
                rounded(starting,14)
        );

        box.addView(
                colorPreview,
                new LinearLayout.LayoutParams(
                        -1,dp(70)
                )
        );

        SeekBar red =
                new SeekBar(this);

        SeekBar green =
                new SeekBar(this);

        SeekBar blue =
                new SeekBar(this);

        red.setMax(255);
        green.setMax(255);
        blue.setMax(255);

        red.setProgress(Color.red(starting));
        green.setProgress(Color.green(starting));
        blue.setProgress(Color.blue(starting));

        TextView rLabel =
                tv("Red",13,true);

        TextView gLabel =
                tv("Green",13,true);

        TextView bLabel =
                tv("Blue",13,true);

        box.addView(rLabel);
        box.addView(red);

        box.addView(gLabel);
        box.addView(green);

        box.addView(bLabel);
        box.addView(blue);

        SeekBar.OnSeekBarChangeListener listener =
                new SeekBar.OnSeekBarChangeListener() {

                    void update() {

                        int color =
                                Color.rgb(
                                        red.getProgress(),
                                        green.getProgress(),
                                        blue.getProgress()
                                );

                        colorPreview.setBackground(
                                rounded(color,14)
                        );
                    }

                    @Override
                    public void onProgressChanged(
                            SeekBar s,
                            int p,
                            boolean f
                    ) {
                        update();
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar s
                    ) {}

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar s
                    ) {}
                };

        red.setOnSeekBarChangeListener(listener);
        green.setOnSeekBarChangeListener(listener);
        blue.setOnSeekBarChangeListener(listener);

        new AlertDialog.Builder(this)
                .setTitle("Custom Color")
                .setView(box)
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Apply",
                        (d,w) -> {

                            chosenColor[0] =
                                    Color.rgb(
                                            red.getProgress(),
                                            green.getProgress(),
                                            blue.getProgress()
                                    );

                            chosenMode[0] = "color";

                            modeText.setText(
                                    customizationText(
                                            chosenMode[0]
                                    )
                            );
                        }
                )
                .show();
    }

    // ---------------------------------------------------------
    // IMAGE PICKER
    // ---------------------------------------------------------

    String[] pendingImageHolder;
    ImageView pendingPreview;
    Button pendingImageButton;

    void pickImage() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.setType("image/*");

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        startActivityForResult(
                intent,
                77
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent dataIntent
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                dataIntent
        );

        if (requestCode != 77 ||
                resultCode != RESULT_OK ||
                dataIntent == null)
            return;

        Uri uri =
                dataIntent.getData();

        if (uri == null)
            return;

        try {

            InputStream in =
                    getContentResolver()
                            .openInputStream(uri);

            Bitmap bitmap =
                    BitmapFactory.decodeStream(in);

            if (in != null)
                in.close();

            if (bitmap == null) {
                Toast.makeText(
                        this,
                        "Could not load image",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            showCropDialog(bitmap);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open image",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    void showCropDialog(Bitmap source) {

        CropView crop =
                new CropView(this,source);

        SeekBar zoom =
                new SeekBar(this);

        zoom.setMax(100);
        zoom.setProgress(50);

        crop.zoomBar = zoom;

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(8),dp(5),dp(8),0
        );

        box.addView(
                crop,
                new LinearLayout.LayoutParams(
                        -1,dp(300)
                )
        );

        box.addView(
                zoom,
                new LinearLayout.LayoutParams(
                        -1,dp(48)
                )
        );

        new AlertDialog.Builder(this)
                .setTitle("Crop Image")
                .setView(box)
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Use Image",
                        (d,w) -> {

                            Bitmap result =
                                    crop.getCroppedBitmap();

                            String path =
                                    saveBitmap(result);

                            if (path != null) {

                                if (pendingImageHolder != null)
                                    pendingImageHolder[0] = path;

                                if (pendingPreview != null)
                                    pendingPreview
                                            .setImageBitmap(result);

                                if (pendingImageButton != null)
                                    pendingImageButton
                                            .setText("Change Image");
                            }
                        }
                )
                .show();
    }

    String saveBitmap(Bitmap bitmap) {

        if (bitmap == null)
            return null;

        File file =
                new File(
                        getFilesDir(),
                        "ach_" +
                                System.currentTimeMillis() +
                                ".jpg"
                );

        try {

            FileOutputStream out =
                    new FileOutputStream(file);

            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    92,
                    out
            );

            out.flush();
            out.close();

            return file.getAbsolutePath();

        } catch (Exception e) {

            return null;
        }
    }

    Bitmap decodeImage(String path) {

        try {

            if (path == null ||
                    path.isEmpty())
                return null;

            return BitmapFactory.decodeFile(path);

        } catch (Exception e) {

            return null;
        }
    }

    // ---------------------------------------------------------
    // CROP VIEW
    // ---------------------------------------------------------

    class CropView extends View {

        Bitmap bitmap;
        Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        float offsetX = 0;
        float offsetY = 0;

        float scale = 1f;

        float downX;
        float downY;

        SeekBar zoomBar;

        CropView(
                Context context,
                Bitmap b
        ) {

            super(context);

            bitmap = b;

            setBackgroundColor(Color.BLACK);

            setLayerType(
                    View.LAYER_TYPE_SOFTWARE,
                    null
            );
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            if (bitmap == null)
                return;

            float vw = getWidth();
            float vh = getHeight();

            float base =
                    Math.max(
                            vw / bitmap.getWidth(),
                            vh / bitmap.getHeight()
                    );

            float finalScale =
                    base * (0.7f + scale * 1.3f);

            float w =
                    bitmap.getWidth() *
                            finalScale;

            float h =
                    bitmap.getHeight() *
                            finalScale;

            float left =
                    (vw-w)/2f + offsetX;

            float top =
                    (vh-h)/2f + offsetY;

            RectF dst =
                    new RectF(
                            left,
                            top,
                            left+w,
                            top+h
                    );

            paint.setFilterBitmap(true);

            canvas.drawBitmap(
                    bitmap,
                    null,
                    dst,
                    paint
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(dp(2));

            paint.setColor(Color.WHITE);

            float size =
                    Math.min(vw,vh)-dp(24);

            float l =
                    (vw-size)/2f;

            float t =
                    (vh-size)/2f;

            canvas.drawRect(
                    l,t,l+size,t+size,
                    paint
            );

            paint.setStyle(
                    Paint.Style.FILL
            );
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (event.getAction()
                    == MotionEvent.ACTION_DOWN) {

                downX = event.getX();
                downY = event.getY();

                return true;
            }

            if (event.getAction()
                    == MotionEvent.ACTION_MOVE) {

                float dx =
                        event.getX()-downX;

                float dy =
                        event.getY()-downY;

                offsetX += dx;
                offsetY += dy;

                downX = event.getX();
                downY = event.getY();

                invalidate();

                return true;
            }

            return true;
        }

        Bitmap getCroppedBitmap() {

            float size =
                    Math.min(
                            getWidth(),
                            getHeight()
                    ) - dp(24);

            Bitmap result =
                    Bitmap.createBitmap(
                            (int)size,
                            (int)size,
                            Bitmap.Config.ARGB_8888
                    );

            Canvas canvas =
                    new Canvas(result);

            float vw = getWidth();
            float vh = getHeight();

            float base =
                    Math.max(
                            vw / bitmap.getWidth(),
                            vh / bitmap.getHeight()
                    );

            float finalScale =
                    base * (0.7f + scale * 1.3f);

            float w =
                    bitmap.getWidth() *
                            finalScale;

            float h =
                    bitmap.getHeight() *
                            finalScale;

            float left =
                    (vw-w)/2f + offsetX;

            float top =
                    (vh-h)/2f + offsetY;

            float cropLeft =
                    (vw-size)/2f;

            float cropTop =
                    (vh-size)/2f;

            float drawLeft =
                    left-cropLeft;

            float drawTop =
                    top-cropTop;

            paint.setFilterBitmap(true);

            canvas.drawBitmap(
                    bitmap,
                    null,
                    new RectF(
                            drawLeft,
                            drawTop,
                            drawLeft+w,
                            drawTop+h
                    ),
                    paint
            );

            return result;
        }
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    void confirmDelete(int index) {

        if (index < 0 ||
                index >= data.size())
            return;

        Achievement a =
                data.get(index);

        new AlertDialog.Builder(this)
                .setTitle("Delete Achievement?")
                .setMessage(
                        "Delete \"" +
                                a.title +
                                "\"?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Delete",
                        (d,w) ->
                                animateDelete(index)
                )
                .show();
    }

    void animateDelete(int index) {

        if (index < 0 ||
                index >= data.size())
            return;

        Achievement target =
                data.get(index);

        View card = null;

        for (int i=0;i<list.getChildCount();i++) {

            View v =
                    list.getChildAt(i);

            if (v.getTag() == target) {
                card = v;
                break;
            }
        }

        if (card == null) {

            data.remove(index);
            save();
            render();

            return;
        }

        card.animate()
                .alpha(0)
                .translationX(dp(60))
                .setDuration(180)
                .withEndAction(() -> {

                    data.remove(index);

                    save();
                    render();

                })
                .start();
    }

    // ---------------------------------------------------------
    // MENU
    // ---------------------------------------------------------

    void showMenu() {

        PopupWindow popup =
                new PopupWindow(
                        this
                );

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(8),dp(8),dp(8),dp(8)
        );

        box.setBackground(
                rounded(
                        isLight()
                                ? Color.WHITE
                                : Color.rgb(50,53,58),
                        16
                )
        );

        TextView add =
                rowButton("Add Achievement");

        TextView reset =
                rowButton("Reset completion");

        TextView deleteAll =
                rowButton("Delete all");

        box.addView(add);
        box.addView(reset);
        box.addView(deleteAll);

        add.setOnClickListener(v -> {

            popup.dismiss();
            showEditor(-1);

        });

        reset.setOnClickListener(v -> {

            popup.dismiss();

            for (Achievement a : data)
                a.done = false;

            save();
            render();
        });

        deleteAll.setOnClickListener(v -> {

            popup.dismiss();

            new AlertDialog.Builder(this)
                    .setTitle("Delete all achievements?")
                    .setMessage(
                            "This cannot be undone."
                    )
                    .setNegativeButton(
                            "Cancel",
                            null
                    )
                    .setPositiveButton(
                            "Delete all",
                            (d,w) -> {

                                data.clear();

                                save();
                                render();
                            }
                    )
                    .show();
        });

        popup.setContentView(box);

        popup.setWidth(dp(220));
        popup.setHeight(
                WindowManager.LayoutParams.WRAP_CONTENT
        );

        popup.setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );

        popup.setOutsideTouchable(true);
        popup.setFocusable(true);

        popup.setElevation(dp(12));

        popup.showAtLocation(
                findViewById(android.R.id.content),
                Gravity.TOP | Gravity.RIGHT,
                dp(12),
                dp(65)
        );
    }

    TextView rowButton(String text) {

        TextView b =
                tv(text,14,true);

        b.setGravity(
                Gravity.CENTER_VERTICAL
        );

        b.setPadding(
                dp(14),0,dp(14),0
        );

        b.setBackground(
                rounded(
                        Color.TRANSPARENT,
                        10
                )
        );

        addPressAnimation(b);

        return b;
    }

    // ---------------------------------------------------------
    // SIDE MENU
    // ---------------------------------------------------------

    void showSideMenu() {

        final PopupWindow popup =
                new PopupWindow(this);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(15),dp(25),dp(15),dp(20)
        );

        box.setBackground(
                rounded(
                        isLight()
                                ? Color.WHITE
                                : Color.rgb(45,48,53),
                        20
                )
        );

        TextView title =
                tv("My Achievements",20,true);

        title.setPadding(
                dp(8),dp(5),dp(8),dp(20)
        );

        box.addView(title);

        TextView add =
                rowButton("＋  Add Achievement");

        TextView settings =
                rowButton("⚙  Settings");

        TextView close =
                rowButton("×  Close");

        box.addView(add);
        box.addView(settings);
        box.addView(close);

        add.setOnClickListener(v -> {

            popup.dismiss();
            showEditor(-1);

        });

        settings.setOnClickListener(v -> {

            popup.dismiss();
            showSettings();

        });

        close.setOnClickListener(
                v -> popup.dismiss()
        );

        popup.setContentView(box);

        popup.setWidth(dp(280));
        popup.setHeight(
                WindowManager.LayoutParams.MATCH_PARENT
        );

        popup.setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );

        popup.setOutsideTouchable(true);
        popup.setFocusable(true);

        popup.setAnimationStyle(
                android.R.style.Animation_Dialog
        );

        popup.showAtLocation(
                findViewById(android.R.id.content),
                Gravity.LEFT | Gravity.TOP,
                dp(8),
                dp(8)
        );
    }

    // ---------------------------------------------------------
    // SETTINGS
    // ---------------------------------------------------------

    void showSettings() {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(18),dp(5),dp(18),0
        );

        Switch theme =
                new Switch(this);

        theme.setText("Day mode");
        theme.setTextColor(primaryText());
        theme.setChecked(isLight());

        box.addView(
                theme,
                new LinearLayout.LayoutParams(
                        -1,dp(55)
                )
        );

        Button pin =
                editorButton(
                        hasPin()
                                ? "Change PIN"
                                : "Set PIN"
                );

        box.addView(
                pin,
                new LinearLayout.LayoutParams(
                        -1,dp(50)
                )
        );

        Button removePin =
                editorButton("Remove PIN");

        box.addView(
                removePin,
                new LinearLayout.LayoutParams(
                        -1,dp(50)
                )
        );

        theme.setOnCheckedChangeListener(
                (button,checked) -> {

                    prefs.edit()
                            .putBoolean(
                                    "lightMode",
                                    checked
                            )
                            .apply();

                    recreate();
                }
        );

        pin.setOnClickListener(
                v -> showPinSetDialog()
        );

        removePin.setOnClickListener(v -> {

            prefs.edit()
                    .remove("pin")
                    .apply();

            Toast.makeText(
                    this,
                    "PIN removed",
                    Toast.LENGTH_SHORT
            ).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(box)
                .setPositiveButton(
                        "Close",
                        null
                )
                .show();
    }

    // ---------------------------------------------------------
    // PIN
    // ---------------------------------------------------------

    boolean hasPin() {

        String p =
                prefs.getString("pin","");

        return p != null &&
                !p.isEmpty();
    }

    void showPinSetDialog() {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(18),dp(5),dp(18),0
        );

        EditText pin =
                new EditText(this);

        pin.setHint("4 digit PIN");
        pin.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

       pin.setFilters(
        new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(4)
        }
);

        box.addView(
                pin,
                new LinearLayout.LayoutParams(
                        -1,dp(55)
                )
        );

        new AlertDialog.Builder(this)
                .setTitle("Set PIN")
                .setView(box)
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Save",
                        (d,w) -> {

                            String value =
                                    pin.getText()
                                            .toString();

                            if (value.length() != 4) {

                                Toast.makeText(
                                        this,
                                        "PIN must contain 4 digits.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            prefs.edit()
                                    .putString(
                                            "pin",
                                            value
                                    )
                                    .apply();

                            Toast.makeText(
                                    this,
                                    "PIN saved.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .show();
    }

    void showPinGate() {

        final Dialog dialog =
                new Dialog(this);

        dialog.setCancelable(false);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setGravity(Gravity.CENTER);
        box.setPadding(
                dp(30),dp(35),dp(30),dp(35)
        );

        box.setBackground(
                rounded(bg(),20)
        );

        TextView title =
                tv("🔒",38,true);

        title.setGravity(Gravity.CENTER);

        box.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,dp(65)
                )
        );

        TextView text =
                tv("Enter your PIN",18,true);

        text.setGravity(Gravity.CENTER);

        box.addView(
                text,
                new LinearLayout.LayoutParams(
                        -1,dp(50)
                )
        );

        EditText input =
                new EditText(this);

        input.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        input.setGravity(Gravity.CENTER);
        input.setTextSize(24);
       input.setFilters(
        new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(4)
        }
);

        box.addView(
                input,
                new LinearLayout.LayoutParams(
                        -1,dp(60)
                )
        );

        LinearLayout keypad =
                new LinearLayout(this);

        keypad.setOrientation(
                LinearLayout.VERTICAL
        );

        String[][] keys = {
                {"1","2","3"},
                {"4","5","6"},
                {"7","8","9"},
                {"⌫","0","✓"}
        };

        for (String[] row : keys) {

            LinearLayout line =
                    new LinearLayout(this);

            for (String key : row) {

                Button b =
                        new Button(this);

                b.setText(key);
                b.setTextSize(19);
                b.setAllCaps(false);

                line.addView(
                        b,
                        new LinearLayout.LayoutParams(
                                0,dp(58),1
                        )
                );

                b.setOnClickListener(v -> {

                    String k =
                            ((Button)v)
                                    .getText()
                                    .toString();

                    if ("⌫".equals(k)) {

                        String s =
                                input.getText()
                                        .toString();

                        if (!s.isEmpty()) {

                            input.setText(
                                    s.substring(
                                            0,
                                            s.length()-1
                                    )
                            );
                        }

                    } else if ("✓".equals(k)) {

                        checkPin(
                                input,
                                dialog
                        );

                    } else {

                        if (input.length() < 4)
                            input.append(k);
                    }
                });

                addPressAnimation(b);
            }

            keypad.addView(line);
        }

        box.addView(
                keypad,
                new LinearLayout.LayoutParams(
                        -1,dp(235)
                )
        );

        dialog.setContentView(box);

        Window w =
                dialog.getWindow();

        if (w != null) {

            w.setBackgroundDrawable(
                    new ColorDrawable(
                            Color.TRANSPARENT
                    )
            );

            w.setLayout(
                    dp(330),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }

        dialog.show();

        if (dialog.getWindow() != null) {

            dialog.getWindow()
                    .setLayout(
                            dp(330),
                            WindowManager.LayoutParams.WRAP_CONTENT
                    );
        }
    }

    void checkPin(
            EditText input,
            Dialog dialog
    ) {

        String saved =
                prefs.getString("pin","");

        if (saved.equals(
                input.getText().toString()
        )) {

            dialog.dismiss();

        } else {

            input.setText("");

            Toast.makeText(
                    this,
                    "Wrong PIN",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ---------------------------------------------------------
    // PERSISTENCE
    // ---------------------------------------------------------

    void save() {

        JSONArray arr =
                new JSONArray();

        try {

            for (Achievement a : data) {

                JSONObject o =
                        new JSONObject();

                o.put("title",a.title);
                o.put("desc",a.desc);
                o.put("medal",a.medal);
                o.put("done",a.done);
                o.put("imagePath",
                        a.imagePath == null
                                ? ""
                                : a.imagePath
                );

                o.put("imageShape",
                        a.imageShape == null
                                ? "circle"
                                : a.imageShape
                );

                o.put("pinned",a.pinned);

                // NEW
                o.put(
                        "customColor",
                        a.customColor
                );

                o.put(
                        "backgroundMode",
                        a.backgroundMode == null
                                ? "default"
                                : a.backgroundMode
                );

                arr.put(o);
            }

            prefs.edit()
                    .putString(
                            "data",
                            arr.toString()
                    )
                    .apply();

        } catch (Exception ignored) {}
    }

    void load() {

        data.clear();

        String raw =
                prefs.getString(
                        "data",
                        ""
                );

        if (raw.isEmpty())
            return;

        try {

            JSONArray arr =
                    new JSONArray(raw);

            for (int i=0;
                 i<arr.length();
                 i++) {

                JSONObject o =
                        arr.getJSONObject(i);

                Achievement a =
                        new Achievement(
                                o.optString(
                                        "title",
                                        ""
                                ),
                                o.optString(
                                        "desc",
                                        ""
                                ),
                                o.optString(
                                        "medal",
                                        "Bronze"
                                ),
                                o.optBoolean(
                                        "done",
                                        false
                                ),
                                o.optString(
                                        "imagePath",
                                        ""
                                ),
                                o.optString(
                                        "imageShape",
                                        "circle"
                                ),
                                o.optBoolean(
                                        "pinned",
                                        false
                                )
                        );

                // Backward compatible
                a.customColor =
                        o.optInt(
                                "customColor",
                                0
                        );

                a.backgroundMode =
                        o.optString(
                                "backgroundMode",
                                "default"
                        );

                data.add(a);
            }

        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    String medalEmoji(String medal) {

        if ("Platinum".equals(medal))
            return "🏆";

        if ("Gold".equals(medal))
            return "🥇";

        if ("Silver".equals(medal))
            return "🥈";

        return "🥉";
    }

    int dp(int value) {

        return (int)(
                value *
                getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}

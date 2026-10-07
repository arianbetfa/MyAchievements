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

    /*
     * PIN re-lock state
     */
    boolean appStartedOnce = false;
    boolean wasInBackground = false;
    boolean pinGateShowing = false;
    boolean imagePickerActive = false;

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


    static class Achievement {

        String title;
        String desc;
        String medal;
        String imagePath;
        String imageShape;

        boolean done;
        boolean pinned;

        int customColor;
        String backgroundMode;

        Achievement(
                String t,
                String d,
                String m,
                boolean c
        ) {
            this(
                    t,
                    d,
                    m,
                    c,
                    "",
                    "circle",
                    false
            );
        }

        Achievement(
                String t,
                String d,
                String m,
                boolean c,
                String img
        ) {
            this(
                    t,
                    d,
                    m,
                    c,
                    img,
                    "circle",
                    false
            );
        }

        Achievement(
                String t,
                String d,
                String m,
                boolean c,
                String img,
                boolean pin
        ) {
            this(
                    t,
                    d,
                    m,
                    c,
                    img,
                    "circle",
                    pin
            );
        }

        Achievement(
                String t,
                String d,
                String m,
                boolean c,
                String img,
                String shape,
                boolean pin
        ) {
            title = t;
            desc = d;
            medal = m;
            done = c;

            imagePath =
                    img == null
                            ? ""
                            : img;

            imageShape =
                    shape == null
                            ? "circle"
                            : shape;

            pinned = pin;

            customColor = Color.TRANSPARENT;
            backgroundMode = "default";
        }
    }


    @Override
    public void onCreate(Bundle b) {

        prefs =
                getSharedPreferences(
                        "achievements",
                        MODE_PRIVATE
                );

        setTheme(
                prefs.getBoolean(
                        "lightMode",
                        false
                )
                        ? R.style.AppTheme_Light
                        : R.style.AppTheme
        );

        super.onCreate(b);

        getWindow().setStatusBarColor(
                isLight()
                        ? BG_LIGHT
                        : BG_DARK
        );

        getWindow().setNavigationBarColor(
                isLight()
                        ? Color.rgb(
                                230,
                                232,
                                235
                        )
                        : Color.rgb(
                                23,
                                24,
                                26
                        )
        );

        load();
        buildUi();

        /*
         * First launch / first entry.
         */
        if (hasPin()) {
            showPinGate();
        }

        appStartedOnce = true;
    }


    @Override
    protected void onStart() {

        super.onStart();

        /*
         * When the Activity comes back after the user
         * left the app, require the PIN again.
         *
         * This does NOT affect the first launch because
         * onCreate() already handles that.
         */
        if (
                appStartedOnce
                        && wasInBackground
                        && hasPin()
                        && !pinGateShowing
        ) {

            showPinGate();
        }

        wasInBackground = false;
    }


    @Override
    protected void onStop() {

        super.onStop();

        /*
         * The Activity has gone to the background.
         * Returning to it will require the PIN.
         */
        if (!isFinishing() && !imagePickerActive) {
            wasInBackground = true;
        }
    }


    boolean isLight() {

        return prefs.getBoolean(
                "lightMode",
                false
        );
    }


    int bg() {

        return isLight()
                ? BG_LIGHT
                : BG_DARK;
    }


    int cardColor(Achievement a) {

        if (
                a != null
                        && "color".equals(
                                a.backgroundMode
                        )
                        && a.customColor != Color.TRANSPARENT
        ) {
            return a.customColor;
        }

        if (
                a != null
                        && "image".equals(
                                a.backgroundMode
                        )
                        && a.imagePath != null
                        && !a.imagePath.isEmpty()
        ) {
            return Color.TRANSPARENT;
        }

        return a.done
                ? (
                    isLight()
                            ? DONE_LIGHT
                            : DONE_DARK
                )
                : (
                    isLight()
                            ? CARD_LIGHT
                            : CARD_DARK
                );
    }


    int panelColor() {

        return isLight()
                ? PANEL_LIGHT
                : PANEL_DARK;
    }


    int visualColor(Achievement a) {

        return a.done
                ? (
                    isLight()
                            ? DONE_PANEL_LIGHT
                            : DONE_PANEL_DARK
                )
                : panelColor();
    }


    int primaryText() {

        return isLight()
                ? Color.rgb(
                        30,
                        31,
                        34
                )
                : Color.WHITE;
    }


    int secondaryText() {

        return isLight()
                ? Color.rgb(
                        85,
                        87,
                        92
                )
                : Color.LTGRAY;
    }


    int achievementPrimaryText(Achievement a) {

        if (
                a != null
                        && (
                            "image".equals(
                                    a.backgroundMode
                            )
                                    || (
                                        "color".equals(
                                                a.backgroundMode
                                        )
                                                && isDarkColor(
                                                        a.customColor
                                                )
                                    )
                        )
        ) {
            return Color.WHITE;
        }

        if (
                a.done
                        && !isLight()
        ) {
            return Color.WHITE;
        }

        return primaryText();
    }


    int achievementSecondaryText(
            Achievement a
    ) {

        if (
                a != null
                        && (
                            "image".equals(
                                    a.backgroundMode
                            )
                                    || (
                                        "color".equals(
                                                a.backgroundMode
                                        )
                                                && isDarkColor(
                                                        a.customColor
                                                )
                                    )
                        )
        ) {
            return Color.rgb(
                    235,
                    240,
                    245
            );
        }

        if (
                a.done
                        && !isLight()
        ) {
            return Color.rgb(
                    225,
                    240,
                    250
            );
        }

        return secondaryText();
    }


    boolean isDarkColor(
            int color
    ) {

        if (color == Color.TRANSPARENT)
            return false;

        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        double brightness =
                (
                        0.299 * r
                                + 0.587 * g
                                + 0.114 * b
                );

        return brightness < 145;
    }


    void buildUi() {

        FrameLayout frame =
                new FrameLayout(this);

        frame.setBackgroundColor(bg());

        frame.setOnApplyWindowInsetsListener(
                (v, insets) -> {

                    v.setPadding(
                            0,
                            insets.getSystemWindowInsetTop(),
                            0,
                            insets.getSystemWindowInsetBottom()
                    );

                    return insets;
                }
        );

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(bg());


        /*
         * TOP BAR
         */
        LinearLayout top =
                new LinearLayout(this);

        top.setGravity(
                Gravity.CENTER_VERTICAL
        );

        top.setPadding(
                dp(10),
                dp(5),
                dp(8),
                dp(3)
        );

        TextView menu =
                tv(
                        "☰",
                        30,
                        primaryText()
                );

        menu.setGravity(
                Gravity.CENTER
        );

        addPressAnimation(menu);

        menu.setOnClickListener(
                v -> showSideMenu()
        );

        top.addView(
                menu,
                new LinearLayout.LayoutParams(
                        dp(50),
                        dp(50)
                )
        );

        screenTitle =
                tv(
                        "All Achievements",
                        27,
                        primaryText()
                );

        screenTitle.setGravity(
                Gravity.CENTER_VERTICAL
        );

        top.addView(
                screenTitle,
                new LinearLayout.LayoutParams(
                        0,
                        dp(50),
                        1
                )
        );

        TextView more =
                tv(
                        "⋮",
                        32,
                        primaryText()
                );

        more.setGravity(
                Gravity.CENTER
        );

        addPressAnimation(more);

        more.setOnClickListener(
                v -> showMenu(more)
        );

        top.addView(
                more,
                new LinearLayout.LayoutParams(
                        dp(45),
                        dp(50)
                )
        );

        root.addView(top);


        /*
         * COUNTS
         */
        counts =
                tv(
                        "",
                        17,
                        primaryText()
                );

        counts.setGravity(
                Gravity.CENTER
        );

        counts.setPadding(
                0,
                0,
                0,
                dp(10)
        );

        root.addView(counts);


        buildCategoryBar(root);


        /*
         * LIST
         */
        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.setBackgroundColor(bg());

        list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        list.setPadding(
                dp(8),
                dp(1),
                dp(8),
                dp(72)
        );

        list.setOnDragListener(
                (v, e) -> handleDrag(e)
        );

        scroll.addView(list);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        frame.addView(
                root,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );


        /*
         * ADD BUTTON
         */
        TextView add =
                tv(
                        "+",
                        34,
                        Color.WHITE
                );

        add.setGravity(
                Gravity.CENTER
        );

        GradientDrawable fab =
                new GradientDrawable();

        fab.setColor(CYAN);

        fab.setShape(
                GradientDrawable.OVAL
        );

        add.setBackground(fab);

        add.setElevation(dp(8));

        addPressAnimation(add);

        add.setOnClickListener(
                v -> showEditor(-1)
        );

        FrameLayout.LayoutParams fp =
                new FrameLayout.LayoutParams(
                        dp(62),
                        dp(62),
                        Gravity.RIGHT
                                | Gravity.BOTTOM
                );

        fp.setMargins(
                0,
                0,
                dp(18),
                dp(16)
        );

        frame.addView(add, fp);

        setContentView(frame);

        render();
    }


    void buildCategoryBar(
            LinearLayout root
    ) {

        HorizontalScrollView hsv =
                new HorizontalScrollView(this);

        hsv.setHorizontalScrollBarEnabled(false);

        categoryBar =
                new LinearLayout(this);

        categoryBar.setGravity(
                Gravity.CENTER_VERTICAL
        );

        categoryBar.setPadding(
                dp(8),
                0,
                dp(8),
                dp(7)
        );

        String[] cats = {
                "All",
                "Platinum",
                "Gold",
                "Silver",
                "Bronze"
        };

        for (String c : cats) {

            TextView b =
                    tv(
                            c,
                            14,
                            categoryTextColor(c)
                    );

            b.setGravity(
                    Gravity.CENTER
            );

            b.setTypeface(
                    android.graphics.Typeface.create(
                            "sans-serif-medium",
                            android.graphics.Typeface.NORMAL
                    )
            );

            b.setPadding(
                    dp(12),
                    0,
                    dp(12),
                    0
            );

            b.setTag(c);

            addPressAnimation(b);

            b.setOnClickListener(
                    v -> {

                        currentFilter =
                                (String) v.getTag();

                        updateCategoryButtons();

                        render();
                    }
            );

            LinearLayout.LayoutParams bp =
                    new LinearLayout.LayoutParams(
                            dp(98),
                            dp(38)
                    );

            bp.setMargins(
                    dp(5),
                    0,
                    dp(5),
                    0
            );

            categoryBar.addView(
                    b,
                    bp
            );
        }

        hsv.addView(
                categoryBar,
                new HorizontalScrollView.LayoutParams(
                        -2,
                        dp(45)
                )
        );

        root.addView(
                hsv,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(45)
                )
        );

        updateCategoryButtons();
    }


    void updateCategoryButtons() {

        if (categoryBar == null)
            return;

        for (
                int i = 0;
                i < categoryBar.getChildCount();
                i++
        ) {

            TextView b =
                    (TextView)
                            categoryBar.getChildAt(i);

            String c =
                    (String) b.getTag();

            boolean sel =
                    c.equals(currentFilter);

            GradientDrawable d =
                    new GradientDrawable();

            d.setCornerRadius(dp(19));

            d.setColor(
                    sel
                            ? (
                                isLight()
                                        ? Color.rgb(
                                                235,
                                                238,
                                                242
                                        )
                                        : Color.rgb(
                                                53,
                                                56,
                                                61
                                        )
                            )
                            : (
                                isLight()
                                        ? Color.WHITE
                                        : CARD_DARK
                            )
            );

            b.setBackground(d);

            b.setTextColor(
                    categoryTextColor(c)
            );
        }

        if (screenTitle != null) {

            screenTitle.setText(
                    currentFilter.equals("All")
                            ? "All Achievements"
                            : currentFilter
            );
        }
    }


    int categoryTextColor(
            String c
    ) {

        if ("Platinum".equals(c))
            return isLight()
                    ? Color.rgb(
                            45,
                            47,
                            52
                    )
                    : Color.WHITE;

        if ("Gold".equals(c))
            return Color.rgb(
                    255,
                    193,
                    7
            );

        if ("Silver".equals(c))
            return Color.rgb(
                    90,
                    165,
                    235
            );

        if ("Bronze".equals(c))
            return Color.rgb(
                    181,
                    112,
                    55
            );

        return primaryText();
    }


    TextView tv(
            String s,
            float size,
            int color
    ) {

        TextView t =
                new TextView(this);

        t.setText(s);

        t.setTextSize(size);

        t.setTextColor(color);

        t.setGravity(
                Gravity.CENTER_VERTICAL
        );

        t.setTypeface(
                android.graphics.Typeface.create(
                        "sans-serif",
                        android.graphics.Typeface.NORMAL
                )
        );

        return t;
    }


    void styleEditorText(
            EditText e
    ) {

        e.setTextSize(16);

        e.setTextColor(
                primaryText()
        );

        e.setHintTextColor(
                Color.GRAY
        );

        e.setTypeface(
                android.graphics.Typeface.create(
                        "sans-serif",
                        android.graphics.Typeface.NORMAL
                )
        );

        e.setPadding(
                dp(4),
                0,
                dp(4),
                0
        );
    }


    void addPressAnimation(
            View v
    ) {

        v.setOnTouchListener(
                (view, event) -> {

                    if (
                            event.getAction()
                                    == MotionEvent.ACTION_DOWN
                    ) {

                        view.animate()
                                .scaleX(.94f)
                                .scaleY(.94f)
                                .setDuration(80)
                                .start();

                    } else if (
                            event.getAction()
                                    == MotionEvent.ACTION_UP
                                    ||
                            event.getAction()
                                    == MotionEvent.ACTION_CANCEL
                    ) {

                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .start();
                    }

                    return false;
                }
        );
    }


    boolean handleDrag(
            DragEvent e
    ) {

        if (
                e.getAction()
                        == DragEvent.ACTION_DRAG_STARTED
        )
            return true;

        if (
                e.getAction()
                        == DragEvent.ACTION_DRAG_LOCATION
                        && dragged != null
        ) {

            float y = e.getY();

            int target =
                    list.getChildCount();

            for (
                    int i = 0;
                    i < list.getChildCount();
                    i++
            ) {

                View c =
                        list.getChildAt(i);

                if (c == dragged)
                    continue;

                if (
                        y <
                                c.getTop()
                                        + c.getHeight() / 2f
                ) {

                    target = i;
                    break;
                }
            }

            if (
                    target >
                            list.getChildCount() - 1
            )
                target =
                        list.getChildCount() - 1;

            int current =
                    list.indexOfChild(dragged);

            if (
                    target >= 0
                            && target != current
                            && target != current + 1
            ) {

                list.removeView(dragged);

                if (
                        target >
                                list.getChildCount()
                )
                    target =
                            list.getChildCount();

                list.addView(
                        dragged,
                        target
                );

                for (
                        int i = 0;
                        i < list.getChildCount();
                        i++
                ) {

                    View c =
                            list.getChildAt(i);

                    c.setTranslationY(
                            i == target
                                    ? dp(2)
                                    : 0
                    );
                }
            }

            return true;
        }

        if (
                e.getAction()
                        == DragEvent.ACTION_DROP
        ) {

            saveOrderFromViews();

            finishDrag();

            render();

            Toast.makeText(
                    this,
                    "Order saved",
                    Toast.LENGTH_SHORT
            ).show();

            return true;
        }

        if (
                e.getAction()
                        == DragEvent.ACTION_DRAG_ENDED
        ) {

            finishDrag();

            return true;
        }

        return true;
    }


    void finishDrag() {

        if (dragged != null) {

            dragged.setAlpha(1f);

            dragged.setScaleX(1f);
            dragged.setScaleY(1f);

            dragged.setTranslationY(0);

            dragged.setElevation(
                    dp(3)
            );
        }

        for (
                int i = 0;
                list != null
                        && i < list.getChildCount();
                i++
        ) {

            list.getChildAt(i)
                    .setTranslationY(0);
        }

        dragged = null;
    }


    View makeCard(
            Achievement a,
            int index
    ) {

        final LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                dp(3),
                dp(2),
                dp(3),
                dp(2)
        );


        if (
                "image".equals(
                        a.backgroundMode
                )
                        && a.imagePath != null
                        && !a.imagePath.isEmpty()
        ) {

            try {

                Bitmap background =
                        BitmapFactory.decodeFile(
                                a.imagePath
                        );

                if (background != null) {

                    card.setBackground(
                            new AchievementBackgroundDrawable(
                                    background
                            )
                    );

                } else {

                    setCardSolidBackground(
                            card,
                            a
                    );
                }

            } catch (Exception e) {

                setCardSolidBackground(
                        card,
                        a
                );
            }

        } else {

            setCardSolidBackground(
                    card,
                    a
            );
        }

        card.setElevation(
                dp(2)
        );


        /*
         * VISUAL
         */
        FrameLayout visual =
                new FrameLayout(this);

        GradientDrawable visualBg =
                new GradientDrawable();

        boolean hasImage =
                a.imagePath != null
                        && !a.imagePath.isEmpty();

        if (hasImage) {

            visualBg.setColor(
                    Color.TRANSPARENT
            );

            visualBg.setCornerRadius(0);

        } else {

            visualBg.setColor(
                    visualColor(a)
            );

            visualBg.setCornerRadius(
                    dp(12)
            );
        }

        visual.setBackground(
                visualBg
        );


        /*
         * IMAGE
         */
        if (hasImage) {

            try {

                ImageView im =
                        new ImageView(this);

                Bitmap bm =
                        BitmapFactory.decodeFile(
                                a.imagePath
                        );

                if (bm != null) {

                    im.setImageBitmap(bm);

                    im.setScaleType(
                            ImageView.ScaleType.CENTER_CROP
                    );

                    GradientDrawable imageBg =
                            new GradientDrawable();

                    imageBg.setColor(
                            Color.TRANSPARENT
                    );

                    if (
                            "square".equals(
                                    a.imageShape
                            )
                    ) {

                        imageBg.setShape(
                                GradientDrawable.RECTANGLE
                        );

                        imageBg.setCornerRadius(
                                dp(3)
                        );

                    } else {

                        imageBg.setShape(
                                GradientDrawable.OVAL
                        );
                    }

                    imageBg.setStroke(
                            dp(1),
                            medalBorderColor(
                                    a.medal
                            )
                    );

                    im.setBackground(
                            imageBg
                    );

                    im.setClipToOutline(true);

                    FrameLayout.LayoutParams imageLp =
                            new FrameLayout.LayoutParams(
                                    dp(64),
                                    dp(64)
                            );

                    imageLp.gravity =
                            Gravity.CENTER;

                    visual.addView(
                            im,
                            imageLp
                    );

                } else {

                    visualBg.setColor(
                            visualColor(a)
                    );

                    visualBg.setCornerRadius(
                            dp(12)
                    );

                    visual.setBackground(
                            visualBg
                    );

                    addMedal(
                            visual,
                            a
                    );
                }

            } catch (Exception ignored) {

                visualBg.setColor(
                        visualColor(a)
                );

                visualBg.setCornerRadius(
                        dp(12)
                );

                visual.setBackground(
                        visualBg
                );

                addMedal(
                        visual,
                        a
                );
            }

        } else {

            addMedal(
                    visual,
                    a
            );
        }


        /*
         * DRAG HANDLE
         */
        TextView grip =
                tv(
                        "☷",
                        19,
                        a.done
                                ? (
                                    isLight()
                                            ? GREEN
                                            : Color.WHITE
                                )
                                : secondaryText()
                );

        grip.setGravity(
                Gravity.CENTER
        );

        visual.addView(
                grip,
                new FrameLayout.LayoutParams(
                        dp(26),
                        dp(26),
                        Gravity.LEFT
                                | Gravity.BOTTOM
                )
        );

        LinearLayout.LayoutParams vp =
                new LinearLayout.LayoutParams(
                        dp(66),
                        dp(66)
                );

        vp.setMargins(
                dp(2),
                0,
                dp(3),
                0
        );

        card.addView(
                visual,
                vp
        );


        /*
         * TEXT
         */
        LinearLayout text =
                new LinearLayout(this);

        text.setOrientation(
                LinearLayout.VERTICAL
        );

        text.setGravity(
                Gravity.CENTER_VERTICAL
        );

        text.setPadding(
                dp(6),
                0,
                dp(3),
                0
        );

        int textMain =
                achievementPrimaryText(a);

        int textSecond =
                achievementSecondaryText(a);


        TextView title =
                tv(
                        a.title,
                        17,
                        textMain
                );

        title.setTypeface(
                android.graphics.Typeface.create(
                        "sans-serif-medium",
                        android.graphics.Typeface.BOLD
                )
        );

        title.setGravity(
                Gravity.RIGHT
                        | Gravity.CENTER_VERTICAL
        );

        title.setTextDirection(
                View.TEXT_DIRECTION_ANY_RTL
        );

        title.setSingleLine(false);

        title.setMaxLines(2);

        title.setEllipsize(null);

        title.setIncludeFontPadding(false);


        TextView desc =
                tv(
                        a.desc,
                        12.5f,
                        textSecond
                );

        desc.setTypeface(
                android.graphics.Typeface.create(
                        "sans-serif",
                        android.graphics.Typeface.NORMAL
                )
        );

        desc.setGravity(
                Gravity.RIGHT
                        | Gravity.CENTER_VERTICAL
        );

        desc.setTextDirection(
                View.TEXT_DIRECTION_ANY_RTL
        );

        desc.setSingleLine(false);

        desc.setMaxLines(2);

        desc.setEllipsize(null);

        desc.setIncludeFontPadding(true);

        // Keep Persian two-line descriptions compact enough to leave room for status.
        desc.setLineSpacing(-2.0f, 1.0f);

        text.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        text.addView(
                desc,
                new LinearLayout.LayoutParams(
                        -1,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        /*
         * STATUS
         * Tiny, centered text directly under the description.
         */
        TextView status =
                tv(
                        a.done
                                ? "Status: Completed"
                                : "Status: Not Completed",
                        7.5f,
                        a.done
                                ? Color.argb(170, 100, 199, 240)
                                : Color.argb(125, 190, 190, 190)
                );

        status.setGravity(
                Gravity.CENTER
        );

        status.setTextDirection(
                View.TEXT_DIRECTION_ANY_RTL
        );

        status.setSingleLine(true);

        status.setIncludeFontPadding(false);

        text.addView(
                status,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(9)
                )
        );

        card.addView(
                text,
                new LinearLayout.LayoutParams(
                        0,
                        dp(70),
                        1
                )
        );


        /*
         * ACTIONS
         */
        LinearLayout actions =
                new LinearLayout(this);

        actions.setOrientation(
                LinearLayout.VERTICAL
        );

        actions.setGravity(
                Gravity.CENTER
        );


        TextView done =
                tv(
                        a.done
                                ? "✓"
                                : "○",
                        20,
                        a.done
                                ? (
                                    isLight()
                                            ? GREEN
                                            : CYAN
                                )
                                : secondaryText()
                );

        done.setGravity(
                Gravity.CENTER
        );

        addPressAnimation(done);

        done.setOnClickListener(
                v -> {

                    a.done = !a.done;

                    save();

                    render();
                }
        );


        TextView pin =
                tv(
                        a.pinned
                                ? "★"
                                : "☆",
                        21,
                        a.pinned
                                ? (
                                    isLight()
                                            ? GREEN
                                            : CYAN
                                )
                                : secondaryText()
                );

        pin.setGravity(
                Gravity.CENTER
        );

        addPressAnimation(pin);

        pin.setOnClickListener(
                v -> {

                    a.pinned = !a.pinned;

                    save();

                    render();
                }
        );


        TextView delete =
                tv(
                        "🗑",
                        16,
                        secondaryText()
                );

        delete.setGravity(
                Gravity.CENTER
        );

        addPressAnimation(delete);

        delete.setOnClickListener(
                v -> {

                    int realIndex =
                            data.indexOf(a);

                    if (realIndex >= 0) {

                        confirmDelete(
                                realIndex
                        );
                    }
                }
        );


        actions.addView(
                done,
                new LinearLayout.LayoutParams(
                        dp(32),
                        dp(27)
                )
        );

        actions.addView(
                pin,
                new LinearLayout.LayoutParams(
                        dp(32),
                        dp(27)
                )
        );

        actions.addView(
                delete,
                new LinearLayout.LayoutParams(
                        dp(32),
                        dp(27)
                )
        );

        card.addView(
                actions,
                new LinearLayout.LayoutParams(
                        dp(34),
                        dp(84)
                )
        );


        card.setTag(a);

        addPressAnimation(card);

        card.setOnClickListener(
                v -> {

                    int realIndex =
                            data.indexOf(a);

                    if (realIndex >= 0)
                        showEditor(realIndex);
                }
        );


        card.setOnLongClickListener(
                v -> {

                    dragged = card;

                    card.setAlpha(.72f);

                    card.setScaleX(1.025f);
                    card.setScaleY(1.025f);

                    card.setElevation(
                            dp(10)
                    );

                    Toast.makeText(
                            this,
                            "Hold and drag to reorder",
                            Toast.LENGTH_SHORT
                    ).show();

                    if (
                            android.os.Build.VERSION.SDK_INT
                                    >= 24
                    ) {

                        card.startDragAndDrop(
                                null,
                                new View.DragShadowBuilder(
                                        card
                                ),
                                null,
                                View.DRAG_FLAG_GLOBAL
                        );

                    } else {

                        card.startDrag(
                                null,
                                new View.DragShadowBuilder(
                                        card
                                ),
                                null,
                                0
                        );
                    }

                    return true;
                }
        );


        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(90)
                );

        lp.setMargins(
                0,
                dp(2),
                0,
                dp(2)
        );

        card.setLayoutParams(lp);

        return card;
    }


    void setCardSolidBackground(
            LinearLayout card,
            Achievement a
    ) {

        GradientDrawable bgd =
                new GradientDrawable();

        bgd.setColor(
                cardColor(a)
        );

        bgd.setCornerRadius(
                dp(14)
        );

        card.setBackground(bgd);
    }


    class AchievementBackgroundDrawable
            extends Drawable {

        Bitmap bitmap;

        Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                                | Paint.FILTER_BITMAP_FLAG
                );

        Paint overlay =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        RectF destination =
                new RectF();

        AchievementBackgroundDrawable(
                Bitmap b
        ) {

            bitmap = b;

            overlay.setColor(
                    0x66000000
            );
        }

        @Override
        public void draw(
                Canvas canvas
        ) {

            if (bitmap == null)
                return;

            Rect bounds =
                    getBounds();

            float bw =
                    bitmap.getWidth();

            float bh =
                    bitmap.getHeight();

            float vw =
                    bounds.width();

            float vh =
                    bounds.height();

            float scale =
                    Math.max(
                            vw / bw,
                            vh / bh
                    );

            float dw =
                    bw * scale;

            float dh =
                    bh * scale;

            float left =
                    bounds.left
                            + (vw - dw) / 2f;

            float top =
                    bounds.top
                            + (vh - dh) / 2f;

            destination.set(
                    left,
                    top,
                    left + dw,
                    top + dh
            );

            Path path =
                    new Path();

            float radius =
                    dp(14);

            path.addRoundRect(
                    new RectF(bounds),
                    radius,
                    radius,
                    Path.Direction.CW
            );

            int save =
                    canvas.save();

            canvas.clipPath(path);

            canvas.drawBitmap(
                    bitmap,
                    null,
                    destination,
                    paint
            );

            canvas.drawRect(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    overlay
            );

            canvas.restoreToCount(save);
        }

        @Override
        public void setAlpha(
                int alpha
        ) {

            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(
                android.graphics.ColorFilter filter
        ) {

            paint.setColorFilter(filter);
        }

        @Override
        public int getOpacity() {

            return PixelFormat.TRANSLUCENT;
        }
    }


    void confirmDelete(
            final int index
    ) {

        if (
                index < 0
                        || index >= data.size()
        )
            return;

        final Achievement target =
                data.get(index);

        new AlertDialog.Builder(this)
                .setTitle(
                        "Delete achievement?"
                )
                .setMessage(
                        target.title
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Delete",
                        (d,w) ->
                                animateDelete(
                                        target
                                )
                )
                .show();
    }


    void animateDelete(
            final Achievement target
    ) {

        if (list == null) {

            data.remove(target);

            save();

            render();

            return;
        }

        View targetView = null;

        for (
                int i = 0;
                i < list.getChildCount();
                i++
        ) {

            View child =
                    list.getChildAt(i);

            if (
                    child.getTag()
                            == target
            ) {

                targetView = child;

                break;
            }
        }

        if (targetView == null) {

            data.remove(target);

            save();

            render();

            return;
        }

        final View finalTarget =
                targetView;

        finalTarget.animate()
                .alpha(0f)
                .translationX(dp(90))
                .scaleX(.82f)
                .scaleY(.82f)
                .setDuration(240)
                .setInterpolator(
                        new AccelerateInterpolator()
                )
                .withEndAction(
                        () -> {

                            data.remove(
                                    target
                            );

                            save();

                            render();
                        }
                )
                .start();
    }


    void addMedal(
            FrameLayout f,
            Achievement a
    ) {

        TextView medal =
                tv(
                        medalEmoji(a.medal),
                        48,
                        medalColor(a.medal)
                );

        medal.setGravity(
                Gravity.CENTER
        );

        f.addView(
                medal,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );
    }


    String medalEmoji(
            String m
    ) {

        if ("Bronze".equals(m))
            return "🥉";

        if ("Silver".equals(m))
            return "🥈";

        if ("Gold".equals(m))
            return "🥇";

        return "🏆";
    }


    int medalColor(
            String m
    ) {

        if ("Bronze".equals(m))
            return Color.rgb(
                    205,
                    127,
                    50
            );

        if ("Silver".equals(m))
            return Color.LTGRAY;

        if ("Gold".equals(m))
            return Color.rgb(
                    255,
                    193,
                    7
            );

        return CYAN;
    }


    int medalBorderColor(
            String m
    ) {

        if ("Bronze".equals(m))
            return Color.rgb(
                    150,
                    95,
                    45
            );

        if ("Silver".equals(m))
            return Color.WHITE;

        if ("Gold".equals(m))
            return Color.rgb(
                    255,
                    193,
                    7
            );

        return Color.rgb(
                35,
                65,
                110
        );
    }


    boolean matchesFilter(
            Achievement a
    ) {

        return currentFilter.equals("All")
                || a.medal.equals(
                        currentFilter
                );
    }


    int actualIndex(
            Achievement a
    ) {

        return data.indexOf(a);
    }


    ArrayList<Achievement>
    visibleAchievements() {

        ArrayList<Achievement> out =
                new ArrayList<>();

        for (Achievement a : data) {

            if (matchesFilter(a))
                out.add(a);
        }

        Collections.sort(
                out,
                (x,y) ->
                        Boolean.compare(
                                !x.pinned,
                                !y.pinned
                        )
        );

        return out;
    }


    void render() {

        if (list == null)
            return;

        list.removeAllViews();

        ArrayList<Achievement> visible =
                visibleAchievements();

        for (Achievement a : visible) {

            list.addView(
                    makeCard(
                            a,
                            actualIndex(a)
                    )
            );
        }


        int bronzeTotal = 0;
        int bronzeDone = 0;

        int silverTotal = 0;
        int silverDone = 0;

        int goldTotal = 0;
        int goldDone = 0;

        int platinumTotal = 0;
        int platinumDone = 0;


        for (Achievement a : data) {

            if ("Bronze".equals(a.medal)) {

                bronzeTotal++;

                if (a.done)
                    bronzeDone++;

            } else if ("Silver".equals(a.medal)) {

                silverTotal++;

                if (a.done)
                    silverDone++;

            } else if ("Gold".equals(a.medal)) {

                goldTotal++;

                if (a.done)
                    goldDone++;

            } else {

                platinumTotal++;

                if (a.done)
                    platinumDone++;
            }
        }


        counts.setText(
                "🥉 " + bronzeTotal
                        + " - " + bronzeDone
                        + "    "
                        + "🥈 " + silverTotal
                        + " - " + silverDone
                        + "    "
                        + "🥇 " + goldTotal
                        + " - " + goldDone
                        + "    "
                        + "🏆 " + platinumTotal
                        + " - " + platinumDone
        );

        updateCategoryButtons();
    }


    void showEditor(
            final int index
    ) {

        boolean edit =
                index >= 0;

        Achievement old =
                edit
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


        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(18),
                dp(2),
                dp(18),
                0
        );


        /*
         * TITLE
         */
        EditText title =
                new EditText(this);

        title.setHint(
                "Achievement title"
        );

        title.setText(
                old.title
        );

        title.setSingleLine(true);

        title.setGravity(
                Gravity.CENTER_VERTICAL
        );

        styleEditorText(title);

        LinearLayout.LayoutParams titleLp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(50)
                );

        box.addView(
                title,
                titleLp
        );


        /*
         * DESCRIPTION
         *
         * Fixed:
         * - no gravity at the top
         * - hint sits with the field
         * - enough height for multiline text
         */
        EditText desc =
                new EditText(this);

        desc.setHint(
                "Description"
        );

        desc.setText(
                old.desc
        );

        desc.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                );

        desc.setMinLines(2);

        desc.setMaxLines(3);

        desc.setGravity(
                Gravity.CENTER_VERTICAL
        );

        styleEditorText(desc);

        LinearLayout.LayoutParams descLp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(72)
                );

        box.addView(
                desc,
                descLp
        );


        /*
         * MEDAL
         */
        Spinner spinner =
                new Spinner(this);

        String[] medals = {
                "Bronze",
                "Silver",
                "Gold",
                "Platinum"
        };

        ArrayAdapter<String> medalAdapter =
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        medals
                );

        spinner.setAdapter(
                medalAdapter
        );

        for (
                int i = 0;
                i < medals.length;
                i++
        ) {

            if (
                    medals[i].equals(
                            old.medal
                    )
            ) {

                spinner.setSelection(i);
            }
        }

        LinearLayout.LayoutParams spinnerLp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(48)
                );

        box.addView(
                spinner,
                spinnerLp
        );


        /*
         * CUSTOMIZATION
         */
        TextView customizeTitle =
                tv(
                        "Customize Achievement",
                        15,
                        primaryText()
                );

        customizeTitle.setTypeface(
                android.graphics.Typeface.create(
                        "sans-serif-medium",
                        android.graphics.Typeface.BOLD
                )
        );

        customizeTitle.setPadding(
                0,
                dp(4),
                0,
                dp(2)
        );

        box.addView(
                customizeTitle,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(30)
                )
        );


        TextView customizationHint =
                tv(
                        "Choose a custom color or use the achievement image as the card background.",
                        12,
                        secondaryText()
                );

        customizationHint.setPadding(
                0,
                0,
                0,
                dp(4)
        );

        box.addView(
                customizationHint,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(32)
                )
        );


        /*
         * CUSTOMIZATION BUTTONS
         *
         * Fixed:
         * - all 3 have exactly the same height
         * - zero minimum width
         * - zero internal side padding
         * - slightly smaller text
         * - no vertical clipping
         */
        LinearLayout customizationButtons =
                new LinearLayout(this);

        customizationButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        customizationButtons.setGravity(
                Gravity.CENTER_VERTICAL
        );

        Button defaultBtn =
                editorOptionButton(
                        "Default"
                );

        Button colorBtn =
                editorOptionButton(
                        "Custom Color"
                );

        Button imageBgBtn =
                editorOptionButton(
                        "Image Background"
                );


        customizationButtons.addView(
                defaultBtn,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );

        customizationButtons.addView(
                colorBtn,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );

        customizationButtons.addView(
                imageBgBtn,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );


        box.addView(
                customizationButtons,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(48)
                )
        );


        TextView customizationStatus =
                tv(
                        "",
                        12,
                        secondaryText()
                );

        customizationStatus.setGravity(
                Gravity.CENTER
        );

        customizationStatus.setSingleLine(false);

        customizationStatus.setMaxLines(2);

        customizationStatus.setIncludeFontPadding(true);

        customizationStatus.setPadding(
                0,
                0,
                0,
                0
        );

        updateCustomizationStatus(
                old,
                customizationStatus
        );

        box.addView(
                customizationStatus,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(40)
                )
        );


        defaultBtn.setOnClickListener(
                v -> {

                    old.backgroundMode =
                            "default";

                    customizationStatus.setText(
                            "Using the normal Day/Dark appearance"
                    );

                    customizationStatus.setTextColor(
                            secondaryText()
                    );
                }
        );


        colorBtn.setOnClickListener(
                v -> {

                    showColorPicker(
                            old,
                            customizationStatus
                    );
                }
        );


        imageBgBtn.setOnClickListener(
                v -> {

                    if (
                            chosenImage[0] == null
                                    || chosenImage[0].isEmpty()
                    ) {

                        Toast.makeText(
                                this,
                                "Add an image first.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    old.backgroundMode =
                            "image";

                    customizationStatus.setText(
                            "Achievement image is used as the card background"
                    );

                    customizationStatus.setTextColor(
                            secondaryText()
                    );
                }
        );


        /*
         * IMAGE SHAPE
         */
        TextView shapeTitle =
                tv(
                        "Image Shape",
                        14,
                        primaryText()
                );

        shapeTitle.setTypeface(
                android.graphics.Typeface.create(
                        "sans-serif-medium",
                        android.graphics.Typeface.BOLD
                )
        );

        shapeTitle.setPadding(
                0,
                dp(5),
                0,
                0
        );

        box.addView(
                shapeTitle,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(28)
                )
        );


        RadioGroup shapeGroup =
                new RadioGroup(this);

        shapeGroup.setOrientation(
                RadioGroup.HORIZONTAL
        );

        RadioButton circleOption =
                new RadioButton(this);

        circleOption.setText(
                "Circle"
        );

        circleOption.setTextSize(14);

        circleOption.setTextColor(
                primaryText()
        );

        RadioButton squareOption =
                new RadioButton(this);

        squareOption.setText(
                "Square"
        );

        squareOption.setTextSize(14);

        squareOption.setTextColor(
                primaryText()
        );

        shapeGroup.addView(
                circleOption,
                new RadioGroup.LayoutParams(
                        0,
                        dp(42),
                        1
                )
        );

        shapeGroup.addView(
                squareOption,
                new RadioGroup.LayoutParams(
                        0,
                        dp(42),
                        1
                )
        );

        if (
                "square".equals(
                        old.imageShape
                )
        ) {

            squareOption.setChecked(
                    true
            );

        } else {

            circleOption.setChecked(
                    true
            );
        }

        box.addView(
                shapeGroup,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(42)
                )
        );


        /*
         * PIN
         */
        CheckBox pinBox =
                new CheckBox(this);

        pinBox.setText(
                "Pin this achievement in its section"
        );

        pinBox.setTextColor(
                primaryText()
        );

        pinBox.setChecked(
                old.pinned
        );

        box.addView(
                pinBox,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(48)
                )
        );


        /*
         * IMAGE PREVIEW
         */
        ImageView preview =
                new ImageView(this);

        preview.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        preview.setBackgroundColor(
                panelColor()
        );

        preview.setAdjustViewBounds(true);

        preview.setMinimumHeight(
                dp(80)
        );

        if (
                chosenImage[0] != null
                        && !chosenImage[0].isEmpty()
        ) {

            Bitmap previewBitmap =
                    BitmapFactory.decodeFile(
                            chosenImage[0]
                    );

            if (previewBitmap != null)
                preview.setImageBitmap(
                        previewBitmap
                );
            else
                preview.setImageResource(
                        android.R.drawable.ic_menu_gallery
                );

        } else {

            preview.setImageResource(
                    android.R.drawable.ic_menu_gallery
            );
        }

        box.addView(
                preview,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(80)
                )
        );


        /*
         * ADD / CHANGE IMAGE
         */
        Button imageBtn =
                editorWideButton(
                        chosenImage[0].isEmpty()
                                ? "ADD IMAGE"
                                : "CHANGE IMAGE"
                );

        imageBtn.setOnClickListener(
                v ->
                        pickImage(
                                chosenImage,
                                preview,
                                imageBtn
                        )
        );

        box.addView(
                imageBtn,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(46)
                )
        );


        /*
         * REMOVE IMAGE
         *
         * Fixed:
         * visible, same size as Add Image,
         * and explicitly added to the editor.
         */
        Button removeImage =
                editorWideButton(
                        "REMOVE IMAGE"
                );

        removeImage.setVisibility(
                edit
                        && !chosenImage[0].isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );

        removeImage.setOnClickListener(
                v -> {

                    chosenImage[0] = "";

                    preview.setImageResource(
                            android.R.drawable.ic_menu_gallery
                    );

                    imageBtn.setText(
                            "ADD IMAGE"
                    );

                    removeImage.setVisibility(
                            View.GONE
                    );

                    if (
                            "image".equals(
                                    old.backgroundMode
                            )
                    ) {

                        old.backgroundMode =
                                "default";

                        updateCustomizationStatus(
                                old,
                                customizationStatus
                        );
                    }
                }
        );

        box.addView(
                removeImage,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(46)
                )
        );


        AlertDialog d =
                new AlertDialog.Builder(this)
                        .setTitle(
                                edit
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


        d.setOnShowListener(
                x -> {

                    d.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                String t =
                                        title.getText()
                                                .toString()
                                                .trim();

                                if (
                                        t.length()
                                                == 0
                                ) {

                                    title.setError(
                                            "Enter a title"
                                    );

                                    return;
                                }

                                old.title = t;

                                old.desc =
                                        desc.getText()
                                                .toString()
                                                .trim();

                                old.medal =
                                        medals[
                                                spinner
                                                        .getSelectedItemPosition()
                                        ];

                                old.imagePath =
                                        chosenImage[0];

                                old.imageShape =
                                        squareOption.isChecked()
                                                ? "square"
                                                : "circle";

                                old.pinned =
                                        pinBox.isChecked();


                                if (
                                        "image".equals(
                                                old.backgroundMode
                                        )
                                                && (
                                                    old.imagePath == null
                                                            || old.imagePath.isEmpty()
                                                )
                                ) {

                                    old.backgroundMode =
                                            "default";
                                }


                                if (!edit)
                                    data.add(old);

                                save();

                                render();

                                d.dismiss();
                            }
                    );
                }
        );

        d.show();
    }


    Button editorOptionButton(
            String text
    ) {

        Button b =
                new Button(this);

        b.setText(text);

        b.setTextSize(11);

        b.setTextColor(
                primaryText()
        );

        b.setAllCaps(false);

        b.setGravity(
                Gravity.CENTER
        );

        b.setPadding(
                0,
                0,
                0,
                0
        );

        b.setMinWidth(0);

        b.setMinimumWidth(0);

        b.setMinHeight(0);

        b.setMinimumHeight(0);

        b.setIncludeFontPadding(false);

        return b;
    }


    Button editorWideButton(
            String text
    ) {

        Button b =
                new Button(this);

        b.setText(text);

        b.setTextSize(13);

        b.setAllCaps(false);

        b.setGravity(
                Gravity.CENTER
        );

        b.setPadding(
                0,
                0,
                0,
                0
        );

        b.setMinHeight(0);

        b.setMinimumHeight(0);

        b.setTextColor(
                primaryText()
        );

        return b;
    }


    void updateCustomizationStatus(
            Achievement a,
            TextView status
    ) {

        if (
                "color".equals(
                        a.backgroundMode
                )
        ) {

            status.setText(
                    "Custom color selected"
            );

        } else if (
                "image".equals(
                        a.backgroundMode
                )
        ) {

            status.setText(
                    "Achievement image is used as the card background"
            );

        } else {

            status.setText(
                    "Using the normal Day/Dark appearance"
            );
        }
    }


    void showColorPicker(
            Achievement a,
            TextView status
    ) {

        final int startColor =
                a.customColor != Color.TRANSPARENT
                        ? a.customColor
                        : (
                            isLight()
                                    ? Color.rgb(
                                            70,
                                            130,
                                            180
                                    )
                                    : Color.rgb(
                                            60,
                                            120,
                                            170
                                    )
                        );

        final int[] rgb = {
                Color.red(startColor),
                Color.green(startColor),
                Color.blue(startColor)
        };


        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(18),
                dp(8),
                dp(18),
                dp(4)
        );


        View preview =
                new View(this);

        GradientDrawable previewBg =
                new GradientDrawable();

        previewBg.setColor(
                Color.rgb(
                        rgb[0],
                        rgb[1],
                        rgb[2]
                )
        );

        previewBg.setCornerRadius(
                dp(14)
        );

        preview.setBackground(
                previewBg
        );

        box.addView(
                preview,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)
                )
        );


        TextView label =
                tv(
                        "",
                        14,
                        primaryText()
                );

        label.setGravity(
                Gravity.CENTER
        );

        box.addView(
                label,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(40)
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

        red.setProgress(rgb[0]);
        green.setProgress(rgb[1]);
        blue.setProgress(rgb[2]);


        box.addView(
                colorSliderLabel("Red")
        );

        box.addView(red);

        box.addView(
                colorSliderLabel("Green")
        );

        box.addView(green);

        box.addView(
                colorSliderLabel("Blue")
        );

        box.addView(blue);


        SeekBar.OnSeekBarChangeListener listener =
                new SeekBar.OnSeekBarChangeListener() {

                    public void onProgressChanged(
                            SeekBar s,
                            int value,
                            boolean fromUser
                    ) {

                        rgb[0] =
                                red.getProgress();

                        rgb[1] =
                                green.getProgress();

                        rgb[2] =
                                blue.getProgress();

                        int color =
                                Color.rgb(
                                        rgb[0],
                                        rgb[1],
                                        rgb[2]
                                );

                        previewBg.setColor(
                                color
                        );

                        label.setText(
                                "RGB "
                                        + rgb[0]
                                        + ", "
                                        + rgb[1]
                                        + ", "
                                        + rgb[2]
                        );

                        preview.invalidate();
                    }

                    public void onStartTrackingTouch(
                            SeekBar s
                    ) {
                    }

                    public void onStopTrackingTouch(
                            SeekBar s
                    ) {
                    }
                };


        red.setOnSeekBarChangeListener(
                listener
        );

        green.setOnSeekBarChangeListener(
                listener
        );

        blue.setOnSeekBarChangeListener(
                listener
        );


        label.setText(
                "RGB "
                        + rgb[0]
                        + ", "
                        + rgb[1]
                        + ", "
                        + rgb[2]
        );


        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Choose Custom Color"
                        )
                        .setView(box)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Use Color",
                                null
                        )
                        .create();


        dialog.setOnShowListener(
                x -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                a.customColor =
                                        Color.rgb(
                                                rgb[0],
                                                rgb[1],
                                                rgb[2]
                                        );

                                a.backgroundMode =
                                        "color";

                                status.setText(
                                        "Custom color selected"
                                );

                                status.setTextColor(
                                        secondaryText()
                                );

                                dialog.dismiss();
                            }
                    );
                }
        );

        dialog.show();
    }


    TextView colorSliderLabel(
            String text
    ) {

        TextView t =
                tv(
                        text,
                        13,
                        secondaryText()
                );

        t.setPadding(
                0,
                dp(5),
                0,
                0
        );

        return t;
    }


    void pickImage(
            String[] chosen,
            ImageView preview,
            Button btn
    ) {

        Intent i =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        i.setType("image/*");

        i.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        imagePickerActive = true;

        startActivityForResult(
                i,
                77
        );

        pendingImageHolder = chosen;

        pendingPreview = preview;

        pendingImageButton = btn;
    }


    String[] pendingImageHolder;

    ImageView pendingPreview;

    Button pendingImageButton;


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

        if (requestCode == 77) {
        imagePickerActive = false;
        wasInBackground = false;
        }

        if (
                requestCode != 77
                        || resultCode != RESULT_OK
                        || dataIntent == null
        )
            return;

        Uri uri =
                dataIntent.getData();

        if (uri == null)
            return;

        try {

            Bitmap b =
                    BitmapFactory.decodeStream(
                            getContentResolver()
                                    .openInputStream(uri)
                    );

            if (b != null)
                showCropDialog(b);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open image",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    void showCropDialog(
            Bitmap source
    ) {

        final CropView crop =
                new CropView(
                        this,
                        source
                );

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(10),
                0,
                dp(10),
                0
        );

        box.addView(
                crop,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(280)
                )
        );


        TextView hint =
                tv(
                        "Crop amount",
                        15,
                        secondaryText()
                );

        hint.setGravity(
                Gravity.CENTER
        );

        box.addView(
                hint,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(36)
                )
        );


        SeekBar seek =
                new SeekBar(this);

        seek.setMax(70);

        seek.setProgress(35);

        seek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    public void onProgressChanged(
                            SeekBar s,
                            int p,
                            boolean f
                    ) {

                        crop.setCropFraction(
                                .25f
                                        + .7f
                                        * (
                                            p
                                                    / 70f
                                        )
                        );
                    }

                    public void onStartTrackingTouch(
                            SeekBar s
                    ) {
                    }

                    public void onStopTrackingTouch(
                            SeekBar s
                    ) {
                    }
                }
        );

        box.addView(seek);


        AlertDialog d =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Crop image"
                        )
                        .setMessage(
                                "Move the slider to choose how much of the image stays visible."
                        )
                        .setView(box)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Use image",
                                null
                        )
                        .create();


        d.setOnShowListener(
                x ->
                        d.getButton(
                                AlertDialog.BUTTON_POSITIVE
                        ).setOnClickListener(
                                v -> {

                                    Bitmap out =
                                            crop.getCroppedBitmap();

                                    try {

                                        File f =
                                                new File(
                                                        getFilesDir(),
                                                        "ach_"
                                                                + System.currentTimeMillis()
                                                                + ".jpg"
                                                );

                                        FileOutputStream os =
                                                new FileOutputStream(
                                                        f
                                                );

                                        out.compress(
                                                Bitmap.CompressFormat.JPEG,
                                                90,
                                                os
                                        );

                                        os.close();

                                        pendingImageHolder[0] =
                                                f.getAbsolutePath();

                                        pendingPreview
                                                .setImageBitmap(
                                                        out
                                                );

                                        pendingImageButton
                                                .setText(
                                                        "CHANGE IMAGE"
                                                );

                                        d.dismiss();

                                    } catch (
                                            Exception e
                                    ) {

                                        Toast.makeText(
                                                this,
                                                "Could not save image",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                }
                        )
        );

        d.show();
    }


    class CropView extends View {

        Bitmap bmp;

        Paint p =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        float fraction = .6f;

        float downX;
        float downY;

        float offsetX;
        float offsetY;


        CropView(
                Context c,
                Bitmap b
        ) {

            super(c);

            bmp = b;

            p.setFilterBitmap(true);
        }


        void setCropFraction(
                float f
        ) {

            fraction = f;

            invalidate();
        }


        protected void onDraw(
                Canvas c
        ) {

            super.onDraw(c);

            float scale =
                    Math.max(
                            getWidth()
                                    / (float) bmp.getWidth(),
                            getHeight()
                                    / (float) bmp.getHeight()
                    );

            float w =
                    bmp.getWidth()
                            * scale;

            float h =
                    bmp.getHeight()
                            * scale;

            float left =
                    (getWidth() - w) / 2
                            + offsetX;

            float top =
                    (getHeight() - h) / 2
                            + offsetY;

            c.drawBitmap(
                    bmp,
                    null,
                    new RectF(
                            left,
                            top,
                            left + w,
                            top + h
                    ),
                    p
            );


            float size =
                    Math.min(
                            getWidth(),
                            getHeight()
                    ) * fraction;

            float cx =
                    getWidth() / 2f
                            + offsetX;

            float cy =
                    getHeight() / 2f
                            + offsetY;


            p.setColor(
                    0x99000000
            );

            p.setStyle(
                    Paint.Style.FILL
            );


            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    Math.max(
                            0,
                            cy - size / 2
                    ),
                    p
            );

            c.drawRect(
                    0,
                    cy + size / 2,
                    getWidth(),
                    getHeight(),
                    p
            );

            c.drawRect(
                    0,
                    cy - size / 2,
                    Math.max(
                            0,
                            cx - size / 2
                    ),
                    cy + size / 2,
                    p
            );

            c.drawRect(
                    cx + size / 2,
                    cy - size / 2,
                    getWidth(),
                    cy + size / 2,
                    p
            );


            p.setColor(
                    Color.WHITE
            );

            p.setStyle(
                    Paint.Style.STROKE
            );

            p.setStrokeWidth(
                    dp(2)
            );

            c.drawRect(
                    cx - size / 2,
                    cy - size / 2,
                    cx + size / 2,
                    cy + size / 2,
                    p
            );

            p.setStyle(
                    Paint.Style.FILL
            );
        }


        public boolean onTouchEvent(
                android.view.MotionEvent e
        ) {

            if (
                    e.getAction()
                            == MotionEvent.ACTION_DOWN
            ) {

                downX = e.getX();

                downY = e.getY();

                return true;
            }


            if (
                    e.getAction()
                            == MotionEvent.ACTION_MOVE
            ) {

                offsetX +=
                        e.getX() - downX;

                offsetY +=
                        e.getY() - downY;

                downX = e.getX();

                downY = e.getY();

                invalidate();

                return true;
            }

            return true;
        }


        Bitmap getCroppedBitmap() {

            float scale =
                    Math.max(
                            getWidth()
                                    / (float) bmp.getWidth(),
                            getHeight()
                                    / (float) bmp.getHeight()
                    );

            float w =
                    bmp.getWidth()
                            * scale;

            float h =
                    bmp.getHeight()
                            * scale;

            float left =
                    (getWidth() - w) / 2
                            + offsetX;

            float top =
                    (getHeight() - h) / 2
                            + offsetY;


            float size =
                    Math.min(
                            getWidth(),
                            getHeight()
                    ) * fraction;

            float cx =
                    getWidth() / 2f
                            + offsetX;

            float cy =
                    getHeight() / 2f
                            + offsetY;


            float sx =
                    (
                            cx
                                    - size / 2
                                    - left
                    ) / scale;

            float sy =
                    (
                            cy
                                    - size / 2
                                    - top
                    ) / scale;

            float ss =
                    size / scale;


            sx =
                    Math.max(
                            0,
                            Math.min(
                                    bmp.getWidth() - ss,
                                    sx
                            )
                    );

            sy =
                    Math.max(
                            0,
                            Math.min(
                                    bmp.getHeight() - ss,
                                    sy
                            )
                    );


            return Bitmap.createBitmap(
                    bmp,
                    (int) sx,
                    (int) sy,
                    Math.max(
                            1,
                            (int) ss
                    ),
                    Math.max(
                            1,
                            (int) ss
                    )
            );
        }
    }


    void showMenu(
            View anchor
    ) {

        PopupMenu pm =
                new PopupMenu(
                        this,
                        anchor
                );

        pm.getMenu().add(
                "Add Achievement"
        );

        pm.getMenu().add(
                "Reset all completion"
        );

        pm.getMenu().add(
                "Delete all"
        );

        pm.setOnMenuItemClickListener(
                item -> {

                    String s =
                            item.getTitle()
                                    .toString();

                    if (
                            s.equals(
                                    "Add Achievement"
                            )
                    ) {

                        showEditor(-1);

                    } else if (
                            s.equals(
                                    "Reset all completion"
                            )
                    ) {

                        for (Achievement a : data)
                            a.done = false;

                        save();

                        render();

                    } else if (
                            s.equals(
                                    "Delete all"
                            )
                    ) {

                        new AlertDialog.Builder(this)
                                .setTitle(
                                        "Delete all?"
                                )
                                .setMessage(
                                        "This cannot be undone."
                                )
                                .setNegativeButton(
                                        "Cancel",
                                        null
                                )
                                .setPositiveButton(
                                        "Delete",
                                        (d,w) -> {

                                            data.clear();

                                            save();

                                            render();
                                        }
                                )
                                .show();
                    }

                    return true;
                }
        );

        pm.show();
    }


    void showSideMenu() {

        final Dialog dialog =
                new Dialog(this);

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );


        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(20),
                dp(18),
                dp(20),
                dp(18)
        );


        GradientDrawable panel =
                new GradientDrawable();

        panel.setColor(
                panelColor()
        );

        panel.setCornerRadii(
                new float[]{
                        0,0,
                        dp(22),dp(22),
                        dp(22),dp(22),
                        0,0
                }
        );

        box.setBackground(panel);


        TextView h =
                tv(
                        "My Achievements",
                        23,
                        primaryText()
                );

        h.setTypeface(
                android.graphics.Typeface.create(
                        "sans-serif-medium",
                        android.graphics.Typeface.NORMAL
                )
        );

        box.addView(
                h,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                )
        );


        TextView sub =
                tv(
                        "Manage your achievements",
                        13,
                        secondaryText()
                );

        sub.setGravity(
                Gravity.CENTER_VERTICAL
        );

        box.addView(
                sub,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(34)
                )
        );


        View line =
                new View(this);

        line.setBackgroundColor(
                isLight()
                        ? Color.rgb(
                                215,
                                217,
                                222
                        )
                        : Color.rgb(
                                75,
                                78,
                                83
                        )
        );

        LinearLayout.LayoutParams lineLp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(1)
                );

        lineLp.setMargins(
                0,
                dp(8),
                0,
                dp(12)
        );

        box.addView(
                line,
                lineLp
        );


        TextView settings =
                rowButton(
                        "⚙",
                        "Settings"
                );

        settings.setOnClickListener(
                v -> {

                    animateSideMenuClose(
                            box,
                            dialog,
                            () -> showSettings()
                    );
                }
        );

        LinearLayout.LayoutParams sp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                );

        sp.setMargins(
                0,
                0,
                0,
                dp(8)
        );

        box.addView(
                settings,
                sp
        );


        TextView add =
                rowButton(
                        "＋",
                        "Add Achievement"
                );

        add.setOnClickListener(
                v -> {

                    animateSideMenuClose(
                            box,
                            dialog,
                            () -> showEditor(-1)
                    );
                }
        );

        LinearLayout.LayoutParams ap =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                );

        ap.setMargins(
                0,
                0,
                0,
                dp(8)
        );

        box.addView(
                add,
                ap
        );


        TextView close =
                rowButton(
                        "×",
                        "Close"
                );

        close.setOnClickListener(
                v ->
                        animateSideMenuClose(
                                box,
                                dialog,
                                null
                        )
        );

        box.addView(
                close,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                )
        );


        dialog.setContentView(box);

        dialog.setCanceledOnTouchOutside(true);

        dialog.setCancelable(true);


        dialog.setOnShowListener(
                x -> {

                    Window w =
                            dialog.getWindow();

                    if (w != null) {

                        w.setBackgroundDrawable(
                                new android.graphics.drawable.ColorDrawable(
                                        Color.TRANSPARENT
                                )
                        );

                        WindowManager.LayoutParams lp =
                                w.getAttributes();

                        lp.width =
                                dp(320);

                        lp.height =
                                WindowManager.LayoutParams.MATCH_PARENT;

                        lp.gravity =
                                Gravity.LEFT
                                        | Gravity.TOP;

                        w.setAttributes(lp);

                        w.setDimAmount(.55f);

                        w.addFlags(
                                WindowManager.LayoutParams.FLAG_DIM_BEHIND
                        );


                        box.setTranslationX(
                                -dp(320)
                        );

                        box.setAlpha(.96f);

                        box.animate()
                                .translationX(0)
                                .setDuration(280)
                                .setInterpolator(
                                        new DecelerateInterpolator(
                                                1.5f
                                        )
                                )
                                .start();


                        View[] items = {
                                settings,
                                add,
                                close
                        };


                        for (
                                int i = 0;
                                i < items.length;
                                i++
                        ) {

                            View item =
                                    items[i];

                            item.setAlpha(0f);

                            item.setTranslationX(
                                    -dp(28)
                            );

                            item.setScaleX(.96f);
                            item.setScaleY(.96f);

                            final int delay =
                                    110
                                            + i * 75;

                            item.postDelayed(
                                    () -> {

                                        item.animate()
                                                .alpha(1f)
                                                .translationX(0)
                                                .scaleX(1f)
                                                .scaleY(1f)
                                                .setDuration(230)
                                                .setInterpolator(
                                                        new DecelerateInterpolator(
                                                                1.4f
                                                        )
                                                )
                                                .start();

                                    },
                                    delay
                            );
                        }
                    }
                }
        );

        dialog.show();
    }


    void animateSideMenuClose(
            final View box,
            final Dialog dialog,
            final Runnable next
    ) {

        box.animate()
                .translationX(-dp(320))
                .alpha(.7f)
                .setDuration(180)
                .setInterpolator(
                        new AccelerateInterpolator()
                )
                .withEndAction(
                        () -> {

                            dialog.dismiss();

                            if (next != null)
                                next.run();
                        }
                )
                .start();
    }


    TextView rowButton(
            String icon,
            String label
    ) {

        TextView r =
                tv(
                        icon
                                + "    "
                                + label,
                        16,
                        primaryText()
                );

        r.setTypeface(
                android.graphics.Typeface.create(
                        "sans-serif-medium",
                        android.graphics.Typeface.NORMAL
                )
        );

        r.setGravity(
                Gravity.CENTER_VERTICAL
                        | Gravity.LEFT
        );

        r.setPadding(
                dp(10),
                0,
                dp(10),
                0
        );

        addPressAnimation(r);


        GradientDrawable g =
                new GradientDrawable();

        g.setColor(
                isLight()
                        ? Color.WHITE
                        : Color.rgb(
                                52,
                                55,
                                60
                        )
        );

        g.setCornerRadius(
                dp(14)
        );

        r.setBackground(g);

        return r;
    }


    void showSettings() {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(18),
                dp(5),
                dp(18),
                0
        );


        Button theme =
                new Button(this);

        theme.setText(
                isLight()
                        ? "Switch to Dark Mode"
                        : "Switch to Day Mode"
        );

        theme.setOnClickListener(
                v -> {

                    prefs.edit()
                            .putBoolean(
                                    "lightMode",
                                    !isLight()
                            )
                            .apply();

                    recreate();
                }
        );

        box.addView(theme);


        Button pin =
                new Button(this);

        pin.setText(
                hasPin()
                        ? "Change / Remove PIN"
                        : "Set PIN"
        );

        pin.setOnClickListener(
                v -> showPinSettings()
        );

        box.addView(pin);


        new AlertDialog.Builder(this)
                .setTitle(
                        "Settings"
                )
                .setView(box)
                .setNegativeButton(
                        "Close",
                        null
                )
                .show();
    }


    boolean hasPin() {

        return prefs.getString(
                "pin",
                ""
        ).length() > 0;
    }


    void showPinSettings() {

        if (hasPin()) {

            new AlertDialog.Builder(this)
                    .setTitle("PIN")
                    .setItems(
                            new String[]{
                                    "Change PIN",
                                    "Remove PIN"
                            },
                            (d,w) -> {

                                if (w == 0) {

                                    showPinPad(
                                            "Change PIN",
                                            false,
                                            p -> savePin(p)
                                    );

                                } else {

                                    prefs.edit()
                                            .remove("pin")
                                            .apply();

                                    Toast.makeText(
                                            this,
                                            "PIN removed",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                    )
                    .show();

        } else {

            showPinPad(
                    "Set PIN",
                    false,
                    p -> savePin(p)
            );
        }
    }


    void savePin(
            String p
    ) {

        if (
                p.length() < 4
                        || p.length() > 8
        ) {

            Toast.makeText(
                    this,
                    "PIN must be 4 to 8 digits",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        prefs.edit()
                .putString(
                        "pin",
                        p
                )
                .apply();

        Toast.makeText(
                this,
                "PIN saved",
                Toast.LENGTH_SHORT
        ).show();
    }


    interface PinCallback {
        void done(String pin);
    }


    void showPinGate() {

        if (pinGateShowing)
            return;

        pinGateShowing = true;

        showPinPad(
                "Enter PIN",
                true,
                p -> {
                }
        );
    }


    void showPinPad(
            String title,
            boolean gate,
            PinCallback callback
    ) {

        final Dialog dialog =
                new Dialog(this);

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );


        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        root.setPadding(
                dp(28),
                dp(34),
                dp(28),
                dp(28)
        );

        root.setBackgroundColor(
                bg()
        );


        TextView heading =
                tv(
                        title,
                        27,
                        primaryText()
                );

        heading.setGravity(
                Gravity.CENTER
        );

        root.addView(
                heading,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)
                )
        );


        TextView dots =
                tv(
                        "",
                        25,
                        primaryText()
                );

        dots.setGravity(
                Gravity.CENTER
        );

        root.addView(
                dots,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                )
        );


        final StringBuilder pin =
                new StringBuilder();


        View.OnClickListener digit =
                v -> {

                    if (pin.length() < 8) {

                        pin.append(
                                ((TextView) v)
                                        .getText()
                        );

                        updatePinDots(
                                dots,
                                pin.length()
                        );
                    }
                };


        int[][] nums = {
                {1,2,3},
                {4,5,6},
                {7,8,9}
        };


        for (int[] row : nums) {

            LinearLayout r =
                    new LinearLayout(this);

            r.setGravity(
                    Gravity.CENTER
            );

            for (int n : row) {

                Button b =
                        pinButton(
                                String.valueOf(n)
                        );

                if (gate) {
                    b.setTypeface(
                            android.graphics.Typeface.create(
                                    "sans-serif-light",
                                    android.graphics.Typeface.NORMAL
                            )
                    );
                    b.setTextSize(22);
                }

                b.setOnClickListener(
                        digit
                );

                r.addView(
                        b,
                        new LinearLayout.LayoutParams(
                                dp(78),
                                dp(62)
                        )
                );
            }

            root.addView(r);
        }


        LinearLayout last =
                new LinearLayout(this);

        last.setGravity(
                Gravity.CENTER
        );


        Button clear =
                pinButton("C");

        if (gate) {
            clear.setTypeface(
                    android.graphics.Typeface.create(
                            "sans-serif-light",
                            android.graphics.Typeface.NORMAL
                    )
            );
            clear.setTextSize(22);
        }

        clear.setOnClickListener(
                v -> {

                    pin.setLength(0);

                    updatePinDots(
                            dots,
                            0
                    );
                }
        );


        Button zero =
                pinButton("0");

        if (gate) {
            zero.setTypeface(
                    android.graphics.Typeface.create(
                            "sans-serif-light",
                            android.graphics.Typeface.NORMAL
                    )
            );
            zero.setTextSize(22);
        }

        zero.setOnClickListener(
                digit
        );


        Button back =
                pinButton("⌫");

        if (gate) {
            back.setTypeface(
                    android.graphics.Typeface.create(
                            "sans-serif-light",
                            android.graphics.Typeface.NORMAL
                    )
            );
            back.setTextSize(22);
        }

        back.setOnClickListener(
                v -> {

                    if (pin.length() > 0)
                        pin.deleteCharAt(
                                pin.length() - 1
                        );

                    updatePinDots(
                            dots,
                            pin.length()
                    );
                }
        );


        last.addView(
                clear,
                new LinearLayout.LayoutParams(
                        dp(78),
                        dp(62)
                )
        );

        last.addView(
                zero,
                new LinearLayout.LayoutParams(
                        dp(78),
                        dp(62)
                )
        );

        last.addView(
                back,
                new LinearLayout.LayoutParams(
                        dp(78),
                        dp(62)
                )
        );

        root.addView(last);


        Button action =
                pinButton(
                        gate
                                ? "Unlock"
                                : "Save"
                );

        if (gate) {
            action.setTypeface(
                    android.graphics.Typeface.create(
                            "sans-serif-medium",
                            android.graphics.Typeface.NORMAL
                    )
            );
            action.setTextSize(18);

            android.graphics.drawable.GradientDrawable unlockBg =
                    new android.graphics.drawable.GradientDrawable();
            unlockBg.setColor(
                    isLight()
                            ? Color.WHITE
                            : Color.rgb(48, 50, 54)
            );
            unlockBg.setCornerRadius(dp(10));
            action.setBackground(unlockBg);
        }

        action.setOnClickListener(
                v -> {

                    if (pin.length() < 4) {

                        Toast.makeText(
                                this,
                                "Enter at least 4 digits",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    String entered =
                            pin.toString();


                    if (gate) {

                        if (
                                entered.equals(
                                        prefs.getString(
                                                "pin",
                                                ""
                                        )
                                )
                        ) {

                            pinGateShowing = false;

                            wasInBackground = false;

                            dialog.dismiss();

                        } else {

                            updatePinDots(
                                    dots,
                                    0
                            );

                            pin.setLength(0);

                            Toast.makeText(
                                    this,
                                    "Wrong PIN",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } else {

                        callback.done(
                                entered
                        );

                        dialog.dismiss();
                    }
                }
        );


        LinearLayout.LayoutParams ap =
                new LinearLayout.LayoutParams(
                        dp(250),
                        dp(58)
                );

        ap.setMargins(
                0,
                dp(18),
                0,
                0
        );

        root.addView(
                action,
                ap
        );


        dialog.setContentView(root);

        dialog.setCancelable(!gate);

        dialog.setCanceledOnTouchOutside(false);


        dialog.setOnDismissListener(
                d -> {

                    if (gate)
                        pinGateShowing = false;
                }
        );


        dialog.setOnShowListener(
                x -> {

                    Window w =
                            dialog.getWindow();

                    if (w != null) {

                        w.setBackgroundDrawable(
                                new android.graphics.drawable.ColorDrawable(
                                        bg()
                                )
                        );

                        w.setDimAmount(1f);

                        w.addFlags(
                                WindowManager.LayoutParams.FLAG_DIM_BEHIND
                        );

                        w.setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                        );

                        w.setLayout(
                                -1,
                                -1
                        );
                    }
                }
        );


        dialog.show();


        Window w =
                dialog.getWindow();

        if (w != null) {

            w.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            bg()
                    )
            );

            WindowManager.LayoutParams lp =
                    w.getAttributes();

            lp.width =
                    WindowManager.LayoutParams.MATCH_PARENT;

            lp.height =
                    WindowManager.LayoutParams.MATCH_PARENT;

            lp.dimAmount = 1f;

            w.setAttributes(lp);

            w.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            );
        }
    }


    Button pinButton(
            String s
    ) {

        Button b =
                new Button(this);

        b.setText(s);

        b.setTextSize(21);

        b.setTextColor(
                primaryText()
        );

        b.setBackgroundColor(
                isLight()
                        ? Color.WHITE
                        : Color.rgb(
                                48,
                                50,
                                54
                        )
        );

        return b;
    }


    void updatePinDots(
            TextView t,
            int n
    ) {

        StringBuilder s =
                new StringBuilder();

        for (int i = 0; i < n; i++)
            s.append("● ");

        t.setText(
                s.toString().trim()
        );
    }


    void saveOrderFromViews() {

        ArrayList<Achievement> visible =
                new ArrayList<>();

        for (
                int i = 0;
                i < list.getChildCount();
                i++
        ) {

            Object tag =
                    list.getChildAt(i)
                            .getTag();

            if (
                    tag instanceof Achievement
            )
                visible.add(
                        (Achievement) tag
                );
        }


        if (visible.isEmpty())
            return;


        ArrayList<Integer> slots =
                new ArrayList<>();

        for (
                int i = 0;
                i < data.size();
                i++
        ) {

            if (
                    matchesFilter(
                            data.get(i)
                    )
            ) {

                slots.add(i);
            }
        }


        if (
                slots.size()
                        == visible.size()
        ) {

            for (
                    int i = 0;
                    i < slots.size();
                    i++
            ) {

                data.set(
                        slots.get(i),
                        visible.get(i)
                );
            }
        }

        save();
    }


    void save() {

        JSONArray arr =
                new JSONArray();

        try {

            for (Achievement a : data) {

                JSONObject o =
                        new JSONObject();

                o.put(
                        "title",
                        a.title
                );

                o.put(
                        "desc",
                        a.desc
                );

                o.put(
                        "medal",
                        a.medal
                );

                o.put(
                        "done",
                        a.done
                );

                o.put(
                        "imagePath",
                        a.imagePath
                );

                o.put(
                        "imageShape",
                        a.imageShape
                );

                o.put(
                        "pinned",
                        a.pinned
                );

                o.put(
                        "customColor",
                        a.customColor
                );

                o.put(
                        "backgroundMode",
                        a.backgroundMode
                );

                arr.put(o);
            }

        } catch (Exception ignored) {
        }


        prefs.edit()
                .putString(
                        "items",
                        arr.toString()
                )
                .apply();
    }


    void load() {

        String raw =
                prefs.getString(
                        "items",
                        ""
                );

        if (raw.length() == 0)
            return;


        try {

            JSONArray a =
                    new JSONArray(raw);


            for (
                    int i = 0;
                    i < a.length();
                    i++
            ) {

                JSONObject o =
                        a.getJSONObject(i);


                Achievement item =
                        new Achievement(
                                o.optString(
                                        "title"
                                ),
                                o.optString(
                                        "desc"
                                ),
                                o.optString(
                                        "medal",
                                        "Platinum"
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


                item.customColor =
                        o.optInt(
                                "customColor",
                                Color.TRANSPARENT
                        );


                item.backgroundMode =
                        o.optString(
                                "backgroundMode",
                                "default"
                        );


                if (
                        item.backgroundMode == null
                                || item.backgroundMode.isEmpty()
                ) {

                    item.backgroundMode =
                            "default";
                }


                data.add(item);
            }

        } catch (Exception ignored) {
        }
    }


    int dp(
            int x
    ) {

        return (int) (
                x
                        * getResources()
                                .getDisplayMetrics()
                                .density
                        + .5f
        );
    }
}

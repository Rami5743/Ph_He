package org.rami5743.heph1;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The on-screen keyboard: five rows of keys built from {@link Layout}. */
final class KeyboardPanel extends LinearLayout {

    /** What the panel reports back to the input method service. */
    interface Listener {
        void onText(String text);
        void onBackspace();
        void onEnter();
    }

    private static final int REPEAT_FIRST_MS = 400;
    private static final int REPEAT_MS = 60;

    private final Listener listener;
    private final boolean dark;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean shift;
    private boolean shiftLocked;
    private boolean symbols;

    KeyboardPanel(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        int mode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        this.dark = mode == Configuration.UI_MODE_NIGHT_YES;
        setOrientation(VERTICAL);
        // the keys keep their QWERTY order even when the phone's language is
        // Hebrew, which would otherwise lay every row out right to left
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        setBackgroundColor(dark ? 0xFF14171C : 0xFFD8DCE3);
        int pad = dp(4);
        setPadding(pad, pad, pad, pad);
        build();
    }

    private int rowHeight() {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        return dp(landscape ? 42 : 54);
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    private void build() {
        removeAllViews();
        Layout.Key[][] rows = symbols ? Layout.SYMBOLS : Layout.LETTERS;
        for (Layout.Key[] row : rows) {
            LinearLayout line = new LinearLayout(getContext());
            line.setOrientation(HORIZONTAL);
            line.setLayoutDirection(LAYOUT_DIRECTION_LTR);
            // a fixed row height: the input view is measured WRAP_CONTENT by the
            // framework, so rows that only carry a weight would collapse to nothing
            addView(line, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight()));
            for (Layout.Key key : row) {
                line.addView(makeKey(key), new LayoutParams(0,
                        ViewGroup.LayoutParams.MATCH_PARENT, key.weight));
            }
        }
    }

    private int faceColor(Layout.Key key) {
        boolean accent = key.code != Layout.Key.NORMAL;
        if (key.code == Layout.Key.SHIFT && (shift || shiftLocked)) {
            return dark ? 0xFF3C6FD4 : 0xFF2F6FE0;
        }
        if (dark) return accent ? 0xFF2A2F38 : 0xFF3A414D;
        return accent ? 0xFFBFC6D2 : 0xFFFFFFFF;
    }

    private View makeKey(final Layout.Key key) {
        final TextView view = new TextView(getContext());
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setTextColor(dark ? 0xFFF2F4F8 : 0xFF11151C);
        boolean wide = key.code == Layout.Key.SPACE || key.code == Layout.Key.TO_SYMBOLS
                || key.code == Layout.Key.TO_LETTERS;
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, wide ? 15 : 21);
        view.setText(key.labelFor(shift || shiftLocked));

        final GradientDrawable face = new GradientDrawable();
        face.setCornerRadius(dp(6));
        face.setColor(faceColor(key));
        view.setBackground(face);

        int m = dp(2);
        view.setPadding(m, m, m, m);
        LinearLayout holder = new LinearLayout(getContext());
        holder.setPadding(m, m, m, m);
        holder.addView(view, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        view.setOnTouchListener(new KeyTouch(key, face));
        return holder;
    }

    /** Repeats backspace while the key is held down. */
    private final class Repeater implements Runnable {
        @Override public void run() {
            listener.onBackspace();
            handler.postDelayed(this, REPEAT_MS);
        }
    }

    /** Press, release and hold handling of one key. */
    private final class KeyTouch implements OnTouchListener {
        private final Layout.Key key;
        private final GradientDrawable face;
        private Repeater repeater;

        KeyTouch(Layout.Key key, GradientDrawable face) {
            this.key = key;
            this.face = face;
        }

        @Override public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    face.setColor(pressedColor(key));
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                    fire(key);
                    if (key.code == Layout.Key.BACKSPACE) {
                        repeater = new Repeater();
                        handler.postDelayed(repeater, REPEAT_FIRST_MS);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (repeater != null) {
                        handler.removeCallbacks(repeater);
                        repeater = null;
                    }
                    face.setColor(faceColor(key));
                    return true;
                default:
                    return true;
            }
        }
    }

    private int pressedColor(Layout.Key key) {
        return dark ? 0xFF4E5666 : 0xFF9FB4DC;
    }

    private void fire(Layout.Key key) {
        switch (key.code) {
            case Layout.Key.SHIFT:
                if (shiftLocked) {
                    shiftLocked = false;
                    shift = false;
                } else if (shift) {
                    shiftLocked = true;
                } else {
                    shift = true;
                }
                build();
                return;
            case Layout.Key.BACKSPACE:
                listener.onBackspace();
                return;
            case Layout.Key.ENTER:
                listener.onEnter();
                return;
            case Layout.Key.SPACE:
                listener.onText(" ");
                return;
            case Layout.Key.TO_SYMBOLS:
                symbols = true;
                build();
                return;
            case Layout.Key.TO_LETTERS:
                symbols = false;
                build();
                return;
            default:
                listener.onText(key.outputFor(shift || shiftLocked));
                if (shift && !shiftLocked) {
                    shift = false;
                    build();
                }
        }
    }

    /** Called when a new text field is opened, so the panel starts clean. */
    void reset() {
        if (shift || shiftLocked || symbols) {
            shift = false;
            shiftLocked = false;
            symbols = false;
            build();
        }
    }
}

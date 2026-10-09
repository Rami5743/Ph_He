package org.rami5743.heph1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Two buttons, in the order the two steps have to be taken: first enable the
 * keyboard in the system list, then pick it as the active one.
 */
public class SetupActivity extends Activity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        root.addView(title(getString(R.string.app_name)));
        root.addView(body(getString(R.string.intro)));
        root.addView(body(getString(R.string.tip_lang)));

        root.addView(body(getString(R.string.step1)));
        root.addView(button(getString(R.string.btn_enable), new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }));

        root.addView(body(getString(R.string.step2)));
        root.addView(button(getString(R.string.btn_pick), new View.OnClickListener() {
            @Override public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        }));

        root.addView(body(getString(R.string.try_here)));
        EditText probe = new EditText(this);
        probe.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        probe.setGravity(Gravity.END);
        probe.setTextDirection(View.TEXT_DIRECTION_RTL);
        root.addView(probe, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    private TextView title(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        t.setGravity(Gravity.END);
        t.setPadding(0, 0, 0, dp(12));
        return t;
    }

    private TextView body(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setGravity(Gravity.END);
        t.setPadding(0, dp(16), 0, dp(6));
        return t;
    }

    private Button button(String text, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        b.setAllCaps(false);
        GradientDrawable face = new GradientDrawable();
        face.setCornerRadius(dp(8));
        face.setColor(0xFF2F6FE0);
        b.setBackground(face);
        b.setTextColor(Color.WHITE);
        b.setOnClickListener(onClick);
        return b;
    }
}

package jackpal.androidterm.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class ShowSoftKeyboard {
    private static final View.OnFocusChangeListener SHOW_ON_FOCUS = (v, hasFocus) -> {
        if (hasFocus) {
            show(v);
        }
    };

    private static final View.OnClickListener SHOW_ON_CLICK = ShowSoftKeyboard::show;

    public static void onFocus(View... views) {
        for (View view : views)
            view.setOnFocusChangeListener(SHOW_ON_FOCUS);
    }

    public static void onClick(View... views) {
        for (View view : views)
            view.setOnClickListener(SHOW_ON_CLICK);
    }

    public static void show(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    private ShowSoftKeyboard() {
    }
}

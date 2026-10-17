package com.hyperos.quickopen.ui;

import android.text.Editable;
import android.text.TextWatcher;

/** 简化 TextWatcher：文本变化时执行给定回调 */
class SimpleTextWatcher implements TextWatcher {

    private final Runnable onChanged;

    SimpleTextWatcher(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (onChanged != null) onChanged.run();
    }
}

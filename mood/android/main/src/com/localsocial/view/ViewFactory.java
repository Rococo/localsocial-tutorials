package com.localsocial.view;

import android.view.View;
import android.view.ViewGroup;

public interface ViewFactory {
    View getView(Object target, View convertView, ViewGroup parent);
    boolean bindView(Object target, View v);
}

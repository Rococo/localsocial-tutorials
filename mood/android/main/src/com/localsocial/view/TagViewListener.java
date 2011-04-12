package com.localsocial.view;

import com.localsocial.model.Tag;

import android.view.View;

public abstract class TagViewListener implements View.OnClickListener{
    protected Tag m_target;
    void setTarget(Tag target){
        this.m_target = target;
    }
}

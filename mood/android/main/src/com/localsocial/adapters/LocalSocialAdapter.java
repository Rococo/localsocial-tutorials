package com.localsocial.adapters;

import android.widget.ListAdapter;

import com.localsocial.view.ViewFactory;

public interface LocalSocialAdapter extends ListAdapter{

    /**
     * set the factory to use to generate views for this array  
     * @param vFactory the view factory
     */
    public abstract void setViewFactory(ViewFactory viewFactory);
    
    /**
     * get the row object by id or position  and convert it from a cursor if necessary
     * @param idOrPosition the id for a cursor adapter or position for an array adapter
     * @return the object
     */
    public abstract Object getObject(int idOrPosition);
    
}
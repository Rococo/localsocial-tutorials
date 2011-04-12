/**
 * Lorensius W. L. T
 * 
 * http://www.londatiga.net
 * 
 * lorenz@londatiga.net 
 */

package com.localsocial.actions;

import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;

import android.os.AsyncTask;
import android.view.View.OnClickListener;

/**
 * Action item, displayed as menu with icon and text.
 * 
 * @author Lorensius. W. L. T
 * 
 */
public class ActionItem {
    private Drawable icon;
    private Bitmap thumb;
    private String title;
    private boolean selected;
    private OnClickListener listener;
    private Command command;

    /**
     * Constructor
     */
    public ActionItem() {
    }

    /**
     * Constructor
     * 
     * @param icon
     *            {@link Drawable} action icon
     */
    public ActionItem(Drawable icon) {
        this.icon = icon;
    }

    /**
     * Set action command
     * 
     * @param command
     *            the action command
     */
    public void setCommand(Command command) {
        this.command = command;
    }

    /**
     * execute the command
     * 
     * @param param
     *            the target of the command
     */
    public void execute(Object param) {
        if (this.command != null)
            command.execute(param);
    }

    /**
     * should the action item be shown
     * 
     * @param param
     *            the target of the command
     * @return true id the action item should be shown
     */
    public boolean show(Object param) {
        if (this.command != null)
            return command.show(param);
        return true;
    }

    /**
     * Set action title
     * 
     * @param title
     *            action title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get action title
     * 
     * @return action title
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Set action icon
     * 
     * @param icon
     *            {@link Drawable} action icon
     */
    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    /**
     * Get action icon
     * 
     * @return {@link Drawable} action icon
     */
    public Drawable getIcon() {
        return this.icon;
    }

    /**
     * Set on click listener
     * 
     * @param listener
     *            on click listener {@link View.OnClickListener}
     */
    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    /**
     * Get on click listener
     * 
     * @return on click listener {@link View.OnClickListener}
     */
    public OnClickListener getListener() {
        return this.listener;
    }

    /**
     * Set selected flag;
     * 
     * @param selected
     *            Flag to indicate the item is selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Check if item is selected
     * 
     * @return true or false
     */
    public boolean isSelected() {
        return this.selected;
    }

    /**
     * Set thumb
     * 
     * @param thumb
     *            Thumb image
     */
    public void setThumb(Bitmap thumb) {
        this.thumb = thumb;
    }

    /**
     * Get thumb image
     * 
     * @return Thumb image
     */
    public Bitmap getThumb() {
        return this.thumb;
    }
}

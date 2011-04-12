package com.localsocial.view.actions;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.localsocial.actions.ActionItem;
import com.localsocial.actions.Command;
import com.localsocial.actions.QuickAction;
import com.localsocial.adapters.LocalSocialAdapter;
import com.localsocial.mood.R;

/**
 * a factory object to help create QuickActions and the ActionItems
 * @author jimoleary
 *
 */
public class ActionFactory {
    
    /**
     * create a new action factory
     * @param context the context to use to create objects
     */
    public ActionFactory(Context context) {
        setContext(context);
    }
    
    /**
     * set the context
     * @param context the context to use to create objects
     */
    public void setContext(Context context) {
        this.m_context = context;
    }
    
    /**
     * set the ui handler to use for ui commands
     * @param handler the ui handler
     */
//    public void setHandler(Handler handler) {
//        this.m_handler = handler;
//    }
    
    /**
     * set the collect flag, if set to true the current will return an array of the last few ActionItems
     * created
     * @param collect true => collect the action items as they are created
     */
    public void setCollect(boolean collect) {
        m_collect = collect;
    }

    /**
     * clear the list of collected action items
     */
    public void clear() {
        actions.clear();
    }

    /**
     * get the list of action items (it doesn't clear the list)
     * @return an array of action items
     */
    public ActionItem[] current(){
        return current(false);
    }
    
    /**
     * get the list of action items
     * @param clear if true then clear the list 
     * @return an array of action items
     */
    public ActionItem[] current(boolean clear){
        ActionItem[] current = actions.toArray(new ActionItem[]{});
        if(clear)
            actions.clear();
        return current;
    }
    
    /**
     * create an action item with no command
     * @param title the action item title
     * @param drawable the action item icon
     * @return a new ActionItem
     */
    public ActionItem createActionItem(String title, int drawable) {
        return createActionItem(title, drawable,null);
    }
    
    /**
     * create an action item 
     * @param title the action item title
     * @param drawable the action item icon
     * @param command the command to directly execute when this action item is clicked
     * @return a new ActionItem
     */
    public ActionItem createActionItem(String title, int drawable,Command command ) {
        final ActionItem item = new ActionItem();
        item.setTitle(title);
        item.setIcon(getDrawable(drawable));
        item.setCommand(command);
        if(m_collect)
            actions.add(item);
        return item;
    }

    /**
     * add the array of action items to the quick action
     * @param id the id of the image item to update with the more drawable
     * @param list the list to attach the quick action to
     * @param actions the array of actions to attach
     * @param adapter the array of objects attached to the list
     */
    public static void addQuickActionToList(final ListView list,final ActionItem []actions,final int vid) {
        ListAdapter la = list.getAdapter();
        if(la instanceof HeaderViewListAdapter) {
            HeaderViewListAdapter hvla =(HeaderViewListAdapter)la;
            la = hvla.getWrappedAdapter();
        }
        System.out.println("la="+ la);
        final BaseAdapter adapter = (BaseAdapter)la;
        LocalSocialAdapter temp = null;
        if(adapter instanceof LocalSocialAdapter) {
            temp = (LocalSocialAdapter)adapter;
            
        }
        final LocalSocialAdapter lsa = temp;
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int id, long position) {
                
                QuickAction mQuickAction = new QuickAction(view);
                Object known;
                if(lsa != null) {
                    known = lsa.getObject(id);
                } else {
                    known =adapter.getItem((int) position);
                }

                updateSelectImage(view, vid, R.drawable.ic_list_more_selected); 

                for(ActionItem action: actions) {
                    attachQuickAction(action.getTitle(), action, mQuickAction, known);
                }

                mQuickAction.setAnimStyle(QuickAction.ANIM_AUTO);
                mQuickAction.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        adapter.notifyDataSetChanged();
                    }
                });

                mQuickAction.show();
            }

            private void attachQuickAction(final String type, final ActionItem item, final QuickAction action,final Object o) {
                if(item.show(o)){
                    action.addActionItem(item);
                    item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            action.dismiss();
                            item.execute(o);
                        }
    
                    });
                }
            }

            private void updateSelectImage(View row, int id,int drawable) {
                ImageView iv = (ImageView) row.findViewById(id);
                if(iv != null)
                    iv.setImageResource(drawable);//R.drawable.ic_list_more_selected);
            }

        });
    }

    /**
     * get the drawable for the id
     * @param drawable the id
     * @return the Drawable resource
     */
    Drawable getDrawable(int drawable) {
        return m_context.getResources().getDrawable(drawable);
    }
    
    private boolean m_collect = false;
    private List<ActionItem> actions = new ArrayList<ActionItem>();
    private Context m_context;
//    private Handler m_handler;
}

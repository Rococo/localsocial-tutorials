package com.localsocial.nhw;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.localsocial.Device;
import com.localsocial.LoggerFactory;

public class EventItemAdapter extends ArrayAdapter<Event> {

    public EventItemAdapter(Context context, int customViewId,
            List<Event> eventList, Map<String, Device> favourites) {
        super(context, 0, eventList);
        Log.i(TAG, "<ctor>");
        this.customViewId = customViewId;
        this.favourites = favourites;
        this.inflater = LayoutInflater.from(context);

    }

    // @ Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "getView(" + position + "," + convertView + "," + parent
                + ")");
        View v = convertView;
        final Event event = getItem(position);
        final Device device = event.getDevice();
        Log.d(TAG, "getView(" + position + ") event=" + event);
        if (v == null) {
            v = inflater.inflate(customViewId, null);
        }
        TextView tview;
        Log.d(TAG, "getView(" + position + ") map.get(" + device + ") ");

        ImageView iview = (ImageView) v.findViewById(R.id.mood_icon);
        if (event.getType().contains("property")) {
            iview.setImageResource(R.drawable.ic_menu_info_details);
        } else if (event.getType().contains("scanModeChanged")) {
            if (event.getType().contains("result=0")) {
                iview.setImageResource(R.drawable.ic_discoverable);
            } else {
                iview.setImageResource(R.drawable.ic_not_discoverable);
            }
        } else if (event.getType().contains("observing")) {
            iview.setImageResource(R.drawable.ic_menu_forward);
        } else if (event.getType().contains("discovered")) {
            iview.setImageResource(R.drawable.create_contact);
        } else if (event.getType().contains("scanStarted")) {
            iview.setImageResource(R.drawable.ic_scan);
        } else if (event.getType().contains("scanStopped")) {
            iview.setImageResource(R.drawable.ic_notification_clear_all);
        } else if (event.getType().contains("inRange")) {
            iview.setImageResource(R.drawable.star_on);
        } else if (event.getType().contains("outOfRange")) {
            iview.setImageResource(R.drawable.star_off);
        } else {
            iview.setImageResource(R.drawable.icon);
        }
        if (device != null) {
            iview = (ImageView) v.findViewById(R.id.fav_icon);
            iview.setVisibility(favourites.containsKey(device.getAddress()) ? View.VISIBLE
                    : View.INVISIBLE);

            tview = (TextView) v.findViewById(R.id.device_details);
            String n;
            if (device.getName() == null) {
                n = device.getAddress();
            } else {
                n = device.getName() + "(" + device.getAddress() + ")";

            }
            tview.setText(n);
        } else {
            iview = (ImageView) v.findViewById(R.id.fav_icon);
            iview.setVisibility(View.INVISIBLE);
            tview = (TextView) v.findViewById(R.id.device_details);
            tview.setText("");

        }

        tview = (TextView) v.findViewById(R.id.mood);
        tview.setText(event.getType());

        return v;
    }

    public void postNotifyDataSetChanged() {
        Log.d(TAG,
                "postNotifyDataSetChanged() calling handler.sendEmptyMessage(0)");
        handler.sendEmptyMessage(0);
    }

    // handler for UI messages
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            Log.d(TAG, "handler handleMessage message=" + message);
            EventItemAdapter.this.notifyDataSetChanged();
        }
    };

    private LayoutInflater inflater;
    private Map<String, Device> favourites;
    private int customViewId;
    private static final String TAG = "NHW/"
            + LoggerFactory.getClassName(EventItemAdapter.class);
}

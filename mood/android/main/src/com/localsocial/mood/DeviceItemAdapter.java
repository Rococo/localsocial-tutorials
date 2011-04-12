package com.localsocial.mood;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.localsocial.model.Tag;

public class DeviceItemAdapter extends ArrayAdapter<Device> {

    public DeviceItemAdapter(Context context, int customViewId,
            List<Device> deviceList, Map<String, Tag[]> tags,
            Map<String, Device> favourites) {
        super(context, 0, deviceList);
        Log.i(TAG, "<ctor>");
        this.context = context;
        this.customViewId = customViewId;
        this.map = tags;
        this.favourites = favourites;
        this.inflater = LayoutInflater.from(context);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "getView(" + position + "," + convertView + "," + parent
                + ")");
        Log.i(TAG, "getView map=" + map);
        View v = convertView;
        final Device device = getItem(position);
        Log.d(TAG, "getView(" + position + ") device=" + device);
        if (v == null) {
            v = inflater.inflate(customViewId, null);
        }
        TextView tview;
        Tag[] tags = map.get(device.getAddress());
        Log.d(TAG, "getView(" + position + ") map.get(" + device.getAddress()
                + ") = " + tags);
        String mood = "<unknown>";
        String icon = null;
        if(tags != null ) {
            for(Tag tag : tags){
                if ("Mood".equalsIgnoreCase(tag.getName())) {
                    mood = tag.getValue();
                }
                if ("MoodIcon".equalsIgnoreCase(tag.getName())) {
                    icon = tag.getValue();
                }
            }
        }
        ImageView iview = (ImageView) v.findViewById(R.id.device_image);
        Bitmap image = getImageFromAsset("images/", icon);
        if (icon == null || image == null) {
            iview.setImageResource(R.drawable.icon);
        } else {
            iview.setImageBitmap(image);
        }

        iview = (ImageView) v.findViewById(R.id.favourite_image);
        Log.d(TAG,"iview=" + iview);
        iview.setVisibility(favourites.containsKey(device.getAddress()) ? View.VISIBLE
                : View.INVISIBLE);

        tview = (TextView) v.findViewById(R.id.device_name);
        tview.setText(mood);

        tview = (TextView) v.findViewById(R.id.device_bdaddr);
        String n;
        if (device.getName() == null) {
            n = device.getAddress();
        } else {
            n = device.getName() + "(" + device.getAddress() + ")";

        }
        tview.setText(n);
        return v;
    }

    public Bitmap getImageFromAsset(String location, String imageName) {
        AssetManager mngr = context.getAssets();
        Bitmap bitmap = null;
        if (imageName != null) {
            try {
                InputStream is = mngr.open(location + imageName);
                bitmap = BitmapFactory.decodeStream(is);
                // also tried "Files/" + imageName per example on Stack
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    public void postNotifyDataSetChanged() {
        Log.d(TAG,
                "postNotifyDataSetChanged() calling handler.sendEmptyMessage(0)");
        handler.sendEmptyMessage(0);
    }

    public String toString() {
        return "{DeviceItemAdapter map=" + map + "}" + super.toString();
    }

    // handler for UI messages
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            Log.d(TAG, "handler handleMessage message=" + message);
            DeviceItemAdapter.this.notifyDataSetChanged();
        }
    };

    private LayoutInflater inflater;
    private Map<String, Tag[]>map;
    private Map<String, Device> favourites;
    private int customViewId;
    private static final String TAG = "Dowser/"
            + LoggerFactory.getClassName(DeviceItemAdapter.class);
    private Context context;

}

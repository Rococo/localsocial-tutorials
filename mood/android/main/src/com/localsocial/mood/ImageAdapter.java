package com.localsocial.mood;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {
    int mGalleryItemBackground;

    public ImageAdapter(Context con, String[] files) {
        this.m_con = con;
        m_resources = new ArrayList<String>(Arrays.asList(files));
        m_views = new ArrayList<ImageView>(m_resources.size());

        TypedArray a = con.obtainStyledAttributes(R.styleable.Theme);
        mGalleryItemBackground = a.getResourceId(
                R.styleable.Theme_android_galleryItemBackground, 0);
        a.recycle();
    }

    @Override
    public int getCount() {
        return m_resources.size();
    }

    @Override
    public Object getItem(int position) {
        return m_views.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView img = new ImageView(m_con);
        Bitmap image = getImageFromAsset("images/" + m_resources.get(position));
        img.setImageBitmap(image);
        m_views.add(img);

        // img.setLayoutParams(new Gallery.LayoutParams(100,100));
        img.setScaleType(ImageView.ScaleType.FIT_XY);
        img.setBackgroundResource(mGalleryItemBackground);
        return img;
    }

    public String getImageName(View view) {
        int position = m_views.indexOf(view);
        String name = null;
        if (position != -1) {
            name = getImageName(position);
        }
        return name;
    }

    public String getImageName(int position) {
        return m_resources.get(position);
    }

    public int getImagePosition(String icon) {
        return m_resources.indexOf(icon);
    }

    public Bitmap getBitmap(int position) {
        return getImageFromAsset("images/" + m_resources.get(position));
    }

    public Bitmap getImageFromAsset(String imageName) {
        AssetManager mngr = m_con.getAssets();
        try {
            m_is = mngr.open(imageName);
            m_bitmap = BitmapFactory.decodeStream(m_is);
            // also tried "Files/" + imageName per example on Stack
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return m_bitmap;
    }

    Context m_con;
    // array to hold the values of image resources
    // String[] m_resources;
    List<String> m_resources;
    List<ImageView> m_views;

    private InputStream m_is;
    private Bitmap m_bitmap;

}
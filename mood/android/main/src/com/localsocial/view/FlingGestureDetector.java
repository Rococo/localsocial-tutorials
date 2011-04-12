package com.localsocial.view;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TabHost;

import com.localsocial.LoggerFactory;

public class FlingGestureDetector extends GestureDetector.SimpleOnGestureListener {
    

    public FlingGestureDetector(){
    }

    public FlingGestureDetector(TabHost tabHost){
        setTabHost(tabHost);
    }

    public void setTabHost(TabHost tabHost){
        this.m_tabHost = tabHost;
    }
    public void setMin(int min){
        this.m_min= min;
    }
    public void setMax(int max){
        this.m_max = max;        
    }
    public void setThreshold(int threshold){
        this.m_threshold = threshold;
    }
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        Log.v(TAG, "onFling e1=" + e1 + ",e2=" + e2 + ",velocityX=" + velocityX + ",velocityY=" + velocityY);
        if (Math.abs(e1.getY() - e2.getY()) > m_max) {
            return false;
        } else {
            try {
                int current = m_tabHost.getCurrentTab();
                int count = m_tabHost.getTabWidget().getTabCount();
                // right to left swipe
                if (e1.getX() - e2.getX() > m_min && Math.abs(velocityX) > m_threshold) {
                    Log.d(TAG, "onFling right to left");
                    m_tabHost.setCurrentTab((current + 1) % count);
                    //left to right swipe
                    return true;
                } else if (e2.getX() - e1.getX() > m_min && Math.abs(velocityX) > m_threshold) {
                    Log.d(TAG, "onFling left to right");
                    int n = current - 1;
                    if (n < 0) {
                        n = count - 1;
                    }
                    m_tabHost.setCurrentTab(n);
                    return true;
                }
            } catch (Exception e) {
                Log.w(TAG, "onFling", e);
                // nothing
            }
            return false;
        }
    }
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;


    private TabHost m_tabHost;
    private int m_max = SWIPE_MAX_OFF_PATH;
    private int m_min = SWIPE_MIN_DISTANCE;
    private int m_threshold = SWIPE_THRESHOLD_VELOCITY;

    private static final String TAG = "Tulsi/" + LoggerFactory.getClassName(FlingGestureDetector.class);
}
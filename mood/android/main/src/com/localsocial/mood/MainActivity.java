/*
 * Copyright (c) 2001 - 2010 Rococo Software Ltd., 3 Lincoln Place,
 * Dublin 2 Ireland. All Rights Reserved.
 *
 * This software is distributed under licenses restricting its use,
 * copying, distribution, and decompilation. No part of this
 * software may be reproduced in any form by any means without prior
 * written authorization of Rococo Software Ltd. and its licensors, if
 * any.
 *
 * This software is the confidential and proprietary information
 * of Rococo Software Ltd. You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of
 * the license agreement you entered into with Rococo Software Ltd.
 * Use is subject to license terms.
 *
 * Rococo Software Ltd. has intellectual property rights relating
 * to the technology embodied in this software. In particular, and
 * without limitation, these intellectual property rights may include
 * one or more patents, or pending patent applications.
 */
package com.localsocial.mood;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.localsocial.Device;
import com.localsocial.LocalSocial;
import com.localsocial.LocalSocialFactory;
import com.localsocial.LoggerFactory;
import com.localsocial.Platform;
import com.localsocial.actions.ActionItem;
import com.localsocial.actions.CommandAdapter;
import com.localsocial.actions.QuickAction;
import com.localsocial.config.SimpleAppConfiguration;
import com.localsocial.model.RemoteDevice;
import com.localsocial.model.Tag;
import com.localsocial.oauth.AccessToken;
import com.localsocial.oauth.OAuthConsumer;
import com.localsocial.oauth.RequestToken;
import com.localsocial.oauth.Verifier;
import com.localsocial.proximity.PeriodicScan;
import com.localsocial.proximity.observers.DeviceObserver;
import com.localsocial.proximity.observers.DeviceObserverAdapter;
import com.localsocial.proximity.observers.NeighbourhoodObserver;
import com.localsocial.proximity.observers.NeighbourhoodObserverAdapter;
import com.localsocial.remote.RemoteFactory;
import com.localsocial.remote.exception.NoSuchObjectException;
import com.localsocial.remote.exception.UnauthorizedException;
import com.localsocial.view.actions.ActionFactory;

/**
 * Main Activity for Mood application. In general methods starting with ;
 * <ul>
 * <li><b>render</b> will update the view</li>
 * <li><b>do</b> or <>post</b> run in a different thread or handler (depending )
 * on the context</li>
 * </ul>
 * 
 * So render method can be called from the main UI thread and methods called
 * doRender can be called from any context.
 * 
 * For example, renderLogin can be called from the onCreate method but
 * postRenderLogin must be called from doAuthoriseAndExchange ;-)
 * 
 * @author jimoleary
 * 
 */
public class MainActivity extends Activity {

    // Android life cycle methods
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Log.i(TAG, LocalSocialFactory.banner());

        imgAdapter = new ImageAdapter(this, listAssets());
        m_devices = new ArrayList<Device>();

        m_localsocial = bootstrap();
        m_localsocial.getNeighbourhood().observeNeighbourhood(getNeighbourhoodObserver());

        m_remote = m_localsocial.getRemoteFactory();
        m_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dAdapter = new DeviceItemAdapter(this, R.layout.deviceview_row, m_devices, m_map, m_favourites);

        createMainView();
    }

    void createMainView(){
        setContentView(R.layout.main);
        updateMoodView(m_mood);
        updateMoodIconView(m_icon);
        
        ImageButton image = (ImageButton) findViewById(R.id.mood_icon);
        createPopupForView(image);
        
        Button button = (Button) findViewById(R.id.save_btn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView tv = (TextView) findViewById(R.id.mood_edit);
                mood(tv.getText().toString());
            }
        });
        ListView dView = (ListView) findViewById(R.id.devicesview);
        TextView tv = new TextView(this);
        tv.setText("Discovered Devices");
        tv.setGravity(Gravity.CENTER);
        tv.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (m_localsocial.getNeighbourhood().isScanning()) {
                    toast("scan already in progress");
                } else {
                    toast("scan starting");
                    restartScan();
                }
            }
        });

        dView.addHeaderView(tv);
        dView.setAdapter(dAdapter);
        ActionFactory af  = new ActionFactory(this);
        ActionItem[] actions =  createActions(af);
        ActionFactory.addQuickActionToList(dView, actions, R.id.mood_icon);
        
        dView.setFocusable(true);
        
    }
    /**
     * Called when-ever the activity is displayed. The order of execution is :
     * 
     */
    @Override
    protected void onStart() {
        Log.i(TAG, "onStart() ");
        super.onStart();
        start();
    }

    /**
     * Called when-ever the activity is displayed. The order of execution is :
     * 
     */
    @Override
    protected void onStop() {
        Log.i(TAG, "onStop() ");
        super.onStop();
        stop();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.about:
            Intent myIntent = new Intent(this.getApplicationContext(), About.class);
            startActivity(myIntent);
            break;
        case R.id.discovery_status:
            setDiscoverable();
            break;
        case R.id.scan_status:
            restartScan();
            break;
        case R.id.clear:
            clear();
            break;
        }
        return true;
    }

    /**
     * handle activity startup
     * <ul>
     * <li>check if we have an access token</li>
     * <li>no token then try load one</li>
     * <li>if we have an access token now then go to the main screen</li>
     * <li>if not then get one</li>
     * </ul>
     */
    protected void start() {
        if (!authorised()) {
            Log.d(TAG, "attempting to load access token");
            try {
                m_aToken = m_localsocial.loadAccessToken();
            } catch (UnauthorizedException e) {
                Log.w(TAG, "error recreating access token", e);
            }
        }
        if (authorised()) {
            if(m_loadTask == null) {
                m_loadTask = new LoadRemoteDataTask();
                m_loadTask.execute();
            } else {
                updateMoodView(m_mood);
                updateMoodIconView(m_icon);
            }
        } else {
            renderLogin();
        }
    }

    /**
     * update the text view with the current mood 
     * @param mood the current mood
     */
    protected void updateMoodView(String mood) {
        // set up the ui
        TextView tv = (TextView) findViewById(R.id.mood_edit);
        if(tv!= null) {
            if (mood == null)
                tv.setText("<no mood set>");
            else
                tv.setText(mood);            
        }
    }

    /**
     * update the image view with the current mood icon 
     * @param icon the current mood icon
     */
    protected void updateMoodIconView(String icon) {
        ImageButton image = (ImageButton) findViewById(R.id.mood_icon);
        if(image!= null) {
            Bitmap bm = imgAdapter.getImageFromAsset("images/" + icon);
            if (bm != null) {
                image.setImageBitmap(bm);
            } else {
                image.setImageResource(R.drawable.icon);
            }
        }
    }

    
    /**
     * stop the activity
     * <ul>
     * <li>remove all observers</li>
     * <li>stop the scan</li>
     * </ul>
     */
    protected void stop() {
        if (m_localsocial != null) {
            Log.d(TAG, "removing neighbourhood observer");
            m_localsocial.getNeighbourhood().removeObserver(getNeighbourhoodObserver());
            if (m_favourites != null) {
                for (Map.Entry<String, Device> entry : m_favourites.entrySet()) {
                    Log.d(TAG, "removing observer for " + entry.getKey());
                    entry.getValue().remove(m_dobby);
                }
            }

            stopScan();
        }

    }

    /**
     * stop the current periodic scan
     * 
     */
    protected void stopScan() {
        if (m_scan != null) {
            Log.d(TAG, "stop scan");
            m_scan.stop();
            m_scan = null;
        } else {
            Log.d(TAG, "not scanning");
        }
    }

    /**
     * start a new scan
     * 
     * scan the neighbourhood
     * 
     */
    protected void startScan() {
        m_scan = m_localsocial.getNeighbourhood().startScan();
    }

    /**
     * stop the current scan if there is one and then start a new one
     * 
     */
    protected void restartScan() {
        stopScan();
        startScan();
    }

    /**
     * stop the current scan if there is one and then start a new one
     * 
     */
    protected void clear() {
        try {
            if(m_localsocial != null) {
                Platform platform = m_localsocial.getConfig().getPlatformContext();
                LocalSocialFactory.clearCredentials(platform);
            }
            if (m_loadTask != null) {
                m_loadTask.cancel(true);
            }
            m_loadTask = null;
            // m_tags = null;
            m_favourites = null;

            renderLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * scan the neighbourhood
     * 
     */
    protected void setDiscoverable() {
        m_localsocial.getNeighbourhood().setDiscoverable();
    }

    /**
     * get the NeighbourhoodObserver
     * 
     * @return the NeighbourhoodObserver
     */
    protected NeighbourhoodObserver getNeighbourhoodObserver() {
        return m_nobby;
    }

    /**
     * check if this application has an access token (note , even if there is a
     * token, it may not be valid).authorise
     * 
     * @return true if we have an access token
     */
    boolean authorised() {
        return m_aToken != null;
    }

    /**
     * start the authorisation process. For OAuth1.0a the process is :
     * <ol>
     * <li>generate a new request token for this client application and device</li>
     * <li>ask the end user to authenticate and then authorise the request token
     * </li>
     * <li>exchange the request token for an access token</li>
     * </ol>
     */
    public void authorise() {
        Log.d(TAG, "authorise()");
        if(m_authTask == null) {
            renderAuthorising();
            m_authTask = new AuthoriseTask();
            m_authTask.execute("Allow");
        }
    }

    /**
     * get a list of the image icons from the file system
     * 
     * @return an array of the assets
     */
    public String[] listAssets() {
        if (m_assets == null) {
            AssetManager assetManager = getAssets();
            String[] tmp = null;
            ArrayList<String> files = new ArrayList<String>();
            try {
                tmp = assetManager.list("images");
            } catch (IOException e) {
                Log.e("tag", e.getMessage());
            }
            for (String a : tmp) {
                if (a.startsWith("y_")) {
                    files.add(a);
                }
            }
            m_assets = new String[files.size()];
            files.toArray(m_assets);
        }
        return m_assets;
    }

    /**
     * select the mood icon
     */
    public void selectIcon() {

        setContentView(R.layout.gallery);
        Gallery gallery = (Gallery) findViewById(R.id.gallery);

        gallery.setAdapter(imgAdapter);
    }

    /**
     * get the local device address
     * 
     * @return the local device address
     */
    String getAddress() {
        if (isBlank(m_address)) {
            m_address = getConfig().getAddress();
            if (isBlank(m_address)) {
                m_address = m_localsocial.getNeighbourhood().getAddress();
            }
        }
        return m_address;
    }

    /**
     * get the local device cod
     * 
     * @return the local device cod
     */
    String getBluetoothCod() {
        if (isBlank(m_cod)) {
            m_cod = getConfig().getBluetoothCod();
            if (isBlank(m_cod)) {
                m_cod = m_localsocial.getNeighbourhood().getType();
            }
        }
        return m_cod;
    }

    /**
     * get the local device name
     * 
     * @return the local device name
     */
    String getBluetoothName() {
        if (isBlank(m_name)) {
            m_name = getConfig().getBluetoothName();
            if (isBlank(m_name)) {
                m_cod = m_localsocial.getNeighbourhood().getName();
            }
        }
        return m_name;
    }

    LocalSocial.Configuration getConfig() {
        return m_localsocial.getConfig();
    }

    /**
     * get the local device name T
     * 
     * @return the local device name
     */
    static final boolean isBlank(String v) {
        return v == null || v.trim().length() == 0;
    }

    public void checkException(Exception e) {
        if(e == null) {
            start();
        } else {
            // do something else
        }
        

    }

    void createPopupForView(View image) {
        // http://www.londatiga.net/it/how-to-create-quickaction-dialog-in-android/
        final List<ActionItem> actions = new ArrayList<ActionItem>();
        image.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                final QuickAction qa = new QuickAction(view);

                String[] assets = listAssets();
                for (String asset : assets) {
                    final String icon = asset;

                    ActionItem item = new ActionItem();

                    Bitmap b = imgAdapter.getImageFromAsset("images/" + asset);
                    BitmapDrawable drawable = new BitmapDrawable(b);
                    item.setTitle(asset.substring(2, asset.length() - 4));
                    item.setIcon(drawable);
                    actions.add(item);

                    final View.OnClickListener l = new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            qa.dismiss();
                            Log.d(TAG, "doUpdateMoodIcon(" + icon + ")");
                            moodIcon(icon);
                        }
                    };
                    item.setOnClickListener(l);
                    qa.addActionItem(item);
                }

                qa.setAnimStyle(QuickAction.ANIM_AUTO);
                qa.show();
            }
        });

    }

    /**
     * create the actions objects to attach to the list adapters (part of
     * onCreate bootstrap process)
     * @param af 
     * @return 
     */
    ActionItem[] createActions(ActionFactory af) {
        af.setCollect(true);

        af.createActionItem("Favourite", R.drawable.ic_add, new CommandAdapter(){
            @Override
            public void execute(Object param) {    
                Device device = (Device)param;
                favourite(device.getAddress());
            }
        });
        af.createActionItem("Ignore", R.drawable.ic_minus, new CommandAdapter(){
            @Override
            public void execute(Object param) {    
                Device device = (Device)param;
                ignore(device.getAddress());
            }
        });
        af.createActionItem("Alias", R.drawable.ic_up, new CommandAdapter(){
            @Override
            public void execute(Object param) {    
                Device device = (Device)param;
                showAliasDialog(device);
            }
        });
        return af.current(true);
    }

    /**
     * alias the device
     * 
     * @param device the device to alias
     */
    void showAliasDialog(final Device device) {
        View view = m_inflater.inflate(R.layout.device_rename_dialog, null);
        final EditText name = (EditText) view.findViewById(R.id.name);
        name.setText(getName(device));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Alias device : " + device.getName()).setCancelable(false).setView(view)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        alias(name.getText().toString(), device.getAddress());
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * get the name of the device
     * 
     * @param address the device address
     * @return the device name
     */
    public String getName(Device device) {
        String name = null;
        String address = device.getAddress();
        Tag alias = m_aliases.get(address);

        if (alias != null) {
            name = alias.getValue();
        }
        if (name == null) {
            try {

                if (device != null) {
                    name = device.getName();
                }
                if (name == null || name.trim().length() == 0) {
                    name = device.getName();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (name == null) {
            RemoteDevice d = m_remotes.get(address);
            if (d != null)
                name = d.getName();
        }
        if (name == null)
            name = address;
        return name;
    }

    /**
     * render the authorising screen
     */
    public void renderAuthorising() {
        renderMessage(R.string.authorising);
    }

    /**
     * render the authorising screen
     */
    public void renderMessage(String message) {
        setContentView(R.layout.authorising);
        TextView tv = (TextView) findViewById(R.id.status);
        tv.setText(message);
    }

    /**
     * render the authorising screen
     */
    public void renderMessage(int sid) {
        setContentView(R.layout.authorising);
        TextView tv = (TextView) findViewById(R.id.status);
        tv.setText(sid);
    }

    /**
     * render the exchanging screen
     */
    public void renderExchanging() {
        setContentView(R.layout.authorising);
        TextView tv = (TextView) findViewById(R.id.status);
        tv.setText(R.string.authorising);
    }

    /**
     * render the login View
     */
    public void renderLogin() {
        setContentView(R.layout.login);
        TextView tv = (TextView) findViewById(R.id.login_text);
        tv.setText(R.string.initial_login_text);
        Button button = (Button) findViewById(R.id.continue_btn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                authorise();
            }
        });
    }

    void toast(Object text) {
        Toast.makeText(getApplicationContext(), text.toString(), Toast.LENGTH_LONG);
    }

    public DoTagTask create(String ns, String name, String value, String target) {
        DoTagTask task = new DoTagTask();
        task.execute(ns, name, value, target);
        return task;
    }

    public DoTagTask favourite(String target) {
        DoTagTask task = new DoTagTask();
        task.execute(Tag.TULSI_NAMESPACE, Tag.FAVOURITE_TAG_NAME, "true", target);
        return task;
    }

    public DoTagTask ignore(String target) {
        DoTagTask task = new DoTagTask();
        task.execute(Tag.TULSI_NAMESPACE, Tag.IGNORE_TAG_NAME, "true", target);
        return task;
    }

    public DoTagTask alias(String alias, String target) {
        DoTagTask task = new DoTagTask();
        task.execute(Tag.TULSI_NAMESPACE, Tag.ALIAS_TAG_NAME, alias, target);
        return task;
    }

    public DoTagTask mood(String mood) {
        DoTagTask task = new DoTagTask();
        task.execute(MOOD_NAMESPACE, MOOD_NAME, mood, m_address);
        return task;
    }

    public DoTagTask moodIcon(String icon) {
        DoTagTask task = new DoTagTask();
        task.execute(MOOD_NAMESPACE, MOOD_ICON_NAME, icon, m_address);
        return task;
    }

    /**
     * get the remote device info, again checking for a register email
     */
    class DoTagTask extends AsyncTask<String, Void, Exception> {
        String ns;
        String name;
        String value;
        String target;

        @Override
        protected Exception doInBackground(String... params) {
            ns = params[0];
            name = params[1];
            value = params[2];
            target = params[3];

            Exception e = null;
            try {
                m_remote.getTagRemote().updateOrCreate(ns, name, value, target);
            } catch (Exception ex) {
                e = ex;
            }
            return e;
        }

        protected void onPostExecute(Exception e) {
            if (e == null) {
                if(ns == MOOD_NAMESPACE) {
                    if(name == MOOD_NAME) {
                        updateMoodView(value);
                    }
                    if(name == MOOD_ICON_NAME) {
                        updateMoodIconView(value);
                    }
                }
                toast(new StringBuffer("set ").append(name).append(" to ").append(value));
            }
        }
    };

    /**
     * update the email address registered with this device
     */
    class UpdateEmailTask extends AsyncTask<String, Void, Exception> {

        String email;

        @Override
        protected Exception doInBackground(String... params) {
            email = params[0];
            Exception e = null;
            try {
                m_device.setEmail(email);
                m_device.setName(getBluetoothName());
                m_device.setCod(getBluetoothCod());
                m_device = m_remote.getDeviceRemote().updateDevice(m_device);
            } catch (Exception ex) {
                e = ex;
            }
            return e;
        }

        protected void onPostExecute(Exception e) {
            if (e == null) {
                toast(new StringBuffer("Updated email ").append(email));
            }
        }

    };

    void processMoodTags(Tag[] moods){
        if(moods != null && moods.length> 0) {
            for(Tag tag : moods) {
                if(MOOD_NAME.equals(tag.getName())){
                    m_mood = tag.getValue();
                    continue;
                }
                if(MOOD_ICON_NAME.equals(tag.getName())){
                    m_icon = tag.getValue();
                    continue;
                }
            }
        }
    }
    
    void processFavourites(Tag[] favourites){
        if(favourites != null && favourites.length > 0) {
            for(String address: m_favourites.keySet()){
                m_localsocial.getNeighbourhood().removeObserver(address, m_dobby);
            }
        }
        m_favourites.clear();
        
        if(favourites != null && favourites.length> 0) {
            for (Tag tag : favourites) {            
                String target = tag.getTarget();
                Device device = m_localsocial.getNeighbourhood().get(target);
                if (!m_favourites.containsKey(target)) {
                    m_favourites.put(target, device);
                }
                m_localsocial.getNeighbourhood().observeDevice(target, m_dobby);
                renderMessage("Neighbourhood :: observing favourite " + device.getAddress());
            }
        }
    }

    void processAliases(Tag[] aliases){
        m_aliases.clear();
        
        if(aliases!= null && aliases.length> 0) {
            for (Tag tag : aliases) {
                m_aliases.put(tag.getTarget(), tag);
            }
        }
    }

    void processIgnores(Tag[] ignores){
        m_ignores.clear();
        
        if(ignores!= null && ignores.length> 0) {
            for (Tag tag : ignores) {
                m_ignores.put(tag.getTarget(), tag);
            }
        }
    }

    /**
     * get the remote device info, again checking for a register email
     */
    class LoadRemoteDataTask extends AsyncTask<Void, String, Exception> {
        Tag[] moods = null;
        Tag[] favourites = null;
        Tag[] aliases = null;
        Tag[] ignores = null;
        
        @Override
        protected Exception doInBackground(Void... dummy) {
            Exception e = null;
            try {
                publishProgress("Getting Remote Device Info");
                m_device = m_remote.getDeviceRemote().getDevice();

                publishProgress("Getting Mood");
                try {
                    moods = m_remote.getTagRemote().get(MOOD_NAMESPACE, MOOD_TAG_NAMES, m_address, m_address);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }

                publishProgress("Getting Favourites");
                favourites = m_remote.getTulsiRemote().getFavourites();
                publishProgress(new StringBuffer("Got ").append(favourites == null ? 0 : favourites.length)
                        .append(" Favourites").toString());

 
                publishProgress("Getting Aliases");
                aliases = m_remote.getTulsiRemote().getAliases();
                publishProgress(new StringBuffer("Got ").append(aliases == null ? 0 : aliases.length)
                        .append(" Alias").append((aliases == null || aliases.length !=1) ? "es" : "").toString());

                publishProgress("Getting Ignores");
                ignores = m_remote.getTulsiRemote().getAliases();
                publishProgress(new StringBuffer("Got ").append(ignores == null ? 0 : ignores.length)
                        .append(" Ignore").append((aliases == null || aliases.length !=1) ? "s" : "").toString());

            } catch (Exception ex) {
                e = ex;
            }
            return e;
        }

        protected void onProgressUpdate(String... messages) {
            renderMessage(messages[0]);
        }

        protected void onPostExecute(Exception e) {

            processMoodTags(moods);
            processFavourites(favourites);
            processAliases(aliases);
            processIgnores(ignores);

            createMainView();
            checkException(e);
        }

    };

    /**
     * do the actual oauth steps :
     * <ul>
     * <li>1 - generate a new request token</li>
     * <li>2 - verify the request token</li>
     * <li>3 - exchange the request token for an access token</li>
     * <li>4 - get the remote device info</li>
     * <li>5 - on success, check the device to see if there is an email and ask
     * the user to provide one if necessary</li>
     * </ul>
     */
    class AuthoriseTask extends AsyncTask<String, String, Exception> {
        boolean allow = false;

        @Override
        protected Exception doInBackground(String... commit) {
            allow = "Allow".equals(commit[0]);
            Exception e = null;
            try {
                OAuthConsumer consumer = m_localsocial.getOAuthConsumer();
                RequestToken request = consumer.generateRequestToken();

                publishProgress("Verifying Request Token");

                Verifier verifier = consumer.authorise(request, allow);

                publishProgress("Exchanging Token");
                AccessToken access = consumer.exchange(request, verifier);
                m_localsocial.saveAccessToken(access);

                publishProgress("Checking Remote Device");
            } catch (Exception ex) {
                publishProgress("Unexpected Exception : " + ex.getMessage());
                e = ex;
            }
            return e;
        }

        protected void onProgressUpdate(String... messages) {
            renderMessage(messages[0]);
        }

        protected void onPostExecute(Exception e) {
            if(m_authTask == this)
                m_authTask = null;
            checkException(e);
        }

    };

    /**
     * handle a device discovered
     */
    class DeviceDiscoveredTask extends AsyncTask<Device, String, Object> {
        Device device;

        @Override
        protected Object doInBackground(Device... params) {
            Object o = null;
            device = params[0];

            String address = device.getAddress();

            Log.d(TAG, "discovered before notify device " + device );
            try {
                o = m_remote.getTagRemote().get(MOOD_NAMESPACE, MOOD_TAG_NAMES, address, address);
                System.out.println("o=" + o);
            } catch (Exception ex) {
                o = ex;
            }
            return o;
        }

        protected void onPostExecute(Object o) {
            Tag[] tags = null;
            if (o == null || o instanceof Tag[]|| o instanceof NoSuchObjectException) {
                if (o == null || o instanceof Tag[]) {
                    tags = (Tag[])o;
                }
                Log.d(TAG, "discovered " + device + " adding ");
                m_map.put(device.getAddress(), tags);
                dAdapter.notifyDataSetChanged();

            } else {
                Exception e = (Exception) o;
                checkException(e);
            }

        }

    };
    
    DeviceObserver m_dobby = new DeviceObserverAdapter() {
        @Override
        public void outOfRange(Device device) {
            String name = device.getName();
            if (name == null)
                name = device.getAddress();
            toast(name + " has gone out of range");
        }

        @Override
        public void inRange(Device device) {
            String name = device.getName();
            if (name == null)
                name = device.getAddress();
            toast(name + " has come in range");
        }
    };

    NeighbourhoodObserver m_nobby = new NeighbourhoodObserverAdapter() {

        @Override
        public void discovered(Device device) {
            boolean known = true;
            String address = device.getAddress();
            synchronized (m_map) {
                Log.d(TAG, "discovered " + device + " map=" + m_map);
                known = m_map.containsKey(address);
                Log.d(TAG, "discovered " + device + " known=" + known);
            }
            if(!known) {
                dAdapter.add(device);
                dAdapter.notifyDataSetChanged();                
                new DeviceDiscoveredTask().execute(device);
            }
        }
    };
    
    /**
     * Configure the localsocial factory and get the LocalSocial instance . The 
     * key and secret allow localsocial to generate signed OAuth Requests correctly.
     * 
     * 
     * @return the localsocial instance (note you don't have to save this instance if you 
     * don't want to, calling LocalSocialFactory.getLocalSocial() again will return the 
     * same instance 
     */
    LocalSocial bootstrap() {
        SimpleAppConfiguration sac = new SimpleAppConfiguration();
        Platform platform = new Platform();
        platform.setContext(getApplication());
        
        sac.setPlatformContext(platform);
        sac.setServiceName("Mood");
        sac.setConsumerKey("XXX GEY A KEY XXX");
        sac.setConsumerSecret("XXX GEY A KEY XXX");
        
        LocalSocialFactory.setDefaultConfig(sac);

        // calling getLocalSocial() will always return the same instance
        return LocalSocialFactory.getLocalSocial();
    }

    
    private static final String MOOD_ICON_NAME = "MoodIcon";
    private static final String MOOD_NAME = "Mood";
    private static final String MOOD_TAG_NAMES = new StringBuffer(MOOD_NAME).append(",").append(MOOD_ICON_NAME).toString();

    private static final String MOOD_NAMESPACE = "com.localsocial.mood";

    String[] m_assets;
    LoadRemoteDataTask m_loadTask = null;
    AuthoriseTask m_authTask = null;
    List<Device> m_devices = new ArrayList<Device>();

    ImageAdapter imgAdapter;
    private DeviceItemAdapter dAdapter;

    private Map<String, Tag[]> m_map = (Map<String, Tag[]>) Collections.synchronizedMap(new HashMap<String, Tag[]>());
    private Map<String, Device> m_favourites = new HashMap<String, Device>();
    public HashMap<String, Device> m_seen = new HashMap<String, Device>();
    public HashMap<String, Tag> m_aliases = new HashMap<String, Tag>();
    public HashMap<String, Tag> m_ignores= new HashMap<String, Tag>();;

    private RemoteDevice m_device;
    private String m_mood = null;// "<no mood>";
    private String m_icon = null;// "<no mood>";
    private AccessToken m_aToken;
    HashMap<String, RemoteDevice> m_remotes = new HashMap<String, RemoteDevice>();

    PeriodicScan m_scan;
    
    LayoutInflater m_inflater;
    String m_address = null;
    String m_cod = null;
    String m_name = null;
    private LocalSocial m_localsocial;
    RemoteFactory m_remote;
    private static final String TAG = "Mood/" + LoggerFactory.getClassName(MainActivity.class);
}
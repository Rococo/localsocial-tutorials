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
package com.localsocial.nhw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.localsocial.Device;
import com.localsocial.LocalSocial;
import com.localsocial.LocalSocialFactory;
import com.localsocial.LoggerFactory;
import com.localsocial.Platform;
import com.localsocial.config.SimpleAppConfiguration;
import com.localsocial.model.RemoteDevice;
import com.localsocial.model.Tag;
import com.localsocial.oauth.AccessToken;
import com.localsocial.oauth.OAuthConsumer;
import com.localsocial.oauth.RequestToken;
import com.localsocial.oauth.Verifier;
import com.localsocial.proximity.PeriodicScan;
import com.localsocial.proximity.observers.DeviceObserver;
import com.localsocial.proximity.observers.NeighbourhoodObserver;
import com.localsocial.remote.RemoteFactory;
import com.localsocial.remote.exception.UnauthorizedException;

/**
 * Main Activity for Neighbourhood application. In general methods starting with ;
 * <ul>
 * <li><b>render</b> will update the view</li>
 * <li><b>do</b> or <>post</b> run in a different thread or handler (depending )
 * on the context</li>
 * </ul>
 * <p/>
 * So render method can be called from the main UI thread and methods called
 * doRender can be called from any context.
 * <p/>
 * For example, renderLogin can be called from the onCreate method but
 * postRenderLogin must be called from doAuthoriseAndExchange ;-)
 * 
 * @author jimoleary
 */
public class MainActivity extends ListActivity {

    // Android life cycle events
    
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate"); 
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.i_title);


        eAdapter = new EventItemAdapter(this, R.layout.deviceview_row, m_events, m_favourites);
        setContentView(R.layout.main);

        ListView dView = getListView();
        TextView tv = new TextView(this);
        tv.setText("Events");
        tv.setGravity(Gravity.CENTER);
        tv.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                restartScan();
            }
        });
        dView.addHeaderView(tv);

        dView.setAdapter(eAdapter);
        addQuickActionToList(dView, eAdapter);
        
        m_localsocial = bootstrap();
        m_localsocial.getNeighbourhood().observeNeighbourhood(getNeighbourhoodObserver());
        m_remote = m_localsocial.getRemoteFactory();
        
        m_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    /**
     * Called when-ever the activity is displayed. The order of execution is :
     */
    @Override
    protected void onStart() {
        Log.i(TAG, "onStart() ");
        super.onStart();
        gotFavourites = false;
        start();
    }
    
    public void start(){
        if (!authorised()) {
            Log.d(TAG, "attempting to load access token");

            try {
                m_aToken = m_localsocial.loadAccessToken();
            } catch (UnauthorizedException e) {
                Log.w(TAG, "error recreating access token", e);
            }
        }
        if (authorised()) {
            renderMain();
        } else {
            authorise();
        }
    }

    /**
     * Called when-ever the activity is hidden
     */
    @Override
    protected void onStop() {
        Log.i(TAG, "onStop() ");
        super.onStop();
        if (m_localsocial != null) {
            stopScan();
            Log.d(TAG, "removing neighbourhood observer");
            m_localsocial.getNeighbourhood().removeObserver(getNeighbourhoodObserver());
            for (String address : m_seen.keySet()){
                m_localsocial.getNeighbourhood().removeObserver(address,m_dobby);
            }
        }

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.about:
             Intent myIntent = new Intent(this.getApplicationContext(),
             About.class);
             startActivity(myIntent);
            break;
        case R.id.discovery_status:
            setDiscoverable();
            updateTitle();
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
     * stop the current periodic scan
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
     */
    protected void startScan() {
        m_scan = m_localsocial.getNeighbourhood().startScan();
    }

    /**
     * stop the current scan if there is one and then start a new one
     */
    protected void restartScan() {
        stopScan();
        startScan();
    }

    /**
     * scan the neighbourhood
     */
    protected void clear() {
        try {
            m_devices.clear();
            eAdapter.clear();
            eAdapter.notifyDataSetChanged();
            if(m_localsocial != null) {
                Platform platform = m_localsocial.getConfig().getPlatformContext();
                LocalSocialFactory.clearCredentials(platform);
            }
            m_gotDevice = false;
            for (String address : m_seen.keySet()){
                m_localsocial.getNeighbourhood().removeObserver(address,m_dobby);
            }

            m_favourites.clear();
            m_aliases.clear();
            m_ignores.clear();
            m_seen.clear();
            authorise();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * scan the neighbourhood
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
     * token, it may not be valid).
     * 
     * @return true if we have an access token
     */
    boolean authorised() {
        return m_aToken != null;
    }

    void checkException(Exception e) {
        if(e != null)
            toast(new StringBuffer("unexpected exception : ").append(e.getMessage()));
        start();
    }

    void toast(Object text){
        Toast.makeText(getApplicationContext(), text.toString(), Toast.LENGTH_LONG);
    }
    
    /**
     * alias the device
     * 
     * @param device the device to alias
     */
    void showAliasDialog(final Device device) {
        View view= m_inflater.inflate(R.layout.device_rename_dialog, null);
        final EditText name = (EditText) view.findViewById(R.id.name);
        name.setText(getName(device));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Alias device : " + device.getName()).setCancelable(false).setView(view)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        new AliasTask().execute(device, name.getText().toString());
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
            RemoteDevice d = m_devices.get(address);
            if(d != null)
                name = d.getName();
        }
        if (name == null)
            name = address;
        return name;
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

    /**
     * get the local device name T
     * 
     * @return the local device name
     */
    static final boolean isBlank(String v) {
        return v == null || v.trim().length() == 0;
    }

    /**
     * update the application title
     */
    private void updateTitle() {
        ImageView disc = (ImageView) findViewById(R.id.title_discoverable);
        disc.setVisibility(m_localsocial.getNeighbourhood().isDiscoverable() ? View.VISIBLE : View.INVISIBLE);

        ProgressBar spinner = (ProgressBar) findViewById(R.id.title_spinner);
        spinner.setVisibility(m_localsocial.getNeighbourhood().isCurrentlyScanning() ? ImageView.VISIBLE
                : ImageView.INVISIBLE);
    }

    /**
     * if there is no device then get the remote device if there are no tags
     * then get the remote tags otherwise display the main screen
     */
    protected void renderMain() {
        updateTitle();

        if (m_gotDevice == false) {
            new LoadRemoteDataTask().execute();
            return;
        }
    }

    protected void addQuickActionToList(ListView mList, final EventItemAdapter adapter) {
        final ActionItem favourite = new ActionItem();

        favourite.setTitle("Favourite");
        favourite.setIcon(getResources().getDrawable(R.drawable.ic_add));

        final ActionItem ignore = new ActionItem();

        ignore.setTitle("Ignore");
        ignore.setIcon(getResources().getDrawable(R.drawable.ic_minus));

        final ActionItem alias = new ActionItem();

        alias.setTitle("Alias");
        alias.setIcon(getResources().getDrawable(R.drawable.ic_up));

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int id, long position) {
                final QuickAction mQuickAction = new QuickAction(view);

                final ImageView mMoreImage = (ImageView) view.findViewById(R.id.mood_icon);// i_more);

                final Event event = adapter.getItem((int) position);
                final Device device = event.getDevice();
                if (device == null)
                    return;

                final String text;
                if (device.getName() == null)
                    text = device.getAddress();
                else
                    text = device.getName();

                mMoreImage.setImageResource(R.drawable.ic_list_more_selected);

                favourite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mQuickAction.dismiss();
                        new FavouriteTask().execute(device);
                    }
                });

                ignore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mQuickAction.dismiss();
                        new IgnoreTask().execute(device);
                    }
                });

                alias.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mQuickAction.dismiss();
                        toast(new StringBuffer("Upload ").append(text) );
                        showAliasDialog(device);
                    }
                });

                mQuickAction.addActionItem(favourite);
                mQuickAction.addActionItem(ignore);
                mQuickAction.addActionItem(alias);

                mQuickAction.setAnimStyle(QuickAction.ANIM_AUTO);

                mQuickAction.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        adapter.postNotifyDataSetChanged();
                    }
                });

                mQuickAction.show();
            }
        });

    }

    protected void renderEmail() {
        setContentView(R.layout.email);

        Button button = (Button) findViewById(R.id.update_btn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText tv = (EditText) findViewById(R.id.email_edit);
                new UpdateEmailTask().execute(tv.getText().toString());
            }
        });
        button = (Button) findViewById(R.id.skip_btn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                renderMain();
            }
        });
    }

    /**
     * authorise the application
     */
    public void authorise() {
        new AuthoriseTask().execute("Allow");
    }

    LocalSocial.Configuration getConfig() {
        return m_localsocial.getConfig();
    }

    Event createEvent(final String type, final Device device) {
        return new Event() {

            @Override
            public Device getDevice() {
                return device;
            }

            @Override
            public String getType() {
                return type;
            }

            public String toString() {
                return "Event { type=" + type + ",device=" + device + "}";
            }
        };

    }

    Event createEvent(Object o) {
        final String type = o.toString();
        return new Event() {

            @Override
            public Device getDevice() {
                return null;
            }

            @Override
            public String getType() {
                return type;
            }

            public String toString() {
                return "Event { type=" + type + "}";
            }
        };

    }

    class RemoteDeviceTask extends  AsyncTask<String, String, Exception> {

        @Override
        protected Exception doInBackground(String... addresses) {
            Exception e = null;
            
            try {
                for(String address: addresses) {
                    RemoteDevice device = m_devices .get(address);
                    if(device == null) {
                        device = m_remote.getDeviceRemote().getDevice(address);
                        m_devices.put(address,device);
                    }
                }
            } catch (Exception ex) {
                publishProgress("Unexpected Exception : " + ex.getMessage());
                e = ex;
            }
            return e;
        }

        protected void onPostExecute(Exception e) {
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

            } catch (Exception ex) {
                publishProgress("Unexpected Exception : " + ex.getMessage());
                e = ex;
            }
            return e;
        }

        protected void onProgressUpdate(String... messages) {
            for (String m : messages) {
                eAdapter.add(createEvent(m));
            }
            eAdapter.notifyDataSetChanged();
        }

        protected void onPostExecute(Exception e) {
            m_gotDevice = false;
            checkException(e);
        }

    };
    
    /**
     * process the favourite tags, in this case we will observe each of them.
     * @param favourites the array of favourites 
     */
    void processFavourites(Tag [] favourites){
        if(favourites != null && favourites.length > 0) {
            for(Tag tag: favourites) {
                String target = tag.getTarget();
                if (!m_favourites.containsKey(target)) {
                    m_favourites.put(target, m_localsocial.getNeighbourhood().get(target));
                }
                if (!m_seen.containsKey(target)) {
                    Device device = m_localsocial.getNeighbourhood().observeDevice(target, m_dobby);
                    m_seen.put(target, device);
                    addEvent(createEvent("Neighbourhood :: observing favourite ", device));
                }
            }
        }
    }

    /**
     * process the aliases, just store them
     * @param aliases the array of aliases 
     */
    void processAliases(Tag [] aliases){
        if(aliases != null && aliases.length > 0) {
            for(Tag tag: aliases) {
                m_aliases.put(tag.getTarget(),tag);
            }
        }
    }
    
    /**
     * process the ignores , just store them
     * @param ignores the array of ignores 
     */
    void processIgnores(Tag [] ignores){
        if(ignores != null && ignores.length > 0) {
            for(Tag tag: ignores) {
                m_ignores.put(tag.getTarget(),tag);
            }
        }            
    }


    /**
     * Load all the remote data : my device info and tulsi tags favourite, aliases and ignores
     */
    class LoadRemoteDataTask extends AsyncTask<Void, Event, Exception> {

        Tag[] favourites;
        Tag[] aliases;
        Tag[] ignores;
        
        @Override
        protected Exception doInBackground(Void... dummy) {
            Exception e = null;
            try {
                publishProgress(createEvent("Getting Remote Device Info"));
                m_device = m_remote.getDeviceRemote().getDevice(); 

                publishProgress(createEvent("Getting Favourites"));
                favourites=m_remote.getTulsiRemote().getFavourites();
                publishProgress(createEvent(new StringBuffer("Got ").append(favourites== null ? 0 : favourites.length).append(" Favourite").append((favourites!= null && favourites.length == 1) ? "" : "s")));
                
                publishProgress(createEvent("Getting Aliases"));
                aliases=m_remote.getTulsiRemote().getAliases();
                publishProgress(createEvent(new StringBuffer("Got ").append(aliases== null ? 0 : aliases.length).append(" Alias").append((aliases!= null && aliases.length == 1) ? "" : "es")));

                publishProgress(createEvent("Getting Ignores"));
                ignores=m_remote.getTulsiRemote().getAliases();
                publishProgress(createEvent(new StringBuffer("Got ").append(ignores== null ? 0 : ignores.length).append(" Ignore").append((ignores!= null && ignores.length == 1) ? "" : "s")));
                
            } catch (Exception ex) {
                e = ex;
            }
            return e;
        }

        protected void onProgressUpdate(Event... events) {
            eAdapter.add(events[0]);
            eAdapter.notifyDataSetChanged();
        }
        
        protected void onPostExecute(Exception e) {
            processFavourites(favourites);
            processAliases(aliases);
            processIgnores(ignores);

            m_gotDevice = true;
            checkException(e);
        }

    };

    /**
     * Favourite the device
     */
    class FavouriteTask extends AsyncTask<Device, Event, Void> {

        @Override
        protected Void doInBackground(Device... devices) {
            Device device = devices[0];
            try {
                String target = device.getAddress();
                Tag tag = m_remote.getTulsiRemote().getTag(Tag.FAVOURITE_TAG_NAME, m_address, target);
                if(tag == null) {
                    tag = m_remote.getTulsiRemote().favourite(target);
                } else {
                    tag = m_remote.getTagRemote().update(tag, "true");
                }
                
                publishProgress(createEvent(new StringBuffer("Favourited ").append(device.getAddress())));
            } catch (Exception ex) {
            }

            return null;
        }
        protected void onProgressUpdate(Event... events) {
            eAdapter.add(events[0]);
            eAdapter.notifyDataSetChanged();
        }
    };

    /**
     * Ignore the device
     */
    class IgnoreTask extends AsyncTask<Device, Event, Void> {

        @Override
        protected Void doInBackground(Device... devices) {
            Device device = devices[0];
            try {
                String target = device.getAddress();
                Tag tag = m_remote.getTulsiRemote().getTag(Tag.IGNORE_TAG_NAME, m_address, target);
                if(tag == null) {
                    tag = m_remote.getTulsiRemote().ignore(target);
                } else {
                    tag = m_remote.getTagRemote().update(tag, "true");
                }

                publishProgress(createEvent(new StringBuffer("Ignored ").append(device.getAddress())));
            } catch (Exception ex) {
            }
            return null;
        }
        protected void onProgressUpdate(Event... events) {
            eAdapter.add(events[0]);
            eAdapter.notifyDataSetChanged();
        }
    };

    /**
     * Alias the device
     */
    class AliasTask extends AsyncTask<Object, Event, Exception> {

        Device device;
        String alias;
        String name;
        @Override
        protected Exception doInBackground(Object... params) {
            Exception e = null;
            device = (Device)params[0];
            alias = (String)params[1];
            name= device.getName();
            try {
                m_remote.getTagRemote().updateOrCreate(Tag.TULSI_NAMESPACE, Tag.ALIAS_TAG_NAME, alias, device.getAddress());
                publishProgress(createEvent(new StringBuffer("Aliased ").append(device.getAddress())));
            } catch (Exception ex) {
                e =ex;
            }
            return e;
        }
        protected void onProgressUpdate(Event... events) {
            eAdapter.add(events[0]);
            eAdapter.notifyDataSetChanged();
        }
        protected void onPostExecute(Exception e) {
            if(e == null) {
                toast(new StringBuffer("Aliased ").append(name).append(" to ").append(alias));
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
                m_remote.getDeviceRemote().updateDevice(m_device);
            } catch (Exception ex) {
                e =ex;
            }
            return e;
        }
        protected void onPostExecute(Exception e) {
            if(e == null) {
                toast(new StringBuffer("Updated email ").append(email));
            }
        }

    };

    protected void addEvent(Event event) {
        eAdapter.add(event);
        eAdapter.notifyDataSetChanged();
        updateTitle();
    }

    DeviceObserver m_dobby = new DeviceObserver() {

        @Override
        public void propertyChanged(Device device, String property) {
            StringBuffer p = new StringBuffer("Device :: property ").append(property);
            if (property.equals(Device.RSSI)) {
                p.append(" : (").append(device.getRssi()).append(")");
            } else if (property.equals(Device.COD)) {
                p.append(" : (").append(device.getCod()).append(")");
            } else if (property.equals(Device.FRIENDLY_NAME)) {
                p.append(" : (").append(device.getName()).append(")");
            }
            addEvent(createEvent(p.toString(), device));
        }

        @Override
        public void outOfRange(Device device) {
            addEvent(createEvent("Device :: outOfRange", device));
        }

        @Override
        public void inRange(Device device) {
            addEvent(createEvent("Device :: inRange", device));
        }
    };
    
    NeighbourhoodObserver m_nobby = new NeighbourhoodObserver() {

        @Override
        public void discovered(Device device) {
            String address = device.getAddress();
            if (!m_seen.containsKey(address)) {
                m_seen.put(address, device);
                device.add(m_dobby);
                addEvent(createEvent("Neighbourhood :: observing", device));
            }
        }

        @Override
        public void inRange(Device device) {
            addEvent(createEvent("Neighbourhood :: inRange", device));
        }
        
        @Override
        public void outOfRange(Device device) {
            addEvent(createEvent("Neighbourhood :: outOfRange", device));
        }

        @Override
        public void scanStarted(boolean first) {
            addEvent(createEvent("Neighbourhood :: scanStarted first=" + first));
        }

        @Override
        public void scanStopped(int result) {
            addEvent(createEvent("Neighbourhood :: scanStopped result=" + result));
        }

        @Override
        public void scanModeChanged(int mode) {
            addEvent(createEvent("Neighbourhood :: scanModeChanged result=" + mode));
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
        sac.setServiceName("NeighbourhoodWatch");
        sac.setConsumerKey("XXX GET A KEY XXX");
        sac.setConsumerSecret("XXX GET A KEY XXX");
        
        LocalSocialFactory.setDefaultConfig(sac);
        
        // calling getLocalSocial() will always return the same instance
        return LocalSocialFactory.getLocalSocial();
    }

    List<Event> m_events = new ArrayList<Event>();

    private EventItemAdapter eAdapter;
    private Map<String, Device> m_favourites = new HashMap<String, Device>();
    private Map<String, Tag> m_aliases = new HashMap<String, Tag>();
    private Map<String, Tag> m_ignores= new HashMap<String, Tag>();
    boolean gotFavourites = false;
    Map<String, Device> m_seen = new HashMap<String, Device>();

    private RemoteDevice m_device;
    private boolean m_gotDevice;
    private AccessToken m_aToken;

    HashMap<String,RemoteDevice> m_devices = new HashMap<String,RemoteDevice> ();

    PeriodicScan m_scan;
    String m_address = null;
    String m_cod = null;
    String m_name = null;
    private LocalSocial m_localsocial;
    LayoutInflater m_inflater;
    RemoteFactory m_remote;

    private static final String TAG = "NHW/" + LoggerFactory.getClassName(MainActivity.class);
}

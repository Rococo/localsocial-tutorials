package com.localsocial.simple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.localsocial.LocalSocial;
import com.localsocial.LocalSocialFactory;
import com.localsocial.Platform;
import com.localsocial.config.SimpleAppConfiguration;
import com.localsocial.model.Tag;
import com.localsocial.oauth.AccessToken;
import com.localsocial.oauth.OAuthConsumer;
import com.localsocial.oauth.RequestToken;
import com.localsocial.oauth.Verifier;
import com.localsocial.remote.RemoteFactory;
import com.localsocial.remote.exception.NoSuchObjectException;
import com.localsocial.remote.exception.UnauthorizedException;

public class MainActivity extends ListActivity {
    private static final String NAMESPACE = "com.localsocial.simple";
    private static final String LAST_RUN_TAG_NAME = "LastRun";
    LocalSocial ls;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        System.out.println(LocalSocialFactory.banner());
        ls = bootstrap();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        setListAdapter(adapter);
    }


    /**
     * Called when-ever the activity is displayed. The order of execution is :
     */
    @Override
    protected void onStart() {
        super.onStart();
        renderMessage("starting");
        new AuthTask().execute();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear:
                clear();
                break;
            case R.id.about:
                Intent intent = new Intent(this, About.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    /**
     * stop the current scan if there is one and then start a new one
     */
    protected void clear() {
        try {
            list.clear();
            if (ls != null) {
                Platform platform = ls.getConfig().getPlatformContext();
                LocalSocialFactory.clearCredentials(platform);
                platform.restart();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configure the localsocial factory and get the LocalSocial instance . The
     * key and secret allow localsocial to generate signed OAuth Requests correctly.
     *
     * @return the localsocial instance (note you don't have to save this instance if you
     *         don't want to, calling LocalSocialFactory.getLocalSocial() again will return the
     *         same instance
     */
    LocalSocial bootstrap() {
        SimpleAppConfiguration sac = new SimpleAppConfiguration();
        Platform platform = new Platform();
        platform.setContext(getApplication());

        sac.setPlatformContext(platform);
        sac.setServiceName("Simple");
        sac.setConsumerKey("XXX GET A KEY XXX");
        sac.setConsumerSecret("XXX GET A KEY XXX");

        LocalSocialFactory.setDefaultConfig(sac);

        // calling getLocalSocial() will always return the same instance
        return LocalSocialFactory.getLocalSocial();
    }

    /**
     * render the authorising screen
     */
    public void renderMessage(String message) {
        list.add(message);
        adapter.notifyDataSetChanged();
    }

    class AuthTask extends AsyncTask<Void, String, Exception> {
        protected void onPreExecute() {
            renderMessage("authorising");
        }

        @Override
        protected Exception doInBackground(Void... dummy) {
            Exception e = null;
            try {
                OAuthConsumer c = ls.getOAuthConsumer();
                AccessToken at = null;
                try {
                    at = ls.loadAccessToken();
                    publishProgress("Using existing token");
                } catch (UnauthorizedException uae) {
                    publishProgress("No Token");

                    RequestToken token = c.generateRequestToken();
                    Verifier verifier = c.authorise(token);
                    at = c.exchange(token, verifier);
                    ls.saveAccessToken(at);
                    publishProgress("New Token generated");

                }
                RemoteFactory remote = ls.getRemoteFactory();

                Tag lr;
                String value = new Date().toString();
                try {
                    lr = remote.getTagRemote().getTag(NAMESPACE, LAST_RUN_TAG_NAME);
                    publishProgress("got existing  " + LAST_RUN_TAG_NAME + " tag : " + lr.getValue());
                    lr = remote.getTagRemote().update(lr, value);
                } catch (NoSuchObjectException nsoe) {
                    publishProgress("creating new tag " + LAST_RUN_TAG_NAME + "  : " + value);
                    lr = remote.getTagRemote().create(NAMESPACE, LAST_RUN_TAG_NAME, value);
                }
                publishProgress("LastRun : " + lr.getValue());
                Log.d(TAG, "last run " + lr.getValue());
            } catch (Exception ex) {
                ex.printStackTrace();
                e = ex;
                publishProgress("Unexpected Exception : " + e.getMessage());
            }
            return e;
        }

        protected void onProgressUpdate(String... messages) {
            for (String m : messages) {
                renderMessage(m);
            }
        }

        protected void onPostExecute(Exception e) {
            if (e == null) {
                renderMessage("authorised");
            } else {
                e.printStackTrace();
                if (e instanceof UnauthorizedException) {
                    renderMessage("Unauthorized :: did you revoke the access token? \n restart the app to generate a new token");
                    try {
                        LocalSocialFactory.clearCredentials();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                } else {
                    renderMessage("Unexpected Exception : " + e.getMessage());
                }
            }
        }

    }

    List<String> list = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    public static final String TAG = "Simple";

}

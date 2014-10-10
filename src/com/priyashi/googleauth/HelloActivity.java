/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.priyashi.googleauth;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * The TokenInfoActivity is a simple app that allows users to acquire, inspect and invalidate
 * authentication tokens for a different accounts and scopes.
 *
 * In addition see implementations of {@link AbstractGetNameTask} for an illustration of how to use
 * the {@link GoogleAuthUtil}.
 */
public class HelloActivity extends Activity {
    private static final String TAG = "PlayHelloActivity";
    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
    public static final String EXTRA_ACCOUNTNAME = "extra_accountname";
    private static String clientId="714694817830-ahl8jt847g5u892jjsv9ei0143g4c7as.apps.googleusercontent.com";
   // private static final String SCOPE = "oauth2:server:client_id:"+clientId+":api_scope:https://www.googleapis.com/auth/userinfo.profile";
    
  
    private TextView mOut;
    private ImageView imageView;
    private Button gplusBtn;

    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;

    private String mEmail;

    private Type requestType;
    private String googleLink="";

    public static String TYPE_KEY = "type_key";
    public static enum Type {FOREGROUND, BACKGROUND, BACKGROUND_WITH_SYNC}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts_tester);

        mOut = (TextView) findViewById(R.id.message);
        imageView=(ImageView) findViewById(R.id.imageView1);
        gplusBtn=(Button) findViewById(R.id.button1);
        
        Bundle extras = getIntent().getExtras();
        requestType = Type.valueOf(extras.getString(TYPE_KEY));
        setTitle(getTitle() + " - " + requestType.name());
        
        if (extras.containsKey(EXTRA_ACCOUNTNAME)) {
            mEmail = extras.getString(EXTRA_ACCOUNTNAME);
            getTask(HelloActivity.this, mEmail, SCOPE).execute();
        }
        
        else {
        	Log.e("Account HERE ", "No Email FOund "+mEmail);
        }
        
        gplusBtn.setEnabled(false);
        gplusBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleLink));
				startActivity(browserIntent);
				
			}
		});
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
        	Log.e("ON Activity Result ","PIC Account HERE");
        	
            if (resultCode == RESULT_OK) {
                mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                getUsername();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "You must pick an account", Toast.LENGTH_SHORT).show();
            }
        }
        
        else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
                && resultCode == RESULT_OK) {
        	Log.e("Error on Onactivity Result ", "negnjkernjhnvjdfnjgvnjkdfnvrns");
        	
            handleAuthorizeResult(resultCode, data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleAuthorizeResult(int resultCode, Intent data) {
        if (data == null) {
            show("Unknown error, click the button again");
            return;
        }
        if (resultCode == RESULT_OK) {
            Log.i(TAG, "Retrying");
            getTask(this, mEmail, SCOPE).execute();
            return;
        }
        if (resultCode == RESULT_CANCELED) {
            show("User rejected authorization.");
            return;
        }
        show("Unknown error, click the button again");
    }

    public void show(final String string) {
    	runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOut.setText(string);
                
                
            }
        });
	}

	/** Called by button in the layout */
    public void greetTheUser(View view) {
        getUsername();
    }

    /** Attempt to get the user name. If the email address isn't known yet,
     * then call pickUserAccount() method so the user can pick an account.
     */
    private void getUsername() {
        if (mEmail == null) {
            pickUserAccount();
            
            
        } 
        
        else {
        	Log.e("Email Not null",mEmail +"----scope:  "+SCOPE);
        	
            if (isDeviceOnline()) {
                getTask(HelloActivity.this, mEmail, SCOPE).execute();
            } else {
                Toast.makeText(this, "No network connection available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Starts an activity in Google Play Services so the user can pick an account */
    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    /** Checks whether the device currently has a network connection */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }


    /**
     * This method is a hook for background threads and async tasks that need to update the UI.
     * It does this by launching a runnable under the UI thread.
     */
    public void show(final UserProfileModel model) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOut.setText("Hello "+model.getUserName()+" !");
                uploadImageUsingVolley(model.getUserPicture(),imageView);
                gplusBtn.setEnabled(true);
                googleLink=model.getUser_google_link();
                
            }
        });
    }

    /**
     * This method is a hook for background threads and async tasks that need to provide the
     * user a response UI when an exception occurs.
     */
    public void handleException(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            HelloActivity.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

    /**
     * Note: This approach is for demo purposes only. Clients would normally not get tokens in the
     * background from a Foreground activity.
     */
    private AbstractGetNameTask getTask(
            HelloActivity activity, String email, String scope) {
        switch(requestType) {
            case FOREGROUND:
                return new GetNameInForeground(activity, email, scope);
            case BACKGROUND:
                return new GetNameInBackground(activity, email, scope);
            case BACKGROUND_WITH_SYNC:
                return new GetNameInBackgroundWithSync(activity, email, scope);
            default:
                return new GetNameInBackground(activity, email, scope);
        }
    }
    
    
    // Volley android network operation API 
    //.............##########################################################################............................//
    
 /*   ref link 
  * 
  * 
    http://developer.android.com/training/volley/requestqueue.html#singleton
    
    
    http://developer.android.com/training/volley/request-custom.html
    
    
*/    	
    
    private void uploadImageUsingVolley(String url,final ImageView imageView) {
    	// Retrieves an image specified by the URL, displays it in the UI.
    	ImageRequest re=new ImageRequest(url,new Response.Listener<Bitmap>() {

			@Override
			public void onResponse(final Bitmap arg0) {
				imageView.setImageBitmap(arg0);
			}
		}, 0,0,null, null);
    	
    	RequestQueue reqQueue=Volley.newRequestQueue(getApplicationContext());
    	reqQueue.add(re);
       }
   
}


  


//format

/*{
	 "id": "112995633952642313321",
	 "name": "Ashish Saini",
	 "given_name": "Ashish",
	 "family_name": "Saini",
	 "link": "https://plus.google.com/112995633952642313321",
	 "picture": "https://lh3.googleusercontent.com/-VbZV0gVUZvE/AAAAAAAAAAI/AAAAAAAAASo/8RJF5RhH9W8/photo.jpg",
	 "gender": "male",
	 "locale": "en"
	}*/

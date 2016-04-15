package com.firebase.samples.logindemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();


    /* TextView that is used to display information about the logged in user */
    private TextView mLoggedInStatusTextView;

    /* A dialog that is presented until the Firebase authentication finished. */
    private ProgressDialog mAuthProgressDialog;


    /* Data from the authenticated user */
    private AuthData mAuthData;
    
    /* Listener for Firebase session changes */
    private Firebase.AuthStateListener mAuthStateListener;

    private Button mPasswordLoginButton;
    Firebase firebase; //firebase reference
    Activity context;


    public void createUser(final String username, final String password) {

        final Firebase rootRef = firebase;

        rootRef.createUser(
                username,
                password,
                new Firebase.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        // Great, we have a new user. Now log them in:
                        rootRef.authWithPassword(
                                username,
                                password,
                                new Firebase.AuthResultHandler() {
                                    @Override
                                    public void onAuthenticated(AuthData authData) {
                                        // Great, the new user is logged in.
                                        // Create a node under "/users/uid/" and store some initial information,
                                        // where "uid" is the newly generated unique id for the user:
                                        rootRef.child("users").child(authData.getUid()).child("status").setValue("New User");
                                        rootRef.child("users").child(authData.getUid()).child("email").setValue(username);
                                       // rootRef.child("users").child(authData.getUid()).child("em").setValue(username);
                                        Toast toast = Toast.makeText(context,"User is registered and logged in!",Toast.LENGTH_LONG);
                                        toast.show();
                                    }

                                    @Override
                                    public void onAuthenticationError(FirebaseError error) {
                                        // Should hopefully not happen as we just created the user.
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(FirebaseError error) {
                        // Couldn't create the user, probably invalid email.
                        // Show the error message and give them another chance.
                        Toast toast = Toast.makeText(context,"User error registering: "+error,Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
        );
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Load the view and display it */
        setContentView(R.layout.activity_main);
        context = this;
        //TODO - you need to input your firebase root url in the line below
       firebase = new Firebase("your firebase root url");

       Button button = (Button) findViewById(R.id.registerButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText editUser = (EditText) findViewById(R.id.inputUser);
                String userName = editUser.getText().toString().trim();
                EditText editPassword = (EditText) findViewById(R.id.inputPassword);
                String password = editPassword.getText().toString().trim();
                //now create new firebase user
                createUser(userName,password);


            }
        });

        mPasswordLoginButton = (Button) findViewById(R.id.login_with_password);
        mPasswordLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginWithPassword();
            }
        });

        mLoggedInStatusTextView = (TextView) findViewById(R.id.login_status);


        /* Setup the progress dialog that is displayed later when authenticating with Firebase */
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle("Loading");
        mAuthProgressDialog.setMessage("Authenticating with Firebase...");
        mAuthProgressDialog.setCancelable(false);
        mAuthProgressDialog.show();

        mAuthStateListener = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                mAuthProgressDialog.hide();
                setAuthenticatedUser(authData);
            }
        };
        /* Check if the user is authenticated with Firebase already. If this is the case we can set the authenticated
         * user and hide hide any login buttons */
        firebase.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        
        // if changing configurations, stop tracking firebase session.
        firebase.removeAuthStateListener(mAuthStateListener);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* If a user is currently authenticated, display a logout menu */
        if (this.mAuthData != null) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Unauthenticate from Firebase and from providers where necessary.
     */
    private void logout() {
        if (this.mAuthData != null) {
            /* logout of Firebase */
            firebase.unauth();

            /* Update authenticated user and show login buttons */
            setAuthenticatedUser(null);
        }
    }



    /**
     * Once a user is logged in, take the mAuthData provided from Firebase and "use" it.
     */
    private void setAuthenticatedUser(AuthData authData) {
        if (authData != null) {
            /* Hide all the login buttons */
            mPasswordLoginButton.setVisibility(View.GONE);
            mLoggedInStatusTextView.setVisibility(View.VISIBLE);
            /* show a provider specific status text */
            String name = null;
            if (authData.getProvider().equals("anonymous")
                    || authData.getProvider().equals("password")) {
                name = authData.getUid();
            } else {
                Log.e(TAG, "Invalid provider: " + authData.getProvider());
            }
            if (name != null) {
                mLoggedInStatusTextView.setText("Logged in as " + name + " (" + authData.getProvider() + ")");
            }
        } else {
            /* No authenticated user show all the login buttons */
            mPasswordLoginButton.setVisibility(View.VISIBLE);
            mLoggedInStatusTextView.setVisibility(View.GONE);
        }
        this.mAuthData = authData;
        /* invalidate options menu to hide/show the logout button */
        supportInvalidateOptionsMenu();
    }

    /**
     * Show errors to users
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Utility class for authentication results
     */
    private class MyAuthResultHandler implements Firebase.AuthResultHandler {

        private final String provider;
        private String email;

        public MyAuthResultHandler(String provider,String email) {
            this.provider = provider;
            this.email = email;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            mAuthProgressDialog.hide();
            Log.i(TAG, provider + " auth successful");
            setAuthenticatedUser(authData);
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            mAuthProgressDialog.hide();
            showErrorDialog(firebaseError.toString()+ ", user:"+email);
        }
    }







    public void loginWithPassword() {
        mAuthProgressDialog.show();
        EditText editUser = (EditText) findViewById(R.id.inputUser);
        String userName = editUser.getText().toString().trim();
        EditText editPassword = (EditText) findViewById(R.id.inputPassword);
        String password = editPassword.getText().toString().trim();
        //martinknud@gmail.com, 123
        firebase.authWithPassword(userName, password, new MyAuthResultHandler("password", userName));
    }


}

package com.sonata.socialapp.activities.sonata;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.sonata.socialapp.R;
import com.sonata.socialapp.utils.Util;
import com.sonata.socialapp.utils.MyApp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(Util.getNightMode()){
            setTheme(R.style.ThemeNight);
        }else{
            setTheme(R.style.ThemeDay);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);



        MyApp.ses=false;


        if(ParseUser.getCurrentUser()!=null){
            MobileAds.initialize(StartActivity.this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                    startActivity(new Intent(StartActivity.this,MainActivity.class));
                    finish();
                }
            });
        }
        else{
            JSONArray userList = Util.getSavedUsersFinal(StartActivity.this);
            asdasdas(0,userList);

        }


    }

    private void asdasdas (int a,JSONArray userList){
        if(userList.length()>0){
            if(userList.length()>a){
                try {
                    JSONObject usob = (JSONObject) userList.get(a);

                    ParseUser.becomeInBackground(usob.getString("session"), new LogInCallback() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if(e==null){
                                String text = String.format(getResources().getString(R.string.accsw), "@"+ParseUser.getCurrentUser().getUsername());
                                Util.ToastLong(StartActivity.this,text);
                                MobileAds.initialize(StartActivity.this, new OnInitializationCompleteListener() {
                                    @Override
                                    public void onInitializationComplete(InitializationStatus initializationStatus) {
                                        startActivity(new Intent(StartActivity.this,MainActivity.class));
                                        finish();
                                    }
                                });
                            }
                            else{
                                if(e.getCode()==ParseException.INVALID_SESSION_TOKEN){
                                    try {
                                        Util.removeUserFromCache(usob.getString("id"), StartActivity.this);
                                    } catch (JSONException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                asdasdas(a+1,userList);
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                startActivity(new Intent(this,LoginActivity.class));
                finish();
            }
        }
        else{
            startActivity(new Intent(this,LoginActivity.class));
            finish();
        }

    }



}

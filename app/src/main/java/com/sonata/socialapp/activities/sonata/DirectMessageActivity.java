package com.sonata.socialapp.activities.sonata;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.SubscriptionHandling;
import com.sonata.socialapp.R;
import com.sonata.socialapp.utils.GenelUtil;
import com.sonata.socialapp.utils.MyApp;
import com.sonata.socialapp.utils.VideoUtils.AutoPlayUtils;
import com.sonata.socialapp.utils.adapters.MessageAdapter;
import com.sonata.socialapp.utils.classes.Chat;
import com.sonata.socialapp.utils.classes.Encryption;
import com.sonata.socialapp.utils.classes.Message;
import com.sonata.socialapp.utils.classes.SonataUser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cn.jzvd.Jzvd;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import tgio.rncryptor.RNCryptorNative;

public class DirectMessageActivity extends AppCompatActivity {

    TextView topUsername;
    SonataUser user;
    Chat chat;
    RelativeLayout back, bottomCommentLayout;
    EditText editText;
    RNCryptorNative rncryptor;
    ImageButton send;
    RecyclerView recyclerView;
    MessageAdapter adapter;
    List<Message> list;
    LinearLayoutManager linearLayoutManager;
    ParseQuery<Message> parseQuery;
    ProgressBar progressBar;
    String to = "";
    ParseLiveQueryClient parseLiveQueryClient;
    Date date;
    boolean hasmore,loading;

    RecyclerView.OnScrollListener onScrollListener;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(GenelUtil.getNightMode()){
            setTheme(R.style.ThemeNight);
        }else{
            setTheme(R.style.ThemeDay);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_message);

        topUsername = findViewById(R.id.profileusernametext);
        user = getIntent().getParcelableExtra("user");
        to = user.getObjectId();
        if(user == null) return;
        topUsername.setText("@"+user.getUsername());

        send = findViewById(R.id.sendbutton);
        back = findViewById(R.id.backbuttonbutton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        recyclerView = findViewById(R.id.commentrecyclerview);

        bottomCommentLayout = findViewById(R.id.makecommentlayout);
        progressBar = findViewById(R.id.progressBar);
        editText = findViewById(R.id.commentedittext);
        rncryptor = new RNCryptorNative();
        //chat = getIntent() != null ? getIntent().getParcelableExtra("chat") : null;
        bottomCommentLayout.setVisibility(View.INVISIBLE);
        getChatAndMessages(to);

    }

    private void setUpRecyclerView(String key){
        if(!GenelUtil.isAlive(this)) return;
        list=new ArrayList<>();
        linearLayoutManager=new LinearLayoutManager(DirectMessageActivity.this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter=new MessageAdapter();
        adapter.setContext(list
                , Glide.with(DirectMessageActivity.this)
                ,user
                ,ParseUser.getCurrentUser().getObjectId()
                ,key);
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        onScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);


            }
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(dy<0&&linearLayoutManager.findFirstVisibleItemPosition()==0&&!loading&&hasmore){
                    if(chat==null)return;
                    loading=true;
                    getMessages(date,chat);
                }
            }
        };
        recyclerView.addOnScrollListener(onScrollListener);
        //adapter.notifyDataSetChanged();
    }

    private void getMessages(Date date,Chat chat){
        if(!GenelUtil.isAlive(this)) return;
        loading = true;
        HashMap<String,Object> params = new HashMap<>();
        params.put("date",date);
        params.put("chat",chat.getObjectId());
        ParseCloud.callFunctionInBackground("getMessages", params, new FunctionCallback<HashMap>() {
            @Override
            public void done(HashMap object, ParseException e) {
                Log.e("done","cloud code done");
                if(!GenelUtil.isAlive(DirectMessageActivity.this)) return;
                if(e==null){

                    setUpNewMessages((List<Message>) object.get("messages"),(boolean)object.get("hasmore"),(Date)object.get("date"));


                }
                else{
                    if(e.getCode()==ParseException.CONNECTION_FAILED){
                        getMessages(date,chat);
                    }
                    else{
                        GenelUtil.ToastLong(DirectMessageActivity.this,getString(R.string.error));
                        finish();
                    }
                }
            }
        });
    }

    private void getChatAndMessages(String id){
        if(!GenelUtil.isAlive(this)) return;
        loading = true;
        HashMap<String,Object> params = new HashMap<>();
        params.put("to",id);
        ParseCloud.callFunctionInBackground("getChatAndMessages", params, new FunctionCallback<HashMap>() {
            @Override
            public void done(HashMap object, ParseException e) {
                Log.e("done","cloud code done");
                if(!GenelUtil.isAlive(DirectMessageActivity.this)) return;
                if(e==null){
                    chat = object.get("chat") != null ? (Chat) object.get("chat") : null;
                    setUpAfterFetch((List<Message>) object.get("messages"),(boolean)object.get("hasmore"),(Date)object.get("date"));


                }
                else{
                    if(e.getCode()==ParseException.CONNECTION_FAILED){
                        getChatAndMessages(id);
                    }
                    else{
                        GenelUtil.ToastLong(DirectMessageActivity.this,getString(R.string.error));
                        finish();
                    }
                }
            }
        });
    }

    private void setUpNewMessages(List<Message> tempList, boolean hasmore, Date date) {
        if(!GenelUtil.isAlive(this)) return;
        this.hasmore = hasmore;
        this.date = date;



        //Collections.reverse(tempList);
        Observable.fromCallable(() -> {
            Collections.reverse(tempList);
            try{
                for(int i = 0; i < tempList.size(); i++){
                    Message message = tempList.get(i);
                    message.setMessage(rncryptor.decrypt(message.getEncryptedMessage(), chat.getKey()));
                }
                return true;
            }catch (Exception e){
                Log.e("rxjava",e.toString());
                return false;
            }

        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    if(!GenelUtil.isAlive(DirectMessageActivity.this)) return;
                    if(list.size()>0 && list.get(0).getType().equals("load")){
                        list.remove(0);
                        adapter.notifyDataSetChanged();
                    }

                    int preSize = list.size();
                    if(hasmore){
                        Message m = new Message();
                        m.setType("load");
                        tempList.add(0,m);
                    }
                    list.addAll(0,tempList);


                    adapter.notifyItemRangeInserted(0, tempList.size());
                    loading=false;

                });



    }

    private void setUpAfterFetch(List<Message> tempList, boolean hasmore, Date date) {
        if(!GenelUtil.isAlive(this)) return;
        this.hasmore = hasmore;
        this.date = date;
        if(chat != null) {
            setUpRecyclerView(chat.getKey());
            int preSize = list.size();


            //Collections.reverse(tempList);
            Observable.fromCallable(() -> {
                Collections.reverse(tempList);
                try{
                    for(int i = 0; i < tempList.size(); i++){
                        Message message = tempList.get(i);
                        message.setMessage(rncryptor.decrypt(message.getEncryptedMessage(), chat.getKey()));
                    }
                    return true;
                }catch (Exception e){
                    Log.e("rxjava",e.toString());
                    return false;
                }

            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((result) -> {
                        if(!GenelUtil.isAlive(DirectMessageActivity.this)) return;

                        list.addAll(0,tempList);
                        if(hasmore){
                            Message m = new Message();
                            m.setType("load");
                            list.add(0,m);
                        }
                        bottomCommentLayout.setVisibility(View.VISIBLE);

                        adapter.notifyItemRangeInserted(preSize, list.size()-preSize);
                        setSendClick();
                        progressBar.setVisibility(View.INVISIBLE);
                        connectLiveServer(chat);
                        HashMap<String,Object> params = new HashMap<>();
                        params.put("chat",chat.getObjectId());
                        ParseCloud.callFunctionInBackground("setChatAsRead",params);
                        loading=false;
                    });

        }else{
            setUpRecyclerView("");
            bottomCommentLayout.setVisibility(View.VISIBLE);
            setSendClick();
            progressBar.setVisibility(View.INVISIBLE);
            loading=false;
            //connectLiveServer(chat);
        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApp.whereAmI = "directMessage"+to;
        //if(chat != null && parseLiveQueryClient != null) parseLiveQueryClient.reconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApp.whereAmI = "";
        //if(parseLiveQueryClient != null) parseLiveQueryClient.disconnect();
    }

    private void setSendClick(){
        if(!GenelUtil.isAlive(this)) return;
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editText.getText().toString().trim().length()>0){
                    if(chat != null){
                        Message message = new Message();
                        message.setChat(chat.getObjectId());
                        message.setOwner(ParseUser.getCurrentUser().getObjectId());
                        message.setMessage(editText.getText().toString().trim());
                        message.setEncryptedMessage(new String(rncryptor.encrypt(editText.getText().toString().trim()
                                , chat.getKey())));
                        list.add(message);
                        //Log.e("chat id",chat.getObjectId());
                        adapter.notifyItemInserted(list.size()-1);
                        recyclerView.scrollToPosition(list.size()-1);

                    }
                    else{
                        sendFirstMessage();
                    }
                    editText.setText("");
                }
            }
        });
    }

    private void sendFirstMessage(){
        if(!GenelUtil.isAlive(this)) return;
        ProgressDialog progressDialog = new ProgressDialog(DirectMessageActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.sendmessage));
        progressDialog.show();

        String key = new String(rncryptor.encrypt(user.getObjectId()+ ParseUser.getCurrentUser().getObjectId()
                , user.getObjectId()+ ParseUser.getCurrentUser().getObjectId()));
        adapter.setkey(key);

        HashMap<String,Object> params = new HashMap<>();
        params.put("to",user.getObjectId());
        params.put("key",key);

        String text = new String(rncryptor.encrypt(editText.getText().toString().trim()
                , key));

        params.put("text",text);

        ParseCloud.callFunctionInBackground("sendFirstMessage", params, new FunctionCallback<HashMap>() {
            @Override
            public void done(HashMap object, ParseException e) {
                if(!GenelUtil.isAlive(DirectMessageActivity.this)) return;
                if(e==null){

                    chat = object.get("chat") != null ? (Chat) object.get("chat") : null;
                    Message message = (Message) object.get("message");
                    message.setMessage(rncryptor.decrypt(message.getEncryptedMessage(), chat.getKey()));
                    list.add(message);
                    adapter.notifyItemInserted(list.size()-1);
                    recyclerView.scrollToPosition(list.size()-1);
                    connectLiveServer(chat);
                    progressDialog.dismiss();
                }
                else{
                    GenelUtil.ToastLong(DirectMessageActivity.this,getString(R.string.error));
                    progressDialog.dismiss();
                }
            }
        });
    }

    private void connectLiveServer(Chat chat){

        try {
            parseLiveQueryClient = ParseLiveQueryClient.Factory.getClient(new URI("wss://ws-server.sonatasocialapp.com/parse/"));
            parseQuery = ParseQuery.getQuery(Message.class);
            parseQuery.whereEqualTo("chat",chat.getObjectId());
            parseQuery.whereEqualTo("owner",to);

            SubscriptionHandling<Message> subscriptionHandling = parseLiveQueryClient.subscribe(parseQuery);

            subscriptionHandling.handleEvent(SubscriptionHandling.Event.CREATE, new SubscriptionHandling.HandleEventCallback<Message>() {
                @Override
                public void onEvent(ParseQuery<Message> query, Message object) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            Log.e("LiveQuery","id "+object.getObjectId());
                            if(!object.getOwner().equals(GenelUtil.getCurrentUser().getObjectId())){
                                object.setMessage(rncryptor.decrypt(object.getEncryptedMessage(), chat.getKey()));
                                list.add(object);
                                adapter.notifyItemInserted(list.size()-1);
                                recyclerView.scrollToPosition(list.size()-1);
                            }
                        }
                    });
                }
            });

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onBackPressed() {
        if(getIntent()!=null) {
            if(getIntent().getBooleanExtra("notif",false)){
                if(this.isTaskRoot()){
                    startActivity(new Intent(this,MainActivity.class));
                    finish();
                }
                else{
                    super.onBackPressed();
                }
            }
            else{
                super.onBackPressed();
            }
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(parseLiveQueryClient != null) parseLiveQueryClient.unsubscribe(parseQuery);
    }
}

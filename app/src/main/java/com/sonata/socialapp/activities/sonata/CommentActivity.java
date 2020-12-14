package com.sonata.socialapp.activities.sonata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;


import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.jcminarro.roundkornerlayout.RoundKornerRelativeLayout;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;

import com.parse.ParseUser;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;
import com.sonata.socialapp.R;
import com.sonata.socialapp.socialview.Mention;
import com.sonata.socialapp.socialview.SocialAutoCompleteTextView;
import com.sonata.socialapp.socialview.SocialView;
import com.sonata.socialapp.socialview.SocialViewHelper;
import com.sonata.socialapp.utils.VideoUtils.AutoPlayUtils;
import com.sonata.socialapp.utils.adapters.ComAdapter;
import com.sonata.socialapp.utils.adapters.HomeAdapter;
import com.sonata.socialapp.utils.adapters.MentionAdapter;
import com.sonata.socialapp.utils.classes.Comment;
import com.sonata.socialapp.utils.GenelUtil;
import com.sonata.socialapp.utils.classes.Post;
import com.sonata.socialapp.utils.classes.SonataUser;
import com.sonata.socialapp.utils.interfaces.CommentAdapterClick;
import com.theartofdev.edmodo.cropper.CropImage;

import com.zxy.tiny.Tiny;
import com.zxy.tiny.callback.FileCallback;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import cn.jzvd.Jzvd;
import jp.wasabeef.glide.transformations.BlurTransformation;


public class CommentActivity extends AppCompatActivity implements CommentAdapterClick {
    List<ParseObject> list;
    RecyclerView recyclerView;
    private boolean postson=false;
    private LinearLayoutManager linearLayoutManager;
    ComAdapter adapter;
    Post post;
    private boolean loading=true;
    Date date;
    ImageButton sendbutton;
    SocialAutoCompleteTextView commenttext;
    private long lastClick=0;
    RecyclerView.OnScrollListener onScrollListener;
    SwipeRefreshLayout swipeRefreshLayout;
    SwipeRefreshLayout.OnRefreshListener onRefreshListener;

    RelativeLayout back;

    int a = 0;



    ProgressDialog alertDialog;
    RoundKornerRelativeLayout imageCancel,addImageLayout;
    ImageView commentimage;
    RelativeLayout imagelayout;
    RelativeLayout addImageRipple;



    Uri uri;


    private final int imagerequestcode = 9514;


    RelativeLayout makecommentlayout;

    //RoundKornerRelativeLayout errorlayout;


    ProgressBar progressBar;

    private ArrayAdapter<Mention> defaultMentionAdapter;



    @Override
    protected void onDestroy() {

        recyclerView.removeOnScrollListener(onScrollListener);
        onScrollListener=null;
        alertDialog.dismiss();
        alertDialog = null;

        date=null;
        adapter=null;
        postson=false;

        list.clear();
        recyclerView.setAdapter(null);
        list=null;
        adapter=null;
        linearLayoutManager=null;
        recyclerView.removeOnScrollListener(onScrollListener);
        onScrollListener=null;
        recyclerView=null;
        commenttext=null;
        sendbutton.setOnClickListener(null);
        sendbutton=null;
        onRefreshListener=null;
        swipeRefreshLayout.setOnRefreshListener(null);
        swipeRefreshLayout = null;
        back.setOnClickListener(null);
        back=null;
        post=null;
        imageCancel.setOnClickListener(null);
        imageCancel = null;
        addImageLayout = null;
        imagelayout = null;
        addImageRipple.setOnClickListener(null);
        addImageRipple = null;
        commentimage = null;
        uri=null;
        makecommentlayout = null;
        //errorlayout = null;
        super.onDestroy();




    }


    @Override
    protected void onResume() {
        super.onResume();
        if(adapter!=null){
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Jzvd.releaseAllVideos();
    }

    public void showReplies(Comment comment){
        startActivity(new Intent(this,CommentReplyActivity.class).putExtra("post",post)
                .putExtra("parentcomment",comment));
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(GenelUtil.getNightMode()){
            setTheme(R.style.ThemeNight);
        }else{
            setTheme(R.style.ThemeDay);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        if(getIntent().getExtras()==null){
            finish();
            return;
        }
        progressBar = findViewById(R.id.commentProgress);
        //errorlayout=findViewById(R.id.errorlayout);
        makecommentlayout = findViewById(R.id.makecommentlayout);
        alertDialog = new ProgressDialog(this);
        alertDialog.setCancelable(false);

        addImageLayout=findViewById(R.id.addImagebuttonlayout);
        imageCancel = findViewById(R.id.commentimagecancel);

        commentimage = findViewById(R.id.commentimageview);
        imagelayout = findViewById(R.id.commentimagelayout);
        addImageRipple=findViewById(R.id.commentaddimageripple);
        imageCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - lastClick < 1000){
                    return;
                }
                lastClick = SystemClock.elapsedRealtime();

                uri=null;
                commentimage.setImageDrawable(null);
                commentimage.setImageBitmap(null);
                Glide.with(commentimage).clear(commentimage);
                addImageLayout.setVisibility(View.VISIBLE);
                imagelayout.setVisibility(View.GONE);


            }
        });

        addImageRipple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - lastClick < 1000){
                    return;
                }
                lastClick = SystemClock.elapsedRealtime();
                String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                Permissions.check(CommentActivity.this,permissions,null, null, new PermissionHandler() {
                    @Override
                    public void onGranted() {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        startActivityForResult(Intent.createChooser(intent,getString(R.string.video)),imagerequestcode);
                    }
                    @Override
                    public void onDenied(Context context, ArrayList<String> deniedPermissions){

                    }
                });

            }
        });
        back = findViewById(R.id.backbuttonbutton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        sendbutton = findViewById(R.id.sendbutton);
        commenttext = findViewById(R.id.commentedittext);
        commenttext.setMentionPattern(Pattern.compile("(^|[^\\w])@([\\w\\_\\.]+)"));
        commenttext.setHashtagColor(getResources().getColor(R.color.blue));
        commenttext.setMentionColor(getResources().getColor(R.color.blue));
        commenttext.setHyperlinkColor(getResources().getColor(R.color.blue));

        defaultMentionAdapter = new MentionAdapter(CommentActivity.this);
        commenttext.setMentionAdapter(defaultMentionAdapter);

        commenttext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(count > before){
                    if(s.charAt(start) == ' ' || s.charAt(start) == '\n') return;

                    final String text = s.toString();

                    if(start > 0){


                        int spacePosition = -1;
                        int cursorPosition = start + count -1;
                        if(cursorPosition < 0) return;
                        while (true) {
                            if(cursorPosition < 0) {
                                break;
                            }
                            else {
                                if(s.charAt(cursorPosition) == ' ' || s.charAt(cursorPosition) == '\n'){
                                    spacePosition = cursorPosition + 1;
                                    break;
                                }
                                else {
                                    cursorPosition -=1;
                                }
                            }

                        }
                        if(spacePosition < 0) return;
                        if((start + count) - spacePosition < 1) return;
                        final String queryString = text.substring(spacePosition,start + count);
                        Log.e("Query String",queryString);
                        final String t = s.toString();
                        if(queryString.startsWith("@")&&queryString.replace("@","").length()>0){
                            //Do Query


                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // Do something after 5s = 5000ms
                                    if(GenelUtil.isAlive(CommentActivity.this)){
                                        if(t.equals(commenttext.getText().toString())){
                                            HashMap<String, Object> params = new HashMap<>();
                                            params.put("text",queryString.replace("@","").toLowerCase());
                                            ParseCloud.callFunctionInBackground("searchPerson", params, (FunctionCallback<List<SonataUser>>) (object, e) -> {
                                                Log.e("done","done");
                                                if(GenelUtil.isAlive(CommentActivity.this)){
                                                    if(e==null){
                                                        if(t.equals(commenttext.getText().toString())){
                                                            if(object!= null){
                                                                defaultMentionAdapter.clear();
                                                                for(int i = 0; i < object.size(); i++){
                                                                    if(object.get(i).getHasPp()){
                                                                        defaultMentionAdapter.add(new Mention(object.get(i).getUsername()
                                                                                ,object.get(i).getName(),object.get(i).getPPAdapter()));
                                                                    }
                                                                    else{
                                                                        defaultMentionAdapter.add(new Mention(object.get(i).getUsername()
                                                                                ,object.get(i).getName(),"empty"));
                                                                    }

                                                                }
                                                                defaultMentionAdapter.notifyDataSetChanged();

                                                                //initList(objects);
                                                            }
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }

                                }
                            }, 1000);
                        }


                    }


                }
                else{
                    defaultMentionAdapter.clear();
                    defaultMentionAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!s.toString().contains(" ")){
                    if(s.toString().startsWith("@")){
                        final String t = s.toString();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Do something after 5s = 5000ms
                                if(GenelUtil.isAlive(CommentActivity.this)){
                                    if(t.equals(commenttext.getText().toString())){
                                        HashMap<String, Object> params = new HashMap<>();
                                        params.put("text",t.replace("@","").toLowerCase());
                                        ParseCloud.callFunctionInBackground("searchPerson", params, (FunctionCallback<List<SonataUser>>) (object, e) -> {
                                            Log.e("done","done");
                                            if(GenelUtil.isAlive(CommentActivity.this)){
                                                if(e==null){
                                                    if(t.equals(commenttext.getText().toString())){
                                                        if(object!= null){
                                                            defaultMentionAdapter.clear();
                                                            for(int i = 0; i < object.size(); i++){
                                                                if(object.get(i).getHasPp()){
                                                                    defaultMentionAdapter.add(new Mention(object.get(i).getUsername()
                                                                            ,object.get(i).getName(),object.get(i).getPPAdapter()));
                                                                }
                                                                else{
                                                                    defaultMentionAdapter.add(new Mention(object.get(i).getUsername()
                                                                            ,object.get(i).getName(),"empty"));
                                                                }

                                                            }
                                                            defaultMentionAdapter.notifyDataSetChanged();

                                                            //initList(objects);
                                                        }
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }

                            }
                        }, 1000);
                    } else {
                        defaultMentionAdapter.clear();
                        defaultMentionAdapter.notifyDataSetChanged();
                    }
                } else {
                    defaultMentionAdapter.clear();
                    defaultMentionAdapter.notifyDataSetChanged();
                }

            }
        });


        recyclerView = findViewById(R.id.commentrecyclerview);
        swipeRefreshLayout = findViewById(R.id.homeFragmentSwipeRefreshLayout);


        onScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if(recyclerView!=null&&linearLayoutManager!=null){
                        AutoPlayUtils.onScrollPlayVideo(recyclerView, R.id.masterExoPlayer, linearLayoutManager.findFirstVisibleItemPosition(), linearLayoutManager.findLastVisibleItemPosition());
                        if(!recyclerView.canScrollVertically(-1)){
                            Jzvd.releaseAllVideos();
                        }
                    }
                }


            }
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if(linearLayoutManager.findLastVisibleItemPosition()>(list.size()-7)&&!loading&&!postson){
                    loading=true;
                    getComments(date,false);
                }


            }
        };





        list=new ArrayList<>();
        if(getIntent()!=null){
            post = getIntent().getParcelableExtra("post");
            if(post!=null){
                progressBar.setVisibility(View.INVISIBLE);
                list.add(post);

                Comment load = new Comment();
                load.setType("load");
                list.add(load);
                a = 2;

                linearLayoutManager=new LinearLayoutManager(CommentActivity.this);
                linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
                recyclerView.setLayoutManager(linearLayoutManager);
                adapter=new ComAdapter();
                adapter.setContext(list,Glide.with(CommentActivity.this),this);
                adapter.setHasStableIds(true);
                recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(list.size()-1);



                onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        if(!loading){
                            loading=true;

                            date=null;

                            getComments(null,true);
                            refreshPost(post.getObjectId());

                        }
                    }
                };
                swipeRefreshLayout.setOnRefreshListener(onRefreshListener);



                recyclerView.addOnScrollListener(onScrollListener);




                setSendButtonClickListener();
                if(!CommentActivity.this.isFinishing()&&!CommentActivity.this.isDestroyed()){

                    getComments(null,false);
                    refreshPost(post.getObjectId());
                }
            }
            else{
                Log.e("Info","post null");
                if(getIntent().getStringExtra("id")!=null){
                    refreshPostIlk(getIntent().getStringExtra("id"));
                }
                else{
                    Log.e("Info","postid null");
                    if(getIntent().getStringExtra("commentid")!=null){
                        getObjects(getIntent().getStringExtra("commentid"));
                    }
                    else{
                        Log.e("Info","commentid null");
                    }
                }

            }
        }




    }

    private void getObjects(String id){
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        ParseCloud.callFunctionInBackground("getNotifObjects", params, new FunctionCallback<List<ParseObject>>() {
            @Override
            public void done(List<ParseObject>  listObjects, ParseException e) {
                if(GenelUtil.isAlive(CommentActivity.this)){
                    if(e==null){
                        if(listObjects!=null){

                            Post p2 = (Post)listObjects.get(0);
                            p2.setLikenumber(p2.getLikenumber2());
                            p2.setCommentnumber(p2.getCommentnumber2());
                            p2.setSaved(p2.getSaved2());
                            p2.setCommentable(p2.getCommentable2());
                            p2.setLiked(p2.getLiked2());
                            list.add(p2);

                            Comment comment = (Comment)listObjects.get(1);
                            comment.setVote(comment.getVote2());
                            comment.setReplyCount(comment.getReplyCount2());
                            comment.setUpvote(comment.getUpvote2());
                            comment.setDownvote(comment.getDownvote2());
                            comment.setSaved(comment.getSaved2());
                            list.add(comment);
                            a = 3;

                            if(listObjects.size()>2){
                                Comment comment2 = (Comment)listObjects.get(2);
                                comment2.setVote(comment2.getVote2());
                                comment2.setReplyCount(comment2.getReplyCount2());
                                comment2.setUpvote(comment2.getUpvote2());
                                comment2.setDownvote(comment2.getDownvote2());
                                comment2.setSaved(comment2.getSaved2());
                                list.add(comment2);
                                a = 4;
                            }

                            post = p2;

                            progressBar.setVisibility(View.INVISIBLE);

                            Comment load = new Comment();
                            load.setType("load");
                            list.add(load);

                            linearLayoutManager=new LinearLayoutManager(CommentActivity.this);
                            linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
                            recyclerView.setLayoutManager(linearLayoutManager);
                            adapter=new ComAdapter();
                            adapter.setContext(list,Glide.with(CommentActivity.this),CommentActivity.this);
                            adapter.setHasStableIds(true);
                            recyclerView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                            recyclerView.scrollToPosition(list.size()-1);


                            onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
                                @Override
                                public void onRefresh() {
                                    if(!loading){
                                        loading=true;

                                        date=null;

                                        getComments(null,true);
                                        refreshPost(post.getObjectId());

                                    }
                                }
                            };
                            swipeRefreshLayout.setOnRefreshListener(onRefreshListener);



                            recyclerView.addOnScrollListener(onScrollListener);




                            setSendButtonClickListener();
                            if(!CommentActivity.this.isFinishing()&&!CommentActivity.this.isDestroyed()){

                                getComments(null,false);
                            }


                        }


                    }
                    else{
                        if(e.getCode()!=ParseException.OBJECT_NOT_FOUND&&e.getMessage().equals("denied")){
                            getObjects(id);
                        }
                        else if(e.getCode()==ParseException.CONNECTION_FAILED){
                            getObjects(id);
                        }


                    }
                }
            }
        });
    }
    private void refreshPostIlk(String id){
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        ParseCloud.callFunctionInBackground("updatePost", params, new FunctionCallback<Post>() {
            @Override
            public void done(Post  p2, ParseException e) {
                if(!CommentActivity.this.isFinishing()&&!CommentActivity.this.isDestroyed()){
                    if(e==null){

                        p2.setLikenumber(p2.getLikenumber2());
                        p2.setCommentnumber(p2.getCommentnumber2());
                        p2.setSaved(p2.getSaved2());
                        p2.setCommentable(p2.getCommentable2());
                        p2.setLiked(p2.getLiked2());
                        post = p2;

                        progressBar.setVisibility(View.INVISIBLE);
                        list.add(post);

                        Comment load = new Comment();
                        load.setType("load");
                        list.add(load);
                        a = 2;

                        linearLayoutManager=new LinearLayoutManager(CommentActivity.this);
                        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
                        recyclerView.setLayoutManager(linearLayoutManager);
                        adapter=new ComAdapter();
                        adapter.setContext(list,Glide.with(CommentActivity.this),CommentActivity.this);
                        adapter.setHasStableIds(true);
                        recyclerView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(list.size()-1);


                        onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
                            @Override
                            public void onRefresh() {
                                if(!loading){
                                    loading=true;

                                    date=null;

                                    getComments(null,true);
                                    refreshPost(post.getObjectId());

                                }
                            }
                        };
                        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);



                        recyclerView.addOnScrollListener(onScrollListener);




                        setSendButtonClickListener();
                        if(!CommentActivity.this.isFinishing()&&!CommentActivity.this.isDestroyed()){

                            getComments(null,false);
                        }



                    }
                    else{
                        if(e.getCode()!=ParseException.OBJECT_NOT_FOUND&&e.getMessage().equals("Denied")){
                            refreshPost(id);
                        }


                    }
                }
            }
        });
    }


    private void refreshPost(String id){
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        ParseCloud.callFunctionInBackground("updatePost", params, new FunctionCallback<Post>() {
            @Override
            public void done(Post  p2, ParseException e) {
                if(!CommentActivity.this.isFinishing()&&!CommentActivity.this.isDestroyed()){
                    if(e==null){

                        p2.setLikenumber(p2.getLikenumber2());
                        p2.setCommentnumber(p2.getCommentnumber2());
                        p2.setSaved(p2.getSaved2());
                        p2.setCommentable(p2.getCommentable2());
                        p2.setLiked(p2.getLiked2());
                        post = p2;
                        if(adapter!=null){
                            adapter.notifyDataSetChanged();
                        }

                    }
                    else{
                        if(e.getCode()!=ParseException.OBJECT_NOT_FOUND&&e.getMessage().equals("Denied")){
                            refreshPost(id);
                        }

                        Log.e("error code",""+e.getCode());
                        Log.e("error message",e.getMessage());

                    }
                }
            }
        });
    }

    private void refreshSetting(){
        list.clear();
        list.add(post);
        setSendButtonClickListener();


    }


    private void getComments(Date date1,boolean isRefresh){
        if(GenelUtil.isAlive(CommentActivity.this)){
            HashMap<String, Object> params = new HashMap<String, Object>();
            if(date1!=null){
                params.put("date",date1);
            }
            params.put("post", post.getObjectId());
            ParseCloud.callFunctionInBackground("getComments", params, new FunctionCallback<List<Comment>>() {
                @Override
                public void done(List<Comment>  objects, ParseException e) {
                    Log.e("done","done");
                    if(GenelUtil.isAlive(CommentActivity.this)){
                        if(e==null){

                            if(objects!= null){
                                if(isRefresh){
                                    refreshSetting();
                                }
                                initObjects(objects,isRefresh);
                            }

                        }
                        else{
                            if(e.getCode()!=ParseException.OBJECT_NOT_FOUND&&!e.getMessage().equals("denied")){
                                getComments(date1,isRefresh);
                            }

                        }
                    }
                }
            });

        }
    }



    public void reply(Comment comment){
        if(comment!=null){
            startActivity(new Intent(this,CommentReplyActivity.class).putExtra("reply",true).putExtra("post",post)
                    .putExtra("parentcomment",comment));
        }
    }



    public void comment(){
        if(commenttext.getText().toString().length()>0){
            commenttext.setText(commenttext.getText().toString()+" ");
        }


        commenttext.append("");
        commenttext.setSelection(commenttext.getText().length());
        commenttext.requestFocus();
        GenelUtil.showKeyboard(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==imagerequestcode&&data.getData()!=null){
                uri=data.getData();
                try {
                    CropImage.activity(uri)
                            .setActivityTitle(getString(R.string.cropimage))
                            .setCropMenuCropButtonTitle(getString(R.string.crop)).start(this);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                }







            }
            else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                uri=result.getUri();
                Glide.with(this).load(uri).into(commentimage);
                addImageLayout.setVisibility(View.GONE);
                imagelayout.setVisibility(View.VISIBLE);

            }


        }
    }


    private void initObjects(List<Comment> objects,boolean isRefresh){
        if(GenelUtil.isAlive(CommentActivity.this)){
            if(objects.size()==0){
                postson =true;
                swipeRefreshLayout.setRefreshing(false);
                if(list.get(list.size()-1).getString("type").equals("load")){
                    list.remove(list.size()-1);
                }
                if(list.size()==1){
                    Comment load = new Comment();
                    load.setType("boş");
                    list.add(load);
                }


                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(list.size()-1);
                loading =false;
                swipeRefreshLayout.setRefreshing(false);
            }
            else{
                if(list.size()>0){
                    if(list.get(list.size()-1).getString("type").equals("load")){
                        list.remove(list.size()-1);
                    }
                }

                if(objects.size()<20){
                    date = objects.get(objects.size()-1).getCreatedAt();
                    postson =true;
                    loading =false;
                    for(int i=0;i<objects.size();i++){
                        if(!list.contains(objects.get(i))){
                            Comment comment = objects.get(i);
                            comment.setVote(comment.getVote2());
                            comment.setReplyCount(comment.getReplyCount2());
                            comment.setUpvote(comment.getUpvote2());
                            comment.setDownvote(comment.getDownvote2());
                            comment.setSaved(comment.getSaved2());

                            list.add(comment);
                        }
                    }

                    adapter.notifyDataSetChanged();



                    loading =false;
                    swipeRefreshLayout.setRefreshing(false);


                }
                else{
                    date = objects.get(objects.size()-1).getCreatedAt();
                    for(int i=0;i<objects.size();i++){
                        if(!list.contains(objects.get(i))){
                            Comment comment = objects.get(i);
                            comment.setVote(comment.getVote2());
                            comment.setReplyCount(comment.getReplyCount2());
                            comment.setUpvote(comment.getUpvote2());
                            comment.setDownvote(comment.getDownvote2());
                            comment.setSaved(comment.getSaved2());

                            list.add(comment);
                        }



                    }



                    Comment load = new Comment();
                    load.setType("load");
                    list.add(load);

                    adapter.notifyDataSetChanged();

                    loading =false;
                    swipeRefreshLayout.setRefreshing(false);
                }

            }



        }
    }


    private void setSendButtonClickListener(){
        sendbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - lastClick < 1000){
                    return;
                }
                lastClick = SystemClock.elapsedRealtime();
                if(post!=null){
                    if(commenttext.getText().toString().trim().length()>0||imagelayout.getVisibility()==View.VISIBLE){
                        alertDialog.show();
                        alertDialog.setMessage(getString(R.string.commenting));

                        if(imagelayout.getVisibility()==View.VISIBLE){
                            alertDialog.setMessage(getString(R.string.preparingimage));
                            Tiny.FileCompressOptions options = new Tiny.FileCompressOptions();
                            options.size=55;
                            Tiny.getInstance().source(uri.getPath()).asFile().withOptions(options).compress(new FileCallback() {
                                @Override
                                public void callback(boolean isSuccess, String outfile, Throwable t) {
                                    if(GenelUtil.isAlive(CommentActivity.this)){
                                        if(!isSuccess){
                                            alertDialog.dismiss();
                                            GenelUtil.ToastLong(getString(R.string.error),getApplicationContext());
                                        }
                                        else{

                                            ParseFile media = new ParseFile(new File(outfile));
                                            media.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    if(GenelUtil.isAlive(CommentActivity.this)){
                                                        HashMap<String, Object> params = new HashMap<String, Object>();
                                                        params.put("text", commenttext.getText().toString().trim());
                                                        params.put("post",post.getObjectId());
                                                        params.put("media",media);
                                                        ParseCloud.callFunctionInBackground("commentImage", params, new FunctionCallback<Comment>() {
                                                            @Override
                                                            public void done(Comment postID, ParseException e) {
                                                                if(!CommentActivity.this.isFinishing()&&!CommentActivity.this.isDestroyed()){
                                                                    alertDialog.dismiss();
                                                                    if(e==null){
                                                                        if(list.get(list.size()-1).getString("type").equals("load")||list.get(list.size()-1).getString("type").equals("boş")){
                                                                            list.remove(list.size()-1);
                                                                        }

                                                                        imageCancel.performClick();
                                                                        list.add(postID);
                                                                        post.increment("commentnumber");
                                                                        adapter.notifyDataSetChanged();
                                                                        recyclerView.scrollToPosition(list.size()-1);
                                                                        alertDialog.dismiss();
                                                                        GenelUtil.hideKeyboard(CommentActivity.this);
                                                                        commenttext.setText("");
                                                                        commenttext.clearFocus();


                                                                    }
                                                                    else{
                                                                        if(e.getCode()==ParseException.INVALID_SESSION_TOKEN){
                                                                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                                                            if (notificationManager != null) {
                                                                                notificationManager.cancelAll();
                                                                            }
                                                                            ParseUser.logOut();
                                                                            Intent intent = new Intent(CommentActivity.this, LoginActivity.class);
                                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                            startActivity(intent);
                                                                            finish();
                                                                        }
                                                                        else{
                                                                            if(e.getMessage().equals("CommentsDisabled")){
                                                                                GenelUtil.ToastLong(getApplicationContext(),getString(R.string.commentdisable));
                                                                            }
                                                                            else{
                                                                                GenelUtil.ToastLong(getApplicationContext(),getString(R.string.error));
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }
                                                }
                                            }, new ProgressCallback() {
                                                @Override
                                                public void done(Integer percentDone) {
                                                    if(GenelUtil.isAlive(CommentActivity.this)){
                                                        alertDialog.setMessage(getString(R.string.uploading)+" ("+percentDone+"%)");
                                                    }
                                                }
                                            });


                                        }
                                    }
                                }
                            });








                        }
                        else{


                            HashMap<String, Object> params = new HashMap<String, Object>();
                            params.put("text", commenttext.getText().toString().trim());
                            params.put("post",post.getObjectId());
                            ParseCloud.callFunctionInBackground("commentText", params, new FunctionCallback<Comment>() {
                                @Override
                                public void done(Comment postID, ParseException e) {
                                    if(!CommentActivity.this.isFinishing()&&!CommentActivity.this.isDestroyed()){
                                        alertDialog.dismiss();
                                        if(e==null){
                                            if(list.get(list.size()-1).getString("type").equals("load")||list.get(list.size()-1).getString("type").equals("boş")){
                                                list.remove(list.size()-1);
                                            }
                                            imageCancel.performClick();
                                            list.add(postID);
                                            post.increment("commentnumber");
                                            adapter.notifyDataSetChanged();
                                            recyclerView.scrollToPosition(list.size()-1);
                                            alertDialog.dismiss();
                                            GenelUtil.hideKeyboard(CommentActivity.this);
                                            commenttext.setText("");
                                            commenttext.clearFocus();


                                        }
                                        else{
                                            Log.e("error message",e.getMessage());
                                            if(e.getCode()==ParseException.INVALID_SESSION_TOKEN){
                                                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                                if (notificationManager != null) {
                                                    notificationManager.cancelAll();
                                                }
                                                ParseUser.logOut();
                                                Intent intent = new Intent(CommentActivity.this, LoginActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            }
                                            else{
                                                if(e.getMessage().equals("CommentsDisabled")){
                                                    GenelUtil.ToastLong(getApplicationContext(),getString(R.string.commentdisable));
                                                }
                                                else{
                                                    GenelUtil.ToastLong(getApplicationContext(),getString(R.string.error));
                                                }
                                            }
                                        }
                                    }
                                }
                            });






                        }


                    }

                }

            }
        });

    }




    @Override
    public void onBackPressed() {

        if(getIntent()!=null){
            if(getIntent().getBooleanExtra("notif",false)){
                if(this.isTaskRoot()){
                    if(commentimage!=null){
                        Glide.with(commentimage).clear(commentimage);
                        commentimage.setImageBitmap(null);
                        commentimage.setImageDrawable(null);
                    }
                    startActivity(new Intent(this,MainActivity.class));
                    finish();
                }
                else{
                    if(commentimage!=null){
                        Glide.with(commentimage).clear(commentimage);
                        commentimage.setImageBitmap(null);
                        commentimage.setImageDrawable(null);
                    }
                    super.onBackPressed();
                }
            }
            else{
                if(commentimage!=null){
                    Glide.with(commentimage).clear(commentimage);
                    commentimage.setImageBitmap(null);
                    commentimage.setImageDrawable(null);
                }
                super.onBackPressed();
            }
        }else{
            if(commentimage!=null){
                Glide.with(commentimage).clear(commentimage);
                commentimage.setImageBitmap(null);
                commentimage.setImageDrawable(null);
            }
            super.onBackPressed();
        }


    }


    @Override
    public void onPostOptionsClick(int position, TextView commentNumber) {
        Post post = (Post) list.get(position);
        ProgressDialog progressDialog = new ProgressDialog(CommentActivity.this);
        progressDialog.setCancelable(false);
        ArrayList<String> other = new ArrayList<>();
        if(post.getUser().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())){
            //Bu gönderi benim
            if(post.getCommentable()){
                other.add(getString(R.string.disablecomment));
            }
            if(!post.getCommentable()){
                other.add(getString(R.string.enablecomment));
            }
            other.add(getString(R.string.delete));

        }
        if(post.getSaved()){
            other.add(getString(R.string.unsavepost));
        }
        if(!post.getSaved()){
            other.add(getString(R.string.savepost));
        }
        if(!post.getUser().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())){
            //Bu gönderi başkasına ait
            other.add(getString(R.string.report));
        }

        String[] popupMenu = other.toArray(new String[other.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(CommentActivity.this);
        builder.setCancelable(true);

        builder.setItems(popupMenu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String select = popupMenu[which];
                if(select.equals(getString(R.string.disablecomment))){

                    progressDialog.setMessage(getString(R.string.disablecomment));
                    progressDialog.show();

                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("enableDisableComment", params, new FunctionCallback<String>() {
                        @Override
                        public void done(String object, ParseException e) {
                            if(e==null){
                                post.setCommentable(false);
                                progressDialog.dismiss();
                                commentNumber.setText(CommentActivity.this.getString(R.string.disabledcomment));
                            }
                            else{
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                            }
                        }
                    });

                }
                if(select.equals(getString(R.string.savepost))){
                    //savePost

                    progressDialog.setMessage(getString(R.string.savepost));
                    progressDialog.show();
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("savePost", params, new FunctionCallback<String>() {
                        @Override
                        public void done(String object, ParseException e) {
                            if(e==null){
                                post.setSaved(true);
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.postsaved));

                            }
                            else{
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                            }
                        }
                    });

                }
                if(select.equals(getString(R.string.unsavepost))){
                    //UnsavePost

                    progressDialog.setMessage(getString(R.string.unsavepost));
                    progressDialog.show();
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("unsavePost", params, new FunctionCallback<String>() {
                        @Override
                        public void done(String object, ParseException e) {
                            if(e==null){
                                post.setSaved(false);
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.postunsaved));

                            }
                            else{
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                            }
                        }
                    });
                }
                if(select.equals(getString(R.string.report))){

                    progressDialog.setMessage(getString(R.string.report));
                    progressDialog.show();
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("reportPost", params, new FunctionCallback<String>() {
                        @Override
                        public void done(String object, ParseException e) {
                            if(e==null){
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.reportsucces));
                                progressDialog.dismiss();
                            }
                            else{
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                            }
                        }
                    });


                }
                if(select.equals(getString(R.string.enablecomment))){

                    progressDialog.setMessage(getString(R.string.enablecomment));
                    progressDialog.show();

                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("enableDisableComment", params, new FunctionCallback<String>() {
                        @Override
                        public void done(String object, ParseException e) {
                            if(e==null){
                                post.setCommentable(true);
                                progressDialog.dismiss();

                                commentNumber.setText(GenelUtil.ConvertNumber((int)post.getCommentnumber(),CommentActivity.this));
                            }
                            else{
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                            }
                        }
                    });

                }
                if(select.equals(getString(R.string.delete))){

                    AlertDialog.Builder builder = new AlertDialog.Builder(CommentActivity.this);
                    builder.setTitle(R.string.deletetitle);
                    builder.setCancelable(true);
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();

                            progressDialog.setMessage(getString(R.string.delete));
                            progressDialog.show();
                            HashMap<String, Object> params = new HashMap<String, Object>();
                            params.put("postID", post.getObjectId());
                            ParseCloud.callFunctionInBackground("deletePost", params, new FunctionCallback<String>() {
                                @Override
                                public void done(String object, ParseException e) {
                                    if(e==null&&object.equals("deleted")){
                                        post.setIsDeleted(true);
                                        finish();
                                    }
                                    else{
                                        progressDialog.dismiss();
                                        GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                                    }
                                }
                            });


                        }
                    });
                    builder.show();

                }
            }
        });
        builder.show();
    }

    @Override
    public void onPostSocialClick(int position, int clickType, String text) {
        if(clickType== HomeAdapter.TYPE_HASHTAG){
            //hashtag
            startActivity(new Intent(CommentActivity.this, HashtagActivity.class).putExtra("hashtag",text.replace("#","")));

        }
        else if(clickType==HomeAdapter.TYPE_MENTION){
            //mention
            String username = text;

            username = username.replace("@","").trim();


            if(!username.equals(ParseUser.getCurrentUser().getUsername())){
                startActivity(new Intent(CommentActivity.this, GuestProfileActivity.class).putExtra("username",username));
            }

        }
        else if(clickType==HomeAdapter.TYPE_LINK){
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            String url = text;
            if(!url.startsWith("http")){
                url = "http://"+url;
            }
            if(GenelUtil.getNightMode()){
                builder.setToolbarColor(Color.parseColor("#303030"));
            }
            else{
                builder.setToolbarColor(Color.parseColor("#ffffff"));
            }
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(CommentActivity.this, Uri.parse(url));
        }
    }

    @Override
    public void onCommentOptionsClick(int position) {
        Comment post = (Comment)list.get(position);
        ArrayList<String> other = new ArrayList<>();
        if(post.getUser().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())){
            //Bu gönderi benim
            other.add(getString(R.string.delete));

        }
        if(post.getSaved()){
            other.add(getString(R.string.unsavecomment));
        }
        if(!post.getSaved()){
            other.add(getString(R.string.savecomment));
        }
        if(!post.getUser().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())){
            //Bu gönderi başkasına ait
            other.add(getString(R.string.report));
        }

        String[] popupMenu = other.toArray(new String[other.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(CommentActivity.this);
        builder.setCancelable(true);

        builder.setItems(popupMenu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String select = popupMenu[which];
                if(select.equals(getString(R.string.savecomment))){
                    //savePost
                    ProgressDialog progressDialog = new ProgressDialog(CommentActivity.this);
                    progressDialog.setCancelable(false);
                    progressDialog.setMessage(getString(R.string.savecomment));
                    progressDialog.show();
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("saveComment", params, new FunctionCallback<String>() {
                        @Override
                        public void done(String object, ParseException e) {
                            if(e==null){
                                post.setSaved(true);
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.commentsaved));

                            }
                            else{
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                            }
                        }
                    });

                }
                if(select.equals(getString(R.string.unsavecomment))){
                    //UnsavePost
                    ProgressDialog progressDialog = new ProgressDialog(CommentActivity.this);
                    progressDialog.setCancelable(false);
                    progressDialog.setMessage(getString(R.string.unsavecomment));
                    progressDialog.show();
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("unsaveComment", params, new FunctionCallback<String>() {
                        @Override
                        public void done(String object, ParseException e) {
                            if(e==null){
                                post.setSaved(false);
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.commentunsaved));

                            }
                            else{
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                            }
                        }
                    });
                }
                if(select.equals(getString(R.string.report))){
                    ProgressDialog progressDialog = new ProgressDialog(CommentActivity.this);
                    progressDialog.setCancelable(false);
                    progressDialog.setMessage(getString(R.string.report));
                    progressDialog.show();
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("reportComment", params, new FunctionCallback<String>() {
                        @Override
                        public void done(String object, ParseException e) {
                            if(e==null){
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.reportsucces));
                                progressDialog.dismiss();
                            }
                            else{
                                progressDialog.dismiss();
                                GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                            }
                        }
                    });

                }

                if(select.equals(getString(R.string.delete))){

                    AlertDialog.Builder builder = new AlertDialog.Builder(CommentActivity.this);
                    builder.setTitle(R.string.deletetitlecomment);
                    builder.setCancelable(true);
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                            ProgressDialog progressDialog = new ProgressDialog(CommentActivity.this);
                            progressDialog.setCancelable(false);
                            progressDialog.setMessage(getString(R.string.delete));
                            progressDialog.show();
                            HashMap<String, Object> params = new HashMap<String, Object>();
                            params.put("postID", post.getObjectId());
                            ParseCloud.callFunctionInBackground("deleteComment", params, new FunctionCallback<String>() {
                                @Override
                                public void done(String object, ParseException e) {
                                    if(e==null&&object.equals("deleted")){
                                        list.remove(position);
                                        adapter.notifyDataSetChanged();
                                        progressDialog.dismiss();
                                    }
                                    else{
                                        progressDialog.dismiss();
                                        GenelUtil.ToastLong(CommentActivity.this,getString(R.string.error));
                                    }
                                }
                            });


                        }
                    });
                    builder.show();

                }
            }
        });
        builder.show();

    }

    @Override
    public void onCommentUpvoteClick(int position, ImageView upvoteimage, ImageView downvoteimage, TextView votecount) {
        Comment post = (Comment) list.get(position);


        if(post.getObjectId()!=null){
            if(post.getDownvote()){
                post.setDownvote(false);
                post.setUpvote(true);
                downvoteimage.setImageDrawable(getDrawable(R.drawable.ic_downvote));
                upvoteimage.setImageDrawable( getDrawable(R.drawable.ic_upvote_blue));
                votecount.setTextColor(Color.parseColor("#2d72bc"));
                post.increment("vote",2);
                votecount.setText(GenelUtil.ConvertNumber((int)post.getVote(),CommentActivity.this));


                //upvote
                HashMap<String, Object> params = new HashMap<String, Object>();
                params.put("postID", post.getObjectId());
                ParseCloud.callFunctionInBackground("upvoteComment", params);

            }
            else{
                if(post.getUpvote()){
                    post.setUpvote(false);
                    upvoteimage.setImageDrawable(getDrawable(R.drawable.ic_upvote));
                    votecount.setTextColor(Color.parseColor("#999999"));
                    post.increment("vote",-1);
                    votecount.setText(GenelUtil.ConvertNumber((int)post.getVote(),CommentActivity.this));


                    //unupvote
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("unvoteComment", params);
                }
                else{
                    post.setUpvote(true);
                    upvoteimage.setImageDrawable(getDrawable(R.drawable.ic_upvote_blue));
                    votecount.setTextColor(Color.parseColor("#2d72bc"));
                    post.increment("vote");
                    votecount.setText(GenelUtil.ConvertNumber((int)post.getVote(),CommentActivity.this));


                    //upvote
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("upvoteComment", params);

                }
            }
        }
    }

    @Override
    public void onCommentDownvoteClick(int position, ImageView upvoteimage, ImageView downvoteimage, TextView votecount) {
        Comment post = (Comment) list.get(position);
        if(post.getObjectId()!=null){
            if(post.getUpvote()){
                post.setUpvote(false);
                upvoteimage.setImageDrawable(getDrawable(R.drawable.ic_upvote));
                post.setDownvote(true);
                downvoteimage.setImageDrawable(getDrawable(R.drawable.ic_downvote_red));
                votecount.setTextColor(Color.parseColor("#a64942"));

                post.increment("vote",-2);
                votecount.setText(GenelUtil.ConvertNumber((int)post.getVote(),CommentActivity.this));



                HashMap<String, Object> params = new HashMap<String, Object>();
                params.put("postID", post.getObjectId());
                ParseCloud.callFunctionInBackground("downvoteComment", params);

            }
            else{
                if(post.getDownvote()){
                    post.setDownvote(false);
                    downvoteimage.setImageDrawable(getDrawable(R.drawable.ic_downvote));
                    votecount.setTextColor(Color.parseColor("#999999"));
                    post.increment("vote");
                    votecount.setText(GenelUtil.ConvertNumber((int)post.getVote(),CommentActivity.this));


                    //undownvote
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("unvoteComment", params);
                }
                else{
                    post.setDownvote(true);
                    downvoteimage.setImageDrawable(getDrawable(R.drawable.ic_downvote_red));
                    votecount.setTextColor(Color.parseColor("#a64942"));
                    post.increment("vote",-1);
                    votecount.setText(GenelUtil.ConvertNumber((int)post.getVote(),CommentActivity.this));


                    //downvote
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("postID", post.getObjectId());
                    ParseCloud.callFunctionInBackground("downvoteComment", params);
                }
            }
        }
    }





    @Override
    public void onCommentShowReplies(int position) {
        Comment post = (Comment) list.get(position);
        if(post.getString("isreply").equals("false")){
            showReplies(post);
        }
    }

    @Override
    public void onCommentReply(int position) {
        Comment post = (Comment) list.get(position);
        if(post.getString("isreply").equals("false")){
            reply(post);
        }
    }


    @Override
    public void onCommentSocialClick(int position, int clickType, String text) {
        if(clickType== HomeAdapter.TYPE_HASHTAG){
            //hashtag
            startActivity(new Intent(CommentActivity.this, HashtagActivity.class).putExtra("hashtag",text.replace("#","")));

        }
        else if(clickType==HomeAdapter.TYPE_MENTION){
            //mention
            String username = text;

            username = username.replace("@","").trim();


            if(!username.equals(ParseUser.getCurrentUser().getUsername())){
                startActivity(new Intent(CommentActivity.this, GuestProfileActivity.class).putExtra("username",username));
            }

        }
        else if(clickType==HomeAdapter.TYPE_LINK){
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            String url = text;
            if(!url.startsWith("http")){
                url = "http://"+url;
            }
            if(GenelUtil.getNightMode()){
                builder.setToolbarColor(Color.parseColor("#303030"));
            }
            else{
                builder.setToolbarColor(Color.parseColor("#ffffff"));
            }
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(CommentActivity.this, Uri.parse(url));
        }
    }

    @Override
    public void onPostLikeClick(int position, ImageView likeImage, TextView likeNumber) {
        Post post = (Post)list.get(position);
        if(!post.getLiked()){
            post.setLiked(true);

            likeImage.setImageDrawable(CommentActivity.this.getDrawable(R.drawable.ic_like_red));
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("postID", post.getObjectId());
            ParseCloud.callFunctionInBackground("likePost", params);


            post.increment("likenumber");
            likeNumber.setText(GenelUtil.ConvertNumber((int)post.getLikenumber(),CommentActivity.this));


        }
        else{
            post.setLiked(false);
            likeImage.setImageDrawable(CommentActivity.this.getDrawable(R.drawable.ic_like));
            if(post.getLikenumber()>0){

                post.increment("likenumber",-1);
                likeNumber.setText(GenelUtil.ConvertNumber((int)post.getLikenumber(),CommentActivity.this));                                                    }
            else{
                post.setLikenumber(0);
                likeNumber.setText("0");
            }
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("postID", post.getObjectId());
            ParseCloud.callFunctionInBackground("unlikePost", params);

        }
    }

    @Override
    public void onGoToProfileClick(int position) {
        ParseObject post =  list.get(position);
        if(!((SonataUser)post.get("user")).getObjectId().equals(ParseUser.getCurrentUser().getObjectId())){
            startActivity(new Intent(CommentActivity.this, GuestProfileActivity.class).putExtra("user",((SonataUser)post.get("user"))));
        }
    }

    @Override
    public void onPostOpenComments(int position) {

    }

    @Override
    public void onPostLinkClick(int position) {
        if(position!=0){
            return;
        }
        Post post = (Post)list.get(position);
        String url = post.getUrl();
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        if(ParseUser.getCurrentUser().getBoolean("nightmode")){
            builder.setToolbarColor(Color.parseColor("#303030"));
        }
        else{
            builder.setToolbarColor(Color.parseColor("#ffffff"));
        }
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(CommentActivity.this, Uri.parse(url));
    }

    @Override
    public void onImageClick(int position,ImageView imageView,int pos) {
        if(position==0){
            Post post = (Post)list.get(position);
            ArrayList<String> ulist = new ArrayList<>();
            ArrayList<String> uList2 = new ArrayList<>();

            ulist.add(post.getMainMedia().getUrl());
            uList2.add(post.getThumbMedia().getUrl());
            if(post.getImageCount()>1){
                ulist.add(post.getMainMedia1().getUrl());
                uList2.add(post.getThumbMedia1().getUrl());
            }
            if(post.getImageCount()>2){
                ulist.add(post.getMainMedia2().getUrl());
                uList2.add(post.getThumbMedia2().getUrl());
            }
            if(post.getImageCount()>3){
                ulist.add(post.getMainMedia3().getUrl());
                uList2.add(post.getThumbMedia3().getUrl());
            }
            GenelUtil.showImage(ulist,uList2,imageView,pos,adapter);
        }
        else{
            Comment post = (Comment)list.get(position);
            ArrayList<String> ulist = new ArrayList<>();
            ArrayList<String> uList2 = new ArrayList<>();

            ulist.add(post.getMainMedia().getUrl());
            uList2.add(post.getThumbMedia().getUrl());

            GenelUtil.showImage(ulist,uList2,imageView,pos,adapter);
        }

    }

    @Override
    public void onReloadImageClick(int position, RoundKornerRelativeLayout reloadLayout, ProgressBar progressBar, ImageView imageView) {
        if(position==0){
            Post post = (Post) list.get(position);

            reloadLayout.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            if(post.getNsfw()){
                Glide.with(CommentActivity.this).load(post.getMainMedia().getUrl()).thumbnail(Glide.with(CommentActivity.this).load(post.getThumbMedia().getUrl())).apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3))).addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.INVISIBLE);
                        reloadLayout.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.INVISIBLE);
                        return false;
                    }
                }).into(imageView);
            }
            else{
                if(post.getRatioH()>1280||post.getRatioW()>1280){
                    int ih = 1280;
                    int iw = 1280;
                    if(post.getRatioH()>post.getRatioW()){
                        ih = 1280;
                        iw = 1280 * (post.getRatioW()/post.getRatioH());
                    }
                    else{
                        iw = 1280;
                        ih = 1280 * (post.getRatioH()/post.getRatioW());
                    }
                    Glide.with(CommentActivity.this).load(post.getMainMedia().getUrl()).override(iw,ih).thumbnail(Glide.with(CommentActivity.this).load(post.getThumbMedia().getUrl())).addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.INVISIBLE);
                            reloadLayout.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.INVISIBLE);
                            return false;
                        }
                    }).into(imageView);

                }
                else{
                    Glide.with(CommentActivity.this).load(post.getMainMedia().getUrl()).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).thumbnail(Glide.with(CommentActivity.this).load(post.getThumbMedia().getUrl())).addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.INVISIBLE);
                            reloadLayout.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.INVISIBLE);
                            return false;
                        }
                    }).into(imageView);

                }
            }
        }
        else{
            Comment post = (Comment) list.get(position);

            reloadLayout.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            if(post.getNsfw()){
                Glide.with(CommentActivity.this).load(post.getMainMedia().getUrl()).apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3))).addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.INVISIBLE);
                        reloadLayout.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.INVISIBLE);
                        return false;
                    }
                }).into(imageView);
            }
            else{
                if(post.getRatioH()>1280||post.getRatioW()>1280){
                    int ih = 1280;
                    int iw = 1280;
                    if(post.getRatioH()>post.getRatioW()){
                        ih = 1280;
                        iw = 1280 * (post.getRatioW()/post.getRatioH());
                    }
                    else{
                        iw = 1280;
                        ih = 1280 * (post.getRatioH()/post.getRatioW());
                    }
                    Glide.with(CommentActivity.this).load(post.getMainMedia().getUrl()).override(iw,ih).thumbnail(Glide.with(CommentActivity.this).load(post.getThumbMedia().getUrl())).addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.INVISIBLE);
                            reloadLayout.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.INVISIBLE);
                            return false;
                        }
                    }).into(imageView);

                }
                else{
                    Glide.with(CommentActivity.this).load(post.getMainMedia().getUrl()).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).thumbnail(Glide.with(CommentActivity.this).load(post.getThumbMedia().getUrl())).addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.INVISIBLE);
                            reloadLayout.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.INVISIBLE);
                            return false;
                        }
                    }).into(imageView);

                }
            }
        }

    }




}
package com.gmu.notesapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NoteDisplayActivity extends AppCompatActivity {

    private String title = null, notes;
    private List<String> tags, allTags;
    private SQLiteDatabase db;
    private DatabaseHandler dbHandler;
    private TextView title_tv, notes_tv;
    private RecyclerView tagsList;
    private RecyclerView.LayoutManager tagsLayoutManager;
    private TagsListAdapter tagsListAdapter;
    private int id = 0;
    private String returnSearch = null;

    public static final String NOTE_DISPLAY_TITLE_SLOT = "com.gmu.notesapp.NOTE_DISPLAY_TITLE_SLOT";

    private static final String NOTE_ID_SAVE_SLOT = "note_id_save_slot";

    public static final int TAG_SELECTED = 0;
    private static final int DB_LOADED = 1;
    private static final int ALL_TAGS_LOADED = 2;
    private static final int NOTE_LOADED = 3;
    private static final int TAGS_LOADED = 4;
    private static final int LAYER_LOADED = 5;

    public Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            int what = msg.what;
            Cursor cursor;
            switch(what){
                case ALL_TAGS_LOADED:
                    allTags = new ArrayList<>();
                    cursor = (Cursor) msg.obj;
                    cursor.moveToFirst();
                    while(!cursor.isAfterLast()){
                        allTags.add(cursor.getString(cursor.getColumnIndex(DatabaseHandler.TAGTB_TAG)));
                        cursor.moveToNext();
                    }
                    cursor.close();
                    break;
                case TAG_SELECTED:
                    callNote((String) msg.obj);
                    break;
                case DB_LOADED:
                    new Thread(new LoadTags(getApplicationContext(), this)).start();
                    new Thread(new LoadNote(getApplicationContext(), this)).start();
                    break;
                case NOTE_LOADED:
                    cursor = (Cursor) msg.obj;
                    cursor.moveToFirst();
                    if(!cursor.isAfterLast()){
                        notes = cursor.getString(cursor.getColumnIndex(DatabaseHandler.NOTETB_NOTE));
                        if(notes != null && !notes.isEmpty()){
                            notes_tv.setText(notes);
                        }
                        //(new Thread(new CreateStructure(handler, getApplicationContext(), notesList.getMeasuredWidth()))).start();
                        //notesListAdapter = new NoteDisplayAdapter(getApplicationContext(), words, notesList.getMeasuredWidth());
                        //notesList.setAdapter(notesListAdapter);
                        id = cursor.getInt(cursor.getColumnIndex(DatabaseHandler.ID));
                        title = cursor.getString(cursor.getColumnIndex(DatabaseHandler.NOTETB_TITLE));
                        title_tv.setText(title);
                    }else{
                        //Make Toast that there is no
                        onBackPressed();
                    }
                    new Thread(new LoadTags2(getApplicationContext(), this)).start();
                    break;
                case TAGS_LOADED:
                    tags = (List<String>) msg.obj;
                    if(tags != null)
                        tagsListAdapter = new TagsListAdapter(tags, handler, TAG_SELECTED);
                    else
                        tagsListAdapter = new TagsListAdapter(new ArrayList<String>(), handler, TAG_SELECTED);
                    tagsList.setAdapter(tagsListAdapter);
                    db.close();
                    break;
                /*case LAYER_LOADED:
                    ArrayList<TextView> layer = (ArrayList<TextView>)msg.obj;
                    //Log.i("LAYER_LOADING", "CHECKPOINT");
                    if(!layer.isEmpty()) {
                        noteListStructure.add(layer);
                    }

                    if(words.isEmpty()) {
                        Log.i("LAYER_LOADING", ("size = " + noteListStructure.size()));
                        notesListAdapter = new NoteDisplayAdapter(getApplicationContext(), noteListStructure);
                        notesList.setAdapter(notesListAdapter);
                    }else{
                        (new Thread(new CreateStructure(handler, getApplicationContext(), notesList.getMeasuredWidth()))).start();
                    }*/
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_display);

        if(savedInstanceState != null){
            onRestoreInstanceState(savedInstanceState);
        }else{
            Intent intent = getIntent();
            title = intent.getStringExtra(NOTE_DISPLAY_TITLE_SLOT);
        }

        title_tv = findViewById(R.id.NoteDisplayTitle);

        tagsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        tagsList = (RecyclerView) findViewById(R.id.TagView);
        tagsList.setLayoutManager(tagsLayoutManager);
        tagsList.setHasFixedSize(true);

        notes_tv = (TextView) findViewById(R.id.notes_tv);

        /*notesLayoutManager = new LinearLayoutManager(this);
        notesList = (RecyclerView) findViewById(R.id.NotesListView);
        notesList.setLayoutManager(notesLayoutManager);
        notesList.setHasFixedSize(true);*/

    }

    public void editNote(View view){

        Intent intent = new Intent(this, ModifyNotesActivity.class);
        intent.putExtra(ModifyNotesActivity.TITLE_SLOT, title);
        intent.putExtra(ModifyNotesActivity.FLAG_SLOT, 1);
        startActivity(intent);
    }

    private void callNote(String tag){
        returnSearch = tag;
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(returnSearch != null) {
            Intent intent = new Intent();
            intent.putExtra(NOTE_DISPLAY_TITLE_SLOT, returnSearch);
            setResult(Activity.RESULT_OK, intent);
        }else{
            setResult(Activity.RESULT_CANCELED);
        }
        finish();
    }

    @Override
    protected void onResume() {
        dbHandler = new DatabaseHandler(this);
        (new Thread(new NoteDisplayActivity.LoadDB(getApplicationContext(), handler))).start();
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        onSaveInstanceState(new Bundle());
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(NOTE_ID_SAVE_SLOT, id);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        id = savedInstanceState.getInt(NOTE_ID_SAVE_SLOT);
    }

    private class LoadDB implements Runnable{

        Context context;
        Handler handler;

        public LoadDB(Context context, Handler handler){
            this.context = context;
            this.handler = handler;
        }

        @Override
        public void run() {
            if(dbHandler != null){
                if(db == null || !db.isOpen()){
                    db = dbHandler.getWritableDatabase();
                }
                handler.sendEmptyMessage(DB_LOADED);
            }
        }
    }

    private class LoadTags implements Runnable{

        Handler handler;
        Context context;

        public LoadTags(Context context, Handler handler){
            this.handler = handler;
            this.context = context;
        }

        @Override
        public void run() {
            if(dbHandler != null) {
                handler.sendMessage(
                        handler.obtainMessage(
                                ALL_TAGS_LOADED,
                                dbHandler.getAllTags(db)
                        )
                );
            }
        }
    }

    private class LoadTags2 implements Runnable{

        Handler handler;
        Context context;

        public LoadTags2(Context context, Handler handler){
            this.handler = handler;
            this.context = context;
        }

        @Override
        public void run() {
            if(dbHandler != null) {
                handler.sendMessage(
                        handler.obtainMessage(
                                TAGS_LOADED,
                                dbHandler.getAllTagsByNote(db,title)
                        )
                );
            }
        }
    }

    private class LoadNote implements Runnable{

        Handler handler;
        Context context;

        public LoadNote(Context context, Handler handler){
            this.handler = handler;
            this.context = context;
        }

        @Override
        public void run() {
            if(dbHandler != null) {
                handler.sendMessage(
                        handler.obtainMessage(
                                NOTE_LOADED,
                                dbHandler.getNote(db, id, title)
                        )
                );
            }
        }
    }

    /*private class CreateStructure implements Runnable{
        Handler handler;
        Context context;
        int lim;

        public CreateStructure(Handler handler, Context context, int lim){
            this.handler = handler;
            this.context = context;
            this.lim = lim;
        }

        @Override
        public void run() {
            ArrayList<TextView> layer = new ArrayList<>();
            //Log.i("LAYER_LOADING", ("lim = " + lim));

            int len = 0, i = 0;
            String s = words.get(0);

            TextView t = new TextView(context);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
            t.setText(s);
            t.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            t.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            //Log.i("LAYER_LOADING", ("t = " + t.getMeasuredWidth()));
            int n = t.getMeasuredWidth();

            while(n >= lim){
                layer.add(t);
                words.remove(0);
                handler.sendMessage(
                        handler.obtainMessage(
                                LAYER_LOADED,
                                layer
                        )
                );
                return;
            }

            int indOf = s.indexOf('\n');

            while(len + n < lim && indOf == -1){
                if(len != 0){
                    int x = layer.size() - 1;
                    TextView prev = layer.get(x);
                    prev.setText((prev.getText().toString() + " "));
                    t.setText(s);
                }
                len += n;
                layer.add(t);
                words.remove(0);
                if(words.isEmpty()) break;
                s = words.get(0);

                t = new TextView(context);
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
                t.setText((s + " "));
                t.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                t.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                //Log.i("LAYER_LOADING", ("t = " + t.getMeasuredWidth()));
                n = t.getMeasuredWidth();
                indOf = s.indexOf('\n');
            }

            if(len + n < lim && !words.isEmpty() && indOf != -1){
                if(indOf == 0){
                    words.remove(0);
                    if(s.length() > 1)
                        words.add(0, s.substring(1));
                }else{
                    words.remove(0);
                    String left = s.substring(0, indOf);
                    if(indOf != s.length() - 1) {
                        String right = s.substring(indOf + 1);
                        words.add(0, right);
                    }
                    if(len != 0){
                        int x = layer.size() - 1;
                        TextView prev = layer.get(x);
                        prev.setText((prev.getText().toString() + " "));
                        t.setText(left);
                    }
                    layer.add(t);
                }
            }

            handler.sendMessage(
                    handler.obtainMessage(
                            LAYER_LOADED,
                            layer
                    )
            );
        }
    }*/
}
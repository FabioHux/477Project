package com.gmu.notesapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ModifyNotesActivity extends AppCompatActivity {

    EditText title;
    EditText tags;
    EditText notes;
    private SQLiteDatabase db;
    private DatabaseHandler dbHandler;
    public int id;
    public int flag;
    public String oldTags;

    private Cursor spinnerCursor;
    private SimpleCursorAdapter spinnerAdapter = null;
    private Spinner fullTags;


    private final static int SPINNER_CURSOR_LOADED = 0;
    private final static int NOTES_LOADED = 1;
    private final static int DB_LOADED = 2;
    public final static int TAGS_LOADED = 3;
    public final static int DB_UPDATED = 4;
    private String auto_select_buffer = null;

    public static final String TITLE_SLOT = "com.gmu.notesapp.title",
            FLAG_SLOT = "com.gmu.notesapp.flag",
            TITLE_SAVE="title_save",
            TAG_SAVE="tag_save",
            NOTES_SAVE="notes_save";
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            int what = msg.what;
            Cursor cursor = null;
            if(what == SPINNER_CURSOR_LOADED || what ==NOTES_LOADED)
                cursor = (Cursor) msg.obj;
            switch(what){
                case SPINNER_CURSOR_LOADED:
                    if(spinnerAdapter == null){
                        spinnerAdapter = new SimpleCursorAdapter(
                                getApplicationContext(),
                                R.layout.activity_main_spinner,
                                cursor,
                                new String[]{DatabaseHandler.TAGTB_TAG},
                                new int[]{R.id.spinner_view},
                                0
                        );
                    }else{
                        spinnerAdapter.swapCursor(cursor);
                    }
                    spinnerCursor = cursor;
                    if(cursor.getCount() > 0){
                        auto_select_buffer = null;
                    }
                    fullTags.setAdapter(spinnerAdapter); //Set up the cursor for the spinner
                    break;
                case NOTES_LOADED:  //Set up the values for the note that was loaded
                    cursor.moveToFirst();
                    id = cursor.getInt(cursor.getColumnIndex(DatabaseHandler.ID));
                    notes.setText(cursor.getString(cursor.getColumnIndex(DatabaseHandler.NOTETB_NOTE)));
                    (new Thread(new LoadTags(getApplicationContext(), handler,title.getText().toString()))).start();
                    break;
                case TAGS_LOADED:   //Set up the tags for the note that was loaded
                    if(msg.obj != null && !((String) msg.obj).isEmpty())
                        tags.setText((String) msg.obj);
                    oldTags = tags.getText().toString();
                    break;
                case DB_UPDATED:    //Returned by both UpdateNote and CreateNote
                    if(((Boolean)msg.obj).booleanValue()){
                        //Call onBackPressed you're done fam...
                        onBackPressed();
                    }else{
                        Toast.makeText(getApplicationContext(),"Title already exists...",Toast.LENGTH_SHORT);
                        //Post a Toast or something cause something dun fucked up :D
                    }
                    break;
                case DB_LOADED:     //Only calls LoadNote if there is a load to note (ie. flag == 1)
                    (new Thread(new LoadAllTags(getApplicationContext(), handler))).start();
                    if(flag == 1)
                        (new Thread(new LoadNote(getApplicationContext(), handler,title.getText().toString()))).start();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_notes);

        Intent intent=getIntent();
        flag=intent.getIntExtra(FLAG_SLOT,0);

        title=(EditText) findViewById(R.id.title);
        tags=(EditText) findViewById(R.id.tags);
        notes=(EditText) findViewById(R.id.notes);
        fullTags=(Spinner)findViewById(R.id.TagSpinner);

        fullTags.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(auto_select_buffer != null)
                    tags.setText((tags.getText().toString() + " " + ((TextView) view).getText().toString()));
                else
                    auto_select_buffer = ((TextView) view).getText().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if(flag==1){
            title.setText(intent.getStringExtra(TITLE_SLOT));
        }
        /*
        ((LinearLayout)findViewById(R.id.notesLayout)).setOnFocusChangeListener(
                new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                            findViewById(R.id.notes).setFocusable(true);
                        return;
                    }
                }
        );
*/
    }

    /*
       Will save everything into the SQLite
     */
    public void done(View view){
        if(title.getText().toString().isEmpty()){
            Toast.makeText(getApplicationContext(), "Title is empty", Toast.LENGTH_SHORT).show();
            //Yell at them for having an empty title with a Toast or something
            return;
        }
        System.out.println(title.getText().toString() + flag);
        if(flag == 1){
            (new Thread(new UpdateNote(getApplicationContext(), handler, title.getText().toString(), notes.getText().toString(), tags.getText().toString(), oldTags))).start();
        }else{
            (new Thread(new CreateNote(getApplicationContext(), handler, title.getText().toString(), notes.getText().toString(), tags.getText().toString()))).start();
        }
    }

    //send an intent
    public void takePicture(View view){
        Intent intent = new Intent(this, ImageTextActivity.class);
        startActivityForResult(intent,0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode!=0) {
            super.onActivityResult(requestCode, resultCode, data);
            notes.getText().append(data.getStringExtra("com.gmu.notesapp.imageString"));
        }
    }

    @Override
    protected void onResume() {

        dbHandler = new DatabaseHandler(this);
        (new Thread(new ModifyNotesActivity.LoadDB(getApplicationContext(), handler))).start();
        super.onResume();
    }

    @Override
    public void onStop() {
        cleanUp();
        super.onStop();
    }

    @Override
    public void onPause() {
        onSaveInstanceState(new Bundle());
        cleanUp();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        /*
        outState.putString(MAIN_SEARCH_SAVE, gquery);
        outState.putStringArrayList(MAIN_TAG_SAVE,(ArrayList<String>) tagsAdapter.tags);
*/

        outState.putString(TITLE_SAVE,title.getText().toString());
        outState.putString(TAG_SAVE,tags.getText().toString());
        outState.putString(NOTES_SAVE,notes.getText().toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {

        super.onRestoreInstanceState(savedInstanceState);
        title=(EditText) findViewById(R.id.title);
        tags=(EditText) findViewById(R.id.tags);
        notes=(EditText) findViewById(R.id.notes);
        title.setText(savedInstanceState.getString(TITLE_SAVE,title.getText().toString()));
        tags.setText(savedInstanceState.getString(TAG_SAVE,tags.getText().toString()));
        notes.setText(savedInstanceState.getString(NOTES_SAVE,notes.getText().toString()));
    }

    private void cleanUp(){

        if(spinnerAdapter != null){
            fullTags.setAdapter(null);
            spinnerAdapter = null;
            spinnerCursor.close();
        }

        if(db != null && db.isOpen()){
            db.close();
        }

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

    private class LoadNote implements Runnable{

        Handler handler;
        Context context;
        String title;

        public LoadNote(Context context, Handler handler, String title){
            this.handler = handler;
            this.context = context;
            this.title = title;
        }

        @Override
        public void run() {
            if(dbHandler != null) {
                handler.sendMessage(
                        handler.obtainMessage(
                                ModifyNotesActivity.NOTES_LOADED,
                                dbHandler.getNote(db, 0, this.title)
                        )
                );
            }
        }
    }

    private class LoadTags implements Runnable{

        Handler handler;
        Context context;
        String title;

        public LoadTags(Context context, Handler handler, String title){
            this.handler = handler;
            this.context = context;
            this.title = title;
        }

        @Override
        public void run() {
            if(dbHandler != null) {
                handler.sendMessage(
                        handler.obtainMessage(
                                ModifyNotesActivity.TAGS_LOADED,
                                dbHandler.getStringOfTagsByNote(db, this.title)
                        )
                );
            }
        }
    }

    private class LoadAllTags implements Runnable{

        Handler handler;
        Context context;

        public LoadAllTags(Context context, Handler handler){
            this.handler = handler;
            this.context = context;
        }

        @Override
        public void run() {
            if(dbHandler != null) {
                handler.sendMessage(
                        handler.obtainMessage(
                                ModifyNotesActivity.SPINNER_CURSOR_LOADED,
                                dbHandler.getAllTags(db)
                        )
                );
            }
        }
    }

    private class UpdateNote implements Runnable{

        Handler handler;
        Context context;
        String title, note, newTags, oldTags;

        public UpdateNote(Context context, Handler handler, String title, String note, String newTags, String oldTags){
            this.handler = handler;
            this.context = context;
            this.title = title;
            this.note = note;
            this.newTags = newTags;
            this.oldTags = oldTags;
        }

        @Override
        public void run() {
            if(dbHandler != null) {
                handler.sendMessage(
                        handler.obtainMessage(
                                ModifyNotesActivity.DB_UPDATED,
                                new Boolean(dbHandler.updateNote(db, id, this.title, this.note, this.newTags, this.oldTags))
                        )
                );
            }
        }
    }

    private class CreateNote implements Runnable{

        Handler handler;
        Context context;
        String title, note, tags;

        public CreateNote(Context context, Handler handler, String title, String note, String tags){
            this.handler = handler;
            this.context = context;
            this.title = title;
            this.note = note;
            this.tags = tags;
        }

        @Override
        public void run() {
            System.out.println("hello");
            if(dbHandler != null) {
                handler.sendMessage(
                        handler.obtainMessage(
                                ModifyNotesActivity.DB_UPDATED,
                                new Boolean(dbHandler.createNote(db, this.title, this.note, this.tags))
                        )
                );
            }
        }
    }

}
package com.gmu.notesapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private DatabaseHandler dbHandler = null;
    private SQLiteDatabase db = null;
    private Cursor spinnerCursor, notesCursor;
    private SimpleCursorAdapter spinnerAdapter = null;
    private NotesListAdapter notesAdapter = null;
    private TagsListAdapter tagsAdapter = null;
    private Spinner fullTags;
    private RecyclerView selectedTags, notesList;
    private SearchView searchbar;
    private RecyclerView.LayoutManager notesLayoutManager, tagsLayoutManager;
    private String gquery = "";
    static final String MAIN_TAG_SAVE = "main_tag_save", MAIN_SEARCH_SAVE = "main_search_save";
    private String auto_select_buffer = null;

    private final static int SPINNER_CURSOR_LOADED = 0;
    private final static int NOTES_CURSOR_LOADED = 1;
    private final static int DB_LOADED = 2;
    public final static int NOTE_SELECTED = 3;
    public final static int TAG_LIST_MODIFIED = 4;

    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            int what = msg.what;
            Cursor cursor = null;
            if(what != NOTE_SELECTED && what != DB_LOADED)
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
                    //Log.i("CURSOR_CHECKER", Integer.toString(cursor.getCount()));
                    tagsAdapter.cleanTags(cursor);
                    fullTags.setAdapter(spinnerAdapter);
                    break;
                case NOTES_CURSOR_LOADED:
                    if(notesAdapter == null){
                        notesAdapter = new NotesListAdapter(cursor, new DeleteNote(this, getApplicationContext()), this); //Might need to change NotesListAdapter to add listeners
                    }else{
                        notesAdapter.swapCursor(cursor);
                    }
                    notesCursor = cursor;
                    notesList.setAdapter(notesAdapter);
                    break;
                case DB_LOADED:
                    (new Thread(new LoadTags(getApplicationContext(), handler))).start();
                case TAG_LIST_MODIFIED:
                    (new Thread(new LoadNotes(getApplicationContext(), handler, gquery, tagsAdapter.tags))).start();
                    break;
                case NOTE_SELECTED:
                    String title = (String) msg.obj;
                    System.out.println(title);
                    //Start next activity. For Fabio to make!

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //Intent intent = getIntent(); //To apply later
        tagsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        selectedTags = (RecyclerView) findViewById(R.id.TagView);
        selectedTags.setLayoutManager(tagsLayoutManager);
        selectedTags.setHasFixedSize(true);
        selectedTags.setItemViewCacheSize(0);

        tagsAdapter = new TagsListAdapter(new ArrayList<String>(), handler, TAG_LIST_MODIFIED);

        selectedTags.setAdapter(tagsAdapter);

        fullTags = (Spinner) findViewById(R.id.TagSpinner);
        fullTags.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(auto_select_buffer != null) {
                    tagsAdapter.addItem(((TextView) view).getText().toString());

                    (new Thread(new LoadNotes(getApplicationContext(), handler, gquery, tagsAdapter.tags))).start();

                }else{
                    auto_select_buffer = ((TextView) view).getText().toString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView){
                if(auto_select_buffer != null && !auto_select_buffer.isEmpty()){
                    tagsAdapter.addItem(auto_select_buffer);

                    (new Thread(new LoadNotes(getApplicationContext(), handler, gquery, tagsAdapter.tags))).start();

                    auto_select_buffer = "";
                }
            }
        });


        notesLayoutManager = new LinearLayoutManager(this);
        notesList = (RecyclerView) findViewById(R.id.NotesListView);
        notesList.setLayoutManager(notesLayoutManager);
        notesList.setHasFixedSize(true);

        searchbar = (SearchView) findViewById(R.id.search_view);
        searchbar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                gquery = query;

                (new Thread(new LoadNotes(MainActivity.this.getApplicationContext(), handler, query, tagsAdapter.tags))).start();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                gquery = newText;

                (new Thread(new LoadNotes(MainActivity.this.getApplicationContext(), handler, newText, tagsAdapter.tags))).start();

                return false;
            }
        });

        if(savedInstanceState != null){
            onRestoreInstanceState(savedInstanceState);
        }
    }

    public void makeNewNote(View view){
        startActivityForResult(new Intent(this, ModifyNotesActivity.class),0);
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

    private class LoadNotes implements Runnable{

        Handler handler;
        String[] query;
        List<String> tags;
        Context context;

        public LoadNotes(Context context, Handler handler, String query, List<String> tags){
            this.handler = handler;
            this.query = query.isEmpty() ? new String[]{} : query.split(" ");
            this.tags = tags;
            this.context = context;
        }

        @Override
        public void run() {
            if(dbHandler != null) {
                handler.sendMessage(
                        handler.obtainMessage(
                                MainActivity.NOTES_CURSOR_LOADED,
                                dbHandler.getNotes(db, query, tags)
                        )
                );
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
                                MainActivity.SPINNER_CURSOR_LOADED,
                                dbHandler.getAllTags(db)
                        )
                );
            }
        }
    }

    public class DeleteNote implements Runnable{
        String title;
        Handler handler;
        Context context;

        public DeleteNote(Handler handler, Context context){
            this.handler = handler;
            this.context = context;
        }

        public void setTitle(String title){
            this.title = title;
        }

        @Override
        public void run() {
            if(dbHandler != null){
                dbHandler.deleteNote(db,title);
                handler.sendEmptyMessage(DB_LOADED); //Since I deleted, I need to refresh the tags and stuff
            }
        }
    }

    @Override
    protected void onResume() {
        dbHandler = new DatabaseHandler(this);
        (new Thread(new LoadDB(getApplicationContext(), handler))).start();
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
        outState.putString(MAIN_SEARCH_SAVE, gquery);
        outState.putStringArrayList(MAIN_TAG_SAVE,(ArrayList<String>) tagsAdapter.tags);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        gquery = savedInstanceState.getString(MAIN_SEARCH_SAVE);
        tagsAdapter = new TagsListAdapter(savedInstanceState.getStringArrayList(MAIN_TAG_SAVE), handler, TAG_LIST_MODIFIED);
    }

    private void cleanUp(){

        if(notesAdapter != null){
            notesList.setAdapter(null);
            notesCursor.close();
            notesAdapter = null;
        }

        if(spinnerAdapter != null){
            fullTags.setAdapter(null);
            spinnerCursor.close();
            spinnerAdapter = null;
        }

        if(db != null && db.isOpen()){
            db.close();
        }
    }
}
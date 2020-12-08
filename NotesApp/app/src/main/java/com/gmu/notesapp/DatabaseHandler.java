package com.gmu.notesapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Arrays;

public class DatabaseHandler extends SQLiteOpenHelper {
    private final Context context;
    private static Integer VERSION = 1;
    final static String DBNAME = "NoteAppDatabase";
    public final static String NOTETBNAME = "NoteTable";
    public final static String TAGTBNAME = "TagTable";
    public final static String RELTBNAME = "NoteTagTable";

    private final static String CREATE_NOTES_TABLE = "CREATE TABLE " + NOTETBNAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL UNIQUE, " +
            "note TEXT NOT NULL)";

    private final static String CREATE_TAG_TABLE = "CREATE TABLE " + TAGTBNAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "tag TEXT NOT NULL UNIQUE)";

    private final static String CREATE_REL_TABLE = "CREATE TABLE " + RELTBNAME + " (note_id INTEGER, " +
            "tag_id INTEGER)";

    public DatabaseHandler(Context context){
        super(context, DBNAME, null, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_NOTES_TABLE);
        db.execSQL(CREATE_REL_TABLE);
        db.execSQL(CREATE_TAG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void deleteTag(SQLiteDatabase db, String tagName){

    }

    public void deleteNote(SQLiteDatabase db, String noteName){

    }

    public void insertRelation(SQLiteDatabase db, int note_id, int tag_id){

    }

    public void deleteRelation(SQLiteDatabase db, int note_id, int tag_id){

    }

    /**
     * NOTE: SHOULD BE RUN ON SEPARATE THREAD;
     */
    public void updateNote(SQLiteDatabase db, String name, String note, String NewTags, String OldTags){
        ArrayList<String> newTagsList = new ArrayList<String>(Arrays.asList(NewTags));
        ArrayList<String> oldTagsList = new ArrayList<String>(Arrays.asList(OldTags));

        //Removing all the same tags from newTagsList and oldTagsList
        //What remains in newTagsList are the new Tags we need to add a relationship to with the note
        //What remains in oldTagsList are the old Tags that have been removed and we need to delete that relationship
        int i = 0;
        while(i < oldTagsList.size()){
            if(newTagsList.indexOf(oldTagsList.get(i)) != -1){
                oldTagsList.remove(i);
            }else{
                i++;
            }
        }

        //

    }

    /**
     * For Vincent
     */

    public String grabAllTags(SQLiteDatabase db, String noteName){
        Cursor noteCursor = db.query(NOTETBNAME, new String[]{"_id"}, "name = ?", new String[]{noteName},null, null, null);
        if(noteCursor.getCount() != 1) return "";

        noteCursor.moveToFirst();
        Cursor relCursor = db.query(RELTBNAME, new String[]{"tag_id"}, "note_id = ?", new String[]{noteCursor.getString(noteCursor.getColumnIndex("_id"))}, null, null, null);

        if(relCursor == null || relCursor.getCount() == 0) return "";

        Cursor tagCursor;
        StringBuilder ret = new StringBuilder();
        final int ind = relCursor.getColumnIndex("tag_id");

        relCursor.moveToFirst();
        tagCursor = db.query(TAGTBNAME, new String[]{"tag"}, "_id = ?", new String[]{relCursor.getString(ind)}, null, null, null);
        if(tagCursor != null && tagCursor.getCount() != 0)
            ret.append(relCursor.getString(tagCursor.getColumnIndex("tag")));
        relCursor.moveToNext();

        while(!relCursor.isAfterLast()){
            tagCursor = db.query(TAGTBNAME, new String[]{"tag"}, "_id = ?", new String[]{relCursor.getString(ind)}, null, null, null);
            if(tagCursor != null && tagCursor.getCount() != 0) {
                ret.append(" ");
                ret.append(relCursor.getString(tagCursor.getColumnIndex("tag")));
            }else{
                System.err.println("Formatting of table is incorrect for some reason.");
            }
            relCursor.moveToNext();
        }

        return ret.toString();
    }
}

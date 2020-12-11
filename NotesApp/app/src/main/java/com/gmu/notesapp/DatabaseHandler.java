package com.gmu.notesapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {
    private final Context context;
    private static Integer VERSION = 1;
    final static String DBNAME = "NoteAppDatabase";
    public final static String ID = "_id";

    public final static String NOTETBNAME = "NoteTable";
    public final static String NOTETB_TITLE = "name";
    public final static String NOTETB_NOTE = "note";
    private final static String CREATE_NOTES_TABLE = "CREATE TABLE " + NOTETBNAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            NOTETB_TITLE + " TEXT NOT NULL UNIQUE, " +
            NOTETB_NOTE + " TEXT NOT NULL)";


    public final static String TAGTBNAME = "TagTable";
    public final static String TAGTB_TAG = "tag";
    private final static String CREATE_TAG_TABLE = "CREATE TABLE " + TAGTBNAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            TAGTB_TAG + " TEXT NOT NULL UNIQUE)";


    public final static String RELTBNAME = "NoteTagTable";
    public final static String RELTB_TAG = "tag_id";
    public final static String RELTB_NOTE = "note_id";
    private final static int REL_CHECK_BOTH = 0;
    private final static int REL_CHECK_NOTE = 1;
    private final static int REL_CHECK_TAG = 2;
    private final static String CREATE_REL_TABLE = "CREATE TABLE " + RELTBNAME + " (" + RELTB_NOTE + " INTEGER, " +
            RELTB_TAG + " INTEGER)";




    public DatabaseHandler(Context context){
        super(context, DBNAME, null, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_NOTES_TABLE);
        db.execSQL(CREATE_REL_TABLE);
        db.execSQL(CREATE_TAG_TABLE);

        //Create some stuff to test
        createNote(db, "Apple", "Fruit that's good and sturdy. Similar to a pear but tougher.", "Fruit Apple Healthy");
        createNote(db, "Pear", "Fruit that's sweet and soft. Similar to an apple but softer.", "Fruit Pear Healthy");
        createNote(db, "Orange", "", "");
        createNote(db, "Watermelon", "Watery stuff", "Fruit Watermelon");
        createNote(db, "Mango", "Tastes weird but it's good!", "Fruit Mango");
        createNote(db, "Papaya", "Literally think it's a modded Mango", "Fruit Papaya");
        createNote(db, "Starfruit", "Fruit that looks like a crossection of a start got some height", "Fruit Star");
        createNote(db, "Apple", "Fruit that's good and sturdy. Similar to a pear but tougher.", "Fruit Apple Healthy");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * Function to insert relation between a note and a tag.
     * @param db    Database to add this to.
     * @param note_id   Note id to insert
     * @param tag_id    Tag id to insert
     * @return Boolean stating success (true) or failure (false)
     */
    private boolean insertRelation(SQLiteDatabase db, int note_id, int tag_id){
        if(invalidDB(db) || existsRelation(db, note_id, tag_id, REL_CHECK_BOTH)) return false;

        ContentValues values = new ContentValues();
        values.put(RELTB_NOTE, note_id);
        values.put(RELTB_TAG, tag_id);

        return db.insert(RELTBNAME, null, values) != -1;

    }

    /**
     * Function to remove a relation between a note and a tag.
     *
     * Function will do nothing if there doesn't exist a relation between the two. If there does exist one,
     * then it will also attempt to delete the Tag (see deleteTag).
     * @param db    Database to remove this from
     * @param note_id   Note id that's being selected
     * @param tag_id    Tag id that's being selected and can get removed.
     */
    private void deleteRelation(SQLiteDatabase db, int note_id, int tag_id){
        if(invalidDB(db) || !existsRelation(db,note_id, tag_id, REL_CHECK_BOTH)) return;

        db.delete(RELTBNAME, RELTB_NOTE + " = ? AND " + RELTB_TAG + " = ?", new String[]{Integer.toString(note_id), Integer.toString(tag_id)});
        deleteTag(db, tag_id);
    }

    /**
     * Function to get a Cursor representing the relation between a note and an id.
     * @param db    Database to search through
     * @param note_id   Note id that's being searched (can be not used if correct flag is selected).
     * @param tag_id    Tag id that's being searched (can be not used if correct flag is selected).
     * @param flag  Flag telling how to search through the list of relations (Both, just note_id, just tag_id)
     * @return  A Cursor with rows containing pairs of ids {note_id, tag_id} where the conditions were met or null.
     */
    private Cursor getRelation(SQLiteDatabase db, int note_id, int tag_id, int flag){
        if(invalidDB(db)) return null;
        switch(flag){
            case REL_CHECK_BOTH: //Check both the note_id and tag_id
                return db.query(RELTBNAME, new String[]{RELTB_NOTE, RELTB_TAG}, RELTB_NOTE + " = ? AND " + RELTB_TAG + " = ?", new String[]{Integer.toString(note_id), Integer.toString(tag_id)}, null, null, null);
            case REL_CHECK_NOTE: //Check note
                return db.query(RELTBNAME, new String[]{RELTB_NOTE, RELTB_TAG}, RELTB_NOTE + " = ?", new String[]{Integer.toString(note_id)}, null, null, null);
            case REL_CHECK_TAG: //Check tag_id
                return db.query(RELTBNAME, new String[]{RELTB_NOTE, RELTB_TAG}, RELTB_TAG + " = ?", new String[]{Integer.toString(tag_id)}, null, null, null);
            default:
                return null;
        }
    }

    /**
     * Function to check to see if a relation already exists.
     * @param db    Database to search through
     * @param note_id   Note id that's being searched
     * @param tag_id    Tag id that's being searched
     * @param flag  Flag telling how to search through the list of relations (Both, just note_id, just tag_id)
     * @return Boolean representing whether it exists (true) or doesn't (false)
     */
    private boolean existsRelation(SQLiteDatabase db, int note_id, int tag_id, int flag){
        Cursor cursor = getRelation(db, note_id, tag_id, flag);
        boolean ret = false;
        if(cursor != null){
            ret = cursor.getCount() > 0;
            cursor.close();
        }
        return ret;
    }

    /**
     * Function to update note (MUST BE DONE ON A SEPARATE THREAD).
     *
     * Function must not have the id of the note be changed (You can request prior to changing with getNote().
     * Function must have a title (name)
     * Function must have a non-null note (meaning just put in "")
     * Function must have a NewTags string (meaning if it's empty, just put "");
     * Function must have an OldTags string (meaning if it's empty, just put "");
     * @param db    Database to do this operation
     * @param note_id   Note id of the note that's being updated
     * @param name  New note title (if there is not a change you MUST put the old title)
     * @param note  New note scripture (if there is not a change you MUST put the old notes)
     * @param NewTags   String containing every Tag the note will now have (Must be separated by a space)
     * @param OldTags   String containing every Tag the note had (Must be separated by a space) which any differences with NewTags will see a change in the DB.
     * @return  Boolean representing whether it was successful in updating, or if there was a formatting error (note_id does not match an existent one, or inputs were wrong).
     */
    public boolean updateNote(SQLiteDatabase db, int note_id, String name, String note, String NewTags, String OldTags){
        if(invalidDB(db) || name == null || name.isEmpty() || note == null || NewTags == null || OldTags == null) return false;

        Cursor noteCursor = getNote(db, note_id, null);

        if(noteCursor != null && noteCursor.getCount() == 0){
            noteCursor.close();
            noteCursor = null;
        }; //If note does not exist, return false. We can't update.

        if(noteCursor == null) return false;

        ArrayList<String> newTagsList = new ArrayList<String>(Arrays.asList(NewTags.split(" ")));
        ArrayList<String> oldTagsList = new ArrayList<String>(Arrays.asList(OldTags.split(" ")));

        //Removing all the same tags from newTagsList and oldTagsList
        //What remains in newTagsList are the new Tags we need to add a relationship to with the note
        //What remains in oldTagsList are the old Tags that have been removed and we need to delete that relationship
        int i = 0;
        while(i < oldTagsList.size() && newTagsList.size() > 0){
            if(newTagsList.contains(oldTagsList.get(i))){
                newTagsList.remove(oldTagsList.get(i));
                oldTagsList.remove(i);
            }else{
                i++;
            }
        }

        //Create newTag Relations

        for(String tag_name : newTagsList){
            if(tag_name.isEmpty()) continue;
            int tag_id = createTag(db, tag_name);
            insertRelation(db, note_id, tag_id);
        }

        //Delete OldTags Relations

        for(String tag_name : oldTagsList){
            Cursor tagCursor = db.query(TAGTBNAME, new String[]{ID}, TAGTB_TAG + " = ?", new String[]{tag_name}, null, null, null);
            if(tagCursor != null && tagCursor.getCount() != 0){
                tagCursor.moveToFirst();
                deleteRelation(db, note_id, tagCursor.getInt(tagCursor.getColumnIndex(ID)));
            }

            if(tagCursor != null){
                tagCursor.close();
            }
        }

        ContentValues values = new ContentValues();
        values.put(NOTETB_TITLE, name);
        values.put(NOTETB_NOTE, note);
        return db.update(NOTETBNAME, values, ID + " = ?", new String[]{Integer.toString(note_id)}) != 0;

    }

    /**
     * Function to delete a Note from the database. It will first delete every relation it has, then it will delete the note itself.
     * @param db    Database to delete from
     * @param noteTitle Note title that's used to search for note to delete
     */
    public void deleteNote(SQLiteDatabase db, String noteTitle){
        if(invalidDB(db) || noteTitle == null || noteTitle.isEmpty()) return;

        Cursor note = getNote(db, 0, noteTitle);

        if(note != null && note.getCount() != 1){
            note.close();
            note = null;
        }

        if(note == null) return;

        note.moveToFirst();
        int note_id = note.getInt(note.getColumnIndex(ID));

        Cursor rel = getRelation(db, note_id, 0, REL_CHECK_NOTE);

        if(rel == null) return;

        rel.moveToFirst();

        while(!rel.isAfterLast()){
            deleteRelation(db, note_id, rel.getInt(rel.getColumnIndex(RELTB_TAG)));
            rel.moveToNext();
        }

        rel.close();

        db.delete(NOTETBNAME, ID + " = ?", new String[]{Integer.toString(note_id)});
    }

    /**
     * Function to create a new note given a title, notes, and tags.
     *
     * Function must have a title (unique)
     * Function must have a non null notes (just put "" if nothing to insert)
     * Function must have a tags string (just put "" if nothing to insert)
     * Assume if false is returned, then there already exists such a note.
     * All tags that haven't been created will automatically be created for you.
     *
     * @param db Database to insert it to
     * @param noteTitle Title of the note. Must be unique and non-null. Will return false otherwise
     * @param notes     Notes of the note entry. Must not be null or it will return false otherwise.
     * @param tags      Tags of the note. Must be a space separated string of every Tag this note is related to.
     * @return  Boolean representing the success of creating the new note and its relations (true) or its failure (false)
     */
    public boolean createNote(SQLiteDatabase db, String noteTitle, String notes, String tags){
        if(invalidDB(db) || noteTitle == null || noteTitle.isEmpty() || notes == null) return false;

        Cursor note_cursor = getNote(db, 0, noteTitle);
        if(note_cursor != null && note_cursor.getCount() > 0){
            note_cursor.close();
            return false; //Note of this title already exists
        }

        ContentValues values = new ContentValues();
        values.put(NOTETB_TITLE, noteTitle);
        values.put(NOTETB_NOTE, notes);
        boolean success = db.insert(NOTETBNAME, null, values) != -1;

        if(!success) return false; //Can't create note for some reason??

        note_cursor = getNote(db, 0, noteTitle);
        if(note_cursor == null) return false; //Supposed to succeed but getting the note failed?

        note_cursor.moveToFirst();
        int note_id = note_cursor.getInt(note_cursor.getColumnIndex(ID)); //Grabbing the id to set up the relations

        ArrayList<String> tagsList = new ArrayList<String>(Arrays.asList(tags.split(" ")));
        for(String tag_name : tagsList){
            if(tag_name.isEmpty()) continue;
            int tag_id = createTag(db, tag_name);
            insertRelation(db, note_id, tag_id);
        }

        note_cursor.close();

        return true;
    }

    /**
     * Searching function for the list of notes. Filter is a list of strings (tokens) that are scanned through the list
     * @param db    Database to query from.
     * @param filter    List of strings that are tokens used to search through all the notes' titles and notes
     * @param tags      List of tags to filter by.
     * @return  Cursor that holds all the notes that follow the specified filter.
     */
    public Cursor getNotes(SQLiteDatabase db, String[] filter, List<String> tags){
        if(invalidDB(db) || filter == null || tags == null) return null;

        return db.rawQuery(noteSearchConditionBuilder(filter, tags),null);
    }

    /**
     * Searching function to grab a single note based on either the note_id (if noteTitle is null) or noteTitle (if it exists)
     * @param db        Database to query from
     * @param note_id   Id of the note used to search with. Only used if noteTitle is null or empty.
     * @param noteTitle Title of note used to search with. Only used if it's not empty or null
     * @return  Cursor that holds the specific note being searched for (may be empty or null if it doesn't exist).
     */
    public Cursor getNote(SQLiteDatabase db, int note_id, String noteTitle){
        if(invalidDB(db)) return null;

        if(noteTitle != null && !noteTitle.isEmpty()){
            //Search by title
            return db.query(NOTETBNAME, new String[]{ID, NOTETB_TITLE, NOTETB_NOTE}, NOTETB_TITLE + " = ?", new String[]{noteTitle}, null, null, null);
        }else{
            return db.query(NOTETBNAME, new String[]{ID, NOTETB_TITLE, NOTETB_NOTE}, ID + " = ?", new String[]{Integer.toString(note_id)}, null, null, null);
        }
    }

    private String noteSearchConditionBuilder(String[] filter, List<String> tags){
        StringBuilder ret = new StringBuilder();

        String[] columns = new String[]{NOTETB_NOTE, NOTETB_TITLE};

        ret.append("SELECT ");
        ret.append(ID);
        ret.append(", ");
        ret.append(NOTETB_TITLE);
        ret.append(", ");
        ret.append(NOTETB_NOTE);
        ret.append(" FROM ");
        ret.append(NOTETBNAME);

        boolean boolHasSelections = false;
        for(int i = 0; i < filter.length; i++){
            if(!filter[i].isEmpty()){
                boolHasSelections = true;
                break;
            }
        }

        if(!boolHasSelections && tags.size() == 0) return ret.toString(); //No search or tag filter, just return everything
        ret.append(" n WHERE ");

        if(boolHasSelections){
            ret.append(queryBuildSearchFilter(filter));

            if(tags.size() > 0){
                ret.append(" AND ");
            }
        }

        if(tags.size() > 0){
            ret.append(queryBuildTagFilter(tags));
        }

        //Log.i("SEARCH_QUERY", ret.toString());
        System.out.println(ret.toString());

        return ret.toString();
    }

    private String queryBuildSearchFilter(String[] filter){
        StringBuilder ret = new StringBuilder();
        String[] columns = new String[]{NOTETB_NOTE, NOTETB_TITLE};
        ret.append("((");

        for(String type : columns){
            int i = 0;
            while(filter[i].isEmpty()){ //Skip all empty ones. Not possible to reach end since boolHasSelections was checked
                i++;
            }

            ret.append(type);
            ret.append(" LIKE '%");
            ret.append(filter[i].replaceAll("/", "//").replaceAll("%", "/%").replaceAll("_","/_").replaceAll("'", "''"));
            ret.append("%'  ESCAPE '/'");
            i++;

            for (; i < filter.length; i++) {
                String f = filter[i];
                if (f.isEmpty()) continue;

                ret.append(" AND ");
                ret.append(type);
                ret.append(" LIKE '%");
                ret.append(f.replaceAll("/", "//").replaceAll("%", "/%").replaceAll("_","/_").replaceAll("'", "''"));
                ret.append("%' ESCAPE '/'");

            }

            ret.append(")");


            if (type.equals(NOTETB_NOTE)) {
                ret.append(" OR ("); //Still have to go through the titles... need to have (...) OR (...)
            } else {
                ret.append(")"); //Finished with the stuff... it's been built
            }

        }

        return ret.toString();
    }

    private String queryBuildTagFilter(List<String> Tag){
        StringBuilder ret = new StringBuilder();

        String mask = queryBuildTagSubFilter();

        ret.append(mask);
        ret.append(Tag.get(0).replaceAll("'", "''"));
        ret.append("'))");

        for(int i = 1; i < Tag.size(); i++){
            ret.append(" AND ");
            ret.append(mask);
            ret.append(Tag.get(i));
            ret.append("'))");
        }
        return ret.toString();
    }

    private String queryBuildTagSubFilter(){
        StringBuilder ret = new StringBuilder();
        ret.append("EXISTS( SELECT ");
        ret.append(RELTB_NOTE);
        ret.append(", ");
        ret.append(RELTB_TAG);
        ret.append(" FROM ");
        ret.append(RELTBNAME);
        ret.append(" r WHERE n.");
        ret.append(ID);
        ret.append(" = r.");
        ret.append(RELTB_NOTE);
        ret.append(" AND EXISTS( SELECT ");
        ret.append(ID);
        ret.append(", ");
        ret.append(TAGTB_TAG);
        ret.append(" FROM ");
        ret.append(TAGTBNAME);
        ret.append(" t WHERE t.");
        ret.append(ID);
        ret.append(" = r.");
        ret.append(RELTB_TAG);
        ret.append(" AND t.");
        ret.append(TAGTB_TAG);
        ret.append(" = '");

        return ret.toString();
    }

    /**
     * Function used for the Spinner to have all of the Tags to select from.
     * @param db    Database to query from
     * @return  Cursor that holds all of the Tags (both ID and Tag name) in the database. Null will be returned if an invalidDB is given.
     */
    public Cursor getAllTags(SQLiteDatabase db){
        if(invalidDB(db)) return null;

        return db.query(TAGTBNAME, new String[]{ID, TAGTB_TAG}, null, null, null, null, ID);
    }

    /**
     * Function used to grab all the Tag names that have a relation with a given note
     * @param db        Database to query from
     * @param noteTitle Title of Note in which the tags must have a relation with.
     * @return  Cursor of all Tags that have a relation with a given note or null if invalid values are submitted, the note with noteTitle doesn't exist, the note doesn't have ANY relations with tags.
     */
    public List<String> getAllTagsByNote(SQLiteDatabase db, String noteTitle){
        if(invalidDB(db) || noteTitle == null || noteTitle.equals("")) return null;

        Cursor noteCursor = db.query(NOTETBNAME, new String[]{ID}, NOTETB_TITLE +" = ?", new String[]{noteTitle},null, null, null);
        if(noteCursor == null || noteCursor.getCount() != 1) return null;

        noteCursor.moveToFirst();
        Cursor relCursor = db.query(RELTBNAME, new String[]{RELTB_TAG}, RELTB_NOTE + " = ?", new String[]{Integer.toString(noteCursor.getInt(noteCursor.getColumnIndex(ID)))}, null, null, null);
        if(relCursor == null || relCursor.getCount() == 0) return null;

        Cursor tagCursor;
        final int ind = relCursor.getColumnIndex(RELTB_TAG);
        List<String> ret = new ArrayList<>();

        relCursor.moveToFirst();

        while(!relCursor.isAfterLast()){
            tagCursor = db.query(TAGTBNAME, new String[]{TAGTB_TAG},  ID + " = ?", new String[]{Integer.toString(relCursor.getInt(ind))}, null, null, null);
            if(tagCursor != null && tagCursor.getCount() != 0){
                tagCursor.moveToFirst();
                ret.add(tagCursor.getString(tagCursor.getColumnIndex(TAGTB_TAG)));
            }

            if(tagCursor != null){
                tagCursor.close();
            }
            relCursor.moveToNext();
        }

        noteCursor.close();
        relCursor.close();

        return ret;
    }

    /**
     * Function used to get a single String holding all the Tag names that a given note (through noteName) possesses a relation with (separated by spaces).
     * @param db       Database to query from
     * @param noteName  Title of the note in which tags must have a relation with.
     * @return  Single String holding all the Tag names of tags with a relation to a given note.
     */
    public String getStringOfTagsByNote(SQLiteDatabase db, String noteName){
        List<String> list = getAllTagsByNote(db, noteName);
        if(list == null || list.size() == 0) return "";

        StringBuilder ret = new StringBuilder();
        ret.append(list.get(0));
        int i = 1;

        while(i < list.size()){
            ret.append(" ");
            ret.append(list.get(i));
            i++;
        }

        return ret.toString();
    }

    private void deleteTag(SQLiteDatabase db, int tag_id){
        if(invalidDB(db)) return;

        if(existsRelation(db, 0, tag_id, REL_CHECK_TAG)) return; //There exists a relation so I don't want to delete the Tag

        db.delete(TAGTBNAME, ID + " = ?", new String[]{Integer.toString(tag_id)}); //No relation exists with this Tag so it's okay to delete
    }

    private int createTag(SQLiteDatabase db, String tagName){
        if(invalidDB(db) || tagName == null || tagName.isEmpty()) return -1;

        Cursor cursor = db.query(TAGTBNAME, new String[]{ID}, TAGTB_TAG + " = ?", new String[]{tagName}, null, null, null);
        if(cursor != null && cursor.getCount() > 0){
            cursor.moveToFirst();
            int ret = cursor.getInt(cursor.getColumnIndex(ID)); //If there's already a tag of that name, don't create a new one
            cursor.close();
            return ret;
        }

        if(cursor != null){
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put(TAGTB_TAG, tagName);
        if(db.insert(TAGTBNAME, null, values) != -1){
            cursor = db.query(TAGTBNAME, new String[]{ID}, TAGTB_TAG + " = ?", new String[]{tagName}, null, null, null);
            cursor.moveToFirst();
            int ret =  cursor.getInt(cursor.getColumnIndex(ID));
            cursor.close();
            return ret;
        }else{
            return -1;
        }
    }

    private boolean invalidDB(SQLiteDatabase db){
        return db == null || !db.isOpen();
    }

}

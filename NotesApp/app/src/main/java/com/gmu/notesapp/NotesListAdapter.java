package com.gmu.notesapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class NotesListAdapter extends RecyclerView.Adapter<NotesListViewHolder> {

    Cursor cursor;
    MainActivity.DeleteNote deleteNote;
    Handler handler;



    public NotesListAdapter(Cursor cursor, Runnable deleteNote, Handler handler){
        this.cursor = cursor;
        this.deleteNote = (MainActivity.DeleteNote) deleteNote;
        this.handler = handler;
    }

    @NonNull
    @Override
    public NotesListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_main_notes, parent, false);

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                deleteNote.setTitle(((TextView) v).getText().toString());
                (new Thread(deleteNote)).start();
                return false;
            }
        });

        view.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                handler.sendMessage(
                        handler.obtainMessage(
                                MainActivity.NOTE_SELECTED,
                                ((TextView) v).getText().toString()
                        )
                );
            }
        });

        return new NotesListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotesListViewHolder holder, int position) {
        cursor.moveToPosition(position);
        holder.noteView.setText(cursor.getString(cursor.getColumnIndex(DatabaseHandler.NOTETB_TITLE)));
    }

    @Override
    public int getItemCount() {
        if(cursor.isClosed()){
            System.err.println("Something is happening..");
        }
        return cursor.getCount();
    }

    public void swapCursor(Cursor newCursor){
        if(cursor != null && !cursor.isClosed()){
            cursor.close();
        }
        cursor = newCursor;
    }
}

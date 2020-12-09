package com.gmu.notesapp;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class NotesListViewHolder extends RecyclerView.ViewHolder {

    TextView noteView;

    public NotesListViewHolder(@NonNull View itemView) {
        super(itemView);
        noteView = (TextView) itemView;
    }
}

package com.gmu.notesapp;

import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TagsListViewHolder extends RecyclerView.ViewHolder {

    Button tagView;

    public TagsListViewHolder(@NonNull View itemView) {
        super(itemView);
        tagView = (Button) itemView;
    }
}

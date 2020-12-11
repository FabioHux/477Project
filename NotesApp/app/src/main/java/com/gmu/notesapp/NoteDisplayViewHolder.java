package com.gmu.notesapp;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class NoteDisplayViewHolder  extends RecyclerView.ViewHolder {

    ArrayList<TextView> words;
    LinearLayout encapsulator;


    public NoteDisplayViewHolder(@NonNull View itemView) {
        super(itemView);
        encapsulator = (LinearLayout) itemView;
    }


    public TextView addTV(ArrayList<TextView> layer){
        if(!layer.equals(words)){
            words = layer;
            for(TextView tv : words){
                encapsulator.addView(tv);
            }
        }
        return null;
    }

}

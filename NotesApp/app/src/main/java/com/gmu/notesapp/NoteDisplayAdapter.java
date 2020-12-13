package com.gmu.notesapp;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.List;

public class NoteDisplayAdapter extends RecyclerView.Adapter<NoteDisplayViewHolder> {

    ArrayList<ArrayList<TextView>> structure;
    ArrayList<Boolean> done;
    Context context;
    int counter;

    public NoteDisplayAdapter(Context context, ArrayList<ArrayList<TextView>> structure){
        this.context = context;
        this.structure = structure;
        done = new ArrayList<>(structure.size());
        for(int i = 0; i < structure.size(); i++){
            done.add(new Boolean(false));
        }
        counter = 0;
    }

    @NonNull
    @Override
    public NoteDisplayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_note_display_notes, parent, false);
        NoteDisplayViewHolder ndvh = new NoteDisplayViewHolder(view);
        //view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        //System.out.println(view.getMeasuredWidth());


        /*NoteDisplayViewHolder vh = new NoteDisplayViewHolder(view);
        String s = null;
        TextView ret = null;
        while(ret == null && !words.isEmpty()){
            ret = new TextView(context);
            ret.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
            s = words.remove(0);
            ret.setText((s + " "));
            ret = vh.addTV(ret, parent_width);
        }
        if(ret != null && s != null){
            words.add(0, s);
        }
        num_layers++;
        vhs.add(vh);*/
        Log.i("NOTEDISPLAYER", "Passing through" + counter + " element.");
        Log.i("NOTEDISPLAYER", "List length: " + getItemCount());
        ArrayList<TextView> layer = structure.get(counter);
        if(!done.get(counter).booleanValue()){
            done.set(counter, new Boolean(true));
            ndvh.addTV(layer);
        }
        if(counter + 1 == structure.size()){
            counter = -1;
        }else{
            counter++;
        }
        return ndvh;
    }

    @Override
    public void onBindViewHolder(@NonNull NoteDisplayViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return structure.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}

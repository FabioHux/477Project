package com.gmu.notesapp;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TagsListAdapter extends RecyclerView.Adapter<TagsListViewHolder> {

    List<String> tags = null;
    boolean firstRemoval = true;
    Handler handler;

    public TagsListAdapter(List<String> tags, Handler handler){
        this.tags = tags;
        this.handler = handler;
    }

    @NonNull
    @Override
    public TagsListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Button view = (Button) LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_button, parent, false);
        view.setOnLongClickListener(v -> {
            removeItem(((Button)v).getText().toString());
            handler.sendEmptyMessage(MainActivity.TAG_LIST_MODIFIED);
            return false;
        });
        return new TagsListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagsListViewHolder holder, int position) {
        holder.tagView.setText(tags.get(position));
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public void addItem(String tag){
        if(!tags.contains(tag))
            if(!firstRemoval)
                tags.add(tag);
            else
                firstRemoval = false;
        notifyDataSetChanged();
    }

    public void removeItem(String tag){
        tags.remove(tag);
        notifyDataSetChanged();
    }

    public void resetRemoval(){
        firstRemoval = true;
    }




}

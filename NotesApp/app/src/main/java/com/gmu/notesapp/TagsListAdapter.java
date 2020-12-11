package com.gmu.notesapp;

import android.database.Cursor;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TagsListAdapter extends RecyclerView.Adapter<TagsListViewHolder> {

    private Integer TAG_LIST_ACTION_TYPE;

    List<String> tags = null;
    boolean firstRemoval = true;
    Handler handler;

    public TagsListAdapter(List<String> tags, Handler handler, Integer ACTION_TYPE){
        this.tags = tags;
        this.handler = handler;
        this.TAG_LIST_ACTION_TYPE = ACTION_TYPE;
    }

    @NonNull
    @Override
    public TagsListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Button view = (Button) LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_button, parent, false);

        if(TAG_LIST_ACTION_TYPE.intValue() == MainActivity.TAG_LIST_MODIFIED)
            view.setOnLongClickListener(v -> {
                removeItem(((Button)v).getText().toString());
                if(TAG_LIST_ACTION_TYPE != null)
                    handler.sendEmptyMessage(TAG_LIST_ACTION_TYPE.intValue());
                return false;
            });
        else if(TAG_LIST_ACTION_TYPE.intValue() == NoteDisplayActivity.TAG_SELECTED)
            view.setOnClickListener(v -> {
                if(TAG_LIST_ACTION_TYPE != null)
                    handler.sendMessage(
                            handler.obtainMessage(
                                TAG_LIST_ACTION_TYPE.intValue(),
                                ((Button)v).getText().toString()
                            )
                    );
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
            tags.add(tag);
        notifyDataSetChanged();
    }

    private void removeItem(String tag){
        tags.remove(tag);
        notifyDataSetChanged();
    }


    public void cleanTags(Cursor cursor){
        if(cursor == null || cursor.isClosed()) return;

        List<String> nList = new ArrayList<>();

        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            String tag = cursor.getString(cursor.getColumnIndex(DatabaseHandler.TAGTB_TAG));

            for(int i = 0; i < tags.size(); i++){
                if(tag.equals(tags.get(i))){
                    tags.remove(tag);
                    nList.add(tag);
                    break;
                }
            }

            cursor.moveToNext();
        }

        tags = nList;

        notifyDataSetChanged();
    }



}

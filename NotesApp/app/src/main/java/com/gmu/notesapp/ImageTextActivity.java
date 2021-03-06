package com.gmu.notesapp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.ArrayList;
import java.util.Arrays;

public class ImageTextActivity extends AppCompatActivity {

    private Bitmap bitmap;
    MyCustomAdapter mAdapter;
    RecyclerView recyclerView;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final String SAVE="save";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_text);
        mAdapter = new MyCustomAdapter();
        recyclerView=(RecyclerView)findViewById(R.id.resultStrings);
        recyclerView.setAdapter(mAdapter);

    }
    /*
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {


        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {

        super.onRestoreInstanceState(savedInstanceState);
        title=(EditText) findViewById(R.id.title);
        tags=(EditText) findViewById(R.id.tags);
        notes=(EditText) findViewById(R.id.notes);
        title.setText(savedInstanceState.getString(TITLE_SAVE,title.getText().toString()));
        tags.setText(savedInstanceState.getString(TAG_SAVE,tags.getText().toString()));
        notes.setText(savedInstanceState.getString(NOTES_SAVE,notes.getText().toString()));
    }
     */

    public void open(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
        /*
        Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 0);
         */
    }
    public void done(View view){
        Intent intent = new Intent();
        //intent.putExtra("com.e.lab2_vchen9.bits",bits);
        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        intent.putExtra("com.gmu.notesapp.imageString", mAdapter.toString());
        finish();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode!=0) {
            super.onActivityResult(requestCode, resultCode, data);
            bitmap = (Bitmap) data.getExtras().get("data");
            ImageView ans = (ImageView) findViewById(R.id.image);
            ((ImageView)findViewById(R.id.image)).setImageBitmap(bitmap);
            ((ImageView)findViewById(R.id.image)).setVisibility(View.VISIBLE);
            findViewById(R.id.take_photo_request).setVisibility(View.GONE);
        }
    }
    public void detect(View view){
        if(bitmap==null) {
            Toast.makeText(getApplicationContext(), "Add a Picture to detect", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseVisionImage image =  FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getCloudTextRecognizer();
        FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("en", "hi"))
                .build();


        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                // Task completed successfully
                                // ...
                                process(firebaseVisionText);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });

    }

    public void process(FirebaseVisionText result){
        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
            mAdapter.addItem(block.getText());
        }
    }

    private class MyCustomAdapter extends RecyclerView.Adapter<ViewHolder> {

        private ArrayList<String> mData = new ArrayList<>();
        private ArrayList<Boolean> booleans = new ArrayList<>();
        private ArrayList<CheckBox> checkBoxes = new ArrayList<>();
        private LayoutInflater mInflater;

        public MyCustomAdapter() {
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(final String item) {
            mData.add(item);
            booleans.add(false);
            notifyDataSetChanged();
        }
        public void removeItem(final String item) {
            for(int i=0;i<mData.size();i++){
                if(mData.get(i)==item){
                    mData.remove(i);
                    checkBoxes.remove(i);
                }
            }
            notifyDataSetChanged();
        }

        public int getCount() {
            return mData.size();
        }
        public String getIfConfirmed(int position) {
            return booleans.get(position)? mData.get(position):null;
        }
        public String toString(){
            StringBuilder str=new StringBuilder();
            for(int i=0;i<mAdapter.getCount();i++) {
                if(booleans.get(i)){
                    str.append("\n");str.append(mData.get(i));}
            }
            return str.toString();
        }

        public String getItem(int position) {
            return mData.get(position);
        }
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public void clear() {
            mData.clear();
            booleans.clear();
            notifyDataSetChanged();
        }
        /*
        public View getView(int position, View convertView, ViewGroup parent) {
            System.out.println("getView " + position + " " + convertView);
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.line, null);
                holder = new ViewHolder();

                holder.textView= (TextView) convertView.findViewById(R.id.textBlock);
                holder.checkBox= (CheckBox) convertView.findViewById(R.id.checkBox);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.textView.setText(getItem(position));
            holder.checkBox=new CheckBox(getApplicationContext());

            return convertView;
        }
        */
        /**/
        /**
         * Provide a reference to the type of views that you are using
         * (custom ViewHolder).
         */



        // Create new views (invoked by the layout manager)
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            // Create a new view, which defines the UI of the list item
            RelativeLayout view =(RelativeLayout) LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.line, null);

            view.setOnClickListener(v -> {
                ((CheckBox)view.getChildAt(1)).setChecked(!((CheckBox)v).isChecked());
            });
            return new ViewHolder((TextView)view.getChildAt(0));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            viewHolder.getTextView().setText(mData.get(position));
            viewHolder.getCheckBox().setChecked(booleans.get(position));
        }


    }
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;
        private CheckBox checkBox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;

        }

        public TextView getTextView() {
            return textView;
        }
        public CheckBox getCheckBox() {
            return checkBox;
        }
    }



}
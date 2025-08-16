package com.example.tfai;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MObileItemAdapter extends RecyclerView.Adapter<MObileItemAdapter.MObileItemViewHolder>{

    private ArrayList<String> data;
    private DBHelper dbHelper;

    public MObileItemAdapter(Context context, ArrayList<String> data){
        this.data=data;
        this.dbHelper = new DBHelper(context, "MobileContactList");
    }

    @NonNull
    @Override
    public MObileItemAdapter.MObileItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(parent.getContext());
        View view=inflater.inflate(R.layout.mobile_contact,parent,false);

        return new MObileItemAdapter.MObileItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MObileItemAdapter.MObileItemViewHolder holder, int position) {
        String contact = data.get(position);
        holder.contact.setText(data.get(position));
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dbHelper.deleteContact(contact)) {
                    Log.d("Contact","contact is deleted");
                    removeItem(position);
                } else {
                    Log.d("Contact","contact is not deleted");
                }
            }
        });

    }

    private void removeItem(int position) {
        data.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, data.size());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class MObileItemViewHolder extends RecyclerView.ViewHolder{
        TextView contact;
        ImageView delete;
        public MObileItemViewHolder(@NonNull final View itemView) {
            super(itemView);
            contact=itemView.findViewById(R.id.contact_number);
            delete=itemView.findViewById(R.id.delete_icon);
        }
    }

}

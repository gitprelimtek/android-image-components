package com.prelimtek.android.picha.view.adapter;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.prelimtek.android.picha.R;
import com.prelimtek.android.picha.view.PhotoProcUtil;
import com.prelimtek.android.picha.viewmodel.ImageMediaViewModel;

import java.util.List;

public class ImageRecyclerViewAdapter extends RecyclerView.Adapter<ImageRecyclerViewAdapter.ViewHolder> {

    public static final int PAGE_BUFFER_SIZE = 15;

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView name;
        TextView type;

        public ViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.itemImage);
            //holder.name = (TextView)convertView.findViewById(R.id.imageName);
            itemView.setOnClickListener(onClickListener);
            itemView.setFocusable(true);
            //itemView.setTag(imageView.getTag());
        }

        public void bind(String imageId, Bitmap bitmap) {
            PhotoProcUtil.setPic(imageView,bitmap);
            imageView.setTag(imageId);
            itemView.setTag(imageId);
        }

        public void clear() {
            imageView.setTag(null);
            itemView.setTag(null);
            imageView.setImageBitmap(null);
        }
    }

    Context context;
    List<String> rowItems;
    View.OnClickListener onClickListener;
    int layoutId = -1;
    LayoutInflater inflator = null;
    ImageMediaViewModel viewModel = null;

    public ImageRecyclerViewAdapter(@NonNull  Context context, @NonNull List<String> items,int layoutId,@NonNull View.OnClickListener onClickListener,@NonNull ImageMediaViewModel mediaViewModel) {
        this.context = context;
        this.rowItems = items;
        this.onClickListener = onClickListener;
        this.layoutId=layoutId;
        this.inflator = LayoutInflater.from(context);
        this.viewModel = mediaViewModel;
    }

    public void setRowItems(List<String> imageNamesList) {
        this.rowItems = imageNamesList;
    }

    @Override
    public int getItemCount() {
        return rowItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View convertView = inflator.inflate(R.layout.list_image_layout,parent,false);

        ViewHolder holder = new ImageRecyclerViewAdapter.ViewHolder(convertView);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imageId = rowItems.get(position);
        String rowItem = (String) getItem(position);
        String encodedBitmap = viewModel.getLocalImageById(rowItem);

        if(encodedBitmap!=null) {
            Bitmap bitmap = PhotoProcUtil.StringifyBitmapCodec.decode(encodedBitmap);
            holder.bind(imageId,bitmap);
        }else{
            holder.clear();
        }

    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }


    public Object getItem(int position) {
        return rowItems.get(position);
    }

}

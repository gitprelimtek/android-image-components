package com.prelimtek.android.picha.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.prelimtek.android.picha.ImagesModel;
import com.prelimtek.android.picha.view.listener.OnImageDeletedListener;
import com.prelimtek.android.picha.R;
import com.prelimtek.android.picha.dao.MediaDAOInterface;
import com.prelimtek.android.picha.view.adapter.ImageRecyclerViewAdapter;

import java.util.List;

/**
 * This class is used for displaying image lists and adding some listener functionality:
 * Currently using an adaptor with a clickListener to display a larger image.
 *
 * TODO: Future will include delete functionality perhaps using a swipe also set an 'editable' flag in bundle???
 *
 * I have not found a good use for OnImageDeletedListener interface
 *
 * This class can be called from a dialog or activity. To include delete functionality an argument should
 * be passed to
 *
 * @author kaniundungu
 * **/
public class ImageListDisplayFragment extends Fragment {

    public static String TAG = Class.class.getSimpleName();


    public final static String IMAGE_LIST_OBJECT_KEY = "imagesModel";
    public final static String IMAGE_IS_EDITABLE_BOOL_KEY = "editable";
    public int viewedItems = 0;

    OnImageDeletedListener mCallback;

    @NonNull
    private MediaDAOInterface dbHelper;

    private boolean isEditable = false;

    public void setDBHelper(MediaDAOInterface localDao) {
        dbHelper = localDao;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);

        //View view =  inflater.inflate( R.layout.images_list_fragment_layout , container,false);
        View view =  inflater.inflate( R.layout.images_recycler_view_layout , container,false);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        final Context currentContext = this.getActivity();

        try {
            //dbHelper =  AppDAO.builder().open(currentContext);
        } catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }

        //get data object passed by previous activity
        final ImagesModel imagesModel = (ImagesModel)getArguments().getSerializable(IMAGE_LIST_OBJECT_KEY);

        isEditable = getArguments().getBoolean(IMAGE_IS_EDITABLE_BOOL_KEY,false);

        assert imagesModel!=null;


        //if list is not empty display last image
        if(imagesModel.getImageNames()!=null && !imagesModel.getImageNames().isEmpty()){

            int imgCount = imagesModel.getImageNames().size();
            final List<String> imageNamesList = imgCount>ImageRecyclerViewAdapter.PAGE_BUFFER_SIZE?imagesModel.getImageNames().subList(0,ImageRecyclerViewAdapter.PAGE_BUFFER_SIZE) :imagesModel.getImageNames();

            //Now bind the list of Images using an adapter
            LinearLayoutManager layoutManager = new LinearLayoutManager(currentContext,RecyclerView.HORIZONTAL,false);
            RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.imageslist_recyclerview) ;
            ImageRecyclerViewAdapter dataAdapter = new ImageRecyclerViewAdapter(
                    currentContext,
                    imageNamesList,
                    R.layout.list_image_layout,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            String imageid = (String)v.getTag();

                            Snackbar.make(v, "Selected image for Details", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();


                            setImageAsCurrent(v.getRootView(),imageid);


                        }
                    },
                    dbHelper
            );
            //dataAdapter.setDBHelper(dbHelper);
            recyclerView.setAdapter(dataAdapter);
            recyclerView.setOnFlingListener(new PageFlingListerner(layoutManager) {

                boolean isLoading = false;
                @Override
                public void loadNextPage() {

                    int offset = viewedItems==0?0:viewedItems;


                    queryDS(ImageRecyclerViewAdapter.PAGE_BUFFER_SIZE,offset);

                    showFirstPage();
                }

                @Override
                public void loadPreviousPage() {

                    int offset = viewedItems-2*ImageRecyclerViewAdapter.PAGE_BUFFER_SIZE;
                    offset = offset < 0 ? 0 : offset;

                    queryDS(ImageRecyclerViewAdapter.PAGE_BUFFER_SIZE,offset);

                    showLastPage();

                }

                private void queryDS(int rowcount, int offset){

                    if(isLoading) return;

                    showProgress(isLoading = true);

                    try {

                        int size = imagesModel.getImageNames().size();
                        if(offset >= size) return;

                        offset = offset<0?0:offset;
                        int start = offset >= size ? size-1: offset;
                        int end = offset+rowcount >= size ? size: offset+rowcount;

                        List<String> imageNamesList = imagesModel.getImageNames().subList(start,end);

                        if(imageNamesList==null || imageNamesList.isEmpty())return;

                        dataAdapter.setRowItems(imageNamesList);
                        dataAdapter.notifyDataSetChanged();

                    }catch(RuntimeException e){
                        e.printStackTrace();
                    }finally {
                        viewedItems = offset+(imageNamesList==null?0:imageNamesList.size());
                        showProgress(isLoading = false);
                    }
                }

                @Override
                public boolean isLoading() {
                    return isLoading;
                }

                @Override
                public void showProgress(boolean show) {
                    ProgressBar progress = (ProgressBar)view.findViewById(R.id.load_image_progress) ;
                    if(show)
                        progress.setVisibility(ProgressBar.VISIBLE);
                    else
                        progress.setVisibility(ProgressBar.GONE);
                }
            });
            recyclerView.setLayoutManager(layoutManager);


        }
    }



    private void setImageAsCurrent(View v, final String imageid) {
        String encodedBitmap = dbHelper.getImageById(imageid);

        if(encodedBitmap!=null) {
            Bitmap bitmap = PhotoProcUtil.toBitMap(encodedBitmap);
            if (isEditable) {

                final Dialog dialog = PhotoProcUtil.startImageDialog(v.getContext(), bitmap, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface parentDialog, int which) {




                        //TODO add confirmation dialog and change listener to swipe or double click
                        //TODO add tooltip with instruction on how to delete

                        if (mCallback == null)
                            return ;

                        parentDialog.dismiss();;

                        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( getActivity() );
                        dialogBuilder
                                .setMessage(R.string.dialog_changes_message)
                                .setTitle(R.string.dialog_changes_title);

                        dialogBuilder
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface curdialog, int id) {
                                // User clicked OK button
                                Dialog progress = PhotoProcUtil.startProgressDialog( getActivity() );

                                try {

                                        mCallback.onImageDeleted(imageid);

                                }catch(Throwable e){
                                    Log.e(TAG,e.getMessage(),e);
                                    progress.dismiss();
                                    PhotoProcUtil.startErrorDialog( getActivity() ,"An error occurred. '"+e.getLocalizedMessage()+"'" );
                                    return;
                                }

                                curdialog.dismiss();
                                progress.dismiss();

                            }
                        });

                        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });

                        AlertDialog alertDialog = dialogBuilder.create();
                        alertDialog.show();

                        //return true;

                    }
                });

            } else {
                PhotoProcUtil.startImageDialog(v.getContext(), bitmap);
            }
        }
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        //this passes the activity and not the parent fragment
    }

    /**
     * This is setter is called by parent fragment which is child aware
     * */
    public void setCallback(OnImageDeletedListener callback){
        mCallback = callback;
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.

    }


    @Override
    public void onStart() {
        super.onStart();

        // When in two-pane layout, set the listview to highlight the selected list item
        // (We do this during onStart because at the point the listview is available.)
        //if (getFragmentManager().findFragmentById(R.id.fragmented_notes_list ) != null) {
        //    getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        //}
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        //dbHelper.close();
        mCallback = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //removing this to resolve Illegal state exception due to commit transaction in activity
        //super.onSaveInstanceState(outState);

    }


    abstract class PageFlingListerner extends RecyclerView.OnFlingListener{

        LinearLayoutManager layoutManager = null;
        public PageFlingListerner(LinearLayoutManager layoutManager) {
            this.layoutManager = layoutManager;
        }

        @Override
        public boolean onFling(int velocityX, int velocityY) {

            int first = layoutManager.findFirstVisibleItemPosition();
            int last = layoutManager.findLastVisibleItemPosition();
            int count = layoutManager.getInitialPrefetchItemCount();

            if(velocityX>10000 || velocityY>10000 ){
                if(last==count ) {
                    loadNextPage();
                    return true;
                }
            }else if(velocityX < -10000 || velocityY< -10000){
                if(first==0 ){
                    loadPreviousPage();
                    return true;
                }
            }

            return false;
        }

        public void showFirstPage(){
            int count = layoutManager.getInitialPrefetchItemCount();
            if(count>0)
                layoutManager.scrollToPosition(0);
        }

        public void showLastPage(){
            int count = layoutManager.getInitialPrefetchItemCount();
            if(count>0)
                layoutManager.scrollToPosition(count-1);
        }

        abstract public void loadNextPage();
        abstract public void loadPreviousPage();
        abstract public boolean isLoading();
        abstract public void showProgress(boolean show);

    }
}

package com.prelimtek.android.picha.view;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.prelimtek.android.alerts.DisplayAlertsBroadcastReceiver;
import com.prelimtek.android.basecomponents.Configuration;
import com.prelimtek.android.picha.ImagesModel;
import com.prelimtek.android.picha.R;
import com.prelimtek.android.picha.databinding.ImageHandlingFragmentLayoutBinding;
import com.prelimtek.android.picha.view.listener.OnImageDeletedListener;
import com.prelimtek.android.picha.viewmodel.ImageMediaViewModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

//import android.widget.Button;

/**
 * This class' purpose is to edit ImagesModel url list.
 * 1. add url ( a mtini/http server url) based on photo or browsed image stored in the backend
 * 2. inplace of (1.) will be using image id where data is accessible using ViewModel using getMediaViewModel method
 *  */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
public class ImageHandlingDialogFragment extends DialogFragment implements OnImageDeletedListener {


    public interface OnImageEditedModelListener {
        //public void onImageModelEdited(ImagesModel newImages, ImagesModel oldImages);
        public void onImageModelEdited();
    }

    private OnImageEditedModelListener editCallBack = null;

    public final static String TAG = Class.class.getSimpleName();

    public final static String ARG_SELECTED_MODEL_IMAGE = "selectedModelImages";
    public final static String ARG_DB_HELPER = "dbHelper";

    //private ImagesModel currentImagesModel = null;
    private ImagesModel oldImagesModel = null;
    private ImageHandlingFragmentLayoutBinding binding;

    //private MediaDAOInterface dbHelper;
    private Activity callingActivity;
    ImageMediaViewModel mediaViewModel;
    private ImageMediaViewModel getMediaViewModel() {
        if(mediaViewModel==null)
            mediaViewModel = new ViewModelProvider((AppCompatActivity)getActivity(),new ViewModelProvider.AndroidViewModelFactory(getActivity().getApplication())).get(ImageMediaViewModel.class);
        return mediaViewModel;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //oldImagesModel = (ImagesModel)getArguments().getSerializable(ARG_SELECTED_MODEL_IMAGE);
        //dbHelper = (MediaDAOInterface)getArguments().getSerializable(ARG_DB_HELPER);

        callingActivity = getActivity();
        //oldImagesModel = getMediaViewModel().getCurrentImages().getValue();
        /*
        if (savedInstanceState != null) {

            currentImagesModel = (ImagesModel)savedInstanceState.getSerializable(ARG_SELECTED_MODEL_IMAGE);

        }else if(currentImagesModel==null && getArguments() != null) {

            try {
                currentImagesModel = null == oldImagesModel ?null:oldImagesModel.clone();//.createClone();
            }catch(CloneNotSupportedException e){
                e.printStackTrace();
            }

        }*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        System.out.println("!!!!!!!!!!!!     onCreateView called");

        // If activity recreated (such as from screen rotate), restore
        // the previous model selection set by onSaveInstanceState().



        //Bind view to image model
        View view = inflater.inflate( R.layout.image_handling_fragment_layout, container, false);

        binding  = DataBindingUtil.findBinding(view);

        if(binding==null){
            binding = DataBindingUtil.bind(view);
        }

        LifecycleOwner lifecycleOwner = (LifecycleOwner)getActivity();
        getMediaViewModel().getCurrentImages().observe(lifecycleOwner,image -> {
            binding.setImagesModel(image);
        });

        //create action listeners
        View updateBtn = view.findViewById(R.id.update_images_Btn );
        updateBtn.setOnClickListener(

                new View.OnClickListener(){
                    @Override
                    public void onClick(final View v) {

                        v.setEnabled(false);

                        ImageHandlingFragmentLayoutBinding binding  = DataBindingUtil.findBinding(v);
                        final ImagesModel newImagesModel = binding.getImagesModel();

                        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( callingActivity );
                        dialogBuilder.setMessage(R.string.dialog_changes_message)
                                .setTitle(R.string.dialog_changes_title);

                        dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                // User clicked OK button
                                //DisplayAlertsBroadcastReceiver.startGenericProgress(callingActivity,"Updating images dialog");

                                try {
                                    getMediaViewModel().completeUpdates();
                                    dismiss();
                                }catch(Throwable e){
                                    Log.e(TAG,e.getMessage(),e);
                                    DisplayAlertsBroadcastReceiver.sendErrorMessage(callingActivity,"An error occurred. '"+e.getLocalizedMessage()+"'");
                                    return;
                                }finally{
                                    //DisplayAlertsBroadcastReceiver.stopProgress(callingActivity);
                                    //ImageHandlingDialogFragment.super.dismiss();
                                    updateComplete();
                                }
                            }
                        });

                        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });

                        AlertDialog dialog = dialogBuilder.create();
                        dialog.show();
                    }
                }
        );

        getMediaViewModel().getAddImageEnabled().observeForever( enabledBool ->{
            boolean enabled = enabledBool.booleanValue();
            View photoBtn = view.findViewById(R.id.take_a_photo_button);
            photoBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setEnabled(false);
                        if(enabled) {
                            dispatchTakePictureIntent();
                        }else{
                            DisplayAlertsBroadcastReceiver.sendPopupMessage(getActivity(),getString(R.string.upgrage_to_higher));
                            DisplayAlertsBroadcastReceiver.sendNotification(getActivity(),1234,getString(R.string.upgrage_notification_title),getString(R.string.upgrage_to_higher));
                        }
                    }
            });

            View loadInternalPhotoBtn = view.findViewById(R.id.gallery_photo_button);
            loadInternalPhotoBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(enabled) {
                            pickGalleryImage();
                        }else{
                            DisplayAlertsBroadcastReceiver.sendPopupMessage(getActivity(),getString(R.string.upgrage_to_premium));
                            DisplayAlertsBroadcastReceiver.sendNotification(getActivity(),1234,getString(R.string.upgrage_notification_title),getString(R.string.upgrage_to_premium));
                        }
                    }
            });

            View loadExternalPhotoBtn = view.findViewById(R.id.external_photo_button );
            loadExternalPhotoBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if(enabled) {
                        pickExternalStorageImage();
                    }else{
                        DisplayAlertsBroadcastReceiver.sendPopupMessage(getActivity(),getString(R.string.upgrage_to_premium));
                        DisplayAlertsBroadcastReceiver.sendNotification(getActivity(),1234,getString(R.string.upgrage_notification_title),getString(R.string.upgrage_to_premium));
                    }
                    //PhotoProcUtil.pickExternalStorageImage(getActivity());
                }
            });

        });
        binding.setLifecycleOwner(lifecycleOwner);

        //Setup cancel button to ignore changes
        View cancelBtn = view.findViewById(R.id.cancel_images_edit_Btn  );
        cancelBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                }
        );


        return view;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshListFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        System.out.println("!!!!!!!!!!!!     onStart called");

    }

    /**
     * Child fragment is not aware of its parent Fragment but it is aware of the parent Activity
     * Therefore, the parent fragment should set its child aware of itself for delete image purposes
     * **/
    @Override
    public void onAttachFragment(Fragment childFragment) {
        super.onAttachFragment(childFragment);
        try {
            childFragment = (ImageListDisplayFragment) childFragment;
            ((ImageListDisplayFragment) childFragment).setCallback(this);
        } catch (ClassCastException e) {
            Log.e(TAG,e.getMessage());
            throw new ClassCastException(childFragment.toString()
                    + " is not of type ImageListDisplayFragment");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            editCallBack = (OnImageEditedModelListener)context;
        } catch (ClassCastException e) {
            Log.e(TAG,e.getMessage());
            throw new ClassCastException(context
                    + " is not of type OnImageEditedModelListener");
        }
    }


    public static final int REQUEST_IMAGE_CAPTURE_CODE = 1001;
    public static final int REQUEST_TAKE_PHOTO_CODE = 2001;
    public static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 3001;

    private void dispatchTakePictureIntent() {

        if ((ActivityCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                &&
                (ActivityCompat.checkSelfPermission(
                        getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    ex.printStackTrace();
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(getActivity(),
                            PhotoProcUtil.getAuthorityProvider(getActivity()),
                            photoFile);
                    //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    this.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO_CODE);
                    //System.out.println("IntentData = "+takePictureIntent.getData());
                }
            }
        }else{
            //request permission
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_TAKE_PHOTO_CODE
            );
        }
    }



    String mCurrentPhotoPath;


    private File createImageFile() throws IOException {

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);//
        //getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);//

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        //File image = new File(storageDir, imageFileName+".jpg");
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        System.out.println(mCurrentPhotoPath);

        return image;
    }

    @Deprecated
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }

    @Deprecated
    private void setPic(ImageView mImageView) {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

    private Bitmap getCompressedImage(String path, int targetW, int targetH){

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        bmOptions.inScaled = true;
        //bmOptions.outHeight = targetH;
        //bmOptions.outWidth = targetW;

        Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);

        return bitmap;
    }

    @Deprecated
    private Bitmap getCompressedImage(InputStream stream, int targetW, int targetH){

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //bmOptions.inJustDecodeBounds = true;
        //Bitmap largeBitmap = BitmapFactory.decodeStream(stream, null, bmOptions);
        //int photoW = bmOptions.outWidth;
        //int photoH = bmOptions.outHeight;
        Bitmap largeBitmap = BitmapFactory.decodeStream(stream);
        int photoW = largeBitmap.getWidth();
        int photoH = largeBitmap.getHeight();

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        bmOptions.inScaled = true;
        //bmOptions.outHeight = targetH;
        //bmOptions.outWidth = targetW;

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        largeBitmap.compress(Bitmap.CompressFormat.PNG, 0, outStream);

        byte[] largeBitmapBytes = outStream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(largeBitmapBytes, 0,largeBitmapBytes.length,bmOptions);

        return bitmap;
    }

    public void pickGalleryImage() {
        /*Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI);*/
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        /*intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("outputX", 256);
        intent.putExtra("outputY", 256);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("return-data", true);*/
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_CODE);
    }

    private void pickExternalStorageImage() {
        if (ActivityCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            /*Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI);

            intent.setType( "image/*");

            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("outputX", 256);
            intent.putExtra("outputY", 256);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("return-data", true);

            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_CODE);*/

            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Get Image From"), REQUEST_IMAGE_CAPTURE_CODE);

        } else {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_EXTERNAL_STORAGE_REQUEST_CODE
            );
        }
    }


    /**
     * See pickGalleryImage()
    @Deprecated
    private void dispatchLoadPhotoIntent() {
        //Intent searchImageIntent = new Intent(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        Intent searchImageIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI );


        if (searchImageIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                        "io.mtini.android.fileprovider",
                        photoFile);
                searchImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                this.startActivityForResult(searchImageIntent, REQUEST_IMAGE_CAPTURE_CODE);
                //System.out.println("IntentData = "+takePictureIntent.getData());
            }
        }
    }
     */

    /**
     * This is called by the update button upon satisfactory editing in this fragment
     * */
    public void updateComplete(){
        if(editCallBack==null){
            editCallBack = (OnImageEditedModelListener) this.getActivity();
        }
        editCallBack.onImageModelEdited();

    }



    /**
     * Should be called when camera and browsing activities complete successfully.
     * Perform image compression and persistence in cache/db.
     * */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        int h = Configuration.captureImgMaxHeight;
        int w = Configuration.captureImgMaxWidth;

        switch(requestCode) {
            case REQUEST_IMAGE_CAPTURE_CODE:
                if(resultCode == RESULT_OK){

                    try {
                        InputStream inputStream = getActivity().getContentResolver().openInputStream(imageReturnedIntent.getData());

                        Bitmap bitmap = PhotoProcUtil.getCompressedImage(inputStream,w,h);

                        //Bitmap bitmap = (Bitmap)imageReturnedIntent.getExtras().get("data");

                        String encodedImageString = PhotoProcUtil.StringifyBitmapCodec.encode(bitmap);
                        UUID id = UUID.randomUUID();
                        if(getMediaViewModel().addLocalImage(id.toString(), encodedImageString)) {
                            getMediaViewModel().getCurrentImages().getValue().addImageName(id.toString());
                        }

                        //inputStream.close();

                    } catch (Exception e) {

                        Log.e(TAG,e.getMessage(),e);
                        DisplayAlertsBroadcastReceiver.sendErrorMessage(getActivity(),"Request image capture error: "+e.getMessage());
                    }

                }
                break;
            case REQUEST_TAKE_PHOTO_CODE:
                if(resultCode == RESULT_OK){
                    try {

                        /*
                        Uri selectedImage = imageReturnedIntent.getData();
                        String path = selectedImage==null?null:selectedImage.getPath();
                        //String mCurrentPhotoPath = dispatchPhotoFile==null?null:dispatchPhotoFile.getAbsolutePath();
                        Bitmap bitmap = PhotoProcUtil.getCompressedImage(mCurrentPhotoPath, h, w);
                        */
                        Bitmap bitmap = (Bitmap)imageReturnedIntent.getExtras().get("data");

                        String encodedImageString = PhotoProcUtil.StringifyBitmapCodec.encode(bitmap);

                        UUID id = UUID.randomUUID();

                        if(getMediaViewModel().addLocalImage(id.toString(), encodedImageString) ){
                            getMediaViewModel().getCurrentImages().getValue().addImageName(id.toString());
                        }
                    }catch(Exception e){
                        Log.e(TAG,e.getMessage(),e);
                        DisplayAlertsBroadcastReceiver.sendErrorMessage(getActivity(),"Request take photo error: "+e.getMessage());
                    }

                }
                break;
        }

        //refresh fragment
        //updateImagesView(currentImagesModel);
        refreshListFragment();
    }

    //TODO remove and use PhotoProcUtil methods
    private void refreshListFragment(){

        //System.out.println("!!!!!!!!  refreshListFragment called -> currentImagesModel size ="+currentImagesModel.getImageNames().size());

        FragmentManager childTransactionManager = getChildFragmentManager();

        FragmentTransaction transaction = childTransactionManager.beginTransaction();

        Fragment oldFragment = childTransactionManager.findFragmentById(R.id.image_list_fragment);

        ImageListDisplayFragment newImgsListFragment = new ImageListDisplayFragment();
        //newImgsListFragment.setDBHelper(dbHelper);
        Bundle imgBundle = new Bundle();
        //imgBundle.putSerializable(ImageListDisplayFragment.IMAGE_LIST_OBJECT_KEY, currentImagesModel);
        imgBundle.putBoolean(ImageListDisplayFragment.IMAGE_IS_EDITABLE_BOOL_KEY, true);
        newImgsListFragment.setArguments(imgBundle);

        if(oldFragment!=null){
            transaction
                    .replace(R.id.image_list_fragment, newImgsListFragment);
        }else {
            transaction
                    .add(R.id.image_list_fragment, newImgsListFragment);
        }

        transaction.commitAllowingStateLoss();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        System.out.println("!!!!!!!!!!!!     onRequestPermissionsResult called");
        switch(requestCode) {
            case REQUEST_IMAGE_CAPTURE_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            pickExternalStorageImage();
                        }
                    });
                }
                break;
            case REQUEST_TAKE_PHOTO_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED ){

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            dispatchTakePictureIntent();
                        }
                    });


                }
                break;
        }
    }

    /*@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        System.out.println("!!!!!!!!!!!!     onSaveInstanceState called");
        // Save the current imagemodel selection in case we need to recreate the fragment
        outState.putSerializable(ARG_SELECTED_MODEL_IMAGE, currentImagesModel);

    }*/

    ////Handle orientation changes etc
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
       /* if(outState!=null) {
            ImagesModel newImagesModel = binding.getImagesModel();
            outState.putSerializable(ARG_SELECTED_MODEL_IMAGE, newImagesModel);
        }

        */
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        /*if(savedInstanceState!=null) {
            this.currentImagesModel = (ImagesModel) savedInstanceState.getSerializable(ARG_SELECTED_MODEL_IMAGE);
        }*/
    }

    @Override
    public void onImageDeleted(final String imageId) {
        System.out.println("!!!!!!!!!!!!     onImageDeleted called");
        //currentImagesModel.getImageNames().remove(imageId);
        try {
            if(getMediaViewModel().deleteLocalImage(imageId)){
                getMediaViewModel().getCurrentImages().getValue().removeImageName(imageId);
            }
        } catch (Exception e) {

            Log.e(TAG,e.getMessage(),e);
            DisplayAlertsBroadcastReceiver.sendErrorMessage(getActivity(),"Request delete photo error: "+e.getMessage());

        }
        refreshListFragment();
    }

}

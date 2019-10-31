package com.prelimtek.android.picha.view;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


import com.prelimtek.android.basecomponents.Configuration;
import com.prelimtek.android.basecomponents.dialog.DialogUtils;
import com.prelimtek.android.picha.ImagesModel;
import com.prelimtek.android.picha.R;
import com.prelimtek.android.picha.dao.MediaDAOInterface;

public class PhotoProcUtil extends DialogUtils {

    private static String TAG = Class.class.getSimpleName();

    public static final int REQUEST_IMAGE_CAPTURE_CODE = 1001;
    public static final int REQUEST_TAKE_PHOTO_CODE = 2001;
    public static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 3001;

    public static File dispatchTakePictureIntent(Activity activity) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        File photoFile = null;
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // Create the File where the photo should go

            try {
                photoFile = createImageFile(activity);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(activity,
                        getAuthorityProvider(activity),
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                activity.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO_CODE);
                //System.out.println("IntentData = "+takePictureIntent.getData());
            }
        }
        return photoFile;
    }

    public static File createImageFile(Activity activity) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);//
        //getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);//

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        //File image = new File(storageDir, imageFileName+".jpg");
        // Save a file: path for use with ACTION_VIEW intents
        String mCurrentPhotoPath = image.getAbsolutePath();
        System.out.println(mCurrentPhotoPath);
        return image;
    }


    public static void galleryAddPic(Activity activity, String mCurrentPhotoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        activity.sendBroadcast(mediaScanIntent);
    }


    public static void setPic(String mCurrentPhotoPath,ImageView mImageView) {
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
        float scaleFactor = Math.min((float)photoW/targetW, (float)photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = Math.round(scaleFactor);
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

    public static Bitmap getCompressedImage(String path, int targetW, int targetH){

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        float scaleFactor = Math.min((float)photoW/targetW, (float)photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = Math.round(scaleFactor);
        bmOptions.inPurgeable = true;

        bmOptions.inScaled = true;
        //bmOptions.outHeight = targetH;
        //bmOptions.outWidth = targetW;

        Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);

        return bitmap;
    }

    public static Bitmap getCompressedImage(InputStream stream, int targetW, int targetH){

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //bmOptions.inJustDecodeBounds = true;
        //Bitmap largeBitmap = BitmapFactory.decodeStream(stream, null, bmOptions);
        //int photoW = bmOptions.outWidth;
        //int photoH = bmOptions.outHeight;
        Bitmap largeBitmap = BitmapFactory.decodeStream(stream);
        int photoW = largeBitmap.getWidth();
        int photoH = largeBitmap.getHeight();

        // Determine how much to scale down the image
        float scaleFactor = Math.min((float)photoW/targetW, (float)photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = Math.round(scaleFactor);
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
  
    public static void pickGalleryImage(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        intent.setType("image/*");
        //TODO revisit these params
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("outputX", 256);
        intent.putExtra("outputY", 256);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("return-data", true);
        activity.startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_CODE);
    }

    public static void pickExternalStorageImage(Activity activity) {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
            );
            intent.setType( "image/*");
            //intent.putExtra("crop", "true");
            //intent.putExtra("scale", true);
            //intent.putExtra("aspectX", 16);
            //intent.putExtra("aspectY", 9);
            //TODO revisit these params
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("outputX", 256);
            intent.putExtra("outputY", 256);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("return-data", true);

            activity.startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_CODE);
        } else {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_EXTERNAL_STORAGE_REQUEST_CODE
            );
        }
    }

    public static void setPic(ImageView mImageView, String mCurrentPhotoPath) {

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        setPic(mImageView,bitmap);
    }


    public static void setPic(ImageView imageView, Bitmap bitmap){
        ///resize image
        int photoW = bitmap.getWidth();
        int photoH = bitmap.getHeight();

        // Determine how much to scale down the image
        float scaleFactor = 1 ;
        if (Build.VERSION.SDK_INT < 16) {
            scaleFactor = Math.max((float)imageView.getWidth() / photoW, (float)imageView.getHeight() / photoH);
        }else {
             scaleFactor = Math.max( (float)imageView.getMaxWidth() / photoW, (float)imageView.getMaxHeight() / photoH);
        }
        photoW = Math.round(photoW*scaleFactor);
        photoH = Math.round(photoH*scaleFactor);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap,photoW,photoH,false);
        imageView.setImageBitmap(resizedBitmap);

    }

    public static byte[] toBytes(Bitmap bitmap){

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        byte[] bitmapBytes=  stream.toByteArray();
        return bitmapBytes;
    }

    /**
     * Compress and base 64 encode this bitmap to string.
     * */
    public static String toEncodedStringBytes(Bitmap bitmap){

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        byte[] bitmapBytes =  stream.toByteArray();
        byte[] compressedBytes = new CompressionCodec().compress(bitmapBytes);
        return Base64.encodeToString(compressedBytes,Base64.NO_WRAP);
    }

    /**
     * Assume this base64 encoded String of image is compressed. Decode to bytes and Decompress.
     * Then create Bitmap.
     * */
    public static Bitmap toBitMap(String encodedStringBytes){

        byte[] imageBytes = Base64.decode(encodedStringBytes,Base64.NO_WRAP);

        byte[] decompressedBytes = new byte[0];
        try {
            decompressedBytes = new CompressionCodec().decompress(imageBytes);
        } catch (DataFormatException e) {
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
            decompressedBytes = imageBytes;
        }

        Bitmap bitmap =  BitmapFactory.decodeByteArray(decompressedBytes, 0, decompressedBytes.length);

        return bitmap;
    }

    public static void showOrRefreshImageListFragment(@NonNull ImagesModel currentImagesModel, @NonNull FragmentManager childTransactionManager , @NonNull MediaDAOInterface dbHelper, boolean editable){

        System.out.println("!!!!!!!!  showOrRefreshImageListFragment called -> currentImagesModel size ="+currentImagesModel.getImageNames().size());

        //FragmentManager childTransactionManager = getChildFragmentManager();

        FragmentTransaction transaction = childTransactionManager.beginTransaction();

        Fragment oldFragment = childTransactionManager.findFragmentById(R.id.image_list_fragment);


        ImageListDisplayFragment newImgsListFragment = new ImageListDisplayFragment();
        newImgsListFragment.setDBHelper(dbHelper);
        Bundle imgBundle = new Bundle();
        imgBundle.putSerializable(ImageListDisplayFragment.IMAGE_LIST_OBJECT_KEY, currentImagesModel);
        imgBundle.putBoolean(ImageListDisplayFragment.IMAGE_IS_EDITABLE_BOOL_KEY, editable);
        newImgsListFragment.setArguments(imgBundle);


        if(oldFragment!=null){
            //transaction.detach(oldFragment).attach(newImgsListFragment).commit();
            transaction
                    .replace(R.id.image_list_fragment, newImgsListFragment).commit();
        }else {
            transaction
                    .add(R.id.image_list_fragment, newImgsListFragment).commit();
        }


    }

    public static class CompressionCodec{

        private static int BUFFER_SIZE = 1024;

        public byte[] compress(byte[] input){

            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] output = new byte[BUFFER_SIZE];

            Deflater compresser = new Deflater(Deflater.BEST_SPEED);
            compresser.setInput(input,0,input.length);
            compresser.finish();
            while(!compresser.finished()){
                int compressedDataLength = compresser.deflate(output);
                if (compressedDataLength == 0) {
                    break;
                }
                o.write(output, 0, compressedDataLength);
            }

            compresser.end();

            return o.toByteArray();
        }

        public byte[] decompress(byte[] compressed) throws DataFormatException {

            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] output = new byte[BUFFER_SIZE];

            Inflater decompresser = new Inflater();
            decompresser.setInput(compressed,0,compressed.length);
            while(!decompresser.finished()) {
                int decompressedDataLength = decompresser.inflate(output);
                if (decompressedDataLength == 0) {
                    break;
                }
                o.write(output, 0, decompressedDataLength);
            }
            return o.toByteArray();
        }

    }

    public static Dialog startImageDialog(Context context, Bitmap bitmap, DialogInterface.OnClickListener listener){

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        //TODO add icon dialogBuilder.setIcon()
        dialogBuilder.setMessage(R.string.dialog_delete_image)
                .setTitle(R.string.dialog_image_details);
        dialogBuilder.setPositiveButton(R.string.delete,listener);
        dialogBuilder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setCanceledOnTouchOutside(true);

        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(bitmap);
        imageView.setMaxHeight(Configuration.imageDialogMaxHeight);
        imageView.setMaxWidth(Configuration.imageDialogMaxWidth);
        //imageView.setOnLongClickListener(listener);
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);

        //dialog.addContentView(imageView,params);
        dialog.setView(imageView);
        dialog.setCanceledOnTouchOutside(true);

        dialog.show();

        return dialog;
    }

    public static Dialog startImageDialog(Context context, Bitmap bitmap){

        Dialog dialog = new Dialog(context);
        ImageView imageView = new ImageView(context);
        //imageView.setImageBitmap(bitmap);
        imageView.setMaxHeight(Configuration.imageDialogMaxHeight);
        imageView.setMaxWidth(Configuration.imageDialogMaxWidth);
        imageView.setMinimumHeight(bitmap.getHeight() );
        imageView.setMinimumWidth(bitmap.getWidth() );
        setPic(imageView,bitmap);

/*        ///resize image
        int photoW = bitmap.getWidth();
        int photoH = bitmap.getHeight();

        // Determine how much to scale down the image
        //int scaleFactor = Math.max(imageView.getMaxWidth()/photoW, imageView.getMaxHeight()/photoH);
        float scaleFactor = 1 ;
        if (Build.VERSION.SDK_INT < 16) {
            scaleFactor = Math.max((float)imageView.getWidth() / photoW, (float)imageView.getHeight() / photoH);
        }else {
            scaleFactor = Math.max((float)imageView.getMaxWidth() / photoW, (float)imageView.getMaxHeight() / photoH);
        }

        photoW = Math.round(photoW*scaleFactor);
        photoH = Math.round(photoH*scaleFactor);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap,photoW,photoH,false);
        imageView.setImageBitmap(resizedBitmap);
*/

        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);

        dialog.addContentView(imageView,params);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        return dialog;
    }

    public static String getAuthorityProvider(Context context) {

        String res =  context.getResources().getString(R.string.authority_file_provider);
        if(res ==null) return "io.mtini.android.fileprovider";

        return res;
    }
}

package com.prelimtek.android.picha.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.prelimtek.android.alerts.DisplayAlertsBroadcastReceiver;
import com.prelimtek.android.basecomponents.dao.BaseDAOFactory;
import com.prelimtek.android.picha.ImagesModel;
import com.prelimtek.android.picha.dao.MediaDAOInterface;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ImageMediaViewModel extends AndroidViewModel {

    private final static String TAG = ImageMediaViewModel.class.getName();
    private Context context;
    private BaseDAOFactory daoFactory;
    private MediaDAOInterface combinedDao;
    private MediaDAOInterface localDao;
    private MediaDAOInterface remoteDao;
    private ImagesModel oldImages;

    public ImageMediaViewModel(@NonNull Application application) {
        super(application);
        context = getApplication().getApplicationContext();
        try {
            daoFactory = BaseDAOFactory.instance(context);
            combinedDao = (MediaDAOInterface) daoFactory.open(BaseDAOFactory.TYPE.BOTH);
            localDao = (MediaDAOInterface) daoFactory.open(BaseDAOFactory.TYPE.LOCAL);
            remoteDao = (MediaDAOInterface) daoFactory.open(BaseDAOFactory.TYPE.REMOTE);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    public boolean addLocalImage(String imageId, String encodedImage) throws Exception {
        return localDao.addImage(imageId, modelId, encodedImage);
    }

    public boolean deleteLocalImage(String imageId) throws Exception {
        return localDao.deleteImage(imageId);
    }

    public String getLocalImageById(String imageId) {
        return localDao.getImageById(imageId);
    }

    public String[] getLocalImageIdList(String modelId) {
        return localDao.getImageIdList(modelId);
    }

    public boolean addRemoteImage(String imageId, String encodedImage) throws Exception {
        return remoteDao.addImage(imageId, modelId, encodedImage);
    }

    public boolean deleteRemoteImage(String imageId) throws Exception {
        return remoteDao.deleteImage(imageId);
    }

    public String getRemoteImageById(String imageId) {
        return remoteDao.getImageById(imageId);
    }

    public String[] getRemoteImageIdList(String modelId) {
        return remoteDao.getImageIdList(modelId);
    }

    private String modelId;
    private MutableLiveData<ImagesModel> curImages = null;

    private ImagesModel loadCurEstateImages() {

        String[] imageIds = localDao.getImageIdList(modelId);

        ImagesModel images = new ImagesModel(modelId, Arrays.asList(imageIds));

        return images;
    }

    public LiveData<ImagesModel> initializeModel(String modelId) throws CloneNotSupportedException {

        this.modelId = modelId;

        LiveData<ImagesModel> images = new MutableLiveData<ImagesModel>(loadCurEstateImages());

        oldImages = images == null ? null : (images.getValue() == null ? null : images.getValue().clone());

        return images;
    }

    public LiveData<ImagesModel> getCurrentImages() {

        return curImages == null ? new MutableLiveData<ImagesModel>(loadCurEstateImages()) : curImages;
    }

    private void setModelId(String id) {
        modelId = id;
    }

    @Deprecated
    public void updateDBVintage(ImagesModel images, ImagesModel oldImages) throws Exception {

        List<String> commonList = images.getImageNames();
        commonList.retainAll(oldImages.getImageNames());

        List<String> toAdd = images.getImageNames();
        List<String> toDelete = oldImages.getImageNames();
        toDelete.removeAll(commonList);

        for (String imageId : toAdd) {

            String encodedImage = localDao.getImageById(imageId);
            if (encodedImage == null) {
                Log.e(TAG, imageId + " image was found to be null. Expected a value!!!!!!!!!");
            } else {

                remoteDao.addImage(imageId, modelId, encodedImage);
                localDao.updateImage(imageId, modelId);
            }

        }

        for (String imageId : toDelete) {

            remoteDao.deleteImage(imageId);
            localDao.deleteImage(imageId);

        }

    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updateDB(ImagesModel newModel, ImagesModel origModel) {

        DisplayAlertsBroadcastReceiver.updateProgress(getApplication(), "Updating images", 10);

        final String modelId = newModel.getModelId();

        Stream<String> deleteList = (Stream<String>) (origModel == null ? Stream.empty() : origModel.getImageNames().stream()).filter(id -> !newModel.getImageNames().contains(id));

        Stream<String> addList = newModel.getImageNames().stream().filter(id -> !(origModel == null ? Collections.emptyList() : origModel.getImageNames()).contains(id));

        deleteList.forEach(id -> {
            DisplayAlertsBroadcastReceiver.updateProgress(getApplication(), "deleting images", 25);
            try {
                //localDao.deleteImage(id);
                remoteDao.deleteImage(id);
                //combinedDao.deleteImage(id);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                DisplayAlertsBroadcastReceiver.sendErrorMessage(context, e.getMessage());
            }
        });

        addList.forEach(id -> {

            DisplayAlertsBroadcastReceiver.updateProgress(getApplication(), "adding images", 50);

            //get local image
            String decodedImage = localDao.getImageById(id);
            //update remote image
            try {
                remoteDao.addImage(id, modelId, decodedImage);
                localDao.updateImage(id, modelId);
            } catch (Exception e) {
                try {
                    localDao.deleteImage(id);
                } catch (Exception exception) {
                    //do nothing
                    Log.w(TAG, e.getMessage(), e);
                }
                Log.e(TAG, e.getMessage(), e);
                DisplayAlertsBroadcastReceiver.sendErrorMessage(context, e.getMessage());
            }
        });

    }

    public void completeUpdates() throws Exception {

        Handler myHandler = new Handler();
        myHandler.postDelayed(() -> {
            try {
                ImagesModel newImages = loadCurEstateImages();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    updateDB(newImages, oldImages);
                } else {
                    updateDBVintage(newImages, oldImages);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.e(TAG, ex.getMessage(), ex);
            }
        }, 200);
    }
}

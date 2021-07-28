package com.prelimtek.android.picha;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class ImagesModel extends BaseObservable implements Serializable, Cloneable {

    private String modelId;
    private List<String> imageNames = new ArrayList<String>();

    public ImagesModel(String modelId, List<String> imageNames) {
        this.modelId = modelId;
        this.imageNames.addAll(imageNames);
    }

    @Bindable
    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
        notifyPropertyChanged(BR.modelId);
    }

    @Bindable
    public List<String> getImageNames() {
        return imageNames;
    }

    public void setImageNames(List<String> imageNames) {
        this.imageNames = imageNames;
        notifyPropertyChanged(BR.imageNames);
    }

    public void addImageName(String imageName) {
        Collections.addAll(imageNames, imageName);
        notifyPropertyChanged(BR.imageNames);
    }

    public void removeImageName(int imagePos) {
        this.imageNames.remove(imagePos);
        notifyPropertyChanged(BR.imageNames);
    }

    public void removeImageName(String imageName) {
        this.imageNames.remove(imageName);
        notifyPropertyChanged(BR.imageNames);
    }

    @Override
    public String toString() {
        return "ImagesModel{" +
                ", modelId='" + modelId + '\'' +
                ", imageNames=" + Arrays.toString(imageNames.toArray()) +
                '}';
    }


    @Override
    public int hashCode() {

        return Objects.hash(modelId, imageNames);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImagesModel)) return false;
        ImagesModel that = (ImagesModel) o;
        return that.hashCode() == hashCode();

    }

    @Override
    public ImagesModel clone() throws CloneNotSupportedException {
        List<String> imageNamesClone = null;
        if (imageNames != null) {
            imageNamesClone = new ArrayList<String>(imageNames);
        }

        ImagesModel clone = (ImagesModel) super.clone();
        clone.setImageNames(imageNamesClone);
        return clone;
    }
}

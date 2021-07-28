package com.prelimtek.android.picha.dao;

import java.io.Serializable;

public interface MediaDAOInterface extends Serializable {

    String getImageById(String id);

    String[] getImageIdList(String modelId);

    boolean deleteImage(String id) throws Exception;

    boolean addImage(String imageId, String modelId, String encodedImage) throws Exception;

    int updateImage(String imageId, String modelId);
}

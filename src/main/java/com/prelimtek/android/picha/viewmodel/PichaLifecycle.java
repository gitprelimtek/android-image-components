package com.prelimtek.android.picha.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class PichaLifecycle implements LifecycleObserver {

    private Application app;
    private PichaLifecycle(Application app){
        this.app=app;
    }
    private PichaLifecycle(){}
    private static PichaLifecycle INSTANCE;

    public static PichaLifecycle instance(Application app){
        if(INSTANCE==null){
            INSTANCE = new PichaLifecycle(app);
        }
        return INSTANCE;
    }

    private MutableLiveData<Boolean> enableImageControls = new MutableLiveData<>();

    public MutableLiveData<Boolean> getEnableImageControls() {
        return enableImageControls;
    }
}

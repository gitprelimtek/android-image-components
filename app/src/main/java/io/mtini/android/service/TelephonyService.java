package io.mtini.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

public class TelephonyService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public TelephonyService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}

package com.example.fonos_group13;

import android.app.Application;
import android.content.Context;

public class FonosApplication extends Application {
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = createAppContainer();
    }

    protected AppContainer createAppContainer() {
        return new DefaultAppContainer(this);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }

    public static AppContainer container(Context context) {
        Context appContext = context.getApplicationContext();
        if (!(appContext instanceof FonosApplication)) {
            throw new IllegalStateException("FonosApplication is not registered.");
        }
        return ((FonosApplication) appContext).getAppContainer();
    }
}

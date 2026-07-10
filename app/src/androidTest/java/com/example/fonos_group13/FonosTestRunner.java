package com.example.fonos_group13;

import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

public final class FonosTestRunner extends AndroidJUnitRunner {
    @Override
    public Application newApplication(
            ClassLoader classLoader,
            String className,
            Context context
    ) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(classLoader, FonosTestApplication.class.getName(), context);
    }
}

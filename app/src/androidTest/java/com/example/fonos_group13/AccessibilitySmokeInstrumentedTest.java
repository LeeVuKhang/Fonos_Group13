package com.example.fonos_group13;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

import android.view.KeyEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AccessibilitySmokeInstrumentedTest {
    private ActivityScenario<DiscoverActivity> scenario;

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable();
    }

    @After
    public void closeScenario() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void discoverScreenRunsAccessibilityChecksDuringKeyboardNavigation() {
        scenario = ActivityScenario.launch(DiscoverActivity.class);
        onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_TAB));
    }
}

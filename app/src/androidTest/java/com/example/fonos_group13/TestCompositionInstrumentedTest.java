package com.example.fonos_group13;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestCompositionInstrumentedTest {
    @Test
    public void instrumentationUsesFakeApplicationContainer() {
        FonosApplication application = (FonosApplication) InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext()
                .getApplicationContext();

        assertTrue(application instanceof FonosTestApplication);
        assertNotNull(application.getAppContainer().authRepository());
        assertNotNull(application.getAppContainer().catalogRepository());
        assertNotNull(application.getAppContainer().creatorUploadsRepository());
    }
}

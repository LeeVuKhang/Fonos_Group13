package com.example.fonos_group13;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ActivityLaunchConfigurationTest {
    @Test
    public void launcherActivityInitializesARealWindowBeforeHandoff() throws Exception {
        String mainActivity = readFile("src/main/java/com/example/fonos_group13/MainActivity.java");
        String launcherLayout = readFile("src/main/res/layout/activity_main.xml");

        assertTrue(mainActivity.contains("setContentView(R.layout.activity_main)"));
        assertTrue(mainActivity.contains("postDelayed"));
        assertTrue(mainActivity.contains("HANDOFF_DELAY_MS"));
        assertTrue(launcherLayout.contains("@color/background"));
    }

    @Test
    public void splashThemeIsOpaqueSoEmulatorKeepsAValidSurface() throws Exception {
        String theme = readFile("src/main/res/values/themes.xml");

        assertTrue(theme.contains("Theme.Fonos_Group13.Splash"));
        assertTrue(theme.contains("<item name=\"android:windowBackground\">@color/background</item>"));
        assertFalse(theme.contains("<item name=\"android:windowIsTranslucent\">true</item>"));
        assertFalse(theme.contains("@android:color/transparent"));
    }

    @Test
    public void loginToDiscoverTransitionUsesNoAnimation() throws Exception {
        String loginActivity = readFile("src/main/java/com/example/fonos_group13/LoginActivity.java");

        assertTrue(loginActivity.contains("Intent.FLAG_ACTIVITY_NO_ANIMATION"));
        assertTrue(loginActivity.contains("overridePendingTransition(0, 0)"));
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}

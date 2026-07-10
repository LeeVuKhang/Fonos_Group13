package com.example.fonos_group13;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ThemeConfigurationTest {
    @Test
    public void appThemeUsesExplicitLightWindowBackground() throws Exception {
        String defaultTheme = readThemeFile("src/main/res/values/themes.xml");
        String nightTheme = readThemeFile("src/main/res/values-night/themes.xml");
        String api29Theme = readThemeFile("src/main/res/values-v29/themes.xml");
        String api29NightTheme = readThemeFile("src/main/res/values-night-v29/themes.xml");

        assertFalse(defaultTheme.contains("DayNight"));
        assertFalse(nightTheme.contains("DayNight"));
        assertTrue(defaultTheme.contains("Theme.Material3.Light.NoActionBar"));
        assertTrue(nightTheme.contains("Theme.Material3.Light.NoActionBar"));
        assertTrue(defaultTheme.contains("android:windowBackground"));
        assertTrue(nightTheme.contains("android:windowBackground"));
        assertFalse(defaultTheme.contains("android:forceDarkAllowed"));
        assertFalse(nightTheme.contains("android:forceDarkAllowed"));
        assertTrue(api29Theme.contains("android:forceDarkAllowed"));
        assertTrue(api29NightTheme.contains("android:forceDarkAllowed"));
    }

    private String readThemeFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}

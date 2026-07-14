package com.example.fonos_group13;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AccessibilityConfigurationTest {
    @Test
    public void contrastTokensMeetWcagThresholds() throws Exception {
        String colors = read("src/main/res/values/colors.xml");
        assertTrue(colors.contains("<color name=\"accent_text\">#4F6A56</color>"));
        assertTrue(colors.contains("<color name=\"control_stroke\">#7A867E</color>"));
        assertTrue(colors.contains("<color name=\"focus_indicator\">#1F2923</color>"));

        assertTrue(contrast("#4F6A56", "#F7F8F4") >= 4.5d);
        assertTrue(contrast("#4F6A56", "#DCE8DD") >= 4.5d);
        assertTrue(contrast("#7A867E", "#FFFFFF") >= 3.0d);
        assertTrue(contrast("#1F2923", "#5E7C66") >= 3.0d);
    }

    @Test
    public void layoutsDeclareCoreAccessibilityRelationships() throws Exception {
        String login = read("src/main/res/layout/activity_login.xml");
        String register = read("src/main/res/layout/activity_register.xml");
        String reader = read("src/main/res/layout/activity_reader.xml");
        String search = read("src/main/res/layout/activity_search.xml");

        assertTrue(login.contains("android:labelFor=\"@id/inputUsername\""));
        assertTrue(login.contains("android:labelFor=\"@id/inputPassword\""));
        assertTrue(register.contains("android:labelFor=\"@id/et_confirm_password\""));
        assertTrue(login.contains("android:accessibilityLiveRegion=\"assertive\""));
        assertTrue(reader.contains("@string/accessibility_play_audiobook"));
        assertTrue(reader.contains("@string/accessibility_previous_chapter"));
        assertTrue(search.contains("android:inputType=\"text\""));
    }

    @Test
    public void layoutsDoNotUseSubTwelveSpText() throws Exception {
        Path resourceRoot = Paths.get("src/main/res");
        try (java.util.stream.Stream<Path> files = Files.walk(resourceRoot)) {
            files.filter(path -> path.toString().endsWith(".xml"))
                    .filter(path -> path.getParent().getFileName().toString().startsWith("layout"))
                    .forEach(path -> {
                        try {
                            assertFalse(path + " contains 10sp text", read(path).contains("textSize=\"10sp\""));
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    });
        }
    }

    private static String read(String path) throws Exception {
        return read(Paths.get(path));
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static double contrast(String first, String second) {
        double firstLuminance = luminance(first);
        double secondLuminance = luminance(second);
        return (Math.max(firstLuminance, secondLuminance) + 0.05d)
                / (Math.min(firstLuminance, secondLuminance) + 0.05d);
    }

    private static double luminance(String color) {
        int red = Integer.parseInt(color.substring(1, 3), 16);
        int green = Integer.parseInt(color.substring(3, 5), 16);
        int blue = Integer.parseInt(color.substring(5, 7), 16);
        return 0.2126d * linear(red) + 0.7152d * linear(green) + 0.0722d * linear(blue);
    }

    private static double linear(int component) {
        double value = component / 255d;
        return value <= 0.04045d
                ? value / 12.92d
                : Math.pow((value + 0.055d) / 1.055d, 2.4d);
    }
}

package com.example.fonos_group13;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.data.core.RequestHandle;
import com.example.fonos_group13.data.core.Subscription;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AppCompositionContractTest {
    @Test
    public void manifestRegistersApplicationCompositionRoot() throws Exception {
        String manifest = readFile("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\".FonosApplication\""));
    }

    @Test
    public void applicationExposesAnAppScopedContainer() {
        assertNotNull(FonosApplication.class);
        assertNotNull(AppContainer.class);
    }

    @Test
    public void asyncContractsProvideNoOpCancellation() {
        Subscription subscription = Subscription.NONE;
        RequestHandle requestHandle = RequestHandle.NONE;

        subscription.cancel();
        requestHandle.cancel();

        assertTrue(subscription.isCancelled());
        assertTrue(requestHandle.isCancelled());
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}

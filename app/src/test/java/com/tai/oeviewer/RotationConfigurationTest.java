package com.tai.oeviewer;

import org.junit.Test;
import java.io.File;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import static org.junit.Assert.*;

public class RotationConfigurationTest {
    @Test public void composeHandlesSizeChangesWithoutRestartingNavigation() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Element activity = (Element) factory.newDocumentBuilder()
            .parse(new File("src/main/AndroidManifest.xml")).getElementsByTagName("activity").item(0);
        assertEquals(".MainActivity", activity.getAttributeNS("http://schemas.android.com/apk/res/android", "name"));
        assertTrue(Arrays.asList(activity.getAttributeNS("http://schemas.android.com/apk/res/android", "configChanges").split("\\|"))
            .containsAll(Arrays.asList("orientation", "screenSize", "smallestScreenSize", "screenLayout")));
    }
}

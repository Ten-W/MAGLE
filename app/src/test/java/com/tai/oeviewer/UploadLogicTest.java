package com.tai.oeviewer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class UploadLogicTest {
    @Test public void stopsReconnectOnlyForRejectedAuthorizationNotTemporaryNetworkFailure() {
        for (int status : new int[] {400, 401, 403})
            assertTrue(LibraryLogic.authorizationRequired(status));
        for (int status : new int[] {0, 408, 429, 500, 502, 503, 504})
            assertFalse(LibraryLogic.authorizationRequired(status));
    }
    @Test public void permitsLocalUploadsOnlyWithWriteGrant() throws Exception {
        assertTrue(UploadLogic.canUpload("LOCAL", false, true));
        assertFalse(UploadLogic.canUpload("LOCAL", false, false));
        assertFalse(UploadLogic.canUpload("ONEDRIVE", false, true));
        assertTrue(UploadLogic.canUpload("ONEDRIVE", true, false));
        assertTrue(UploadLogic.canUpload("SMB", false, false));
        assertTrue(UploadLogic.canUpload("WEBDAV", false, false));
        assertTrue(UploadLogic.canUpload("GOOGLE", true, true));
        assertTrue(UploadLogic.canUpload("DROPBOX", false, false));
        assertFalse(UploadLogic.canUpload("S3", true, true));
        JSONObject local = new JSONObject().put("source", "LOCAL").put("libraryId", "content://test/library");
        assertTrue(UploadLogic.matchesTarget(local, "LOCAL", "content://test/library", null));
        assertFalse(UploadLogic.matchesTarget(local, "LOCAL", "content://test/other", null));
        assertFalse(UploadLogic.matchesTarget(local, "SMB", "content://test/library", null));
    }

    @Test public void restrictsUnencryptedWebDavToLiteralPrivateAddresses() {
        assertTrue(UploadLogic.isPrivateHttpUrl("http://192.168.1.10:8090/dav/test/"));
        assertTrue(UploadLogic.isPrivateHttpUrl("http://10.0.0.1/"));
        assertTrue(UploadLogic.isPrivateHttpUrl("http://172.31.0.1/"));
        for (String url : java.util.List.of("http://8.8.8.8/", "http://172.32.0.1/", "http://example.com/", "http://192.168.1.256/", "http://user:pass@192.168.1.1/", "http://192.168.1.1/?token=x", "http://192.168.1.1.evil.com/"))
            assertFalse(url, UploadLogic.isPrivateHttpUrl(url));
    }

    @Test public void isolatesPendingUploadsBySourceAndLibrary() throws Exception {
        JSONObject old = new JSONObject().put("libraryId", "library").put("driveId", "drive");
        assertTrue(UploadLogic.matchesTarget(old, "ONEDRIVE", "library", "drive"));
        assertFalse(UploadLogic.matchesTarget(old, "SMB", "library", "drive"));
        JSONObject smb = new JSONObject().put("source", "SMB").put("libraryId", "library");
        assertTrue(UploadLogic.matchesTarget(smb, "SMB", "library", null));
        assertFalse(UploadLogic.matchesTarget(smb, "WEBDAV", "library", null));
        assertFalse(UploadLogic.matchesTarget(smb, "SMB", "other", null));
    }

    @Test public void createsEagleMetadataAndSafeNames() throws Exception {
        String name = UploadLogic.safeFileName("照片/测试.PNG");
        assertEquals("照片_测试.png", name);
        JSONObject meta = UploadLogic.metadata("ID1", name, 123, 640, 480, 1000,
                java.util.List.of("FOLDER1", "FOLDER2", "FOLDER1"), java.util.List.of(" tag ", "tag", "other", ""));
        assertEquals("照片_测试", meta.getString("name"));
        assertEquals("png", meta.getString("ext"));
        assertEquals("FOLDER1", meta.getJSONArray("folders").getString(0));
        assertEquals(2, meta.getJSONArray("folders").length());
        assertEquals("FOLDER2", meta.getJSONArray("folders").getString(1));
        assertEquals(2, meta.getJSONArray("tags").length());
        assertEquals("tag", meta.getJSONArray("tags").getString(0));
        assertEquals("other", meta.getJSONArray("tags").getString(1));
        assertFalse(meta.getBoolean("isDeleted"));
        assertFalse(meta.getBoolean("noThumbnail"));
        assertEquals(1000, meta.getLong("mtime"));
        JSONObject unfiled = UploadLogic.metadata("ID2", name, 1, 1, 1, 1, java.util.List.of(), java.util.List.of());
        assertEquals(0, unfiled.getJSONArray("folders").length());
        assertEquals(0, unfiled.getJSONArray("tags").length());
        String id = UploadLogic.newId();
        assertEquals(36, id.length());
        assertEquals(id.toLowerCase(java.util.Locale.ROOT), java.util.UUID.fromString(id).toString());
        assertNotEquals(id, UploadLogic.newId());
    }

    @Test public void validatesResumableUploadRanges() throws Exception {
        assertEquals(327680, UploadLogic.nextOffset(new JSONObject()
                .put("nextExpectedRanges", new JSONArray().put("327680-")), 1000000));
        try {
            UploadLogic.nextOffset(new JSONObject().put("nextExpectedRanges", new JSONArray().put("999-")), 999);
            fail("Invalid range accepted");
        } catch (IllegalArgumentException expected) { }
        try { UploadLogic.safeFileName("../"); fail("Missing extension accepted"); }
        catch (IllegalArgumentException expected) { }
    }
}

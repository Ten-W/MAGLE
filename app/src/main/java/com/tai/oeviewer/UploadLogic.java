package com.tai.oeviewer;

import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

final class UploadLogic {
    static boolean canUpload(String source, boolean connected, boolean localWritable) {
        return source.equals("ONEDRIVE") && connected || source.equals("LOCAL") && localWritable
                || source.equals("SMB") || source.equals("WEBDAV") || source.equals("GOOGLE") || source.equals("DROPBOX");
    }

    static boolean isPrivateHttpUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            if (!"http".equals(uri.getScheme()) || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) return false;
            String[] parts = uri.getHost().split("\\.");
            if (parts.length != 4) return false;
            int[] ip = new int[4];
            for (int i = 0; i < 4; i++) {
                if (!parts[i].matches("0|[1-9][0-9]{0,2}")) return false;
                ip[i] = Integer.parseInt(parts[i]);
                if (ip[i] > 255) return false;
            }
            return ip[0] == 10 || ip[0] == 192 && ip[1] == 168 || ip[0] == 172 && ip[1] >= 16 && ip[1] <= 31;
        } catch (RuntimeException invalid) { return false; }
    }

    static boolean matchesTarget(JSONObject state, String source, String libraryId, String driveId) {
        return source.equals(state.optString("source", "ONEDRIVE"))
                && libraryId != null && libraryId.equals(state.optString("libraryId"))
                && (!source.equals("ONEDRIVE") || java.util.Objects.equals(driveId, state.optString("driveId")));
    }

    static String newId() {
        // Eagle's loader accepts 13-character IDs or full 36-character UUIDs, not arbitrary lengths.
        return UUID.randomUUID().toString().toUpperCase(java.util.Locale.ROOT);
    }

    static String safeFileName(String raw) {
        String safe = raw.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (safe.endsWith(".")) safe = safe.substring(0, safe.length() - 1);
        if (safe.length() > 160) throw new IllegalArgumentException("文件名过长，请先重命名");
        int dot = safe.lastIndexOf('.');
        if (dot <= 0 || dot == safe.length() - 1) throw new IllegalArgumentException("图片文件需要扩展名");
        if (safe.substring(0, dot).replace(".", "").isEmpty())
            throw new IllegalArgumentException("图片名称无效");
        return safe.substring(0, dot) + "." + safe.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    static JSONObject metadata(String id, String fileName, long size, int width, int height,
                               long now, java.util.Collection<String> folderIds, java.util.Collection<String> tags) throws org.json.JSONException {
        int dot = fileName.lastIndexOf('.');
        JSONObject metadata = new JSONObject().put("id", id).put("name", fileName.substring(0, dot))
                .put("ext", fileName.substring(dot + 1)).put("size", size)
                .put("width", width).put("height", height)
                .put("annotation", "").put("url", "").put("isDeleted", false)
                .put("noThumbnail", false).put("modificationTime", now).put("lastModified", now)
                .put("mtime", now).put("btime", now);
        LibraryLogic.setMetadataList(metadata, "folders", folderIds);
        LibraryLogic.setMetadataList(metadata, "tags", tags);
        return metadata;
    }

    static long nextOffset(JSONObject response, long size) throws org.json.JSONException {
        JSONArray ranges = response.optJSONArray("nextExpectedRanges");
        if (ranges == null || ranges.length() == 0) throw new IllegalArgumentException("上传进度响应无效");
        long offset = Long.parseLong(ranges.getString(0).split("-", 2)[0]);
        if (offset < 0 || offset >= size) throw new IllegalArgumentException("上传进度超出文件范围");
        return offset;
    }

    private UploadLogic() {}
}

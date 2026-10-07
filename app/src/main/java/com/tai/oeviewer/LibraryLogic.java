package com.tai.oeviewer;

import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Collection;
import org.json.JSONObject;
import org.json.JSONArray;

final class LibraryLogic {
    static boolean authorizationRequired(int httpStatus) {
        return httpStatus == 400 || httpStatus == 401 || httpStatus == 403;
    }
    static int folderAssetCount(java.util.Map<String, ? extends Set<String>> assetsByFolder, Collection<String> folderIds) {
        Set<String> ids = new java.util.HashSet<>();
        for (String folderId : folderIds) {
            Set<String> assets = assetsByFolder.get(folderId);
            if (assets != null) ids.addAll(assets);
        }
        return ids.size();
    }
    static Integer positiveSetting(String input) {
        if (input == null || !input.matches("[0-9]+")) return null;
        try {
            int value = Integer.parseInt(input);
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) { return null; }
    }
    static boolean setRecycled(JSONObject metadata, String action) throws org.json.JSONException {
        if (!action.equals("回收站") && !action.equals("还原")) throw new IllegalArgumentException("不支持的回收站操作");
        boolean deleted = action.equals("回收站");
        metadata.put("isDeleted", deleted);
        return deleted;
    }

    static float galleryTopPadding(float statusBarHeight) {
        return statusBarHeight + 60f;
    }
    static float searchPillWidth(float barWidth, float reservedWidth) {
        return Math.max(48f, Math.min(360f, barWidth - reservedWidth));
    }
    static void trimThumbnailCache(java.io.File directory, long maxBytes) {
        if (maxBytes < 0) throw new IllegalArgumentException("Negative cache limit");
        java.io.File[] files = directory.listFiles(file -> file.isFile() && file.getName().endsWith(".thumb"));
        if (files == null) return;
        long total = java.util.Arrays.stream(files).mapToLong(java.io.File::length).sum();
        if (total <= maxBytes) return;
        java.util.Arrays.sort(files, java.util.Comparator.comparingLong(java.io.File::lastModified));
        for (java.io.File file : files) {
            if (total <= maxBytes) break;
            long size = file.length();
            if (file.delete()) total -= size;
        }
    }
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif", "svg");

    static int[] svgRenderSize(float width, float height, int edge) {
        if (!Float.isFinite(width) || !Float.isFinite(height) || width <= 0 || height <= 0) {
            width = 512; height = 512;
        }
        int limit = Math.min(2048, Math.max(1, edge));
        double longest = Math.max(width, height);
        return new int[] { Math.max(1, (int) Math.round(width / longest * limit)),
                Math.max(1, (int) Math.round(height / longest * limit)) };
    }

    static boolean isViewableImage(String extension) {
        return extension != null && IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    static boolean belongsToFolder(Set<String> folders, String selectedFolderId) {
        return selectedFolderId == null || folders.contains(selectedFolderId);
    }

    static Set<String> refreshItemIds(Collection<String> manifestIds, JSONArray unlisted) throws org.json.JSONException {
        Set<String> ids = new LinkedHashSet<>(manifestIds);
        for (int i = 0; i < unlisted.length(); i++) ids.add(unlisted.getString(i));
        return ids;
    }

    static boolean belongsToAnyFolder(Set<String> folders, Set<String> includedIds) {
        return !java.util.Collections.disjoint(folders, includedIds);
    }

    static java.util.SortedMap<String, List<String>> tagSections(Collection<String> tags) {
        java.util.SortedMap<String, List<String>> sections = new java.util.TreeMap<>(
                (a, b) -> a.equals(b) ? 0 : a.equals("#") ? 1 : b.equals("#") ? -1 : a.compareTo(b));
        List<String> sorted = new java.util.ArrayList<>(new LinkedHashSet<>(tags));
        sorted.removeIf(tag -> tag == null || tag.trim().isEmpty());
        sorted.sort(String.CASE_INSENSITIVE_ORDER.thenComparing(java.util.Comparator.naturalOrder()));
        for (String tag : sorted) {
            char first = Character.toUpperCase(tag.trim().charAt(0));
            // ponytail: non-Latin prefixes use #; add native ICU pinyin grouping only if requested.
            String letter = first >= 'A' && first <= 'Z' ? String.valueOf(first) : "#";
            sections.computeIfAbsent(letter, ignored -> new java.util.ArrayList<>()).add(tag);
        }
        return sections;
    }

    static List<JSONObject> tagGroups(JSONObject metadata) throws org.json.JSONException {
        List<JSONObject> result = new java.util.ArrayList<>();
        JSONArray groups = metadata.optJSONArray("tagsGroups");
        Set<String> ids = new LinkedHashSet<>();
        if (groups == null) return result;
        for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.optJSONObject(i);
            if (group == null || group.optString("name").trim().isEmpty()) continue;
            String id = group.optString("id", "group-" + i);
            if (!ids.add(id)) continue;
            Set<String> tags = new LinkedHashSet<>();
            JSONArray values = group.optJSONArray("tags");
            if (values != null) for (int j = 0; j < values.length(); j++) {
                Object value = values.opt(j);
                if (value instanceof String && !((String) value).trim().isEmpty()) tags.add((String) value);
            }
            result.add(new JSONObject().put("id", id).put("name", group.getString("name"))
                    .put("color", group.optString("color")).put("tags", new JSONArray(tags)));
        }
        return result;
    }

    static void renameTagGroup(JSONObject metadata, String id, String rawName) throws org.json.JSONException {
        String name = validTagName(rawName);
        JSONArray groups = metadata.optJSONArray("tagsGroups");
        if (groups != null) for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.optJSONObject(i);
            if (group != null && id.equals(group.optString("id", "group-" + i))) {
                group.put("name", name);
                return;
            }
        }
        throw new IllegalArgumentException("标签组已不存在，请更新索引");
    }

    static List<String> renameTagReferences(JSONObject metadata, String oldName, String rawName) throws org.json.JSONException {
        String name = validTagName(rawName);
        List<String> values = new java.util.ArrayList<>();
        JSONArray tags = metadata.optJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.length(); i++) values.add(oldName.equals(tags.getString(i)) ? name : tags.getString(i));
            metadata.put("tags", new JSONArray(new LinkedHashSet<>(values)));
        }
        JSONArray groups = metadata.optJSONArray("tagsGroups");
        if (groups != null) for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.optJSONObject(i);
            if (group != null) renameTagReferences(group, oldName, name);
        }
        return new java.util.ArrayList<>(new LinkedHashSet<>(values));
    }

    static String validTagName(String raw) {
        String name = raw.trim();
        if (name.isEmpty() || name.length() > 150 || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("标签名称为空、过长或含控制字符");
        return name;
    }

    static String thumbnailCacheKey(String libraryId, String itemId, long mtime) {
        return Integer.toUnsignedString(libraryId.hashCode()) + "-"
                + Integer.toUnsignedString(itemId.hashCode()) + "-" + mtime + ".thumb";
    }

    static boolean shouldShowChildren(String folderId, Set<String> expandedFolderIds) {
        return expandedFolderIds.contains(folderId);
    }

    static float resizeCell(float current, float zoom, float min, float max) {
        return Math.max(min, Math.min(max, current * zoom));
    }

    static boolean usesSideInspector(float widthDp) {
        return widthDp >= 700f;
    }

    static float refreshContentOffset(float fraction, float thresholdPx) {
        return Float.isFinite(fraction) ? Math.max(0f, Math.min(2f, fraction)) * thresholdPx : 0f;
    }

    static String refreshResultMessage(int remaining) {
        return remaining == 0 ? "刷新完成 · 索引已更新" : "刷新未完成 · 仍有 " + remaining + " 项待读取";
    }

    static int columnCount(float widthDp, float cellDp) {
        // Matches the viewer's 12 dp padding and 8 dp gaps.
        boolean tablet = widthDp >= 600f;
        return Math.max(tablet ? 2 : 1, Math.min(tablet ? 8 : 6,
                (int) ((widthDp - 16f) / (cellDp + 8f) + .001f)));
    }

    static float resizeGalleryCell(float widthDp, float current, float zoom, float phoneMax) {
        if (widthDp < 600f) return resizeCell(current, zoom, 32f, phoneMax);
        float min = (widthDp - 16f) / 8f - 8f;
        float max = (widthDp - 16f) / 2f - 8f;
        return resizeCell(resizeCell(current, 1f, min, max), zoom, min, max);
    }

    static int folderColumnCount(float widthDp, float scale) {
        return widthDp < 600f ? Math.max(2, Math.min(6, Math.round(4f / scale)))
                : Math.max(2, Math.min(16, (int) (widthDp / (120f * scale))));
    }

    static String validName(String raw) {
        String name = raw.trim();
        if (name.isEmpty() || name.length() > 150 || name.endsWith(".")
                || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*")
                || name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?"))
            throw new IllegalArgumentException("名称为空、过长或含不允许的字符");
        return name;
    }

    static void changeFolders(JSONObject metadata, Set<String> ids, String parentId, String name) throws org.json.JSONException {
        if (ids.isEmpty()) throw new IllegalArgumentException("没有选中的文件夹");
        JSONArray roots = metadata.getJSONArray("folders");
        List<JSONObject> selected = new java.util.ArrayList<>();
        for (String id : ids) {
            JSONObject node = findFolder(roots, id);
            if (node == null) throw new IllegalArgumentException("文件夹已被其他设备修改，请更新索引");
            JSONArray children = node.optJSONArray("children");
            if (children != null) {
                if (parentId != null && findFolder(children, parentId) != null)
                    throw new IllegalArgumentException("不能移到自己的子文件夹");
                for (String other : ids) if (findFolder(children, other) != null)
                    throw new IllegalArgumentException("不能同时移动父文件夹和子文件夹");
            }
            selected.add(node);
        }
        if (name != null) {
            if (ids.size() != 1) throw new IllegalArgumentException("重命名只能选择一个文件夹");
            selected.get(0).put("name", validName(name));
            return;
        }
        if (parentId != null && ids.contains(parentId)) throw new IllegalArgumentException("不能移到自己");
        JSONObject parent = parentId == null ? null : findFolder(roots, parentId);
        if (parentId != null && parent == null) throw new IllegalArgumentException("目标文件夹已不存在");
        JSONArray destination = roots;
        if (parent != null) {
            destination = parent.optJSONArray("children");
            if (destination == null) { destination = new JSONArray(); parent.put("children", destination); }
        }
        detachFolders(roots, ids);
        for (JSONObject node : selected) destination.put(node);
    }

    private static JSONObject findFolder(JSONArray nodes, String id) {
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.optJSONObject(i);
            if (node == null) continue;
            if (id.equals(node.optString("id"))) return node;
            JSONArray children = node.optJSONArray("children");
            JSONObject found = children == null ? null : findFolder(children, id);
            if (found != null) return found;
        }
        return null;
    }

    private static void detachFolders(JSONArray nodes, Set<String> ids) {
        for (int i = nodes.length() - 1; i >= 0; i--) {
            JSONObject node = nodes.optJSONObject(i);
            if (node == null) continue;
            if (ids.contains(node.optString("id"))) nodes.remove(i);
            else if (node.optJSONArray("children") != null) detachFolders(node.optJSONArray("children"), ids);
        }
    }

    static boolean isTransientThumbnailFailure(int httpStatus) {
        return httpStatus == 0 || httpStatus == 429 || (httpStatus >= 500 && httpStatus <= 599);
    }

    static boolean isConfirmedEmptyDirectory(int metadataStatus, int childCount) {
        return metadataStatus == 404 && childCount == 0;
    }

    static List<Integer> prefetchIndices(int count, int first, int last) {
        List<Integer> result = new java.util.ArrayList<>();
        if (first < 0 || last < first || last >= count) return result;
        for (int offset = 1; offset <= 6; offset++) {
            if (last + offset < count) result.add(last + offset);
            if (first - offset >= 0) result.add(first - offset);
        }
        return result;
    }

    static int thumbnailEdge(int width, int height, int cellWidth, boolean naturalRatio) {
        if (width <= 0 || height <= 0) return 600;
        double ratio = naturalRatio ? Math.max(.55, Math.min(1.8, (double) width / height)) : 1;
        double scale = Math.max((double) cellWidth / width, cellWidth / ratio / height);
        // Bucket sizes so small layout changes reuse decoded images; cover both axes for Crop.
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(1, Math.max(width, height) * scale) / 128) * 128);
    }

    static int bitmapSampleSize(int width, int height, int edge) {
        if (edge <= 0) throw new IllegalArgumentException("Invalid bitmap target");
        int sample = 1;
        while (Math.max(width, height) / (sample * 2L) >= edge) sample *= 2;
        return sample;
    }

    static boolean isSharedImage(String scheme, String mimeType) {
        return "content".equals(scheme) && mimeType != null && mimeType.startsWith("image/");
    }

    static String shareMimeType(List<String> types) {
        if (types.isEmpty()) return "*/*";
        if (types.stream().allMatch(types.get(0)::equals)) return types.get(0);
        String family = types.get(0).split("/", 2)[0];
        return types.stream().allMatch(type -> type.startsWith(family + "/")) ? family + "/*" : "*/*";
    }

    static Set<String> selectionRange(List<String> ids, Set<String> initial, int anchor, int end) {
        Set<String> result = new LinkedHashSet<>(initial);
        if (anchor < 0 || end < 0 || anchor >= ids.size() || end >= ids.size()) return result;
        result.addAll(ids.subList(Math.min(anchor, end), Math.max(anchor, end) + 1));
        return result;
    }

    static void setMetadataList(JSONObject metadata, String field, Collection<String> values) throws org.json.JSONException {
        if (!field.equals("tags") && !field.equals("folders")) throw new IllegalArgumentException("Unsupported metadata field");
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) if (value != null && !value.trim().isEmpty()) unique.add(value.trim());
        metadata.put(field, new JSONArray(unique));
    }

    static List<String> appendTags(JSONObject metadata, Collection<String> tags) throws org.json.JSONException {
        List<String> merged = new java.util.ArrayList<>();
        JSONArray existing = metadata.optJSONArray("tags");
        if (existing != null) for (int i = 0; i < existing.length(); i++) merged.add(existing.getString(i));
        merged.addAll(tags);
        setMetadataList(metadata, "tags", merged);
        List<String> result = new java.util.ArrayList<>();
        JSONArray saved = metadata.getJSONArray("tags");
        for (int i = 0; i < saved.length(); i++) result.add(saved.getString(i));
        return result;
    }

    static List<Integer> paletteColors(JSONObject metadata) {
        List<Integer> colors = new java.util.ArrayList<>();
        JSONArray palettes = metadata.optJSONArray("palettes");
        if (palettes == null) return colors;
        for (int i = 0; i < Math.min(10, palettes.length()); i++) {
            JSONObject entry = palettes.optJSONObject(i);
            JSONArray rgb = entry == null ? null : entry.optJSONArray("color");
            if (rgb == null || rgb.length() != 3) continue;
            int r = rgb.optInt(0, -1), g = rgb.optInt(1, -1), b = rgb.optInt(2, -1);
            if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) continue;
            colors.add(0xff000000 | (r << 16) | (g << 8) | b);
        }
        return colors;
    }

    private LibraryLogic() {}
}

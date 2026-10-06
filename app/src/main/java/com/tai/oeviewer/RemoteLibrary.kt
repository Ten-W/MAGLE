package com.tai.oeviewer

import android.content.Context
import android.net.Uri
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.WriteMode
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.Metadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.CreateFolderErrorException
import com.dropbox.core.v2.files.UploadErrorException
import com.dropbox.core.v2.files.UploadSessionCursor
import io.minio.GetObjectArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import org.json.JSONObject
import java.io.File
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import java.net.URLEncoder

internal fun validS3Endpoint(value: String, debug: Boolean): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.host != null && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
        (uri.scheme == "https" || debug && uri.scheme == "http" && uri.host == "127.0.0.1")
}.getOrDefault(false)

/** One connection is immutable; an index task must never read from a newly selected library. */
internal class RemoteLibrary(
    val kind: String,
    private val config: JSONObject,
    private val secret: JSONObject,
    private val googleBase: String = "https://www.googleapis.com",
    private val dropboxClient: DbxClientV2? = null,
    private val googleToken: () -> String
) {
    private val root = config.getString("root").trimEnd('/')
    private val dropbox by lazy {
        dropboxClient ?: DbxClientV2(DbxRequestConfig.newBuilder("MAGLE/${BuildConfig.VERSION_NAME}").build(), DbxCredential(
            secret.optString("accessToken"), 0L, secret.getString("refreshToken"), config.getString("appKey")
        ))
    }
    private val smbContext by lazy {
        BaseContext(PropertyConfiguration(Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB202")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
            setProperty("jcifs.smb.client.responseTimeout", "30000")
            setProperty("jcifs.smb.client.connTimeout", "15000")
        })).withCredentials(NtlmPasswordAuthenticator(
            config.optString("domain"), config.optString("user"), secret.optString("password")
        ))
    }
    private val s3 by lazy {
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory")
        MinioClient.builder().endpoint(config.getString("endpoint"))
            .credentials(config.getString("accessKey"), secret.getString("secretKey"))
            .region(config.optString("region").ifBlank { "us-east-1" }).build()
    }
    private val googleIds = ConcurrentHashMap<String, String>()
    private val googleClient by lazy { OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).writeTimeout(90, TimeUnit.SECONDS).build() }

    fun googleEmail(): String = JSONObject(String(googleRequest(
        "https://www.googleapis.com/drive/v3/about?fields=user(emailAddress)"
    ), Charsets.UTF_8)).getJSONObject("user").getString("emailAddress")

    fun read(path: String): ByteArray = when (kind) {
        "DROPBOX" -> dropbox.files().download(fullPath(path)).use { it.inputStream.readBytes() }
        "GOOGLE" -> googleRequest("https://www.googleapis.com/drive/v3/files/${urlEncode(googleId(path))}?alt=media&supportsAllDrives=true")
        "SMB" -> smbFile(path).use { it.openInputStream().use { input -> input.readBytes() } }
        "S3" -> s3.getObject(GetObjectArgs.builder().bucket(config.getString("bucket"))
            .`object`(fullPath(path).trimStart('/')).build()).use { it.readBytes() }
        else -> error("未知来源")
    }

    fun write(path: String, bytes: ByteArray) {
        when (kind) {
            "DROPBOX" -> dropbox.files().uploadBuilder(fullPath(path)).withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(bytes.inputStream())
            "GOOGLE" -> googleRequest(
                "https://www.googleapis.com/upload/drive/v3/files/${urlEncode(googleId(path))}?uploadType=media",
                "PATCH", bytes
            )
            "SMB" -> smbFile(path).use { it.openOutputStream().use { output -> output.write(bytes) } }
            "S3" -> s3.putObject(PutObjectArgs.builder().bucket(config.getString("bucket"))
                .`object`(fullPath(path).trimStart('/')).stream(bytes.inputStream(), bytes.size.toLong(), -1)
                .contentType("application/json").build())
            else -> error("未知来源")
        }
    }

    fun mutateSmbJson(path: String, mutate: (JSONObject) -> Unit): JsonChange {
        check(kind == "SMB")
        // Native SMB share mode denies writers/deletion for the entire read-modify-write.
        return smbFile(path).use { target ->
            check(target.exists()) { "素材库文件不存在：$path" }
            target.openRandomAccess("rw", jcifs.SmbConstants.FILE_SHARE_READ).use { file ->
                check(file.length() in 1..(64L * 1024 * 1024)) { "素材库信息文件大小异常" }
                val old = ByteArray(file.length().toInt()).also { file.readFully(it) }
                val json = JSONObject(String(old, Charsets.UTF_8))
                mutate(json)
                val bytes = json.toString().toByteArray(Charsets.UTF_8)
                try { file.seek(0); file.write(bytes); file.setLength(bytes.size.toLong()) }
                catch (error: Exception) {
                    try { file.seek(0); file.write(old); file.setLength(old.size.toLong()) }
                    catch (_: Exception) { throw java.io.IOException("SMB 写入中断且无法回退，请检查库文件", error) }
                    throw error
                }
                JsonChange(old, bytes)
            }
        }
    }

    fun mutateCloudJson(path: String, merge: Boolean, mutate: (JSONObject) -> Unit): JsonChange {
        check(kind == "DROPBOX" || kind == "GOOGLE")
        return updateVersionedJson(read = {
            if (kind == "DROPBOX") dropbox.files().download(fullPath(path)).use {
                VersionedJson(it.inputStream.readBytes(), it.result.rev)
            } else {
                // v2 exposes the file ETag; use the same API version for the conditional update.
                val file = googleJson("/drive/v2/files/${urlEncode(googleId(path))}?fields=id,etag&supportsAllDrives=true")
                val etag = file.optString("etag")
                check(etag.isNotBlank() && etag != "*") { "Google Drive 未返回文件版本，未执行无保护写入" }
                VersionedJson(read(path), etag)
            }
        }, write = { snapshot, bytes ->
            if (kind == "DROPBOX") {
                try {
                    dropbox.files().uploadBuilder(fullPath(path)).withMode(WriteMode.update(snapshot.version))
                        .withAutorename(false).withStrictConflict(true).uploadAndFinish(bytes.inputStream())
                } catch (error: UploadErrorException) {
                    if (error.errorValue.isPath && error.errorValue.pathValue.reason.isConflict) throw LibraryConflict()
                    throw error
                }
            } else googleCall("/upload/drive/v2/files/${urlEncode(googleId(path))}?uploadType=media&supportsAllDrives=true",
                "PUT", bytes.toRequestBody("application/json".toMediaType()), snapshot.version)
        }, merge = merge, mutate = mutate)
    }

    fun assetIds(): Set<String> = when (kind) {
        "DROPBOX" -> buildSet {
            var page = dropbox.files().listFolder(fullPath("images"))
            while (true) {
                page.entries.filterIsInstance<FolderMetadata>().forEach {
                    if (it.name.endsWith(".info", true)) add(it.name.dropLast(5))
                }
                if (!page.hasMore) break
                page = dropbox.files().listFolderContinue(page.cursor)
            }
        }
        "GOOGLE" -> googleChildren(googleId("images")).filter { it.optString("mimeType") == FOLDER }
            .mapNotNull { it.optString("name").takeIf { name -> name.endsWith(".info", true) }?.dropLast(5) }.toSet()
        "SMB" -> smbFile("images/").use { directory ->
            directory.listFiles().mapNotNull { file -> file.use {
                val name = it.name.trimEnd('/')
                name.takeIf { _ -> it.isDirectory && name.endsWith(".info", true) }?.dropLast(5)
            } }.toSet()
        }
        "S3" -> s3.listObjects(ListObjectsArgs.builder().bucket(config.getString("bucket"))
            .prefix(fullPath("images/").trimStart('/')).recursive(false).build())
            .mapNotNull { result ->
                val item = result.get()
                val name = item.objectName().trimEnd('/').substringAfterLast('/')
                name.takeIf { item.isDir && it.endsWith(".info", true) }?.dropLast(5)
            }.toSet()
        else -> error("未知来源")
    }

    fun createAssetDirectory(id: String, state: JSONObject, save: () -> Unit) {
        when (kind) {
            "SMB" -> smbFile("images/$id.info/").use { directory ->
                if (!directory.exists()) directory.mkdir()
                check(directory.isDirectory) { "素材目录不是文件夹" }
            }
            "DROPBOX" -> {
                for (path in listOf("images", "images/$id.info")) {
                    val existing = dropboxMetadata(path)
                    if (existing == null) {
                        try { dropbox.files().createFolderV2(fullPath(path), false) }
                        catch (error: CreateFolderErrorException) {
                            if (dropboxMetadata(path) !is FolderMetadata) throw error
                        }
                    } else check(existing is FolderMetadata) { "素材目录不是文件夹" }
                }
            }
            "GOOGLE" -> {
                ensureGoogleFolder("images", state, save)
                ensureGoogleFolder("images/$id.info", state, save)
            }
            else -> error("此来源暂不支持上传")
        }
    }

    fun uploadFile(path: String, file: File, state: JSONObject, save: () -> Unit) {
        if (kind == "GOOGLE") { uploadGoogleFile(path, file, state, save); return }
        if (kind == "DROPBOX") { uploadDropboxFile(path, file); return }
        check(kind == "SMB")
        smbFile(path).use { target ->
            target.openOutputStream().use { output -> file.inputStream().use { it.copyTo(output) } }
            check(target.length() == file.length()) { "上传后的文件大小不一致" }
        }
    }

    private fun dropboxMetadata(path: String): Metadata? = try { dropbox.files().getMetadata(fullPath(path)) }
        catch (error: GetMetadataErrorException) {
            if (error.errorValue.isPath && error.errorValue.pathValue.isNotFound) null else throw error
        }

    private fun uploadDropboxFile(path: String, file: File) {
        val hash = cloudFileHash(file, true)
        val existing = dropboxMetadata(path)
        if (existing != null) {
            check(existing is FileMetadata) { "同名路径不是文件" }
            verifyCloudUpload(existing.size, existing.contentHash, file, hash)
            return
        }
        // ponytail: failed sessions restart this file; completed files are reused after hash verification.
        val session = dropbox.files().uploadSessionStart().uploadAndFinish(byteArrayOf().inputStream()).sessionId
        var offset = 0L
        val commit = dropboxCommit(fullPath(path))
        val uploaded = file.inputStream().use { input ->
            while (true) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                val chunk = ByteArray(minOf(8L * 1024 * 1024, file.length() - offset).toInt())
                java.io.DataInputStream(input).readFully(chunk)
                val cursor = UploadSessionCursor(session, offset)
                if (offset + chunk.size == file.length())
                    return@use dropbox.files().uploadSessionFinish(cursor, commit).uploadAndFinish(chunk.inputStream())
                check(chunk.isNotEmpty()) { "本地暂存文件大小改变" }
                dropbox.files().uploadSessionAppendV2(cursor).uploadAndFinish(chunk.inputStream())
                offset += chunk.size
            }
            @Suppress("UNREACHABLE_CODE") error("上传未完成")
        }
        verifyCloudUpload(uploaded.size, uploaded.contentHash, file, hash)
    }

    private fun googleJson(path: String, method: String = "GET", json: JSONObject? = null): JSONObject =
        JSONObject(String(googleCall(path, method, json?.toString()?.toRequestBody("application/json".toMediaType())), Charsets.UTF_8))

    private fun reservedGoogleId(path: String, state: JSONObject, save: () -> Unit): String {
        val ids = state.optJSONObject("googleUploadIds") ?: JSONObject().also { state.put("googleUploadIds", it) }
        if (!ids.has(path)) {
            ids.put(path, googleJson("/drive/v3/files/generateIds?count=1&space=drive&type=files").getJSONArray("ids").getString(0))
            save() // Reserve before creation so retries cannot create duplicate files/folders.
        }
        return ids.getString(path)
    }

    private fun ensureGoogleFolder(path: String, state: JSONObject, save: () -> Unit) {
        val parent = googleId(path.substringBeforeLast('/', ""))
        val name = path.substringAfterLast('/')
        val matches = googleChildren(parent, name)
        check(matches.size <= 1) { "同名目录不唯一：$path" }
        if (matches.isNotEmpty()) {
            val item = matches.single()
            check(item.optString("mimeType") == FOLDER) { "素材目录不是文件夹" }
            val reserved = state.optJSONObject("googleUploadIds")?.optString(path).orEmpty()
            if (reserved.isNotEmpty() && reserved != item.getString("id")) throw LibraryConflict("同名目录不是本次上传创建的目录")
            googleIds[path] = item.getString("id")
            return
        }
        val id = reservedGoogleId(path, state, save)
        val body = JSONObject().put("id", id).put("name", name).put("mimeType", FOLDER).put("parents", JSONArray(listOf(parent)))
        try { googleJson("/drive/v3/files?fields=id&supportsAllDrives=true", "POST", body) }
        catch (error: GoogleDriveFailure) { if (error.code != 409) throw error }
        val item = googleJson("/drive/v3/files/${urlEncode(id)}?fields=id,name,mimeType,parents&supportsAllDrives=true")
        check(item.optString("name") == name && item.optString("mimeType") == FOLDER && item.getJSONArray("parents").getString(0) == parent) { "新目录身份校验失败" }
        googleIds[path] = id
    }

    private fun uploadGoogleFile(path: String, file: File, state: JSONObject, save: () -> Unit) {
        val parent = googleId(path.substringBeforeLast('/'))
        val name = path.substringAfterLast('/')
        val hash = cloudFileHash(file, false)
        val id = reservedGoogleId(path, state, save)
        val matches = googleChildren(parent, name)
        if (matches.any { it.getString("id") != id }) throw LibraryConflict("目标文件名已存在，未创建重复文件或覆盖")
        fun verify() {
            val item = googleJson("/drive/v3/files/${urlEncode(id)}?fields=id,name,size,md5Checksum,parents&supportsAllDrives=true")
            check(item.getString("name") == name && item.getJSONArray("parents").getString(0) == parent) { "上传文件身份校验失败" }
            verifyCloudUpload(item.optLong("size", -1), item.optString("md5Checksum"), file, hash)
            googleIds[path] = id
        }
        if (matches.isNotEmpty()) { verify(); return }
        val metadata = JSONObject().put("id", id).put("name", name).put("parents", JSONArray(listOf(parent)))
        // ponytail: stream one multipart request, retry the entire file; add resumable chunks if needed for large uploads.
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(file.asRequestBody("application/octet-stream".toMediaType())).build()
        try { googleCall("/upload/drive/v3/files?uploadType=multipart&fields=id&supportsAllDrives=true", "POST", body) }
        catch (error: GoogleDriveFailure) { if (error.code != 409) throw error }
        verify()
    }

    private fun fullPath(path: String) = "$root/${path.trimStart('/')}"
    private fun smbFile(path: String) = SmbFile(
        Uri.decode(root) + "/" + path, smbContext
    )

    private fun googleId(path: String): String {
        if (path.isBlank()) return root
        googleIds[path]?.let { return it }
        val parentPath = path.substringBeforeLast('/', "")
        val name = path.substringAfterLast('/')
        val matches = googleChildren(googleId(parentPath), name)
        check(matches.size == 1) { "找不到唯一文件：$path（${matches.size} 个结果）" }
        return matches.single().getString("id").also { googleIds[path] = it }
    }

    private fun googleChildren(parent: String, name: String? = null): List<JSONObject> = buildList {
        fun escape(value: String) = value.replace("\\", "\\\\").replace("'", "\\'")
        val query = "'${escape(parent)}' in parents and trashed = false" +
            (name?.let { " and name = '${escape(it)}'" } ?: "")
        var token = ""
        do {
            val url = "https://www.googleapis.com/drive/v3/files?q=${urlEncode(query)}" +
                "&pageSize=1000&fields=nextPageToken,files(id,name,mimeType)&supportsAllDrives=true&includeItemsFromAllDrives=true" +
                if (token.isBlank()) "" else "&pageToken=${urlEncode(token)}"
            val page = JSONObject(String(googleRequest(url), Charsets.UTF_8))
            val files = page.getJSONArray("files")
            for (i in 0 until files.length()) add(files.getJSONObject(i))
            token = page.optString("nextPageToken")
        } while (token.isNotBlank())
    }

    private fun urlEncode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun googleRequest(url: String, method: String = "GET", body: ByteArray? = null): ByteArray =
        googleCall(url.removePrefix("https://www.googleapis.com"), method, body?.toRequestBody("application/json".toMediaType()))

    private class GoogleDriveFailure(val code: Int) : java.io.IOException("Google Drive HTTP $code，请检查网络及读写权限")

    private fun googleCall(path: String, method: String = "GET", body: RequestBody? = null, etag: String? = null): ByteArray {
        val request = Request.Builder().url(googleBase + path).method(method, body)
            .header("Authorization", "Bearer ${googleToken()}").apply { etag?.let { header("If-Match", it) } }.build()
        return googleClient.newCall(request).execute().use {
            if (it.code == 412) throw LibraryConflict()
            if (!it.isSuccessful) throw GoogleDriveFailure(it.code)
            it.body.bytes()
        }
    }

    companion object {
        private const val FOLDER = "application/vnd.google-apps.folder"
        fun load(context: Context, id: String, googleToken: () -> String): RemoteLibrary {
            val config = JSONObject(context.getSharedPreferences("remote-libraries", Context.MODE_PRIVATE)
                .getString(id, null) ?: error("连接配置丢失，请重新添加素材库"))
            val secret = JSONObject(TokenStore.loadSecret(context, "remote-$id") ?: "{}")
            return RemoteLibrary(config.getString("kind"), config, secret, googleToken = googleToken)
        }
        fun save(context: Context, config: JSONObject, secret: JSONObject): String {
            val id = config.getString("kind") + ":" +
                config.optString("endpoint") + ":" + config.optString("bucket") + ":" + config.getString("root")
            TokenStore.saveSecret(context, "remote-$id", secret.toString())
            context.getSharedPreferences("remote-libraries", Context.MODE_PRIVATE).edit().putString(id, config.toString()).apply()
            return id
        }
    }
}

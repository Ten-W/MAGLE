package com.tai.oeviewer

import okhttp3.Credentials
import okhttp3.Request
import okhttp3.RequestBody
import java.net.URI

internal fun buildWebDavRequest(url: String, user: String?, password: String?, allowHttp: Boolean,
    method: String = "GET", body: RequestBody? = null, depth: String? = null, ifMatch: String? = null): Request {
    val uri = URI.create(url)
    require(uri.scheme == "https" || allowHttp && UploadLogic.isPrivateHttpUrl(url)) { "不允许未加密的连接" }
    require(uri.host != null && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) { "WebDAV 地址无效" }
    return Request.Builder().url(url).method(method, body).apply {
        if (!user.isNullOrEmpty()) header("Authorization", Credentials.basic(user, password.orEmpty(), Charsets.UTF_8))
        depth?.let { header("Depth", it) }
        ifMatch?.let { header("If-Match", requireStrongEtag(it)) }
    }.build()
}

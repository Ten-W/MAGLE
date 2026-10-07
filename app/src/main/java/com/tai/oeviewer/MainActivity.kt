package com.tai.oeviewer

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.accounts.Account
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.util.LruCache
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.state.ToggleableState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.xml.parsers.DocumentBuilderFactory
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import androidx.compose.ui.text.input.PasswordVisualTransformation
import java.security.SecureRandom
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    companion object {
        private val CLIENT_ID = BuildConfig.ONEDRIVE_CLIENT_ID
        private const val LEGACY_CLIENT_ID = "d112d645-97e6-4b00-a74e-7d4b4eabab79"
        private const val LOGIN = "https://login.microsoftonline.com/consumers/oauth2/v2.0"
        private const val GRAPH = "https://graph.microsoft.com/v1.0"
        private const val SCOPE = "offline_access Files.ReadWrite User.Read"
        private val REMOTE_SOURCES = setOf(LibrarySource.DROPBOX, LibrarySource.GOOGLE, LibrarySource.SMB, LibrarySource.S3)
    }

    private val work: ExecutorService = Executors.newSingleThreadExecutor()
    private val indexWorkers: ExecutorService = Executors.newFixedThreadPool(8)
    @Volatile private var galleryScrolling = false
    @Volatile private var pauseIndexForEdit = false
    private var tagRenameStatus by mutableStateOf("")
    private var attemptedTagResume = false

    private fun queueEdit(action: () -> Unit) {
        val resume = indexing
        if (resume) pauseIndexForEdit = true
        ContextCompat.startForegroundService(this, Intent(this, IndexingService::class.java).putExtra("editing", true))
        work.execute {
            try { action() }
            catch (error: Exception) { showError("素材库更新中断", error) }
            finally {
                pauseIndexForEdit = false
                stopService(Intent(this, IndexingService::class.java))
                runOnUiThread {
                    batchBusy = false
                    if (resume && !isDestroyed) {
                        indexing = false
                        libraryId?.let { loadLibrary(it, libraryName.orEmpty(), false) }
                    }
                }
            }
        }
    }

    private fun editSnapshot() = loadIndexCache().values.toList().ifEmpty { indexedAssets }
    private val priorityDownloads: ExecutorService = Executors.newSingleThreadExecutor()
    private val pickerStack = ArrayDeque<CloudFolder>()
    private val thumbnailCacheLock = Any()
    private val thumbnailRequests = Semaphore(4)
    // ponytail: bounded striped locks coalesce same-file requests; unrelated collisions may briefly wait.
    private val thumbnailLocks = Array(64) { kotlinx.coroutines.sync.Mutex() }
    private var thumbnailMaintenance: kotlinx.coroutines.Job? = null
    private val bitmapCache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val tokenLock = Any()
    private val localLibraryPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) { cancelNewSource(); return@registerForActivityResult }
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val root = DocumentFile.fromTreeUri(this, uri) ?: return@registerForActivityResult
        if (finishNewSource(SavedLibrary(uri.toString(), root.name ?: "本地 Eagle 素材库",
                LibrarySource.LOCAL, null, uri.toString(), null, null))) return@registerForActivityResult
        source = LibrarySource.LOCAL
        localLibraryUri = uri
        loadLibrary(uri.toString(), root.name ?: "本地 Eagle 素材库", true)
    }

    @Volatile private var accessToken: String? = null
    @Volatile private var accessTokenExpires = 0L
    private var driveId: String? = null
    private var libraryId: String? = null
    private var libraryName: String? = null
    private var source: LibrarySource = LibrarySource.ONEDRIVE
    private var localLibraryUri: Uri? = null
    private var webDavUrl: String? = null
    private var webDavUser: String? = null
    private var webDavPassword: String? = null
    private var localAssetDirectories: Map<String, DocumentFile> = emptyMap()
    private var remoteLibrary: RemoteLibrary? = null
    @Volatile private var googleAccessToken: String? = null
    @Volatile private var googleTokenExpires = 0L
    private var googleAccount: String? = null
    private var pendingGoogleConfig: JSONObject? = null
    private var remoteChooser by mutableStateOf<LibrarySource?>(null)
    private var remoteConnecting by mutableStateOf(false)
    private val googleAuthorization = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            runCatching { Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data) }
                .onSuccess(::finishGoogleAuthorization)
                .onFailure { remoteConnecting = false; showError("Google 授权失败", it) }
        } else { remoteConnecting = false }
    }

    private var status by mutableStateOf("正在检查登录状态…")
    private var connected by mutableStateOf(false)
    private var sessionRestoring by mutableStateOf(false)
    private var sessionRestored by mutableStateOf(false)
    private var sessionNeedsAuthorization by mutableStateOf(false)
    private var libraryLoaded by mutableStateOf(false)
    private var folderTree by mutableStateOf<List<FolderNode>>(emptyList())
    private var tagGroups by mutableStateOf<List<JSONObject>>(emptyList())
    private var expandedTagGroups by mutableStateOf<Set<String>>(emptySet())
    private var includeSubfolderAssets by mutableStateOf(false)
    private var indexedAssets by mutableStateOf<List<AssetMeta>>(emptyList())
    private var selectedFolderId by mutableStateOf<String?>(null)
    private var selectedFolderName by mutableStateOf("全部素材")
    private var selectedSection by mutableStateOf(LibrarySection.ALL)
    private var directoryMenuOpen by mutableStateOf(false)
    private var expandedFolderIds by mutableStateOf<Set<String>>(emptySet())
    private var folderGroupOpen by mutableStateOf(false)
    private var tagManagerOpen by mutableStateOf(false)
    private var selectedTag by mutableStateOf<String?>(null)
    private var browseHistory by mutableStateOf<List<BrowseLocation>>(emptyList())
    private var browseKey by mutableLongStateOf(0L)
    private var browseReset by mutableIntStateOf(0)
    private var nextBrowseKey = 1L
    private var indexing by mutableStateOf(false)
    private var indexProgress by mutableFloatStateOf(0f)
    private var indexDone by mutableIntStateOf(0)
    private var pullRefreshFeedbackPending = false
    private var googleStartupFeedback = false
    private var googleRestoreCheckUpdates = true
    private var indexTotal by mutableIntStateOf(0)
    private var settingsOpen by mutableStateOf(false)
    private var sourceChooserOpen by mutableStateOf(false)
    private var webDavDialogOpen by mutableStateOf(false)
    private var searchQuery by mutableStateOf("")
    private var searchOpen by mutableStateOf(false)
    private var viewMode by mutableStateOf(AssetViewMode.WATERFALL)
    private var viewPickerOpen by mutableStateOf(false)
    private var viewPickerBaseMode by mutableStateOf(AssetViewMode.WATERFALL)
    private var selectedAssetIds by mutableStateOf<Set<String>>(emptySet())
    private var selectedFolderIds by mutableStateOf<Set<String>>(emptySet())
    private var selectedTags by mutableStateOf<Set<String>>(emptySet())
    private var selectedTagGroupIds by mutableStateOf<Set<String>>(emptySet())
    private var renameTagOpen by mutableStateOf(false)
    private var renameFolderOpen by mutableStateOf(false)
    private var moveFoldersOpen by mutableStateOf(false)
    private var detailAssets = emptyList<AssetMeta>()
    private var selectionAssets by mutableStateOf<List<AssetMeta>>(emptyList())
    private var batchBusy by mutableStateOf(false)
    private var gridCellSize by mutableFloatStateOf(112f)
    private var waterfallCellSize by mutableFloatStateOf(140f)
    private var listRowHeight by mutableFloatStateOf(88f)
    private var folderScale by mutableFloatStateOf(1f)
    private var thumbnailCacheCount by mutableIntStateOf(0)
    private var thumbnailCacheBytes by mutableLongStateOf(0L)
    private var thumbnailCacheEpoch by mutableIntStateOf(0)
    private var clearCacheOnExit by mutableStateOf(false)
    private var thumbnailCacheLimitMb by mutableIntStateOf(512)
    private var thumbnailCacheLimitEnabled by mutableStateOf(true)
    private var savedIndexes by mutableStateOf<List<LibraryIndexInfo>>(emptyList())
    private var savedLibraries by mutableStateOf<List<SavedLibrary>>(emptyList())
    private var settingsRefresh: Job? = null
    private var appUpdateChecking by mutableStateOf(false)
    private val appDownload by lazy { AppDownload(this) }
    private var appDownloadState by mutableStateOf(AppDownloadState())
    private var appDownloadJob: Job? = null
    private var appInstallBusy by mutableStateOf(false)
    private var appUpdateStatus by mutableStateOf("")
    private var appRelease by mutableStateOf<AppRelease?>(null)
    private var appReleaseDialogOpen by mutableStateOf(false)
    private val flatFolderTree by derivedStateOf { allFolders(folderTree) }
    private data class BrowseStatistics(val sections: Map<LibrarySection, Int>, val tags: Map<String, Int>, val folders: Map<String, Set<String>>)
    private val browseStatistics by derivedStateOf {
        val tags = mutableMapOf<String, Int>()
        val folders = mutableMapOf<String, MutableSet<String>>()
        var active = 0; var unfiled = 0; var untagged = 0; var deleted = 0
        for (asset in indexedAssets) {
            if (asset.isDeleted) { deleted++; continue }
            active++
            if (asset.folders.isEmpty()) unfiled++
            if (asset.tags.isEmpty()) untagged++
            asset.tags.distinct().forEach { tags[it] = (tags[it] ?: 0) + 1 }
            asset.folders.forEach { folders.getOrPut(it) { mutableSetOf() }.add(asset.id) }
        }
        BrowseStatistics(mapOf(LibrarySection.ALL to active, LibrarySection.UNFILED to unfiled,
            LibrarySection.UNTAGGED to untagged, LibrarySection.TRASH to deleted), tags, folders)
    }
    private val cachedTagGroups by derivedStateOf {
        val assigned = tagGroups.flatMap { group -> group.getJSONArray("tags").let { array -> (0 until array.length()).map { array.getString(it) } } }.toSet()
        val ungrouped = browseStatistics.tags.keys.sorted().filterNot { it in assigned }
        tagGroups + if (ungrouped.isEmpty()) emptyList() else listOf(JSONObject().put("id", "magle:ungrouped").put("name", "未分组").put("tags", JSONArray(ungrouped)))
    }

    private var devicePrompt by mutableStateOf<DevicePrompt?>(null)
    private var pickerOpen by mutableStateOf(false)
    private var pickerTitle by mutableStateOf("OneDrive")
    private var pickerLoading by mutableStateOf(false)
    private var pickerFolders by mutableStateOf<List<CloudFolder>>(emptyList())
    private var infoAsset by mutableStateOf<AssetMeta?>(null)
    private var detailOriginalBitmap by mutableStateOf<Bitmap?>(null)
    private var detailOriginalLoading by mutableStateOf(false)
    private var trashCandidate by mutableStateOf<List<AssetMeta>>(emptyList())
    private var moveCandidate by mutableStateOf<List<AssetMeta>>(emptyList())
    private var tagCandidate by mutableStateOf<List<AssetMeta>>(emptyList())
    private var removeLibraryCandidate by mutableStateOf<SavedLibrary?>(null)
    private var editLibraryCandidate by mutableStateOf<SavedLibrary?>(null)
    private var rebindLibraryCandidate by mutableStateOf<SavedLibrary?>(null)
    private var newSourceForLibrary: SavedLibrary? = null
    private var exportIndexChooser by mutableStateOf(false)
    private var exportIndexLibrary: SavedLibrary? = null
    private var exportIndexLibraries: List<SavedLibrary> = emptyList()
    private val exportIndexesPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val libraries = exportIndexLibraries
        exportIndexLibraries = emptyList()
        if (uri != null && libraries.isNotEmpty()) exportIndexes(libraries, uri)
    }
    private var importedIndex: ImportedIndex? = null
    private val exportIndexPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val saved = exportIndexLibrary
        exportIndexLibrary = null
        if (uri != null && saved != null) exportIndex(saved, uri)
    }
    private val importIndexPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !indexing && !batchBusy && !remoteConnecting) {
            batchBusy = true
            work.execute {
                runCatching {
                    contentResolver.openInputStream(uri)!!.use {
                        readIndexBackup(it, File(cacheDir, "index-import-${java.util.UUID.randomUUID()}"))
                    }
                }.onSuccess { backup -> runOnUiThread {
                    discardImportedIndex()
                    importedIndex = backup
                    batchBusy = false
                    rebindLibraryCandidate = SavedLibrary("backup:${java.util.UUID.randomUUID()}",
                        backup.index.optString("libraryName", "导入的素材库"), LibrarySource.LOCAL, null, null, null, null)
                } }.onFailure {
                    runOnUiThread { batchBusy = false }
                    showError("导入索引失败，现有库未改变", it)
                }
            }
        }
    }
    private var uploadCandidates by mutableStateOf<List<Uri>>(emptyList())
    private var uploadMessage by mutableStateOf("")
    private var pendingUploadCount by mutableIntStateOf(0)
    private var foreground by mutableStateOf(false)
    private var updateOnOpen by mutableStateOf(true)
    private var updateIntervalMinutes by mutableIntStateOf(0)
    private var updateIntervalEnabled by mutableStateOf(false)
    private val uploadPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) uploadCandidates = uris
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appRelease = appDownload.savedRelease()
        val viewerPrefs = getSharedPreferences("viewer", MODE_PRIVATE)
        viewMode = runCatching { AssetViewMode.valueOf(viewerPrefs.getString("mode", "WATERFALL")!!) }.getOrDefault(AssetViewMode.WATERFALL)
        gridCellSize = LibraryLogic.resizeCell(viewerPrefs.getFloat("gridSize", 112f), 1f, 32f, 2048f)
        waterfallCellSize = LibraryLogic.resizeCell(viewerPrefs.getFloat("waterfallSize", 140f), 1f, 32f, 2048f)
        listRowHeight = LibraryLogic.resizeCell(viewerPrefs.getFloat("listHeight", 88f), 1f, 88f / 3f, 88f)
        folderScale = LibraryLogic.resizeCell(viewerPrefs.getFloat("folderScale", 1f), 1f, 2f / 3f, 2f)
        clearCacheOnExit = viewerPrefs.getBoolean("clearCacheOnExit", false)
        thumbnailCacheLimitMb = viewerPrefs.getInt("cacheLimitMb", 512).coerceAtLeast(1)
        thumbnailCacheLimitEnabled = viewerPrefs.getBoolean("cacheLimitEnabled", true)
        work.execute { trimThumbnailCache() }
        enableEdgeToEdge()
        ensureWritePermissionMigration()
        loadSavedLibrary()
        val syncPrefs = getSharedPreferences("index-sync", MODE_PRIVATE)
        updateOnOpen = syncPrefs.getBoolean("onOpen", true)
        val savedInterval = syncPrefs.getInt("interval", 0)
        updateIntervalEnabled = syncPrefs.getBoolean("intervalEnabled", savedInterval > 0)
        updateIntervalMinutes = savedInterval.takeIf { it > 0 } ?: 15
        setContent { AEagleTheme { App() } }
        restoreSession(updateOnOpen)
        receiveSharedImages(intent)
    }

    override fun onStart() {
        super.onStart(); foreground = true
        appDownloadJob = lifecycleScope.launch {
            while (true) {
                try {
                    if (appRelease != null) {
                        val state = withContext(Dispatchers.IO) { appDownload.state() }
                        appDownloadState = state
                        if (state.message.isNotBlank()) appUpdateStatus = state.message
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    appUpdateStatus = "无法读取下载状态，请稍后重试"
                }
                delay(1000)
            }
        }
    }
    override fun onStop() { appDownloadJob?.cancel(); saveViewerPreferences(); foreground = false; super.onStop() }

    private fun saveViewerPreferences() {
        getSharedPreferences("viewer", MODE_PRIVATE).edit().putString("mode", viewMode.name)
            .putFloat("gridSize", gridCellSize).putFloat("waterfallSize", waterfallCellSize)
            .putFloat("listHeight", listRowHeight).putFloat("folderScale", folderScale).apply()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveSharedImages(intent)
    }

    @Suppress("DEPRECATION")
    private fun receiveSharedImages(incoming: Intent) {
        if (incoming.action != Intent.ACTION_SEND && incoming.action != Intent.ACTION_SEND_MULTIPLE) return
        if (batchBusy) { Toast.makeText(this, "请等待当前操作完成后再分享", Toast.LENGTH_LONG).show(); return }
        val uris = runCatching {
            if (incoming.action == Intent.ACTION_SEND) listOfNotNull(incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            else incoming.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }.getOrDefault(emptyList()).distinct()
        val images = uris.filter { uri ->
            runCatching {
                LibraryLogic.isSharedImage(uri.scheme, contentResolver.getType(uri))
            }.getOrDefault(false)
        }
        if (images.isEmpty() || images.size > 100) {
            Toast.makeText(this, "请分享 1–100 张可读取的图片", Toast.LENGTH_LONG).show()
            return
        }
        uploadCandidates = images
        settingsOpen = source != LibrarySource.ONEDRIVE || !connected || !libraryLoaded
        if (settingsOpen) {
            refreshSettings()
            Toast.makeText(this, "图片已接收，请先连接并选择 OneDrive 素材库", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateCurrentIndex(fullScan: Boolean = false): Boolean {
        if (!libraryLoaded || indexing || batchBusy || remoteConnecting || hasSelection()) return false
        if (source == LibrarySource.ONEDRIVE && !connected) return false
        val id = libraryId ?: return false
        loadLibrary(id, libraryName.orEmpty(), false, fullScan)
        return true
    }

    override fun onDestroy() {
        if (clearCacheOnExit && isFinishing && !isChangingConfigurations) clearThumbnailCache(false)
        work.shutdownNow()
        indexWorkers.shutdownNow()
        priorityDownloads.shutdownNow()
        super.onDestroy()
    }

    @Composable
    private fun AEagleTheme(content: @Composable () -> Unit) {
        val dark = androidx.compose.foundation.isSystemInDarkTheme()
        val colors = when {
            Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(this)
            Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(this)
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
        MaterialTheme(colorScheme = colors, content = content)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun App() {
        val appPager = rememberPagerState(initialPage = if (settingsOpen) 1 else 0, pageCount = { 2 })
        LaunchedEffect(settingsOpen) {
            val target = if (settingsOpen) 1 else 0
            if (appPager.currentPage != target) appPager.animateScrollToPage(target)
        }
        LaunchedEffect(appPager) {
            snapshotFlow { appPager.settledPage }.collect { page ->
                if (page == 1 && !settingsOpen) openSettings()
                else if (page == 0 && settingsOpen && !appPager.isScrollInProgress) settingsOpen = false
            }
        }
        LaunchedEffect(foreground, updateIntervalEnabled, updateIntervalMinutes, libraryId, connected, libraryLoaded) {
            if (foreground && updateIntervalEnabled && updateIntervalMinutes > 0) {
                while (isActive) {
                    delay(updateIntervalMinutes * 60_000L)
                    if (foreground && uploadCandidates.isEmpty() && infoAsset == null) updateCurrentIndex()
                }
            }
        }
        LaunchedEffect(foreground, source, libraryId, sessionRestored, sessionNeedsAuthorization) {
            var waitMs = 15_000L
            while (foreground && libraryId != null && !sessionRestored && !sessionNeedsAuthorization) {
                delay(waitMs)
                if (!sessionRestoring && !remoteConnecting && !indexing && !batchBusy) {
                    restoreSession(checkUpdates = false, reopenCache = false)
                }
                waitMs = (waitMs * 2).coerceAtMost(60_000L)
            }
        }
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { outerPadding ->
                Row(Modifier.fillMaxSize().padding(outerPadding)) {
                    Box(Modifier.fillMaxSize()) {
                        key(browseReset) {
                            val savedViews = rememberSaveableStateHolder()
                            BackHandler(enabled = settingsOpen || searchOpen || searchQuery.isNotBlank() || viewPickerOpen || directoryMenuOpen || hasSelection() || selectedSection == LibrarySection.FOLDER || browseHistory.isNotEmpty()) {
                                when {
                                    settingsOpen -> { settingsOpen = false; searchOpen = searchQuery.isNotBlank() }
                                    directoryMenuOpen -> directoryMenuOpen = false
                                    hasSelection() -> if (!batchBusy) clearSelection()
                                    viewPickerOpen -> viewPickerOpen = false
                                    searchOpen || searchQuery.isNotBlank() -> { searchOpen = false; searchQuery = "" }
                                    selectedSection == LibrarySection.FOLDER || browseHistory.isNotEmpty() -> {
                                        val positions = folderPositions(folderTree)
                                        val parent = positions[selectedFolderId]?.second?.let { id ->
                                            positions[id]?.let { id to it.first }
                                        }
                                        val previous = currentBrowse().backTarget(browseHistory, parent, nextBrowseKey++)
                                            ?: return@BackHandler
                                        savedViews.removeState(browseKey)
                                        val previousIndex = browseHistory.indexOfLast { it.key == previous.key }
                                        browseHistory = when {
                                            previousIndex >= 0 -> browseHistory.take(previousIndex)
                                            previous.section == LibrarySection.ALL -> emptyList()
                                            else -> browseHistory
                                        }
                                        restoreBrowse(previous)
                                    }
                                }
                            }
                            HorizontalPager(state = appPager, beyondViewportPageCount = 1,
                                userScrollEnabled = !batchBusy && !searchOpen && !directoryMenuOpen && infoAsset == null && !hasSelection(),
                                modifier = Modifier.fillMaxSize()) { page ->
                                if (page == 0) ViewerScreen(savedViews) else SettingsScreen()
                            }
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp)) {
                AppNavigationBar((appPager.currentPage + appPager.currentPageOffsetFraction).coerceIn(0f, 1f))
            }
        }

        devicePrompt?.let { LoginDialog(it) }
        if (pickerOpen) LibraryPickerDialog()
        infoAsset?.let { AssetInfoSheet(it) }
        if (renameTagOpen) NameDialog(if (selectedTags.isNotEmpty()) "重命名标签" else "重命名标签组",
            selectedTags.singleOrNull() ?: tagGroups.firstOrNull { it.getString("id") in selectedTagGroupIds }?.getString("name").orEmpty(),
            { renameTagOpen = false }) { name -> renameTagOpen = false; renameTagSelection(name) }
        if (renameFolderOpen) NameDialog("重命名文件夹", flatFolderTree.firstOrNull { it.id in selectedFolderIds }?.name.orEmpty(),
            { renameFolderOpen = false }) { name -> renameFolderOpen = false; changeFolders(name = name) }
        if (moveFoldersOpen) FolderPicker(emptySet(), false, { moveFoldersOpen = false }, rootLabel = "库根目录") { target ->
            moveFoldersOpen = false; changeFolders(parentId = target.firstOrNull())
        }
        if (trashCandidate.isNotEmpty()) TrashConfirmation(trashCandidate)
        if (moveCandidate.isNotEmpty()) MoveFolderDialog(moveCandidate)
        if (tagCandidate.isNotEmpty()) TagPicker(emptyList(), { tagCandidate = emptyList() }) { tags ->
            val assets = tagCandidate
            tagCandidate = emptyList()
            if (tags.isNotEmpty()) runBatch(assets, "添加标签", tags = tags)
        }
        removeLibraryCandidate?.let { RemoveLibraryDialog(it) }
        editLibraryCandidate?.let { EditLibraryDialog(it) }
        rebindLibraryCandidate?.let { RebindLibraryDialog(it) }
        if (exportIndexChooser) ExportIndexDialog()
        if (sourceChooserOpen) SourceChooserDialog()
        if (webDavDialogOpen) WebDavDialog()
        remoteChooser?.let { RemoteSourceDialog(it) }
        if (uploadCandidates.isNotEmpty() && canUpload()) UploadConfirmation()
    }

    @Composable
    private fun AppNavigationBar(progress: Float) {
        Surface(shape = CircleShape, shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box(Modifier.width(208.dp).height(64.dp).padding(4.dp)) {
            Surface(Modifier.offset(x = (100f * progress).dp).width(100.dp).height(56.dp),
                shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {}
            Row(Modifier.fillMaxSize()) {
                Surface(onClick = { settingsOpen = false; searchOpen = false },
                    modifier = Modifier.weight(1f).height(56.dp).semantics { selected = !settingsOpen },
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.Transparent) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                        Icon(painterResource(R.drawable.ic_image_24), null, Modifier.size(22.dp))
                        Text("视图", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Surface(onClick = ::openSettings,
                    modifier = Modifier.weight(1f).height(56.dp).semantics { selected = settingsOpen },
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.Transparent) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                        Icon(Icons.Default.Settings, null, Modifier.size(22.dp))
                        Text("设置", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ViewerScreen(savedViews: SaveableStateHolder) {
        val folder = remember(folderTree, selectedFolderId, selectedSection) {
            if (selectedSection == LibrarySection.FOLDER && selectedFolderId == null) FolderNode("magle:root", "文件夹", folderTree)
            else flatFolderTree.firstOrNull { it.id == selectedFolderId }
        }
        val includedIds = remember(folder, selectedFolderId, includeSubfolderAssets) {
            if (includeSubfolderAssets && folder != null) allFolders(listOf(folder)).map { it.id }.toSet() else setOfNotNull(selectedFolderId)
        }
        val sectionAssets = remember(indexedAssets, selectedSection, selectedFolderId, selectedTag, includedIds) { when (selectedSection) {
            LibrarySection.ALL -> indexedAssets.filterNot { it.isDeleted }
            LibrarySection.UNFILED -> indexedAssets.filter { !it.isDeleted && it.folders.isEmpty() }
            LibrarySection.UNTAGGED -> indexedAssets.filter { !it.isDeleted && it.tags.isEmpty() }
            LibrarySection.TAG -> indexedAssets.filter { !it.isDeleted && selectedTag in it.tags }
            LibrarySection.TAGGROUPS, LibrarySection.TAGGROUP -> emptyList()
            LibrarySection.TRASH -> indexedAssets.filter { it.isDeleted }
            LibrarySection.FOLDER -> indexedAssets.filter {
                !it.isDeleted && ((selectedFolderId == null && it.folders.isEmpty()) || LibraryLogic.belongsToAnyFolder(it.folders, includedIds))
            }
        } }
        val query = searchQuery.trim()
        val searchFolders = remember(folderTree, query) { flatFolderTree.filter { query.isNotBlank() && it.name.contains(query, true) } }
        val searchTags = remember(indexedAssets, tagGroups, query) { availableTagGroups().flatMap { group ->
            val tags = group.getJSONArray("tags")
            (0 until tags.length()).map { tags.getString(it) }
        }.distinct().filter { query.isNotBlank() && it.contains(query, true) }.sortedBy { it.lowercase() } }
        val filteredAssets = remember(sectionAssets, indexedAssets, query, selectedSection) { if (query.isBlank()) sectionAssets else indexedAssets.filter { asset ->
            (asset.isDeleted == (selectedSection == LibrarySection.TRASH)) && (
            asset.name.contains(query, true) || asset.tags.any { it.contains(query, true) }
            )
        } }
        // Keep item order stable while a gesture is selecting, even if indexing publishes a new batch.
        val visibleAssets = if (selectedAssetIds.isNotEmpty()) selectionAssets else filteredAssets
        val refreshState = rememberPullToRefreshState()
        val refreshDistancePx = with(LocalDensity.current) { 80.dp.toPx() }
        var refreshRequested by remember { mutableStateOf(false) }
        LaunchedEffect(indexing) { if (!indexing) refreshRequested = false }

        Box(Modifier.fillMaxSize()) {
                when {
                    !libraryLoaded -> Welcome(Modifier.fillMaxSize())
                    else -> savedViews.SaveableStateProvider(browseKey) { PullToRefreshBox(
                        isRefreshing = refreshRequested && indexing,
                        state = refreshState,
                        onRefresh = {
                            refreshRequested = updateCurrentIndex()
                            if (refreshRequested) pullRefreshFeedbackPending = true
                        },
                        modifier = Modifier.fillMaxSize(),
                        indicator = {} // Draw above the floating toolbar, starting at the screen's top edge.
                    ) {
                        val childFolder = folder?.takeIf { selectedSection == LibrarySection.FOLDER && it.children.isNotEmpty() }
                        LibraryContent(visibleAssets, viewMode, Modifier.fillMaxSize().graphicsLayer {
                            translationY = LibraryLogic.refreshContentOffset(refreshState.distanceFraction, refreshDistancePx)
                        }, childFolder,
                            selectedSection == LibrarySection.TAGGROUPS || selectedSection == LibrarySection.TAGGROUP,
                            query, searchTags, searchFolders)
                    } }
                }
                ViewerTopBar(visibleAssets.size)
                if (libraryLoaded) PullToRefreshDefaults.Indicator(state = refreshState,
                    isRefreshing = refreshRequested && indexing, modifier = Modifier.align(Alignment.TopCenter))
            }
    }

    @Composable
    private fun SelectionLabel(count: Int, kind: String, modifier: Modifier) {
        Box(modifier) {
            Surface(onClick = ::clearSelection, enabled = !batchBusy, shape = CircleShape,
                modifier = Modifier.height(48.dp).padding(vertical = 4.dp).widthIn(max = 280.dp), shadowElevation = 0.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Close, "取消$kind 多选，已选择 $count 项", Modifier.size(20.dp))
                    Text("已选 $count", Modifier.padding(start = 6.dp).weight(1f, fill = false),
                        style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    @Composable
    private fun SelectionAction(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
        FilledTonalIconButton(onClick = onClick, enabled = enabled, shape = CircleShape,
            modifier = Modifier.padding(start = 4.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            content = content)
    }

    @Composable
    private fun ViewerTopBar(assetCount: Int) {
        val groups = remember(tagGroups, indexedAssets) { availableTagGroups() }
        val counts = browseStatistics.sections
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(searchOpen) { if (!searchOpen) keyboard?.hide() }
        Box {
            BoxWithConstraints(
                Modifier.fillMaxWidth().statusBarsPadding().height(48.dp)
            ) {
                val barWidth = maxWidth
                val pickerWidth by animateDpAsState(if (viewPickerOpen) 156.dp else 48.dp, label = "view-picker-width")
                val reserved = if (libraryLoaded) pickerWidth + 48.dp + 42.dp else 30.dp
                val targetWidth = if (searchOpen) LibraryLogic.searchPillWidth(barWidth.value, reserved.value).dp else 48.dp
                val searchWidth by animateDpAsState(targetWidth, label = "search-width")
                val directoryLabel = when {
                    !libraryLoaded -> "MAGLE"
                    selectedSection == LibrarySection.TAGGROUPS -> "$selectedFolderName · ${groups.flatMap { group -> val tags = group.getJSONArray("tags"); (0 until tags.length()).map { tags.getString(it) } }.distinct().size} 个标签"
                    selectedSection == LibrarySection.TAGGROUP -> "$selectedFolderName · ${groups.firstOrNull { it.getString("id") == selectedTag }?.getJSONArray("tags")?.length() ?: 0} 个标签"
                    else -> "$selectedFolderName · $assetCount"
                }
                var directorySize by remember { mutableStateOf(IntSize(1, 1)) }
                val menuTransition = remember { MutableTransitionState(false) }
                menuTransition.targetState = directoryMenuOpen
                val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
                val menuHeight = (windowHeight - WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 16.dp).coerceAtLeast(48.dp)
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedFolderIds.isNotEmpty()) {
                        SelectionLabel(selectedFolderIds.size, "文件夹", Modifier.weight(1f))
                        SelectionAction(onClick = { downloadFolders() }, enabled = !batchBusy) { Icon(painterResource(R.drawable.ic_download_24), "下载文件夹内原文件", Modifier.size(20.dp)) }
                        SelectionAction(onClick = { renameFolderOpen = true }, enabled = !batchBusy && selectedFolderIds.size == 1 && canUpload()) { Icon(Icons.Default.Edit, "重命名文件夹", Modifier.size(20.dp)) }
                        SelectionAction(onClick = { moveFoldersOpen = true }, enabled = !batchBusy && canUpload()) { Icon(painterResource(R.drawable.ic_folder_24), "移动文件夹", Modifier.size(20.dp)) }
                    } else if (selectedTags.isNotEmpty() || selectedTagGroupIds.isNotEmpty()) {
                        SelectionLabel(selectedTags.size + selectedTagGroupIds.size, if (selectedTags.isNotEmpty()) "标签" else "标签组", Modifier.weight(1f))
                        SelectionAction(onClick = { downloadTagSelection() }, enabled = !batchBusy) { Icon(painterResource(R.drawable.ic_download_24), "下载标签内原文件", Modifier.size(20.dp)) }
                        SelectionAction(onClick = { renameTagOpen = true }, enabled = !batchBusy && canUpload() && selectedTags.size + selectedTagGroupIds.size == 1 && "magle:ungrouped" !in selectedTagGroupIds) { Icon(Icons.Default.Edit, "重命名标签或标签组", Modifier.size(20.dp)) }
                    } else if (selectedAssetIds.isNotEmpty()) {
                        SelectionLabel(selectedAssetIds.size, "素材", Modifier.weight(1f))
                        if (batchBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        SelectionAction(onClick = { runBatch(selectedAssets(), "下载") }, enabled = !batchBusy) {
                            Icon(painterResource(R.drawable.ic_download_24), contentDescription = "下载选中素材", modifier = Modifier.size(20.dp))
                        }
                        SelectionAction(onClick = { shareAssets(selectedAssets()) }, enabled = !batchBusy) {
                            Icon(Icons.Outlined.Share, contentDescription = "分享选中原文件", modifier = Modifier.size(20.dp))
                        }
                        SelectionAction(onClick = { moveCandidate = selectedAssets() }, enabled = !batchBusy) {
                            Icon(painterResource(R.drawable.ic_folder_24), contentDescription = "移动选中素材", modifier = Modifier.size(20.dp))
                        }
                        SelectionAction(onClick = { tagCandidate = selectedAssets() }, enabled = !batchBusy) {
                            Icon(painterResource(R.drawable.ic_bookmark_24), "添加标签", Modifier.size(20.dp))
                        }
                        val restoring = selectedSection == LibrarySection.TRASH
                        SelectionAction(onClick = {
                            if (restoring) runBatch(selectedAssets(), "还原") else trashCandidate = selectedAssets()
                        }, enabled = !batchBusy) {
                            Icon(painterResource(if (restoring) R.drawable.ic_restore_from_trash_24 else R.drawable.ic_delete_24),
                                contentDescription = if (restoring) "还原选中素材" else "选中素材移到回收站", modifier = Modifier.size(20.dp))
                        }
                    } else {
                    if (!searchOpen || barWidth >= 700.dp) {
                        Box(Modifier.weight(1f)) {
                        Surface(onClick = { viewPickerOpen = false; directoryMenuOpen = true },
                            enabled = libraryLoaded,
                            modifier = Modifier.height(48.dp).padding(vertical = 4.dp).widthIn(max = 280.dp).onSizeChanged { directorySize = it },
                            shape = CircleShape, shadowElevation = 0.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_sidebar_24), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                directoryLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (libraryLoaded && indexing) CircularProgressIndicator(
                                Modifier.padding(start = 6.dp).size(12.dp), strokeWidth = 1.5.dp
                            )
                        }
                        }
                        if (menuTransition.currentState || menuTransition.targetState) Popup(
                            alignment = Alignment.TopStart,
                            onDismissRequest = { directoryMenuOpen = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                        androidx.compose.animation.AnimatedVisibility(menuTransition,
                            modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(20.dp)),
                            enter = expandIn(tween(220), expandFrom = Alignment.TopStart, initialSize = { directorySize }),
                            exit = shrinkOut(tween(180), shrinkTowards = Alignment.TopStart, targetSize = { directorySize })) {
                        Surface(shape = RoundedCornerShape(20.dp), shadowElevation = 0.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        LazyColumn(Modifier.width((barWidth - 24.dp).coerceAtMost(320.dp) * if (barWidth < 700.dp) .9f else 1f).heightIn(max = menuHeight)) {
                            item {
                            Row(Modifier.fillMaxWidth().height(40.dp).clickable { directoryMenuOpen = false }
                                .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.ic_sidebar_24), null, Modifier.size(20.dp))
                                Text(directoryLabel, Modifier.padding(start = 6.dp).weight(1f),
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider()
                            LibraryMenuItem("全部", R.drawable.ic_grid_view_24, LibrarySection.ALL, counts[LibrarySection.ALL])
                            LibraryMenuItem("未分类", R.drawable.ic_folder_off_24, LibrarySection.UNFILED, counts[LibrarySection.UNFILED])
                            LibraryMenuItem("未标签", R.drawable.ic_bookmark_remove_24, LibrarySection.UNTAGGED, counts[LibrarySection.UNTAGGED])
                            DropdownMenuItem(text = { Text("标签管理") }, leadingIcon = { Icon(painterResource(R.drawable.ic_bookmark_24), contentDescription = null) },
                                trailingIcon = { IconButton(onClick = { tagManagerOpen = !tagManagerOpen; if (tagManagerOpen) folderGroupOpen = false }) { Icon(if (tagManagerOpen) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = if (tagManagerOpen) "收起标签管理" else "展开标签管理") } },
                                onClick = { navigateBrowse(LibrarySection.TAGGROUPS, null, null, "标签管理") })
                            if (tagManagerOpen) TagMenuItems()
                            LibraryMenuItem("回收站", R.drawable.ic_delete_24, LibrarySection.TRASH, counts[LibrarySection.TRASH])
                            DropdownMenuItem(text = { Text("文件夹") }, leadingIcon = { Icon(painterResource(R.drawable.ic_folder_24), contentDescription = null) },
                                trailingIcon = { IconButton(onClick = {
                                    folderGroupOpen = !folderGroupOpen
                                    if (folderGroupOpen) tagManagerOpen = false
                                }) { Icon(if (folderGroupOpen) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = if (folderGroupOpen) "收起文件夹" else "展开文件夹") } },
                                onClick = { navigateBrowse(LibrarySection.FOLDER, null, null, "文件夹") })
                            }
                            if (folderGroupOpen) items(flattenVisibleFolders(folderTree, expandedFolderIds, depth = 1), key = { it.node.id }) { row -> FolderMenuItem(row) }
                        }
                        }
                        }
                        }
                        }
                    } else Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(2.dp))
                    SearchPill(searchWidth)
                    if (libraryLoaded) {
                        Spacer(Modifier.width(2.dp))
                        FilledTonalIconButton(
                            onClick = { searchOpen = false; viewPickerOpen = false; uploadPicker.launch(arrayOf("image/*")) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            enabled = canUpload() && !batchBusy
                        ) { Icon(Icons.Default.Add, contentDescription = "上传图片", modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(2.dp))
                        // Keep the selected button anchored and the outgoing choices stable during collapse.
                        Surface(Modifier.width(pickerWidth).height(48.dp).padding(4.dp), shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Box(Modifier.fillMaxSize().clipToBounds()) {
                            Row(Modifier.wrapContentWidth(Alignment.End, unbounded = true).width(156.dp).align(Alignment.CenterEnd).offset(x = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AssetViewMode.entries.filter { it != viewPickerBaseMode }.forEach { mode -> ViewModeButton(mode) }
                                IconButton(
                                    onClick = {
                                        searchOpen = false
                                        if (!viewPickerOpen) viewPickerBaseMode = viewMode
                                        viewPickerOpen = !viewPickerOpen
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) { ViewModeIcon(viewMode, MaterialTheme.colorScheme.onSurface) }
                            }
                        }
                        }
                    }
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchPill(width: androidx.compose.ui.unit.Dp) {
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(searchOpen) {
            if (searchOpen) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
        }
        Surface(
            onClick = { viewPickerOpen = false; searchOpen = true },
            modifier = Modifier.width(width).height(48.dp).padding(4.dp),
            shape = CircleShape,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewPickerOpen = false; searchOpen = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "搜索", modifier = Modifier.size(20.dp))
                }
                if (searchOpen) BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f).padding(end = 12.dp).focusRequester(focusRequester),
                    decorationBox = { input ->
                        if (searchQuery.isBlank()) Text(
                            "搜索名称、标签、文件夹",
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        input()
                    }
                )
            }
        }
    }

    @Composable
    private fun ViewModeButton(mode: AssetViewMode) {
        val selected = viewMode == mode
        IconButton(
            onClick = { if (viewPickerOpen) { searchOpen = false; viewMode = mode; saveViewerPreferences(); viewPickerOpen = false } },
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                ViewModeIcon(
                    mode,
                    if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = .35f)
                )
            }
        }
    }

    @Composable
    private fun ViewModeIcon(mode: AssetViewMode, color: androidx.compose.ui.graphics.Color) {
        Canvas(Modifier.size(18.dp).semantics { contentDescription = mode.label }) {
            val stroke = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            inset(stroke.width / 2) {
            translate(left = size.width * when (mode) { AssetViewMode.GRID -> .09f; AssetViewMode.WATERFALL -> .06f; else -> 0f },
                top = size.height * when (mode) { AssetViewMode.GRID -> .09f; AssetViewMode.LIST -> .04f; else -> 0f }) {
            val radius = 2.2.dp.toPx()
            fun tile(x: Float, y: Float, width: Float, height: Float) =
                drawRoundRect(color, Offset(x, y), Size(width, height), androidx.compose.ui.geometry.CornerRadius(radius), style = stroke)
            when (mode) {
                AssetViewMode.GRID -> {
                    val s = size.width * .34f
                    val gap = size.width * .14f
                    tile(0f, 0f, s, s); tile(s + gap, 0f, s, s)
                    tile(0f, s + gap, s, s); tile(s + gap, s + gap, s, s)
                }
                AssetViewMode.LIST -> {
                    val s = size.width * .34f
                    tile(0f, 0f, s, s); tile(0f, size.height * .58f, s, s)
                    val start = size.width * .52f
                    listOf(.08f, .30f, .66f, .88f).forEach { y ->
                        drawLine(color, Offset(start, size.height * y), Offset(size.width, size.height * y), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    }
                }
                AssetViewMode.WATERFALL -> {
                    val w = size.width * .36f
                    val gap = size.width * .16f
                    tile(0f, 0f, w, size.height * .30f); tile(w + gap, 0f, w, size.height * .56f)
                    tile(0f, size.height * .42f, w, size.height * .58f)
                    tile(w + gap, size.height * .68f, w, size.height * .32f)
                }
            }
            }
            }
        }
    }

    @Composable
    private fun LibraryMenuItem(label: String, icon: Int, section: LibrarySection, count: Int? = null) {
        DropdownMenuItem(
            text = {
                Text(label, fontWeight = if (selectedSection == section) FontWeight.Bold else FontWeight.Normal)
            },
            leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
            trailingIcon = { count?.let { Text(it.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            onClick = {
                navigateBrowse(section, null, null, label)
            }
        )
    }

    @Composable
    private fun TagMenuItems() {
        val counts = remember(indexedAssets) {
            indexedAssets.filterNot { it.isDeleted }.flatMap { it.tags.distinct() }.groupingBy { it }.eachCount().toSortedMap()
        }
        val groups = remember(tagGroups, indexedAssets) { availableTagGroups() }
        groups.forEach { group ->
            val id = group.getString("id")
            val tags = group.getJSONArray("tags").let { array -> (0 until array.length()).map { array.getString(it) } }
            val color = tagGroupColor(group.optString("color"))
            val open = id in expandedTagGroups
            DropdownMenuItem(text = {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_bookmark_24), contentDescription = null, tint = color, modifier = Modifier.padding(horizontal = 6.dp).size(20.dp))
                    Text(group.getString("name"), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }, trailingIcon = { Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tags.size.toString())
                IconButton(onClick = { expandedTagGroups = if (open) expandedTagGroups - id else expandedTagGroups + id }) {
                    Icon(if (open) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, if (open) "收起群组" else "展开群组")
                }
            } }, onClick = { navigateBrowse(LibrarySection.TAGGROUP, null, id, group.getString("name")) })
            if (open) tags.forEach { tag ->
                DropdownMenuItem(text = {
                    Row(Modifier.padding(start = 38.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_bookmark_24), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        Text(tag, Modifier.padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }, trailingIcon = { Text((counts[tag] ?: 0).toString()) }, onClick = {
                    navigateBrowse(LibrarySection.TAG, null, tag, tag)
                })
            }
        }
        if (groups.isEmpty()) DropdownMenuItem(text = { Text("暂无标签群组") }, onClick = {}, enabled = false)
    }

    @Composable
    private fun tagGroupColor(raw: String): androidx.compose.ui.graphics.Color {
        val hex = when (raw.lowercase(Locale.ROOT)) {
            "red" -> "#EF5350"
            "orange" -> "#FFA726"
            "yellow" -> "#FFCA28"
            "green" -> "#66BB6A"
            "blue" -> "#42A5F5"
            "purple" -> "#AB47BC"
            "pink" -> "#EC407A"
            "gray", "grey" -> "#9E9E9E"
            else -> raw
        }
        return runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }
            .getOrDefault(MaterialTheme.colorScheme.onSurfaceVariant)
    }

    private fun availableTagGroups(): List<JSONObject> {
        return cachedTagGroups
    }

    @Composable
    private fun TagCollections(modifier: Modifier = Modifier) {
        val groups = remember(tagGroups, indexedAssets) { availableTagGroups() }
        val group = groups.firstOrNull { it.getString("id") == selectedTag }
        val root = selectedSection == LibrarySection.TAGGROUPS
        var groupsOpen by rememberSaveable { mutableStateOf(true) }
        val colors = remember(groups) { buildMap {
            groups.forEach { entry -> val tags = entry.getJSONArray("tags")
                repeat(tags.length()) { putIfAbsent(tags.getString(it), entry.optString("color")) }
            }
        } }
        val query = searchQuery.trim()
        val tags = if (root) colors.keys.toList() else group?.getJSONArray("tags")?.let { values -> (0 until values.length()).map { values.getString(it) } }.orEmpty()
        val sections = remember(tags, query) { LibraryLogic.tagSections(tags.filter { it.contains(query, true) }) }
        val shownGroups = groups.filter { it.getString("name").contains(query, true) }
        val counts = browseStatistics.tags
        val columns = LibraryLogic.folderColumnCount(with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp().value }, folderScale)
        val iconSize by animateDpAsState((38.4f * folderScale).dp, tween(140), label = "tag-icon-size")
        LazyColumn(modifier.fillMaxSize().navigationBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 68.dp, end = 12.dp, bottom = 104.dp)) {
            if (root && shownGroups.isNotEmpty()) item(key = "groups") {
                Card(Modifier.fillMaxWidth().assetPinchZoom(viewMode, folderArea = true), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().animateContentSize(tween(140)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { groupsOpen = !groupsOpen }, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { groupsOpen = !groupsOpen }, modifier = Modifier.size(40.dp)) {
                            Icon(if (groupsOpen) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                if (groupsOpen) "收起标签群组" else "展开标签群组")
                        }
                        Text("标签群组 · ${shownGroups.size}", style = MaterialTheme.typography.labelLarge)
                    }
                    if (groupsOpen) shownGroups.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { entry ->
                                Column(Modifier.weight(1f).tagInteraction(entry.getString("id"), true) {
                                    navigateBrowse(LibrarySection.TAGGROUP, null, entry.getString("id"), entry.getString("name"))
                                }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(painterResource(R.drawable.ic_bookmark_24), null, tint = tagGroupColor(entry.optString("color")), modifier = Modifier.size(iconSize))
                                    Text("${entry.getString("name")} · ${entry.getJSONArray("tags").length()}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                }
            }
            sections.entries.forEachIndexed { index, (letter, values) ->
                item(key = "heading:$letter") {
                    if (root || index > 0) HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text("$letter · ${values.size}", Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(values, key = { "tag:$it" }) { tag ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).tagInteraction(tag, false) { navigateBrowse(LibrarySection.TAG, null, tag, tag) }
                        .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(6.dp).background(tagGroupColor(if (root) colors[tag].orEmpty() else group?.optString("color").orEmpty()), CircleShape))
                        Text(tag, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("· ${counts[tag] ?: 0}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (sections.isEmpty()) item { Text("暂无标签", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    private fun selectFolder(folder: FolderNode) {
        navigateBrowse(LibrarySection.FOLDER, folder.id, null, folder.name)
    }

    private fun hasSelection() = selectedAssetIds.isNotEmpty() || selectedFolderIds.isNotEmpty() || selectedTags.isNotEmpty() || selectedTagGroupIds.isNotEmpty()

    @Composable
    private fun Modifier.tagInteraction(id: String, group: Boolean, onOpen: () -> Unit): Modifier {
        val selected = if (group) selectedTagGroupIds else selectedTags
        val chosen = id in selected
        return border(if (chosen) 1.5.dp else 0.dp,
            if (chosen) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp))
            .combinedClickable(enabled = !batchBusy && selectedAssetIds.isEmpty() && selectedFolderIds.isEmpty() &&
                (if (group) selectedTags.isEmpty() else selectedTagGroupIds.isEmpty()),
                role = androidx.compose.ui.semantics.Role.Button,
                onLongClickLabel = if (group) "选择标签组" else "选择标签",
                onClick = {
                    if (selected.isEmpty()) onOpen()
                    else if (group) selectedTagGroupIds = if (chosen) selected - id else selected + id
                    else selectedTags = if (chosen) selected - id else selected + id
                }, onLongClick = {
                    if (group) selectedTagGroupIds = selected + id else selectedTags = selected + id
                    searchOpen = false; viewPickerOpen = false; directoryMenuOpen = false
                })
            .semantics { this.selected = chosen }
    }

    private fun downloadTagSelection() {
        val tags = selectedTags + availableTagGroups().filter { it.getString("id") in selectedTagGroupIds }.flatMap { group ->
            group.getJSONArray("tags").let { array -> (0 until array.length()).map { array.getString(it) } }
        }
        val assets = indexedAssets.filter { !it.isDeleted && it.tags.any { tag -> tag in tags } }
        if (assets.isEmpty()) Toast.makeText(this, "选中的标签没有素材", Toast.LENGTH_SHORT).show()
        else runBatch(assets, "下载")
    }

    private fun tagRenameFile() = android.util.AtomicFile(File(noBackupFilesDir, "tag-rename.json"))

    private fun pendingTagRename(): TagRenameTask? = runCatching {
        TagRenameTask(JSONObject(String(tagRenameFile().readFully(), StandardCharsets.UTF_8)))
    }.getOrNull()?.takeIf { it.matches(source.name, libraryId, driveId) }

    private fun saveTagRename(task: TagRenameTask) {
        val file = tagRenameFile()
        val output = file.startWrite()
        try { output.write(task.json.toString().toByteArray(StandardCharsets.UTF_8)); file.finishWrite(output) }
        catch (error: Throwable) { file.failWrite(output); throw error }
    }

    private fun renameTagSelection(name: String) {
        if (batchBusy || !canUpload() || selectedTags.size + selectedTagGroupIds.size != 1) return
        val old = selectedTags.singleOrNull()
        val groupId = selectedTagGroupIds.singleOrNull()
        if (groupId == "magle:ungrouped") return
        val clean = LibraryLogic.validTagName(name)
        if (old == null) {
            val expectedName = tagGroups.firstOrNull { it.optString("id") == groupId }?.optString("name")
            batchBusy = true
            queueEdit {
                val result = runCatching { mutateLibraryNow {
                    if (LibraryLogic.tagGroups(it).firstOrNull { group -> group.optString("id") == groupId }?.optString("name") != expectedName) throw LibraryConflict()
                    LibraryLogic.renameTagGroup(it, groupId!!, clean)
                } }
                runOnUiThread {
                    batchBusy = false
                    result.onSuccess {
                        tagGroups = LibraryLogic.tagGroups(it)
                        browseHistory = browseHistory.map { location ->
                            if (location.section == LibrarySection.TAGGROUP && location.tag == groupId) location.copy(name = clean) else location
                        }
                        if (selectedSection == LibrarySection.TAGGROUP && selectedTag == groupId) selectedFolderName = clean
                        clearSelection()
                    }.onFailure { Toast.makeText(this, "重命名失败：${it.message}", Toast.LENGTH_LONG).show() }
                }
            }
            return
        }
        if (clean == old) return
        if (tagRenameFile().baseFile.exists()) {
            Toast.makeText(this, "还有未完成的标签改名，请先在设置中继续", Toast.LENGTH_LONG).show()
            return
        }
        if (indexedAssets.any { clean in it.tags } || tagGroups.any { clean in jsonStrings(it, "tags") }) {
            Toast.makeText(this, "这个标签名已存在，请使用其他名称", Toast.LENGTH_LONG).show()
            return
        }
        runTagRename(old, clean)
    }

    private fun runTagRename(old: String? = null, name: String? = null) {
        if (batchBusy || !canUpload()) return
        batchBusy = true
        attemptedTagResume = true
        tagRenameStatus = "正在准备标签改名…"
        queueEdit {
            val changed = mutableMapOf<String, AssetMeta>()
            var task = pendingTagRename()
            val snapshot = editSnapshot()
            val result = runCatching {
                if (task == null) {
                    check(old != null && name != null && !tagRenameFile().baseFile.exists()) { "未完成任务属于其他素材库，请切换到原库继续" }
                    val allIds = listAssetIds(libraryId!!) - loadIgnoredIndexCache().keys
                    val ids = mutableListOf<String>()
                    // Preflight the whole library, including uncached items, before any tag is changed.
                    for (chunk in allIds.chunked(8)) {
                        val reads = chunk.map { id -> indexWorkers.submit(Callable { fetchAssetMetadata(id, 0) }) }
                        try {
                            for (read in reads) {
                                val asset = read.get() ?: continue
                                check(name !in asset.tags) { "这个标签名已存在，请使用其他名称；尚未修改素材" }
                                if (old in asset.tags) ids += asset.id
                            }
                        } finally { reads.filterNot { it.isDone }.forEach { it.cancel(true) } }
                    }
                    val root = JSONObject(String(readLibraryFile(libraryId!!, "metadata.json"), StandardCharsets.UTF_8))
                    check(LibraryLogic.tagGroups(root).none { name in jsonStrings(it, "tags") }) { "这个标签名已存在，请使用其他名称" }
                    task = TagRenameTask(JSONObject().put("source", source.name).put("libraryId", libraryId)
                        .put("driveId", driveId.orEmpty()).put("old", old).put("name", name)
                        .put("ids", JSONArray(ids)).put("done", JSONArray()))
                    saveTagRename(task)
                }
                val current = task
                val done = current.done
                for (id in current.ids.filterNot { it in done }) {
                    if (Thread.currentThread().isInterrupted) throw InterruptedException()
                    val asset = fetchAssetMetadata(id, 0)
                    if (asset != null && current.old in asset.tags) {
                        if (current.name in asset.tags) throw LibraryConflict("同一素材已有新旧两个标签，为避免误合并，请先核对")
                        // Replay even an already-renamed item: repair its manifest after a lost response.
                        changed[id] = mutateAssetNow(asset) { LibraryLogic.renameTagReferences(it, current.old, current.name) }
                    } else if (asset != null && current.name in asset.tags) {
                        // A write may have succeeded before progress was saved. Repair only the manifest.
                        mergeLibraryMtime(id, maxOf(asset.mtime, System.currentTimeMillis()))
                        changed[id] = asset
                    }
                    current.complete(id)
                    saveTagRename(current)
                    val progress = "标签改名：已处理 ${current.done.size}/${current.ids.size} 项"
                    runOnUiThread { tagRenameStatus = progress }
                }
                val metadata = mutateLibraryNow {
                    val tags = LibraryLogic.tagGroups(it).flatMap { group -> jsonStrings(group, "tags") }
                    if (current.old in tags && current.name in tags) throw LibraryConflict("标签组已同时包含新旧标签，请先核对，进度已保留")
                    LibraryLogic.renameTagReferences(it, current.old, current.name)
                }
                tagRenameFile().delete()
                metadata
            }
            if (changed.isNotEmpty()) {
                val updated = (snapshot.associateBy { it.id } + changed).values
                runCatching { saveIndexCache(updated) }.onFailure { Log.w("MAGLE", "Tag rename cache needs refresh", it) }
                publishAssets(updated)
            }
            runOnUiThread {
                batchBusy = false
                result.onSuccess {
                    tagGroups = LibraryLogic.tagGroups(it)
                    val current = task!!
                    browseHistory = browseHistory.map { location ->
                        if (location.section == LibrarySection.TAG && location.tag == current.old)
                            location.copy(tag = current.name, name = current.name) else location
                    }
                    if (selectedSection == LibrarySection.TAG && selectedTag == current.old) {
                        selectedTag = current.name; selectedFolderName = current.name
                    }
                    tagRenameStatus = ""
                    clearSelection()
                    Toast.makeText(this, "标签改名完成", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    tagRenameStatus = if (pendingTagRename() != null) "标签改名未完成，进度已保留：${it.message}"
                        else "标签改名尚未开始：${it.message}"
                    Toast.makeText(this, tagRenameStatus, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun currentBrowse() = BrowseLocation(browseKey, selectedSection, selectedFolderId, selectedTag,
        selectedFolderName, searchQuery, includeSubfolderAssets, viewMode)

    private fun navigateBrowse(section: LibrarySection, folderId: String?, tag: String?, name: String) {
        if (batchBusy) return
        val current = currentBrowse()
        val target = BrowseLocation(nextBrowseKey, section, folderId, tag, name, "", includeSubfolderAssets, viewMode)
        if (!current.sameDestination(target) || current.query.isNotBlank()) {
            browseHistory = browseHistory + current
            nextBrowseKey++
            restoreBrowse(target)
        } else searchQuery = ""
        directoryMenuOpen = false
        searchOpen = false
        viewPickerOpen = false
    }

    private fun restoreBrowse(location: BrowseLocation) {
        browseKey = location.key
        selectedSection = location.section
        selectedFolderId = location.folderId
        selectedTag = location.tag
        selectedFolderName = location.name
        searchQuery = location.query
        searchOpen = location.query.isNotBlank()
        includeSubfolderAssets = location.includeChildren
        viewMode = location.mode
        saveViewerPreferences()
    }

    private fun resetBrowseHistory() {
        browseHistory = emptyList()
        browseKey = nextBrowseKey++
        browseReset++
    }

    @Composable
    private fun ChildFolders(folder: FolderNode) {
        var open by rememberSaveable(folder.id) { mutableStateOf(true) }
        val columns = LibraryLogic.folderColumnCount(with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp().value }, folderScale)
        val counts = remember(folder, browseStatistics.folders) {
            folder.children.associate { child ->
                val ids = allFolders(listOf(child)).map { it.id }.toSet()
                child.id to LibraryLogic.folderAssetCount(browseStatistics.folders, ids)
            }
        }
        Card(Modifier.fillMaxWidth().assetPinchZoom(viewMode, folderArea = true), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.animateContentSize(tween(140)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { open = !open }, modifier = Modifier.size(40.dp)) { Icon(if (open) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = if (open) "收起子文件夹" else "展开子文件夹") }
                        Text("子文件夹 · ${folder.children.size}", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(Modifier.heightIn(min = 48.dp).toggleable(value = includeSubfolderAssets, role = androidx.compose.ui.semantics.Role.Checkbox,
                        onValueChange = { includeSubfolderAssets = it }), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("显示子文件夹素材", style = MaterialTheme.typography.labelSmall)
                        Checkbox(checked = includeSubfolderAssets, onCheckedChange = null,
                            modifier = Modifier.size(24.dp).graphicsLayer { scaleX = .8f; scaleY = .8f })
                    }
                }
                if (open) folder.children.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { child ->
                            key(child.id) { FolderCard(child, counts[child.id] ?: 0, Modifier.weight(1f)) }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }

    @Composable
    private fun FolderCard(folder: FolderNode, count: Int, modifier: Modifier) {
        val iconSize by animateDpAsState((38.4f * folderScale).dp, tween(140), label = "folder-icon-size")
        val chosen = folder.id in selectedFolderIds
        Column(modifier.border(if (chosen) 1.5.dp else 0.dp, if (chosen) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp))
            .combinedClickable(enabled = !batchBusy && selectedAssetIds.isEmpty() && selectedTags.isEmpty() && selectedTagGroupIds.isEmpty(), role = androidx.compose.ui.semantics.Role.Button,
                onClick = { if (selectedFolderIds.isEmpty()) selectFolder(folder) else selectedFolderIds = if (chosen) selectedFolderIds - folder.id else selectedFolderIds + folder.id },
                onLongClick = { if (selectedAssetIds.isNotEmpty()) clearSelection(); selectedFolderIds = selectedFolderIds + folder.id; searchOpen = false; viewPickerOpen = false; directoryMenuOpen = false })
            .semantics { selected = chosen }.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_folder_24), contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(iconSize))
            Text("${folder.name} · $count", Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }

    @Composable
    private fun FolderMenuItem(
        row: FolderRow,
        expanded: Set<String> = expandedFolderIds,
        selected: Set<String> = setOfNotNull(selectedFolderId),
        onExpand: (String) -> Unit = { id -> expandedFolderIds = if (id in expandedFolderIds) expandedFolderIds - id else expandedFolderIds + id },
        onSelect: (String) -> Unit = { _ ->
            selectFolder(row.node)
        },
        multiple: Boolean = false
    ) {
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (multiple) { PickerCheckbox(row.node.id in selected,
                        allFolders(row.node.children).any { it.id in selected }); Spacer(Modifier.width(16.dp)) }
                    Spacer(Modifier.width((row.depth * 16).dp))
                    Icon(
                        painterResource(R.drawable.ic_folder_24),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        row.node.name,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (row.node.id in selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!multiple && row.node.id in selected) Icon(Icons.Default.Check, contentDescription = "已选中", modifier = Modifier.size(18.dp))
                }
            },
            trailingIcon = {
                if (row.node.children.isNotEmpty()) IconButton(onClick = { onExpand(row.node.id) }) {
                    Icon(if (row.node.id in expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (row.node.id in expanded) "收起" else "展开")
                }
            },
            onClick = {
                onSelect(row.node.id)
            }
        )
    }

    @Composable
    private fun Welcome(modifier: Modifier = Modifier) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            ElevatedCard(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("连接 Eagle 素材库", style = MaterialTheme.typography.headlineSmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(onClick = { sourceChooserOpen = true }, enabled = !remoteConnecting && !batchBusy) {
                        Text("选择素材库来源")
                    }
                    if (connected) OutlinedButton(onClick = ::openLibraryPicker) {
                        Text("选择 OneDrive 素材库")
                    }
                    Text(
                        "缩略图按需加载，只在查看或下载时读取原图。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun SearchSections(query: String, tags: List<String>, folders: List<FolderNode>) {
        var tagsOpen by rememberSaveable(query) { mutableStateOf(true) }
        var foldersOpen by rememberSaveable(query) { mutableStateOf(true) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(Modifier.fillMaxWidth().clickable { tagsOpen = !tagsOpen }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (tagsOpen) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, "展开或收起标签")
                    Text("标签 · ${tags.size}", style = MaterialTheme.typography.labelLarge)
                }
                if (tagsOpen) FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag -> AssistChip(onClick = { navigateBrowse(LibrarySection.TAG, null, tag, tag) }, label = { Text(tag) }) }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(Modifier.fillMaxWidth().clickable { foldersOpen = !foldersOpen }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (foldersOpen) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, "展开或收起文件夹")
                    Text("文件夹 · ${folders.size}", style = MaterialTheme.typography.labelLarge)
                }
                if (foldersOpen) folders.forEach { folder ->
                    DropdownMenuItem(text = { Text(folder.name) }, leadingIcon = { Icon(painterResource(R.drawable.ic_folder_24), null) }, onClick = { selectFolder(folder) })
                }
            }
        }
    }

    @Composable
    private fun LibraryContent(
        assets: List<AssetMeta>,
        mode: AssetViewMode,
        modifier: Modifier = Modifier,
        childFolder: FolderNode? = null,
        tagsPage: Boolean = false,
        query: String = "",
        searchTags: List<String> = emptyList(),
        searchFolders: List<FolderNode> = emptyList()
    ) {
        if (tagsPage && query.isBlank()) { TagCollections(modifier); return }
        val headerCount = if (childFolder == null && query.isBlank()) 0 else 1
        val contentTop = LibraryLogic.galleryTopPadding(WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value).dp
        val contentLeftPx = with(LocalDensity.current) { 12.dp.toPx() }
        val header: @Composable () -> Unit = {
            if (query.isNotBlank()) SearchSections(query, searchTags, searchFolders)
            else childFolder?.let { ChildFolders(it) }
        }
        val animatedListHeight by animateDpAsState(listRowHeight.dp, animationSpec = tween(140), label = "list-row-height")
        BoxWithConstraints(modifier.fillMaxSize()) {
            if (assets.isEmpty()) {
                Column(Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(start = 12.dp, top = contentTop, end = 12.dp, bottom = 104.dp)) {
                    header()
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (indexing) "" else "这个目录里暂无素材。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                }
            } else if (mode == AssetViewMode.GRID) {
                val state = rememberLazyGridState()
                val columns = LibraryLogic.columnCount(maxWidth.value, gridCellSize)
                val cellWidth = with(LocalDensity.current) { ((maxWidth - 24.dp - 8.dp * (columns - 1)) / columns).roundToPx() }.coerceAtLeast(1)
                ThumbnailPrefetch(assets, cellWidth, scrolling = { state.isScrollInProgress }) { state.layoutInfo.visibleItemsInfo.map { it.index - headerCount }.filter { it >= 0 } }
                LazyVerticalGrid(
                    state = state,
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize().selectionDrag(assets, mode,
                        hitTest = { p -> state.layoutInfo.visibleItemsInfo.firstOrNull { Rect(it.offset.x.toFloat(), it.offset.y.toFloat(), (it.offset.x + it.size.width).toFloat(), (it.offset.y + it.size.height).toFloat()).contains(p - Offset(contentLeftPx, state.layoutInfo.beforeContentPadding.toFloat())) }?.index?.minus(headerCount) },
                        scroll = { state.scrollBy(it) }).assetPinchZoom(mode),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, top = contentTop, end = 12.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (headerCount > 0) item(key = "browse-header", span = { GridItemSpan(maxLineSpan) }) { header() }
                    items(assets, key = { it.id }, contentType = { "asset" }) { asset -> AssetTile(asset, selectionList = assets, modifier = Modifier.animateItem(), cellWidth = cellWidth) }
                }
            } else if (mode == AssetViewMode.LIST) {
                val state = rememberLazyListState()
                val cellWidth = with(LocalDensity.current) { listRowHeight.dp.roundToPx() }
                ThumbnailPrefetch(assets, cellWidth, scrolling = { state.isScrollInProgress }) { state.layoutInfo.visibleItemsInfo.map { it.index - headerCount }.filter { it >= 0 } }
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize().selectionDrag(assets, mode,
                        hitTest = { p -> val y = p.y - state.layoutInfo.beforeContentPadding; state.layoutInfo.visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size }?.index?.minus(headerCount) },
                        scroll = { state.scrollBy(it) }).assetPinchZoom(mode),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, top = contentTop, end = 12.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (headerCount > 0) item(key = "browse-header") { header() }
                    items(assets, key = { it.id }, contentType = { "asset" }) { asset -> AssetListItem(asset, assets, animatedListHeight, Modifier.animateItem(), cellWidth) }
                }
            } else {
                val state = rememberLazyStaggeredGridState()
                val columns = LibraryLogic.columnCount(maxWidth.value, waterfallCellSize)
                val cellWidth = with(LocalDensity.current) { ((maxWidth - 24.dp - 8.dp * (columns - 1)) / columns).roundToPx() }.coerceAtLeast(1)
                ThumbnailPrefetch(assets, cellWidth, true, scrolling = { state.isScrollInProgress }) { state.layoutInfo.visibleItemsInfo.map { it.index - headerCount }.filter { it >= 0 } }
                LazyVerticalStaggeredGrid(
                    state = state,
                    columns = StaggeredGridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize().selectionDrag(assets, mode,
                        hitTest = { p -> state.layoutInfo.visibleItemsInfo.firstOrNull { Rect(it.offset.x.toFloat(), it.offset.y.toFloat(), (it.offset.x + it.size.width).toFloat(), (it.offset.y + it.size.height).toFloat()).contains(p - Offset(contentLeftPx, state.layoutInfo.beforeContentPadding.toFloat())) }?.index?.minus(headerCount) },
                        scroll = { state.scrollBy(it) }).assetPinchZoom(mode),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, top = contentTop, end = 12.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    if (headerCount > 0) item(key = "browse-header", span = StaggeredGridItemSpan.FullLine) { header() }
                    items(assets, key = { it.id }, contentType = { "asset" }) { asset -> AssetTile(asset, true, assets, Modifier.animateItem(), cellWidth) }
                }
            }
        }
    }

    private fun clearSelection() {
        selectedAssetIds = emptySet()
        selectedFolderIds = emptySet()
        selectedTags = emptySet()
        selectedTagGroupIds = emptySet()
        selectionAssets = emptyList()
    }

    private fun selectedAssets() = selectionAssets.filter { it.id in selectedAssetIds }

    private fun beginSelection(asset: AssetMeta, visible: List<AssetMeta>) {
        if (batchBusy) return
        if (selectedTags.isNotEmpty() || selectedTagGroupIds.isNotEmpty() || selectedFolderIds.isNotEmpty()) return
        selectedFolderIds = emptySet()
        if (selectedAssetIds.isEmpty()) selectionAssets = visible
        selectedAssetIds = selectedAssetIds + asset.id
        searchOpen = false; viewPickerOpen = false; directoryMenuOpen = false
    }

    private fun tapAsset(asset: AssetMeta, visible: List<AssetMeta>) {
        if (batchBusy) return
        if (selectedFolderIds.isNotEmpty() || selectedTags.isNotEmpty() || selectedTagGroupIds.isNotEmpty()) return
        if (selectedAssetIds.isEmpty()) { detailAssets = visible; openAssetDetails(asset) }
        else {
            selectedAssetIds = if (asset.id in selectedAssetIds) selectedAssetIds - asset.id else selectedAssetIds + asset.id
            if (selectedAssetIds.isEmpty()) clearSelection()
        }
    }

    @Composable
    private fun Modifier.assetInteraction(asset: AssetMeta, visible: List<AssetMeta>): Modifier {
        val chosen = asset.id in selectedAssetIds
        return this.border(if (chosen) 2.dp else 0.dp,
            if (chosen) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
            RoundedCornerShape(12.dp))
            .semantics {
                selected = chosen
                if (batchBusy) disabled()
                onClick("查看或选择素材") { tapAsset(asset, visible); true }
                onLongClick("选择素材") { beginSelection(asset, visible); true }
            }
    }

    @Composable
    private fun Modifier.selectionDrag(
        assets: List<AssetMeta>, mode: AssetViewMode,
        hitTest: (Offset) -> Int?, scroll: suspend (Float) -> Unit
    ): Modifier {
        val currentAssets by rememberUpdatedState(assets)
        val currentHitTest by rememberUpdatedState(hitTest)
        val currentScroll by rememberUpdatedState(scroll)
        return pointerInput(libraryId, mode) {
            coroutineScope {
                var scrolling: Job? = null
                var position = Offset.Zero
                var anchor = -1
                var last = -1
                var ids = emptyList<String>()
                var initial = emptySet<String>()
                fun extend() {
                    val end = currentHitTest(position) ?: return
                    if (end != last && anchor >= 0) {
                        selectedAssetIds = LibraryLogic.selectionRange(ids, initial, anchor, end)
                        last = end
                    }
                }
                // One recognizer owns tap/long-press/drag: child clicks must not toggle on finger-up.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val visible = currentAssets
                    val startIndex = currentHitTest(down.position) ?: -1
                    val held = awaitLongPressOrCancellation(down.id)
                    if (held == null) {
                        if (currentEvent.changes.any { it.changedToUp() } && startIndex in visible.indices) {
                            tapAsset(visible[startIndex], visible)
                            currentEvent.changes.forEach { it.consume() }
                        }
                    } else if (!batchBusy && selectedFolderIds.isEmpty() && selectedTags.isEmpty() && selectedTagGroupIds.isEmpty() && startIndex in visible.indices) {
                            anchor = startIndex
                                initial = selectedAssetIds
                                ids = visible.map { it.id }
                                position = held.position; last = -1
                                beginSelection(visible[anchor], visible)
                                extend()
                                scrolling = launch {
                                    val edge = 56.dp.toPx()
                                    while (isActive) {
                                        val speed = when {
                                            position.y < edge -> -18.dp.toPx() * ((edge - position.y) / edge).coerceIn(0f, 1f)
                                            position.y > size.height - edge -> 18.dp.toPx() * ((position.y - size.height + edge) / edge).coerceIn(0f, 1f)
                                            else -> 0f
                                        }
                                        if (speed != 0f) { currentScroll(speed); extend() }
                                        delay(16)
                                    }
                                }
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == held.id } ?: break
                                change.consume()
                                if (!change.pressed || event.changes.count { it.pressed } > 1) break
                                position = change.position
                                extend()
                            }
                        } finally { scrolling?.cancel(); anchor = -1 }
                    }
                }
            }
        }
    }

    @Composable
    private fun Modifier.assetPinchZoom(mode: AssetViewMode, folderArea: Boolean = false): Modifier {
        val density = LocalDensity.current.density
        return pointerInput(mode, folderArea, density) {
        val gallery = !folderArea && mode != AssetViewMode.LIST
        awaitEachGesture {
            var resized = false
            do {
                // Claim two-finger gallery gestures before scrolling/long-press consumes them.
                val event = awaitPointerEvent(if (gallery) PointerEventPass.Initial else PointerEventPass.Main)
                if (event.changes.count { it.pressed } >= 2 && !hasSelection() &&
                    (gallery || event.changes.none { it.isConsumed })) {
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) {
                        resized = true
                        if (folderArea) {
                            folderScale = LibraryLogic.resizeCell(folderScale, zoom, 2f / 3f, 2f)
                        } else if (mode == AssetViewMode.GRID) {
                            gridCellSize = LibraryLogic.resizeGalleryCell(size.width / density, gridCellSize, zoom, 220f)
                        } else if (mode == AssetViewMode.WATERFALL) {
                            waterfallCellSize = LibraryLogic.resizeGalleryCell(size.width / density, waterfallCellSize, zoom, 260f)
                        } else {
                            listRowHeight = LibraryLogic.resizeCell(listRowHeight, zoom, 88f / 3f, 88f)
                        }
                        event.changes.forEach { it.consume() }
                    }
                    if (gallery) event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
            if (resized) saveViewerPreferences()
        }
        }
    }

    @Composable
    private fun AssetTile(asset: AssetMeta, naturalRatio: Boolean = false, selectionList: List<AssetMeta>, modifier: Modifier = Modifier, cellWidth: Int) {
        var retryEpoch by remember(asset.id) { mutableIntStateOf(0) }
        val preview = assetThumbnail(asset, LibraryLogic.thumbnailEdge(asset.width, asset.height, cellWidth, naturalRatio), retryEpoch)
        val bitmap = preview.bitmap
        ElevatedCard(
            modifier = modifier.assetInteraction(asset, selectionList)
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(
                    if (naturalRatio && asset.width > 0 && asset.height > 0)
                        (asset.width.toFloat() / asset.height).coerceIn(.55f, 1.8f)
                    else 1f
                )
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap == null) {
                    if (preview.loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else ThumbnailPlaceholder(asset, preview.unavailable) { retryEpoch++ }
                } else {
                    Image(
                        bitmap.asImageBitmap(),
                        contentDescription = asset.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (asset.id in selectedAssetIds) SelectionMark(Modifier.align(Alignment.TopEnd))
            }
        }
    }

    @Composable
    private fun AssetListItem(asset: AssetMeta, selectionList: List<AssetMeta>, rowHeight: androidx.compose.ui.unit.Dp, modifier: Modifier, cellWidth: Int) {
        var retryEpoch by remember(asset.id) { mutableIntStateOf(0) }
        val preview = assetThumbnail(asset, LibraryLogic.thumbnailEdge(asset.width, asset.height, cellWidth, false), retryEpoch)
        val bitmap = preview.bitmap
        ElevatedCard(
            modifier = modifier.fillMaxWidth().assetInteraction(asset, selectionList)
        ) {
            Box {
            Row(Modifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(rowHeight).clip(CardDefaults.shape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap == null && preview.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else if (bitmap == null) {
                        if (preview.unavailable) Text(asset.ext.uppercase(Locale.ROOT), style = MaterialTheme.typography.titleMedium)
                        else TextButton(onClick = { retryEpoch++ }) { Text("重试") }
                    }
                    else Image(bitmap.asImageBitmap(), asset.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(asset.name, maxLines = if (rowHeight < 52.dp) 1 else 2, overflow = TextOverflow.Ellipsis,
                        style = if (rowHeight < 52.dp) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium)
                    if (rowHeight >= 52.dp) Text(
                        "${asset.ext.uppercase(Locale.ROOT)} · ${formatBytes(asset.size)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (asset.id in selectedAssetIds) SelectionMark(Modifier.align(Alignment.TopEnd))
            }
        }
    }

    @Composable
    private fun SelectionMark(modifier: Modifier) {
        Icon(Icons.Default.Check, contentDescription = "已选中",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = modifier.padding(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(3.dp).size(16.dp))
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    private fun AssetInfoSheet(asset: AssetMeta) {
        var detailsOpen by rememberSaveable { mutableStateOf(false) }
        var detailContentHeight by remember(asset.id) { mutableIntStateOf(0) }
        var controlsVisible by rememberSaveable { mutableStateOf(true) }
        var editTags by remember(asset.id) { mutableStateOf(false) }
        var editFolders by remember(asset.id) { mutableStateOf(false) }
        var editName by remember(asset.id) { mutableStateOf(false) }
        var checkerboard by remember { mutableStateOf(getSharedPreferences("viewer", MODE_PRIVATE).getBoolean("checkerboard", false)) }
        val metadata by produceState<JSONObject?>(null, asset.id, asset.mtime, detailsOpen) {
            value = if (!detailsOpen) null else withContext(Dispatchers.IO) {
                runCatching { JSONObject(String(readAssetFile(asset.id, "metadata.json"), StandardCharsets.UTF_8)) }.getOrNull()
            }
        }
        val colors = remember(metadata) { metadata?.let(LibraryLogic::paletteColors).orEmpty() }
        fun date(field: String): String {
            val time = metadata?.optLong(field) ?: 0L
            return if (time > 0) java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(java.util.Date(time)) else "—"
        }
        val pages = remember { detailAssets.ifEmpty { listOf(asset) } }
        val pager = rememberPagerState(initialPage = pages.indexOfFirst { it.id == asset.id }.coerceAtLeast(0), pageCount = { pages.size })
        var zoomed by remember(asset.id) { mutableStateOf(false) }
        LaunchedEffect(pager.settledPage) {
            val target = pages[pager.settledPage]
            if (target.id != infoAsset?.id) openAssetDetails(indexedAssets.firstOrNull { it.id == target.id } ?: target)
        }
        val folderNames = remember(folderTree, asset.folders) { flatFolderTree
            .filter { it.id in asset.folders }
            .map { it.name } }
        val preview: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                Box(Modifier.fillMaxSize()) {
                HorizontalPager(state = pager, reverseLayout = false, beyondViewportPageCount = 1,
                    userScrollEnabled = !zoomed && !batchBusy, modifier = Modifier.fillMaxSize(), key = { pages[it].id }) { page ->
                val asset = indexedAssets.firstOrNull { it.id == pages[page].id } ?: pages[page]
                var retryEpoch by remember(asset.id) { mutableIntStateOf(0) }
                val preview = assetThumbnail(asset, 900, retryEpoch)
                val original = if (infoAsset?.id == asset.id) detailOriginalBitmap else null
                val shownBitmap = original ?: preview.bitmap
                var scale by remember(asset.id, original) { mutableFloatStateOf(1f) }
                var translation by remember(asset.id, original) { mutableStateOf(Offset.Zero) }
                LaunchedEffect(scale, pager.currentPage) { if (pager.currentPage == page) zoomed = scale > 1f }
                Box(
                    Modifier.fillMaxSize()
                        .clipToBounds()
                        .background(androidx.compose.ui.graphics.Color.Black)
                        .pointerInput(asset.id) {
                            detectTapGestures {
                                if (detailsOpen) detailsOpen = false else controlsVisible = !controlsVisible
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (checkerboard) Canvas(Modifier.fillMaxSize()) {
                        val cell = 12.dp.toPx()
                        for (y in 0..(size.height / cell).toInt()) for (x in 0..(size.width / cell).toInt()) {
                            drawRect(if ((x + y) % 2 == 0) androidx.compose.ui.graphics.Color(0xFFE6E6E6) else androidx.compose.ui.graphics.Color(0xFFBDBDBD),
                                topLeft = Offset(x * cell, y * cell), size = Size(cell, cell))
                        }
                    }
                    if (shownBitmap == null && preview.loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else if (shownBitmap == null) ThumbnailPlaceholder(asset, preview.unavailable) { retryEpoch++ }
                    else Image(
                        shownBitmap.asImageBitmap(),
                        contentDescription = asset.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(shownBitmap, asset.id) {
                                awaitEachGesture {
                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.changes.count { it.pressed } >= 2) {
                                            val next = (scale * event.calculateZoom()).coerceIn(1f, 6f)
                                            scale = next
                                            translation = if (next == 1f) Offset.Zero else translation + event.calculatePan()
                                            event.changes.forEach { it.consume() }
                                        } else if (scale > 1f && event.changes.any { it.pressed }) {
                                            translation += event.calculatePan()
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = translation.x
                                translationY = translation.y
                            }
                    )
                    if (infoAsset?.id == asset.id && detailOriginalLoading) CircularProgressIndicator()
                }
                }
                if (controlsVisible || detailsOpen) Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        onClick = ::closeAssetDetails) {
                        Icon(Icons.Default.Close, "关闭全屏预览")
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text("${pager.settledPage + 1} / ${pages.size}", Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
                }
            }
        }
        val details: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .onSizeChanged { detailContentHeight = it.height }.padding(bottom = 16.dp)) {
                Row(Modifier.fillMaxWidth().height(48.dp).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("素材详情", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                }
                if (colors.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { color ->
                        Box(Modifier.size(18.dp).background(androidx.compose.ui.graphics.Color(color), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    }
                }
                Surface(onClick = { editName = true }, enabled = !batchBusy && source == LibrarySource.ONEDRIVE,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text(asset.fileName, Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
                }
                InspectorText(asset.annotation.ifBlank { "暂无注释" }, muted = asset.annotation.isBlank())
                InspectorText(asset.url.ifBlank { "暂无来源网址" }, muted = asset.url.isBlank(), link = asset.url)
                InspectorSection("标签", asset.tags, !batchBusy) { editTags = true }
                InspectorSection("文件夹", folderNames, !batchBusy) { editFolders = true }
                Text("基本信息", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AssetInfoRow("评分", "★".repeat((metadata?.optInt("rating") ?: 0).coerceIn(0, 5)) + "☆".repeat(5 - (metadata?.optInt("rating") ?: 0).coerceIn(0, 5)))
                AssetInfoRow("尺寸", if (asset.width > 0 && asset.height > 0) "${asset.width} × ${asset.height}" else "未知")
                AssetInfoRow("文件大小", formatBytes(asset.size))
                AssetInfoRow("格式", asset.ext.uppercase(Locale.ROOT))
                AssetInfoRow("添加日期", date("btime"))
                AssetInfoRow("创建日期", date("mtime"))
                AssetInfoRow("修改日期", date("modificationTime"))
                if (batchBusy) CircularProgressIndicator(Modifier.padding(16.dp).size(20.dp), strokeWidth = 2.dp)
            }
        }
        Dialog(onDismissRequest = { if (detailsOpen) detailsOpen = false else closeAssetDetails() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            val window = (LocalView.current.parent as DialogWindowProvider).window
            SideEffect { window.setDimAmount(0f) }
            Surface(Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    preview(Modifier.fillMaxSize())
                    val originalButtonWidth = if (maxWidth < 400.dp) 80.dp else 100.dp
                    val compactActions = maxWidth < 384.dp
                    val actionColors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    val originalAction: @Composable () -> Unit = {
                        DetailAction(if (LibraryLogic.isViewableImage(asset.ext)) "查看原图" else "打开文件",
                            originalButtonWidth, !detailOriginalLoading && detailOriginalBitmap == null) {
                            if (LibraryLogic.isViewableImage(asset.ext)) loadDetailOriginal(asset) else openExternalFile(asset)
                        }
                    }
                    val expandedPanelWidth = if (maxWidth < 700.dp) maxWidth - 36.dp else minOf(340.dp, maxWidth - 40.dp)
                    val panelWidth = animateDpAsState(if (detailsOpen) expandedPanelWidth else 40.dp,
                        tween(240), label = "detail-panel-width")
                    val availablePanelHeight = maxHeight -
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() -
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() - 24.dp
                    val footerHeight = if (compactActions) 112.dp else 64.dp
                    val contentHeight = with(LocalDensity.current) { detailContentHeight.toDp() }
                    val expandedPanelHeight = minOf(availablePanelHeight, if (detailContentHeight > 0) contentHeight + footerHeight else 560.dp)
                    val panelHeight = animateDpAsState(if (detailsOpen) expandedPanelHeight.coerceAtLeast(40.dp) else 40.dp,
                        tween(240), label = "detail-panel-height")
                    val panelExpanded by remember { derivedStateOf { panelHeight.value > 40.dp } }
                    if (controlsVisible || detailsOpen || panelExpanded) Surface(
                        Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 12.dp)
                            .layout { measurable, _ ->
                                // Read animated sizes during measurement, not composition; text keeps its final width.
                                val width = panelWidth.value.roundToPx()
                                val height = panelHeight.value.roundToPx()
                                val child = measurable.measure(Constraints.fixed(width, height))
                                layout(width, height) { child.place(0, 0) }
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 0.dp
                    ) {
                        Box {
                            androidx.compose.animation.AnimatedVisibility(detailsOpen,
                                enter = fadeIn(tween(160)), exit = fadeOut(tween(120)),
                                modifier = Modifier.fillMaxSize().wrapContentWidth(Alignment.End, unbounded = true)
                                    .requiredWidth(expandedPanelWidth).padding(bottom = footerHeight)) { details() }
                            FilledTonalIconButton(shape = CircleShape, colors = actionColors,
                                onClick = { detailsOpen = !detailsOpen; controlsVisible = true },
                                modifier = Modifier.align(Alignment.BottomEnd).size(40.dp)) {
                                Icon(if (detailsOpen) Icons.Default.Close else Icons.Outlined.Info,
                                    if (detailsOpen) "收起素材详情" else "展开素材详情")
                            }
                        }
                    }
                    if (controlsVisible || detailsOpen) {
                        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (compactActions) originalAction()
                        Row(Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (!compactActions) originalAction()
                            Spacer(Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            FilledTonalIconButton(shape = CircleShape, colors = actionColors, onClick = {
                                checkerboard = !checkerboard
                                getSharedPreferences("viewer", MODE_PRIVATE).edit().putBoolean("checkerboard", checkerboard).apply()
                            }) { Icon(painterResource(R.drawable.ic_grid_on_24), if (checkerboard) "切换纯色背景" else "切换透明棋盘格背景", Modifier.size(20.dp)) }
                            FilledTonalIconButton(shape = CircleShape, colors = actionColors, enabled = !batchBusy,
                                onClick = { runBatch(listOf(asset), "下载") }) { Icon(painterResource(R.drawable.ic_download_24), "下载原文件", Modifier.size(20.dp)) }
                            FilledTonalIconButton(shape = CircleShape, colors = actionColors, enabled = !batchBusy,
                                onClick = { if (asset.isDeleted) runBatch(listOf(asset), "还原") else trashCandidate = listOf(asset) }) {
                                Icon(painterResource(if (asset.isDeleted) R.drawable.ic_restore_from_trash_24 else R.drawable.ic_delete_24),
                                    if (asset.isDeleted) "还原素材" else "移到回收站", Modifier.size(20.dp))
                            }
                            FilledTonalIconButton(shape = CircleShape, colors = actionColors, enabled = !batchBusy,
                                onClick = { shareAssets(listOf(asset)) }) { Icon(Icons.Outlined.Share, "分享原文件", Modifier.size(20.dp)) }
                            Spacer(Modifier.size(48.dp)) // The morphing info/close button owns this fixed slot.
                            }
                        }
                        }
                    }
                }
            }
        }
        if (editName) NameDialog("重命名素材", asset.name, { editName = false }) { name -> editName = false; renameAsset(asset, name) }
        if (editFolders) FolderPicker(asset.folders, true, { editFolders = false }) { folders ->
            editFolders = false
            saveAssetList(asset, "folders", folders.toList())
        }
        if (editTags) TagPicker(asset.tags, { editTags = false }) { tags -> editTags = false; saveAssetList(asset, "tags", tags) }
    }

    @Composable
    private fun InspectorText(value: String, muted: Boolean = false, link: String = "") {
        Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(value, Modifier.clickable(enabled = link.startsWith("https://") || link.startsWith("http://")) {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                    .onFailure { Toast.makeText(this, "没有可打开网址的应用", Toast.LENGTH_SHORT).show() }
            }.padding(10.dp), style = MaterialTheme.typography.bodyMedium,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun InspectorSection(title: String, values: List<String>, enabled: Boolean, onClick: () -> Unit) {
        Text(title, Modifier.padding(start = 16.dp, top = 10.dp, bottom = 3.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (values.isEmpty()) Text("添加$title", Modifier.clickable(enabled = enabled, onClick = onClick), style = MaterialTheme.typography.bodyMedium)
                    values.forEach { value -> AssistChip(onClick = onClick, enabled = enabled, label = { Text(value) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) }
                }
                IconButton(onClick = onClick, enabled = enabled) { Icon(Icons.Default.Add, contentDescription = "添加或编辑$title", modifier = Modifier.size(18.dp)) }
            }
    }

    private fun saveAssetList(asset: AssetMeta, field: String, values: List<String>) {
        if (batchBusy) return
        val clean = values.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if ((field == "tags" && clean == asset.tags) || (field == "folders" && clean.toSet() == asset.folders)) return
        batchBusy = true
        queueEdit {
            val snapshot = editSnapshot()
            val result = runCatching {
                val updated = mutateAssetNow(asset) { LibraryLogic.setMetadataList(it, field, clean) }
                val items = (snapshot.associateBy { it.id } + (asset.id to updated)).values
                runCatching { saveIndexCache(items) }.onFailure { Log.w("MAGLE", "Metadata saved; index cache needs refresh", it) }
                publishAssets(items)
                updated
            }
            runOnUiThread {
                batchBusy = false
                result.onSuccess { if (infoAsset?.id == asset.id) infoAsset = it }
                    .onFailure { Toast.makeText(this, "保存失败：${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    @Composable
    private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
        var name by remember { mutableStateOf(initial) }
        val valid = runCatching { if (title.contains("标签")) LibraryLogic.validTagName(name) else LibraryLogic.validName(name) }.isSuccess
        AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
            text = { OutlinedTextField(name, { name = it }, label = { Text(if (title.contains("素材")) "名称（不含扩展名）" else "名称") }, singleLine = true, isError = !valid, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = valid) { Text("保存") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun TagPicker(initial: List<String>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
        var selected by remember { mutableStateOf(initial.toSet()) }
        var query by remember { mutableStateOf("") }
        val available = remember(indexedAssets, tagGroups, selected) {
            (indexedAssets.flatMap { it.tags } + tagGroups.flatMap { group ->
                val tags = group.optJSONArray("tags") ?: JSONArray()
                (0 until tags.length()).map { tags.optString(it) }
            } + selected).filter { it.isNotBlank() }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
        AlertDialog(onDismissRequest = onDismiss, title = { Text("选择标签") }, text = {
            Column {
                OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("搜索或新增标签") }, modifier = Modifier.fillMaxWidth())
                val clean = query.trim()
                if (clean.isNotEmpty() && clean !in available) TextButton(onClick = { selected = selected + clean; query = "" }) { Text("＋ 新增“$clean”") }
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    item { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selected.forEach { tag -> FilterChip(selected = true, onClick = { selected = selected - tag }, label = { Text(tag) }, trailingIcon = { Icon(Icons.Default.Close, "移除标签", Modifier.size(16.dp)) }) }
                    } }
                    items(available.filter { it.contains(query, true) }, key = { it }) { tag ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(tag in selected, role = androidx.compose.ui.semantics.Role.Checkbox,
                            onValueChange = { checked -> selected = if (checked) selected + tag else selected - tag }), verticalAlignment = Alignment.CenterVertically) {
                            PickerCheckbox(tag in selected)
                            Spacer(Modifier.width(16.dp))
                            Text(tag, Modifier.weight(1f))
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { onSave(selected.toList()) }) { Text("保存") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
    }

    @Composable
    private fun PickerCheckbox(checked: Boolean, indeterminate: Boolean = false) {
        TriStateCheckbox(if (checked) ToggleableState.On else if (indeterminate) ToggleableState.Indeterminate else ToggleableState.Off,
            onClick = null, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = .75f; scaleY = .75f })
    }

    private fun downloadFolders() {
        val ids = flatFolderTree.filter { it.id in selectedFolderIds }.flatMap { allFolders(listOf(it)) }.map { it.id }.toSet()
        val assets = indexedAssets.filter { !it.isDeleted && LibraryLogic.belongsToAnyFolder(it.folders, ids) }
        if (assets.isEmpty()) Toast.makeText(this, "选中的文件夹没有素材", Toast.LENGTH_SHORT).show()
        else runBatch(assets, "下载")
    }

    private fun changeFolders(parentId: String? = null, name: String? = null) {
        if (batchBusy || !canUpload()) return
        val ids = selectedFolderIds
        val expected = folderPositions(folderTree)
        batchBusy = true
        queueEdit {
            val result = runCatching {
                val metadata = mutateLibraryNow {
                    val current = folderPositions(parseFolders(it.getJSONArray("folders")))
                    if (ids.any { id -> current[id] != expected[id] }) throw LibraryConflict()
                    LibraryLogic.changeFolders(it, ids, parentId, name)
                }
                parseFolders(metadata.getJSONArray("folders"))
            }
            runOnUiThread {
                batchBusy = false
                result.onSuccess { nodes -> folderTree = nodes; selectedFolderName = allFolders(nodes).firstOrNull { it.id == selectedFolderId }?.name ?: selectedFolderName; clearSelection() }
                    .onFailure { Toast.makeText(this, "目录修改失败：${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun folderPositions(nodes: List<FolderNode>, parent: String? = null): Map<String, Pair<String, String?>> = buildMap {
        nodes.forEach { node -> put(node.id, node.name to parent); putAll(folderPositions(node.children, node.id)) }
    }

    private fun mutateJsonFile(relative: String, merge: Boolean = false, mutate: (JSONObject) -> Unit): JsonChange {
        if (source == LibrarySource.SMB) return remote().mutateSmbJson(relative, mutate)
        if (source == LibrarySource.GOOGLE || source == LibrarySource.DROPBOX) return remote().mutateCloudJson(relative, merge, mutate)
        if (source == LibrarySource.LOCAL) {
            val target = if (!relative.contains('/')) localRoot().findFile(relative) else
                localAssetDirectories[relative.substringAfter("images/").substringBefore(".info/")]?.findFile(relative.substringAfterLast('/'))
            // ponytail: SAF providers lack compare-and-swap; local libraries must have one writer.
            val old = readDocument(target)
            val json = JSONObject(String(old, StandardCharsets.UTF_8))
            mutate(json)
            val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
            if (!readDocument(target).contentEquals(old)) throw LibraryConflict()
            writeDocument(target, bytes)
            return JsonChange(old, bytes)
        }
        check(source == LibrarySource.ONEDRIVE || source == LibrarySource.WEBDAV) { "此来源暂不支持安全编辑" }
        val path = if (source == LibrarySource.ONEDRIVE) {
            if (relative.startsWith("images/")) assetFile(relative.substringAfter("images/").substringBefore(".info/"), relative.substringAfterLast('/'))
            else libraryFile(libraryId!!, relative)
        } else webDavPath(relative.split('/').joinToString("/") { encPath(it) })
        return updateVersionedJson(read = {
            if (source == LibrarySource.ONEDRIVE) {
                val version = graphJson(path.removeSuffix(":/content") + ":?\$select=eTag").getString("eTag")
                VersionedJson(graphBytes(path), version)
            } else webDavRequest(path).use { response ->
                val version = requireStrongEtag(response.header("ETag"))
                VersionedJson(response.body.bytes(), version)
            }
        }, write = { snapshot, bytes ->
            try {
                if (source == LibrarySource.ONEDRIVE) graphPut(path, bytes, snapshot.version)
                else webDavRequest(path, "PUT", bytes.toRequestBody("application/json".toMediaType()), ifMatch = snapshot.version).close()
            } catch (error: HttpFailure) {
                if (error.code == 412) throw LibraryConflict() else throw error
            }
        }, merge = merge, mutate = mutate)
    }

    private fun mergeLibraryMtime(id: String, now: Long) {
        mutateJsonFile("mtime.json", merge = true) { mergeMtime(it, id, now) }
    }

    private fun rollbackJsonFile(path: String, change: JsonChange, error: Exception): Nothing {
        try {
            mutateJsonFile(path) { current ->
                if (current.toString() != JSONObject(String(change.after, StandardCharsets.UTF_8)).toString()) throw LibraryConflict()
                val old = JSONObject(String(change.before, StandardCharsets.UTF_8))
                current.keys().asSequence().toList().forEach { current.remove(it) }
                old.keys().forEach { current.put(it, old.get(it)) }
            }
        } catch (_: Exception) {
            throw java.io.IOException("信息已写入，但清单更新失败且无法安全回退；请更新索引确认，未强行覆盖其他修改", error)
        }
        throw error
    }

    private fun mutateLibraryNow(mutate: (JSONObject) -> Unit): JSONObject {
        val now = System.currentTimeMillis()
        val change = mutateJsonFile("metadata.json") { mutate(it); it.put("modificationTime", now) }
        try { mergeLibraryMtime("all", now) }
        catch (error: Exception) { rollbackJsonFile("metadata.json", change, error) }
        val metadata = JSONObject(String(change.after, StandardCharsets.UTF_8))
        runCatching { folderCacheFile().writeText(metadata.toString()) }.onFailure { Log.w("MAGLE", "Library metadata cache needs refresh", it) }
        return metadata
    }

    private fun graphRename(item: JSONObject, name: String): JSONObject {
        val bytes = JSONObject().put("name", name).toString().toByteArray(StandardCharsets.UTF_8)
        return JSONObject(String(readResponse(connection(GRAPH + "/drives/${enc(driveId!!)}/items/${enc(item.getString("id"))}").apply {
            requestMethod = "PATCH"; doOutput = true
            setRequestProperty("Authorization", "Bearer ${graphAccessToken()}")
            setRequestProperty("If-Match", item.getString("eTag"))
            setRequestProperty("Content-Type", "application/json")
            setFixedLengthStreamingMode(bytes.size); outputStream.use { it.write(bytes) }
        }), StandardCharsets.UTF_8))
    }

    private fun renameAsset(asset: AssetMeta, raw: String) {
        if (batchBusy || source != LibrarySource.ONEDRIVE) return
        val name = LibraryLogic.validName(raw)
        if (name == asset.name) return
        batchBusy = true
        queueEdit {
            val snapshot = editSnapshot()
            val result = runCatching {
                val renamed = mutableListOf<Pair<JSONObject, String>>()
                // ponytail: Graph has no cross-file transaction; use conditional rollback and surface uncertain state.
                try {
                    val metadata = JSONObject(String(readAssetFile(asset.id, "metadata.json"), StandardCharsets.UTF_8))
                    check(metadata.getString("name") == asset.name) { "名称已被其他设备修改，请更新索引" }
                    for ((oldName, newName) in listOf(asset.fileName to "$name.${asset.ext}", asset.thumbnailName to "${name}_thumbnail.png")) {
                        val item = try { graphJson(assetFile(asset.id, oldName).removeSuffix(":/content") + ":?\$select=id,eTag,name") }
                        catch (error: HttpFailure) { if (error.code == 404 && oldName == asset.thumbnailName) continue else throw error }
                        renamed += graphRename(item, newName) to oldName
                    }
                    mutateAssetNow(asset) { json -> check(json.getString("name") == asset.name) { "元数据已更改" }; json.put("name", name) }
                } catch (error: Exception) {
                    val currentName = runCatching { JSONObject(String(readAssetFile(asset.id, "metadata.json"), StandardCharsets.UTF_8)).getString("name") }.getOrNull()
                    if (currentName != asset.name) throw java.io.IOException("重命名状态需确认，未回退文件以避免破坏新元数据；请更新索引", error)
                    var rollbackFailed = false
                    renamed.asReversed().forEach { (item, oldName) -> runCatching { graphRename(item, oldName) }.onFailure { rollbackFailed = true } }
                    if (rollbackFailed) throw java.io.IOException("重命名中断且无法完全回退，请更新索引确认状态", error)
                    throw error
                }
            }
            result.onSuccess { updated ->
                val items = (snapshot.associateBy { it.id } + (asset.id to updated)).values
                runCatching { saveIndexCache(items) }; publishAssets(items)
            }
            runOnUiThread {
                batchBusy = false
                result.onSuccess { if (infoAsset?.id == asset.id) infoAsset = it }
                    .onFailure { Toast.makeText(this, "重命名失败：${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    @Composable
    private fun FilePlaceholder(asset: AssetMeta) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(asset.ext.uppercase(Locale.ROOT).ifBlank { "FILE" }, style = MaterialTheme.typography.headlineSmall)
            Text(asset.fileName, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }

    @Composable
    private fun ThumbnailPlaceholder(asset: AssetMeta, unavailable: Boolean, onRetry: () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilePlaceholder(asset)
            if (!unavailable) TextButton(onClick = onRetry) { Text("加载失败 · 重试") }
        }
    }

    private data class ThumbnailState(val bitmap: Bitmap? = null, val loading: Boolean = true, val unavailable: Boolean = false)
    private class NoThumbnailException : java.io.IOException("没有可用缩略图")

    private fun bitmapKey(asset: AssetMeta, edge: Int) = "${thumbnailCacheFile(asset).name}:$edge"

    private suspend fun thumbnailBitmap(asset: AssetMeta, edge: Int): Bitmap {
        val key = bitmapKey(asset, edge)
        bitmapCache.get(key)?.let { return it }
        val lock = thumbnailLocks[(thumbnailCacheFile(asset).name.hashCode() and Int.MAX_VALUE) % thumbnailLocks.size]
        lock.lock()
        try {
            return thumbnailRequests.withPermit {
                withContext(Dispatchers.IO) {
                    bitmapCache.get(key) ?: (decodeSampled(loadThumbnail(asset), edge, coverTarget = true)
                        ?: throw java.io.IOException("缩略图解码失败")).also { it.prepareToDraw(); bitmapCache.put(key, it) }
                }
            }
        } finally { lock.unlock() }
    }

    @Composable
    private fun ThumbnailPrefetch(assets: List<AssetMeta>, cellWidth: Int, naturalRatio: Boolean = false, scrolling: () -> Boolean, visible: () -> List<Int>) {
        LaunchedEffect(assets, cellWidth, naturalRatio, libraryId, connected, thumbnailCacheEpoch) {
            try {
            if (source == LibrarySource.ONEDRIVE && !connected) return@LaunchedEffect
            snapshotFlow { visible().let { Triple(it.minOrNull() ?: -1, it.maxOrNull() ?: -1, scrolling()) to (settingsOpen || infoAsset != null) } }
                .distinctUntilChanged().collectLatest { (range, blocked) ->
                    val (first, last, moving) = range
                    galleryScrolling = moving
                    if (moving || blocked) return@collectLatest
                    delay(180)
                    // ponytail: one nearby prefetch at a time; visible requests retain three of four slots.
                    for (index in LibraryLogic.prefetchIndices(assets.size, first, last)) {
                        val asset = assets[index]
                        try { thumbnailBitmap(asset, LibraryLogic.thumbnailEdge(asset.width, asset.height, cellWidth, naturalRatio)) } catch (error: Exception) {
                            if (error is CancellationException || error is InterruptedException) throw error
                        }
                    }
                }
            } finally { galleryScrolling = false }
        }
    }

    @Composable
    private fun assetThumbnail(asset: AssetMeta, maxEdge: Int, retryEpoch: Int): ThumbnailState {
        // Re-run failed startup requests once authentication becomes ready; failures are not file formats.
        val cached = bitmapCache.get(bitmapKey(asset, maxEdge))
        val state by produceState(ThumbnailState(cached, loading = cached == null), asset.id, asset.mtime, maxEdge, libraryId, thumbnailCacheEpoch, connected, retryEpoch) {
            value = ThumbnailState(cached, loading = cached == null)
            for (attempt in 0..2) {
                try {
                    val bitmap = thumbnailBitmap(asset, maxEdge)
                    value = ThumbnailState(bitmap, loading = false)
                    break
                } catch (error: Exception) {
                    if (error is CancellationException || error is InterruptedException) throw error
                    if (error is NoThumbnailException) { value = ThumbnailState(loading = false, unavailable = true); break }
                    val code = (error as? HttpFailure)?.code ?: 0
                    Log.w("MAGLE", "Thumbnail ${asset.ext}: ${error.javaClass.simpleName}, HTTP $code")
                    if (attempt == 2 || !LibraryLogic.isTransientThumbnailFailure(code)) {
                        value = ThumbnailState(loading = false)
                        break
                    }
                    delay(maxOf(1500L * (attempt + 1), (error as? HttpFailure)?.retryAfterMs ?: 0L).coerceAtMost(60_000L))
                }
            }
        }
        return state
    }

    @Composable
    private fun DetailAction(
        label: String,
        width: androidx.compose.ui.unit.Dp,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.width(width),
            shape = RoundedCornerShape(19.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }

    @Composable
    private fun AssetInfoRow(label: String, value: String) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), verticalAlignment = Alignment.Top) {
            Text(label, Modifier.width(82.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        }
    }

    @Composable
    private fun FolderPicker(initial: Set<String>, multiple: Boolean, onDismiss: () -> Unit, rootLabel: String = "未分类", onConfirm: (Set<String>) -> Unit) {
        var selected by remember { mutableStateOf(initial) }
        var expanded by remember { mutableStateOf(emptySet<String>()) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (multiple) "编辑文件夹" else "选择文件夹") },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    if (!multiple) item {
                        DropdownMenuItem(text = { Text(rootLabel) }, onClick = {
                            selected = emptySet()
                            onConfirm(selected)
                        }, trailingIcon = { if (selected.isEmpty()) Icon(Icons.Default.Check, contentDescription = "已选中") })
                    }
                    items(flattenVisibleFolders(folderTree, expanded), key = { it.node.id }) { row ->
                        FolderMenuItem(row, expanded, selected,
                            onExpand = { id -> expanded = if (id in expanded) expanded - id else expanded + id },
                            onSelect = { id ->
                                selected = if (!multiple) setOf(id) else if (id in selected) selected - id else selected + id
                                if (!multiple) onConfirm(selected)
                            }, multiple = multiple)
                    }
                }
            },
            confirmButton = { if (multiple) TextButton(onClick = { onConfirm(selected) }) { Text("保存") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
        )
    }

    @Composable
    private fun TrashConfirmation(assets: List<AssetMeta>) {
        AlertDialog(
            onDismissRequest = { trashCandidate = emptyList() },
            title = { Text("移到回收站？") },
            text = { Text("将选中的 ${assets.size} 个素材移到 Eagle 回收站。可在这里还原，也可在 Eagle 中恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    trashCandidate = emptyList()
                    runBatch(assets, "回收站")
                }) { Text("移到回收站") }
            },
            dismissButton = {
                TextButton(onClick = { trashCandidate = emptyList() }) { Text("取消") }
            }
        )
    }

    @Composable
    private fun MoveFolderDialog(assets: List<AssetMeta>) {
        val initial = assets.map { it.folders }.reduceOrNull { common, folders -> common.intersect(folders) }.orEmpty()
        FolderPicker(initial, true, { moveCandidate = emptyList() }) { selected ->
            moveCandidate = emptyList()
            runBatch(assets, "移动", selected)
        }
    }

    @Composable
    private fun SettingsScreen() {
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        Scaffold(
            topBar = { CompactTitleBar("设置") }
        ) { padding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding).pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboard?.hide()
                }
            }) {
                val wide = maxWidth >= 840.dp
                Column(
                    Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (wide) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SettingsLibraries()
                                SettingsIndexUpdate()
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SettingsCache()
                                SettingsAbout()
                            }
                        }
                    } else {
                        SettingsLibraries()
                        SettingsIndexUpdate()
                        SettingsCache()
                        SettingsAbout()
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsAbout() {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.height(48.dp), contentAlignment = Alignment.CenterStart) {
                Text("关于", style = MaterialTheme.typography.titleMedium)
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                val colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ListItem(colors = colors, headlineContent = { Text("MAGLE · v${BuildConfig.VERSION_NAME}") },
                    trailingContent = { TextButton(onClick = { openProjectPage(APP_REPOSITORY) }) { Text("GitHub") } })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(colors = colors, headlineContent = { Text("软件更新") },
                    supportingContent = {
                        if (appUpdateChecking || appUpdateStatus.isNotBlank()) Text(if (appUpdateChecking) "正在检查…" else appUpdateStatus,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    },
                    trailingContent = { TextButton(onClick = {
                        if (appRelease != null) downloadOrInstallUpdate() else checkAppUpdate()
                    }, enabled = !appUpdateChecking && !appDownloadState.busy && !appInstallBusy) {
                        Text(if (appInstallBusy) "校验中…" else if (appRelease != null) appDownloadState.button else "检查更新")
                    } })
            }
        }
        if (appReleaseDialogOpen) appRelease?.let { release ->
            AlertDialog(onDismissRequest = { appReleaseDialogOpen = false },
                title = { Text("发现新版本 ${release.version}") },
                text = { Text(release.notes.ifBlank { "在应用内后台下载 APK，安装由系统确认。" },
                    Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { appReleaseDialogOpen = false; downloadOrInstallUpdate() }) { Text("立即更新") } },
                dismissButton = { TextButton(onClick = { appReleaseDialogOpen = false }) { Text("稍后") } })
        }
    }

    private fun openProjectPage(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show() }
    }

    private fun downloadOrInstallUpdate() {
        val release = appRelease ?: return
        if (appInstallBusy || appDownloadState.busy) return
        appInstallBusy = true
        lifecycleScope.launch {
            try {
                val ready = withContext(Dispatchers.IO) {
                    val completed = appDownload.savedRelease()?.version == release.version && appDownload.state().button == "安装更新"
                    if (completed) {
                        try { appDownload.validate(release) } catch (error: Exception) {
                            appDownload.clear(); throw error
                        }
                    } else appDownload.start(release)
                    completed
                }
                if (ready) {
                    if (!packageManager.canRequestPackageInstalls()) Toast.makeText(this@MainActivity,
                        "请允许 MAGLE 安装应用，返回后再点安装更新", Toast.LENGTH_LONG).show()
                    appDownload.install(release)
                }
                appDownloadState = withContext(Dispatchers.IO) { appDownload.state() }
                appUpdateStatus = appDownloadState.message
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                appUpdateStatus = error.message ?: "更新失败，请重试"
                appDownloadState = withContext(Dispatchers.IO) { appDownload.state() }
                Toast.makeText(this@MainActivity, appUpdateStatus, Toast.LENGTH_LONG).show()
            } finally { appInstallBusy = false }
        }
    }

    private fun checkAppUpdate() {
        if (appUpdateChecking) return
        appUpdateChecking = true
        lifecycleScope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    fetchAppRelease(BuildConfig.VERSION_NAME)
                }
                appRelease = release
                appUpdateStatus = if (release == null) "当前已是最新版本" else "发现新版本 ${release.version}"
                Toast.makeText(this@MainActivity, appUpdateStatus, Toast.LENGTH_SHORT).show()
                if (release != null) appReleaseDialogOpen = true
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                appUpdateStatus = error.message ?: "检查更新失败，请检查网络"
                Toast.makeText(this@MainActivity, appUpdateStatus, Toast.LENGTH_LONG).show()
            } finally { appUpdateChecking = false }
        }
    }

    @Composable
    private fun SettingsLibraries() {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            "素材库",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { updateCurrentIndex(fullScan = true) }, modifier = Modifier.size(48.dp),
                            enabled = savedLibraries.isNotEmpty() && libraryLoaded && !indexing && !batchBusy && !remoteConnecting && !sessionRestoring &&
                                (source != LibrarySource.ONEDRIVE || connected)) {
                            val rotation = if (indexing) {
                                val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "index-sync")
                                val angle by transition.animateFloat(0f, 360f,
                                    androidx.compose.animation.core.infiniteRepeatable(tween(1000, easing = androidx.compose.animation.core.LinearEasing)),
                                    label = "index-sync-angle")
                                angle
                            } else 0f
                            Icon(painterResource(R.drawable.ic_sync_24), contentDescription = "更新当前库索引",
                                modifier = Modifier.size(22.dp).rotate(rotation))
                        }
                        IconButton(onClick = {
                            if (savedLibraries.size == 1) beginIndexExport(savedLibraries) else exportIndexChooser = true
                        }, modifier = Modifier.size(48.dp), enabled = savedLibraries.any { saved -> savedIndexes.any { it.libraryId == saved.id } } &&
                            !indexing && !batchBusy && !remoteConnecting && !sessionRestoring) {
                            Icon(painterResource(R.drawable.ic_download_24), contentDescription = "导出索引备份", modifier = Modifier.size(22.dp))
                        }
                        IconButton(
                            onClick = { sourceChooserOpen = true },
                            modifier = Modifier.size(48.dp),
                            enabled = !indexing && !remoteConnecting && !batchBusy
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加素材库", modifier = Modifier.size(22.dp))
                        }
                    }
                if (savedLibraries.isEmpty()) {
                    Text("暂无已添加的素材库。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                savedLibraries.forEach { saved ->
                    val info = savedIndexes.firstOrNull { it.libraryId == saved.id }
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Box(Modifier.fillMaxWidth().padding(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(end = 76.dp)) {
                                Text(saved.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${saved.source.label} 库" +
                                        if (saved.id == libraryId && saved.source == LibrarySource.ONEDRIVE && !connected) " · 离线" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    if (saved.id == libraryId && indexing) {
                                        "${indexedAssets.size} 个素材（含回收站） · 正在索引 · 待检查 ${(indexTotal - indexDone).coerceAtLeast(0)} 项 · ${formatBytes(info?.bytes ?: 0L)}"
                                    } else info?.let {
                                        "${it.itemCount} 个素材（含回收站）" +
                                            (if (it.failedCount > 0) " · ${it.failedCount} 项读取失败" else " · 索引完成") +
                                            " · ${formatBytes(it.bytes)}"
                                    }
                                        ?: "尚未建立本机索引",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (saved.id == libraryId && saved.source == LibrarySource.ONEDRIVE) {
                                    when {
                                        !connected -> TextButton(onClick = {
                                            if (oneDriveRefreshToken() == null) startDeviceLogin() else restoreSession()
                                        }) { Text("重新连接") }
                                        !indexing && (info?.failedCount ?: 0) > 0 -> TextButton(
                                            onClick = { activateLibrary(saved) }
                                        ) { Text("继续索引 ${info!!.failedCount} 项") }
                                    }
                                }
                                if (saved.id == libraryId && saved.source != LibrarySource.ONEDRIVE && saved.source != LibrarySource.LOCAL) {
                                    TextButton(enabled = !indexing && !remoteConnecting, onClick = { activateLibrary(saved) }) {
                                        Text("重新连接 / 同步")
                                    }
                                }
                            }
                            RadioButton(
                                selected = saved.id == libraryId,
                                onClick = { if (saved.id != libraryId) activateLibrary(saved) },
                                enabled = !indexing && !remoteConnecting && !batchBusy,
                                modifier = Modifier.align(Alignment.TopEnd).size(36.dp)
                            )
                            Row(Modifier.align(Alignment.BottomEnd)) {
                                IconButton(onClick = { editLibraryCandidate = saved }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "修改素材库", modifier = Modifier.size(19.dp))
                                }
                                IconButton(onClick = { removeLibraryCandidate = saved }, enabled = !indexing && !batchBusy, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "移除素材库", modifier = Modifier.size(19.dp))
                                }
                            }
                        }
                    }
                }
        }
    }

    @Composable
    private fun SettingsCache() {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val pendingRename = pendingTagRename()
                    if (pendingRename != null || tagRenameStatus.isNotBlank()) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("标签改名任务", style = MaterialTheme.typography.titleMedium)
                                Text(tagRenameStatus.ifBlank { "已处理 ${pendingRename!!.done.size}/${pendingRename.ids.size} 项，进度已保存" })
                                if (pendingRename != null) TextButton(onClick = { runTagRename() }, enabled = !batchBusy && canUpload()) { Text("继续改名") }
                            }
                        }
                    }
                    if (canUpload() && (uploadMessage.isNotBlank() || pendingUploadCount > 0)) {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("上传任务", style = MaterialTheme.typography.titleMedium)
                                if (uploadMessage.isNotBlank()) Text(uploadMessage, style = MaterialTheme.typography.bodySmall)
                                if (pendingUploadCount > 0) TextButton(
                                    enabled = canUpload() && !batchBusy,
                                    onClick = { runUploads(emptyList()) }
                                ) { Text("重试未完成上传 $pendingUploadCount 项") }
                                if (batchBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Box(Modifier.height(48.dp), contentAlignment = Alignment.CenterStart) {
                        Text("缓存", style = MaterialTheme.typography.titleMedium)
                    }
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            headlineContent = { Text("缩略图缓存") },
                            supportingContent = {
                                Text("已缓存 $thumbnailCacheCount 张，${formatBytes(thumbnailCacheBytes)}")
                            },
                            trailingContent = {
                                TextButton(
                                    onClick = { clearThumbnailCache(true) },
                                    enabled = thumbnailCacheCount > 0
                                ) { Text("清除") }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ListItem(colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            headlineContent = { Text("正常退出时清除") },
                            supportingContent = { Text("默认保留；不影响素材库索引和已下载原文件") },
                            trailingContent = { Switch(clearCacheOnExit, onCheckedChange = {
                                clearCacheOnExit = it
                                getSharedPreferences("viewer", MODE_PRIVATE).edit().putBoolean("clearCacheOnExit", it).apply()
                            }) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        NumericSetting("缩略图缓存上限", "开启后，超出上限会清理最久未查看的缩略图",
                            thumbnailCacheLimitEnabled, thumbnailCacheLimitMb, "MB",
                            onEnabledChange = {
                                thumbnailCacheLimitEnabled = it
                                getSharedPreferences("viewer", MODE_PRIVATE).edit().putBoolean("cacheLimitEnabled", it).apply()
                                work.execute { trimThumbnailCache() }
                            }, onSave = {
                                thumbnailCacheLimitMb = it
                                getSharedPreferences("viewer", MODE_PRIVATE).edit().putInt("cacheLimitMb", it).apply()
                                work.execute { trimThumbnailCache() }
                            })
                    }
                    }
    }

    @Composable
    private fun SettingsIndexUpdate() {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.height(48.dp), contentAlignment = Alignment.CenterStart) {
                        Text("同步", style = MaterialTheme.typography.titleMedium)
                    }
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                headlineContent = { Text("启动时检查更新") },
                                supportingContent = { Text("先显示缓存，再检查当前库的新增、修改和删除") },
                                trailingContent = { Switch(checked = updateOnOpen, onCheckedChange = {
                                    updateOnOpen = it
                                    getSharedPreferences("index-sync", MODE_PRIVATE).edit().putBoolean("onOpen", it).apply()
                                }) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            NumericSetting("定时检查", "仅在 APP 前台运行，更新当前素材库",
                                updateIntervalEnabled, updateIntervalMinutes, "分钟",
                                onEnabledChange = {
                                    updateIntervalEnabled = it
                                    getSharedPreferences("index-sync", MODE_PRIVATE).edit().putBoolean("intervalEnabled", it).apply()
                                }, onSave = {
                                    updateIntervalMinutes = it
                                    getSharedPreferences("index-sync", MODE_PRIVATE).edit().putInt("interval", it).apply()
                                })
                        }
                    }
                    if (uploadCandidates.isNotEmpty()) Text("已接收 ${uploadCandidates.size} 张待上传图片，请打开可写的本地库或连接 OneDrive、SMB、WebDAV 库。", modifier = Modifier.padding(8.dp))
                    }
    }

    @Composable
    private fun ExportIndexDialog() {
        var selected by remember { mutableStateOf(setOf<String>()) }
        AlertDialog(onDismissRequest = { exportIndexChooser = false }, title = { Text("选择要备份的素材库") },
            text = { Column {
                Text("备份索引、目录、标签及已有缩略图，不含原文件和登录凭据。多库备份分别保存到所选目录。")
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(savedLibraries, key = { it.id }) { saved ->
                        val available = savedIndexes.any { it.libraryId == saved.id }
                        Row(Modifier.fillMaxWidth().toggleable(saved.id in selected, enabled = available,
                            role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = {
                                selected = if (it) selected + saved.id else selected - saved.id
                            }).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PickerCheckbox(saved.id in selected)
                            Text(saved.name + if (available) "" else " · 尚无索引",
                                color = if (available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } },
            confirmButton = { TextButton(onClick = {
                exportIndexChooser = false
                beginIndexExport(savedLibraries.filter { it.id in selected })
            }, enabled = selected.isNotEmpty()) { Text("确认导出") } },
            dismissButton = { TextButton(onClick = { exportIndexChooser = false }) { Text("取消") } })
    }

    private fun beginIndexExport(libraries: List<SavedLibrary>) {
        if (libraries.size == 1) {
            exportIndexLibrary = libraries.single()
            exportIndexPicker.launch("${libraries.single().name.replace(Regex("[\\\\/:*?\"<>|]"), "_")}.magle-index.zip")
        } else if (libraries.isNotEmpty()) {
            exportIndexLibraries = libraries
            exportIndexesPicker.launch(null)
        }
    }

    private fun writeLibraryIndex(saved: SavedLibrary, uri: Uri) {
        val temporary = File(cacheDir, "index-export-${java.util.UUID.randomUUID()}.zip")
        try {
            val index = JSONObject(File(filesDir, "eagle-index-${saved.id.hashCode()}.json").readText())
            val folders = JSONObject(File(filesDir, "eagle-folders-${saved.id.hashCode()}.json").readText())
            temporary.outputStream().use { output ->
                writeIndexBackup(output, index, folders) { id, mtime ->
                    File(File(cacheDir, "session-thumbnails"), LibraryLogic.thumbnailCacheKey(saved.id, id, mtime))
                }
            }
            contentResolver.openOutputStream(uri, "wt")!!.use { output -> temporary.inputStream().use { it.copyTo(output) } }
        } finally { temporary.delete() }
    }

    private fun exportIndexes(libraries: List<SavedLibrary>, uri: Uri) {
        if (indexing || batchBusy || remoteConnecting || sessionRestoring) {
            Toast.makeText(this, "当前库正在操作，请稍后重新导出", Toast.LENGTH_LONG).show(); return
        }
        batchBusy = true
        work.execute {
            var completed = 0
            runCatching {
                val directory = checkNotNull(DocumentFile.fromTreeUri(this, uri))
                libraries.forEach { saved ->
                    val name = saved.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    val file = checkNotNull(directory.createFile("application/zip", "$name-${java.util.UUID.randomUUID().toString().take(8)}.magle-index.zip"))
                    try { writeLibraryIndex(saved, file.uri); completed++ }
                    catch (error: Exception) { file.delete(); throw error }
                }
                runOnUiThread { Toast.makeText(this, "已导出 $completed 个库的索引备份", Toast.LENGTH_LONG).show() }
            }.onFailure { showError("已导出 $completed 个库，其余导出失败", it) }
            runOnUiThread { batchBusy = false }
        }
    }

    private fun exportIndex(saved: SavedLibrary, uri: Uri) {
        if (indexing || batchBusy || remoteConnecting || sessionRestoring) {
            Toast.makeText(this, "当前库正在操作，请稍后重新导出", Toast.LENGTH_LONG).show()
            return
        }
        batchBusy = true
        work.execute {
            runCatching {
                writeLibraryIndex(saved, uri)
                runOnUiThread { Toast.makeText(this, "已导出 ${saved.name} 的索引备份", Toast.LENGTH_LONG).show() }
            }.onFailure { showError("导出索引失败，请重新导出", it) }
            runOnUiThread { batchBusy = false }
        }
    }

    @Composable
    private fun NumericSetting(title: String, description: String, enabled: Boolean, value: Int,
                               unit: String, onEnabledChange: (Boolean) -> Unit, onSave: (Int) -> Unit) {
        var input by remember(value) { mutableStateOf(value.toString()) }
        val parsed = LibraryLogic.positiveSetting(input)
        ListItem(colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            headlineContent = { Text(title) }, supportingContent = { Text(description) },
            trailingContent = { Switch(enabled, onCheckedChange = onEnabledChange) })
        if (enabled) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(input, { input = it }, label = { Text(if (unit == "分钟") "检查间隔" else "容量上限") },
                    suffix = { Text(unit) }, singleLine = true, isError = parsed == null,
                    supportingText = { if (parsed == null) Text("请输入大于 0 的整数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                TextButton(onClick = { parsed?.let(onSave) }, enabled = parsed != null && parsed != value) { Text("保存") }
            }
        }
    }

    @Composable
    private fun RemoveLibraryDialog(saved: SavedLibrary) {
        AlertDialog(
            onDismissRequest = { removeLibraryCandidate = null },
            title = { Text("移除素材库？") },
            text = { Text("将移除“${saved.name}”及它的本机索引，不会删除 OneDrive、本地文件夹或 WebDAV 中的原始素材。") },
            confirmButton = {
                TextButton(onClick = {
                    removeLibraryCandidate = null
                    removeLibrary(saved)
                }) { Text("移除") }
            },
            dismissButton = { TextButton(onClick = { removeLibraryCandidate = null }) { Text("取消") } }
        )
    }

    @Composable
    private fun EditLibraryDialog(saved: SavedLibrary) {
        var name by remember(saved.id) { mutableStateOf(saved.name) }
        AlertDialog(
            onDismissRequest = { editLibraryCandidate = null },
            title = { Text("修改素材库") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("显示名称") }, singleLine = true)
                    OutlinedButton(onClick = {
                        editLibraryCandidate = null
                        rebindLibraryCandidate = saved
                    }) { Text("切换来源（保留索引） · Beta") }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        editLibraryCandidate = null
                        renameLibrary(saved, name.trim())
                    }
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editLibraryCandidate = null }) { Text("取消") } }
        )
    }

    @Composable
    private fun CompactTitleBar(title: String) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 44.dp, max = 44.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    @Composable
    private fun SourceChooserDialog() {
        AlertDialog(
            onDismissRequest = { sourceChooserOpen = false; cancelNewSource() },
            title = { Text("选择素材库来源") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            sourceChooserOpen = false
                            if (connected) openLibraryPicker() else startDeviceLogin()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Microsoft OneDrive") }
                    listOf(LibrarySource.DROPBOX, LibrarySource.GOOGLE).forEach { kind ->
                        FilledTonalButton(onClick = { sourceChooserOpen = false; remoteChooser = kind },
                            modifier = Modifier.fillMaxWidth()) { Text(kind.label) }
                    }
                    FilledTonalButton(
                        onClick = {
                            sourceChooserOpen = false
                            localLibraryPicker.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("本地文件夹") }
                    FilledTonalButton(
                        onClick = { sourceChooserOpen = false; webDavDialogOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("WebDAV") }
                    listOf(LibrarySource.SMB, LibrarySource.S3)
                        .filter { newSourceForLibrary == null || it != LibrarySource.S3 }.forEach { kind ->
                        FilledTonalButton(
                            onClick = { sourceChooserOpen = false; remoteChooser = kind },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (kind == LibrarySource.S3) "S3（Beta）" else kind.label) }
                    }
                    if (newSourceForLibrary == null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        FilledTonalButton(onClick = {
                            sourceChooserOpen = false
                            importIndexPicker.launch(arrayOf("application/zip", "application/octet-stream"))
                        }, enabled = !indexing && !batchBusy && !remoteConnecting && !sessionRestoring,
                            modifier = Modifier.fillMaxWidth()) { Text("导入备份索引") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { sourceChooserOpen = false; cancelNewSource() }) { Text("取消") } }
        )
    }

    @Composable
    private fun WebDavDialog() {
        var url by androidx.compose.runtime.remember { mutableStateOf(webDavUrl.orEmpty()) }
        var user by androidx.compose.runtime.remember { mutableStateOf(webDavUser.orEmpty()) }
        var password by androidx.compose.runtime.remember { mutableStateOf("") }
        var allowHttp by remember { mutableStateOf(getSharedPreferences("library", MODE_PRIVATE).getBoolean("allowLanWebDavHttp", false)) }
        AlertDialog(
            onDismissRequest = { webDavDialogOpen = false; cancelNewSource() },
            title = { Text("连接 WebDAV 素材库") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("请填写 .library 文件夹的完整地址。", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(allowHttp, { allowHttp = it })
                        Text("允许局域网 HTTP（不加密）", style = MaterialTheme.typography.bodySmall)
                    }
                    if (allowHttp) Text("仅限私有 IPv4 地址；登录凭据和文件会明文传输。建议使用 HTTPS。", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(url, { url = it }, label = { Text("素材库 URL") }, singleLine = true)
                    OutlinedTextField(user, { user = it }, label = { Text("用户名") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, label = { Text("应用密码") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation())
                }
            },
            confirmButton = {
                TextButton(
                    enabled = (url.startsWith("https://") || allowHttp && UploadLogic.isPrivateHttpUrl(url)) && !indexing,
                    onClick = {
                        webDavDialogOpen = false
                        getSharedPreferences("library", MODE_PRIVATE).edit().putBoolean("allowLanWebDavHttp", allowHttp).apply()
                        connectWebDav(url, user, password)
                    }
                ) { Text("连接") }
            },
            dismissButton = { TextButton(onClick = { webDavDialogOpen = false; cancelNewSource() }) { Text("取消") } }
        )
    }

    @Composable
    private fun RemoteSourceDialog(kind: LibrarySource) {
        var name by remember(kind) { mutableStateOf("Eagle 素材库") }
        var root by remember(kind) { mutableStateOf("") }
        var endpoint by remember(kind) { mutableStateOf("https://s3.amazonaws.com") }
        var bucket by remember(kind) { mutableStateOf("") }
        var user by remember(kind) { mutableStateOf("") }
        var password by remember(kind) { mutableStateOf("") }
        var domain by remember(kind) { mutableStateOf("") }
        var region by remember(kind) { mutableStateOf("us-east-1") }
        var appKey by remember(kind) {
            mutableStateOf(getSharedPreferences("provider-config", MODE_PRIVATE).getString("dropboxAppKey", "").orEmpty())
        }
        var code by remember(kind) { mutableStateOf("") }
        var verifier by remember(kind) { mutableStateOf("") }
        val valid = !remoteConnecting && !indexing && name.isNotBlank() && root.isNotBlank() && when (kind) {
            LibrarySource.SMB -> root.startsWith("smb://") && Uri.parse(root).host != null && Uri.parse(root).userInfo == null
            LibrarySource.S3 -> validS3Endpoint(endpoint, BuildConfig.DEBUG) && bucket.isNotBlank() && user.isNotBlank() && password.isNotBlank()
            LibrarySource.DROPBOX -> appKey.isNotBlank() && code.isNotBlank() && verifier.isNotBlank()
            LibrarySource.GOOGLE -> true
            else -> false
        }
        AlertDialog(
            onDismissRequest = { if (!remoteConnecting) { remoteChooser = null; cancelNewSource() } },
            title = { Text("连接 ${kind.label}") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("素材库名称") }, singleLine = true)
                    Text(when (kind) {
                        LibrarySource.DROPBOX -> "填写 .library 文件夹路径，授权后将网页上的授权码粘贴回来。当前测试版需要项目的 Dropbox App Key。"
                        LibrarySource.GOOGLE -> "填写 .library 文件夹的分享链接或文件夹 ID，然后选择 Google 账号授权。需在 Google Cloud 注册 MAGLE 的包名和签名。"
                        LibrarySource.SMB -> "填写共享中的完整素材库地址，例如 smb://192.168.1.10/share/素材.library/。支持 SMB 2/3；手机须能访问服务器。"
                        else -> "连接 S3 兼容服务；填写桶内 .library 前缀，不包含桶名。"
                    }, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(root, { root = it }, label = { Text(when (kind) {
                        LibrarySource.GOOGLE -> "文件夹链接或 ID"
                        LibrarySource.SMB -> "SMB 素材库地址"
                        LibrarySource.S3 -> "素材库前缀，如 Eagle.library"
                        else -> "素材库路径，如 /Eagle.library"
                    }) }, singleLine = true)
                    if (kind == LibrarySource.DROPBOX) {
                        OutlinedTextField(appKey, { appKey = it }, label = { Text("Dropbox App Key") }, singleLine = true)
                        OutlinedButton(enabled = appKey.isNotBlank() && !remoteConnecting, onClick = {
                            val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
                            verifier = Base64.encodeToString(random, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                            val challenge = Base64.encodeToString(MessageDigest.getInstance("SHA-256")
                                .digest(verifier.toByteArray(StandardCharsets.US_ASCII)), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                            getSharedPreferences("provider-config", MODE_PRIVATE).edit().putString("dropboxAppKey", appKey.trim()).apply()
                            val url = Uri.parse("https://www.dropbox.com/oauth2/authorize").buildUpon()
                                .appendQueryParameter("client_id", appKey.trim()).appendQueryParameter("response_type", "code")
                                .appendQueryParameter("token_access_type", "offline")
                                .appendQueryParameter("code_challenge_method", "S256").appendQueryParameter("code_challenge", challenge)
                                .appendQueryParameter("scope", "files.metadata.read files.content.read files.content.write").build()
                            startActivity(Intent(Intent.ACTION_VIEW, url))
                        }) { Text("打开 Dropbox 授权") }
                        OutlinedTextField(code, { code = it }, label = { Text("授权码") }, singleLine = true)
                    }
                    if (kind == LibrarySource.SMB || kind == LibrarySource.S3) {
                        if (kind == LibrarySource.S3) {
                            OutlinedTextField(endpoint, { endpoint = it }, label = { Text("服务地址（HTTPS）") }, singleLine = true)
                            OutlinedTextField(bucket, { bucket = it }, label = { Text("Bucket / 桶名") }, singleLine = true)
                            OutlinedTextField(region, { region = it }, label = { Text("Region / 区域") }, singleLine = true)
                        } else {
                            OutlinedTextField(domain, { domain = it }, label = { Text("域（可留空）") }, singleLine = true)
                        }
                        OutlinedTextField(user, { user = it }, label = { Text(if (kind == LibrarySource.S3) "Access Key" else "用户名") }, singleLine = true)
                        OutlinedTextField(password, { password = it }, label = { Text(if (kind == LibrarySource.S3) "Secret Key" else "密码") },
                            singleLine = true, visualTransformation = PasswordVisualTransformation())
                    }
                    if (remoteConnecting) Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp)); Text("  正在验证连接…")
                    }
                    if (indexing) Text("请等待当前库索引完成后再添加其他来源。", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    val normalizedRoot = when (kind) {
                        LibrarySource.GOOGLE -> if (root.contains("/folders/")) root.substringAfter("/folders/").substringBefore('?').substringBefore('/') else root.trim()
                        LibrarySource.DROPBOX -> "/" + root.trim().trim('/')
                        LibrarySource.SMB -> root.trim().trimEnd('/')
                        else -> root.trim().trim('/')
                    }
                    val config = JSONObject().put("kind", kind.name).put("root", normalizedRoot).put("name", name.trim())
                        .put("endpoint", endpoint.trim()).put("bucket", bucket.trim()).put("region", region.trim())
                        .put("user", user.trim()).put("domain", domain.trim()).put("accessKey", user.trim()).put("appKey", appKey.trim())
                    val secret = JSONObject().put("password", password).put("secretKey", password)
                    if (kind == LibrarySource.GOOGLE) {
                        pendingGoogleConfig = config
                        googleAccount = null
                        googleAccessToken = null
                        authorizeGoogle()
                    } else if (kind == LibrarySource.DROPBOX) {
                        remoteConnecting = true
                        work.execute {
                            runCatching {
                                val token = postForm("https://api.dropboxapi.com/oauth2/token", mapOf(
                                    "grant_type" to "authorization_code", "code" to code.trim(),
                                    "client_id" to appKey.trim(), "code_verifier" to verifier
                                ))
                                secret.put("accessToken", token.getString("access_token")).put("refreshToken", token.getString("refresh_token"))
                                connectRemote(config, secret)
                            }.onFailure { runOnUiThread { remoteConnecting = false }; showError("Dropbox 授权失败", it) }
                        }
                    } else { connectRemote(config, secret) }
                }) { Text(if (kind == LibrarySource.GOOGLE) "选择账号并连接" else "连接") }
            },
            dismissButton = { TextButton(enabled = !remoteConnecting, onClick = { remoteChooser = null; cancelNewSource() }) { Text("取消") } }
        )
    }

    private fun googleRequest() = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive")))
        .apply { googleAccount?.let { setAccount(Account(it, "com.google")) } }.build()

    private fun authorizeGoogle(notifyCompletion: Boolean = false) {
        googleStartupFeedback = notifyCompletion
        remoteConnecting = true
        Identity.getAuthorizationClient(this).authorize(googleRequest()).addOnSuccessListener { result ->
            if (result.hasResolution()) googleAuthorization.launch(IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build())
            else finishGoogleAuthorization(result)
        }.addOnFailureListener {
            remoteConnecting = false
            googleStartupFeedback = false
            showError("Google Drive 授权失败（请检查 Google Play 服务和项目配置）", it)
        }
    }

    private fun finishGoogleAuthorization(result: AuthorizationResult) {
        googleAccessToken = result.accessToken
        googleTokenExpires = System.currentTimeMillis() + 50 * 60_000L
        sessionRestored = googleAccessToken != null
        val config = pendingGoogleConfig
        pendingGoogleConfig = null
        if (config != null) connectRemote(config, JSONObject())
        else {
            remoteConnecting = false
            if (googleRestoreCheckUpdates || !libraryLoaded) {
                libraryId?.let { loadLibrary(it, libraryName ?: "Eagle 素材库", false, notifyCompletion = googleStartupFeedback) }
            }
            googleRestoreCheckUpdates = true
            googleStartupFeedback = false
        }
    }

    @Synchronized
    private fun googleToken(): String {
        if (googleAccessToken != null && System.currentTimeMillis() < googleTokenExpires) return googleAccessToken!!
        val result = Tasks.await(Identity.getAuthorizationClient(this).authorize(googleRequest()), 60, TimeUnit.SECONDS)
        check(!result.hasResolution()) { "Google Drive 需要重新授权，请在素材库中重新连接" }
        googleAccessToken = result.accessToken ?: error("未取得 Google 授权令牌")
        googleTokenExpires = System.currentTimeMillis() + 50 * 60_000L
        return googleAccessToken!!
    }

    private fun connectRemote(config: JSONObject, secret: JSONObject) {
        remoteConnecting = true
        work.execute {
            runCatching {
                val client = RemoteLibrary(config.getString("kind"), config, secret, googleToken = ::googleToken)
                JSONObject(String(client.read("metadata.json"), StandardCharsets.UTF_8))
                if (config.getString("kind") == "GOOGLE") {
                    googleAccount = client.googleEmail()
                    config.put("account", googleAccount)
                }
                val id = RemoteLibrary.save(this, config, secret)
                runOnUiThread {
                    remoteConnecting = false
                    remoteChooser = null
                    if (finishNewSource(SavedLibrary(id, config.getString("name"),
                            LibrarySource.valueOf(config.getString("kind")), null, null, null, null))) return@runOnUiThread
                    source = LibrarySource.valueOf(config.getString("kind"))
                    remoteLibrary = client
                    libraryId = id
                    libraryName = config.getString("name")
                    saveLibrary()
                    openOfflineLibrary()
                    remoteConnecting = false
                    remoteChooser = null
                    settingsOpen = false
                    loadLibrary(id, libraryName!!, false)
                }
            }.onFailure {
                runOnUiThread { remoteConnecting = false }
                showError("连接素材库失败，请检查路径和权限", it)
            }
        }
    }

    @Composable
    private fun LoginDialog(prompt: DevicePrompt) {
        AlertDialog(
            onDismissRequest = { devicePrompt = null },
            title = { Text("登录 Microsoft OneDrive") },
            text = { Text("验证码已复制：\n\n${prompt.code}\n\n在 Microsoft 页面粘贴该码完成登录。") },
            confirmButton = {
                Button(onClick = {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(prompt.uri)))
                    devicePrompt = null
                }) { Text("打开登录页") }
            },
            dismissButton = { TextButton(onClick = { devicePrompt = null }) { Text("稍后") } }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LibraryPickerDialog() {
        Dialog(
            onDismissRequest = { pickerOpen = false; cancelNewSource() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(pickerTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            navigationIcon = {
                                IconButton(onClick = ::pickerBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            }
                        )
                    }
                ) { padding ->
                    if (pickerLoading) {
                        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                            items(pickerFolders, key = { it.id ?: "root" }) { folder ->
                                ListItem(
                                    headlineContent = { Text(folder.name) },
                                    supportingContent = {
                                        if (folder.name.lowercase(Locale.ROOT).endsWith(".library")) {
                                            Text("Eagle 素材库")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingContent = {
                                        FilledTonalButton(onClick = { choosePickerFolder(folder) }) {
                                            Text(if (folder.name.lowercase(Locale.ROOT).endsWith(".library")) "打开" else "进入")
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun restoreSession(checkUpdates: Boolean = true, reopenCache: Boolean = true) {
        if (sessionRestoring || remoteConnecting) return
        sessionNeedsAuthorization = false
        if (source in REMOTE_SOURCES) {
            val id = libraryId ?: return
            val offline = if (reopenCache) openOfflineLibrary() else libraryLoaded
            runCatching {
                val config = JSONObject(getSharedPreferences("remote-libraries", MODE_PRIVATE).getString(id, "{}")!!)
                val restoredAccount = config.optString("account").takeIf(String::isNotBlank)
                if (googleAccount != restoredAccount) googleAccessToken = null
                googleAccount = restoredAccount
                remoteLibrary = RemoteLibrary.load(this, id, ::googleToken)
                if (source == LibrarySource.GOOGLE) {
                    // Restore authorization independently of the index-update preference.
                    if (reopenCache) {
                        googleRestoreCheckUpdates = checkUpdates
                        authorizeGoogle(notifyCompletion = checkUpdates)
                    }
                    else {
                        sessionRestoring = true
                        work.execute {
                            runCatching { googleToken() }
                                .onSuccess { runOnUiThread { sessionRestored = true } }
                                .onFailure { error ->
                                    Log.e("MAGLE", "Google Drive 自动恢复失败", error)
                                    runOnUiThread {
                                        sessionNeedsAuthorization = error is IllegalStateException
                                        status = "Google Drive 暂时离线；需要重新授权时请在设置中连接。"
                                    }
                                }
                            runOnUiThread { sessionRestoring = false }
                        }
                    }
                } else {
                    // Dropbox refreshes in its SDK; SMB/S3 reuse saved credentials on requests.
                    sessionRestored = true
                    if (checkUpdates || !offline) loadLibrary(id, libraryName ?: "Eagle 素材库", false, notifyCompletion = checkUpdates)
                }
            }.onFailure { showError("无法恢复 ${source.label} 连接", it) }
            return
        }
        if (source == LibrarySource.LOCAL) {
            sessionRestored = localLibraryUri != null
            if (!(if (reopenCache) openOfflineLibrary() else libraryLoaded) || checkUpdates) libraryId?.let { loadLibrary(it, libraryName ?: "本地 Eagle 素材库", false, notifyCompletion = checkUpdates) }
            return
        }
        if (source == LibrarySource.WEBDAV) {
            sessionRestored = webDavUrl != null
            if (!(if (reopenCache) openOfflineLibrary() else libraryLoaded) || checkUpdates) libraryId?.let { loadLibrary(it, libraryName ?: "WebDAV Eagle 素材库", false, notifyCompletion = checkUpdates) }
            return
        }
        val offlineOpened = if (reopenCache) openOfflineLibrary() else libraryLoaded
        val refresh = oneDriveRefreshToken()
        if (refresh == null) {
            sessionNeedsAuthorization = true
            status = if (offlineOpened) "OneDrive 未连接，已打开本地索引。" else "选择云端服务或本地文件夹，打开你的 Eagle 素材库。"
            return
        }
        status = if (offlineOpened) "已打开本地索引，正在后台恢复 OneDrive…" else "正在恢复 OneDrive 连接…"
        sessionRestoring = true
        work.execute {
            runCatching {
                graphAccessToken()
                finishConnected(checkUpdates || !offlineOpened, notifyCompletion = checkUpdates)
            }.onFailure { error ->
                runOnUiThread { sessionNeedsAuthorization = error is HttpFailure && LibraryLogic.authorizationRequired(error.code) }
                if (offlineOpened) {
                    Log.e("MAGLE", "OneDrive 恢复失败，使用本地索引", error)
                    updateStatus("OneDrive 暂时离线，已使用本地索引。")
                } else {
                    showError("暂时无法恢复 OneDrive 连接，请检查网络后重试", error)
                }
            }
            runOnUiThread { sessionRestoring = false }
        }
    }

    private fun openOfflineLibrary(): Boolean {
        if (libraryId == null) return false
        val cached = loadIndexCache()
        if (cached.isEmpty()) return false
        val cachedMetadata = runCatching {
            val root = JSONObject(folderCacheFile().readText())
            root
        }.getOrDefault(JSONObject())
        val cachedFolders = runCatching { parseFolders(cachedMetadata.optJSONArray("folders") ?: JSONArray()) }.getOrDefault(emptyList())
        val cachedGroups = runCatching { LibraryLogic.tagGroups(cachedMetadata) }.getOrDefault(emptyList())
        runOnUiThread {
            folderTree = cachedFolders
            tagGroups = cachedGroups
            expandedTagGroups = emptySet()
            tagManagerOpen = false
            expandedFolderIds = emptySet()
            folderGroupOpen = false
            selectedSection = LibrarySection.ALL
            resetBrowseHistory()
            selectedFolderId = null
            selectedFolderName = "全部"
            libraryLoaded = true
            indexTotal = cached.size
            indexedAssets = cached.values.sortedByDescending { it.mtime }
        }
        return true
    }

    private fun startDeviceLogin() {
        status = "正在请求 Microsoft 登录码…"
        work.execute {
            runCatching {
                val device = postForm("$LOGIN/devicecode", mapOf("client_id" to CLIENT_ID, "scope" to SCOPE))
                val code = device.getString("user_code")
                val uri = device.optString("verification_uri", "https://microsoft.com/devicelogin")
                val interval = maxOf(5, device.optInt("interval", 5))
                val expires = device.optInt("expires_in", 900)
                runOnUiThread {
                    (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Microsoft code", code))
                    devicePrompt = DevicePrompt(code, uri)
                    status = "等待 Microsoft 授权，验证码：$code"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                }
                pollDeviceCode(device.getString("device_code"), interval, expires)
            }.onFailure { showError("OneDrive 登录未完成", it) }
        }
    }

    private fun pollDeviceCode(deviceCode: String, interval: Int, expires: Int) {
        val deadline = System.currentTimeMillis() + expires * 1000L
        var delay = interval
        while (System.currentTimeMillis() < deadline && !Thread.currentThread().isInterrupted) {
            Thread.sleep(delay * 1000L)
            val response = runCatching {
                postFormAllowError("$LOGIN/token", mapOf(
                    "client_id" to CLIENT_ID,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    "device_code" to deviceCode
                ))
            }.getOrElse {
                updateStatus("网络暂时不可用，正在继续等待 Microsoft 授权…")
                continue
            }
            if (response.has("access_token")) {
                applyToken(response)
                finishConnected()
                return
            }
            when (val error = response.optString("error")) {
                "authorization_pending" -> Unit
                "slow_down" -> delay += 5
                else -> error(response.optString("error_description", error))
            }
        }
        error("登录等待超时")
    }

    private fun connectWebDav(url: String, user: String, password: String) {
        val uri = Uri.parse(url.trim())
        val allowedHttp = getSharedPreferences("library", MODE_PRIVATE).getBoolean("allowLanWebDavHttp", false) && UploadLogic.isPrivateHttpUrl(url.trim())
        if (uri.scheme != "https" && !allowedHttp || uri.host.isNullOrBlank() || uri.userInfo != null || uri.query != null || uri.fragment != null) {
            Toast.makeText(this, "请输入不带查询参数的 HTTPS 素材库地址", Toast.LENGTH_LONG).show()
            return
        }
        val targetUrl = uri.buildUpon().path(uri.path.orEmpty().trimEnd('/')).build().toString()
        if (newSourceForLibrary != null) {
            TokenStore.saveSecret(this, "webdav-password-${targetUrl.hashCode()}", password)
            finishNewSource(SavedLibrary(targetUrl, Uri.decode(targetUrl.substringAfterLast('/')),
                LibrarySource.WEBDAV, null, null, targetUrl, user))
            return
        }
        source = LibrarySource.WEBDAV
        webDavUrl = uri.buildUpon().path(uri.path.orEmpty().trimEnd('/')).build().toString()
        webDavUser = user
        webDavPassword = password
        loadLibrary(webDavUrl!!, Uri.decode(webDavUrl!!.substringAfterLast('/')), true)
    }

    private fun applyToken(token: JSONObject) {
        token.optString("refresh_token").takeIf { it.isNotBlank() }?.let {
            TokenStore.saveSecret(this, "onedrive-$CLIENT_ID", it)
        }
        accessTokenExpires = System.currentTimeMillis() + (token.optLong("expires_in", 3600L) - 60L).coerceAtLeast(0L) * 1000L
        accessToken = token.getString("access_token")
    }

    private fun graphAccessToken(): String = synchronized(tokenLock) {
        accessToken?.takeIf { System.currentTimeMillis() < accessTokenExpires } ?: run {
            val refresh = oneDriveRefreshToken() ?: error("请授权 MAGLE 连接 OneDrive，现有索引仍保留")
            applyToken(retry(3) { tokenRequest(mapOf("client_id" to CLIENT_ID, "grant_type" to "refresh_token", "refresh_token" to refresh, "scope" to SCOPE)) })
            accessToken!!
        }
    }

    private fun finishConnected(checkUpdates: Boolean = true, notifyCompletion: Boolean = false) {
        val drive = retry(5) { graphJson("/me/drive?\$select=id") }
        driveId = drive.getString("id")
        runOnUiThread {
            connected = true
            sessionRestored = true
            status = "已连接 OneDrive。"
        }
        if (source == LibrarySource.ONEDRIVE && checkUpdates) {
            libraryId?.let { loadLibrary(it, libraryName ?: "Eagle .library", false, notifyCompletion = notifyCompletion) }
        }
    }

    private fun oneDriveRefreshToken(): String? = TokenStore.loadSecret(this, "onedrive-$CLIENT_ID")
        ?: if (CLIENT_ID == LEGACY_CLIENT_ID) TokenStore.load(this) else null

    private fun openLibraryPicker() {
        if (!connected || driveId == null) return
        pickerStack.clear()
        pickerStack.push(CloudFolder(null, "OneDrive"))
        pickerOpen = true
        showPickerFolder()
    }

    private fun showPickerFolder() {
        val current = pickerStack.peek() ?: return
        pickerTitle = current.name
        pickerLoading = true
        work.execute {
            runCatching {
                val path = if (current.id == null) {
                    "/drives/${enc(driveId!!)}/root/children?\$select=id,name,folder"
                } else {
                    "/drives/${enc(driveId!!)}/items/${enc(current.id)}/children?\$select=id,name,folder"
                }
                val entries = graphJson(path).getJSONArray("value")
                buildList {
                    for (i in 0 until entries.length()) {
                        val item = entries.getJSONObject(i)
                        if (item.has("folder")) add(CloudFolder(item.getString("id"), item.getString("name")))
                    }
                }.sortedBy { it.name.lowercase(Locale.ROOT) }
            }.onSuccess {
                runOnUiThread { pickerFolders = it; pickerLoading = false }
            }.onFailure {
                runOnUiThread { pickerLoading = false }
                showError("无法读取该文件夹", it)
            }
        }
    }

    private fun pickerBack() {
        if (pickerStack.size <= 1) {
            pickerOpen = false
        } else {
            pickerStack.pop()
            showPickerFolder()
        }
    }

    private fun choosePickerFolder(folder: CloudFolder) {
        if (folder.name.lowercase(Locale.ROOT).endsWith(".library")) {
            pickerOpen = false
            if (finishNewSource(SavedLibrary(folder.id!!, folder.name, LibrarySource.ONEDRIVE,
                    driveId, null, null, null))) return
            source = LibrarySource.ONEDRIVE
            loadLibrary(folder.id!!, folder.name, true)
        } else {
            pickerStack.push(folder)
            showPickerFolder()
        }
    }

    private fun loadLibrary(id: String, name: String, save: Boolean, fullScan: Boolean = false, notifyCompletion: Boolean = false) {
        if (indexing || batchBusy) return
        if (notifyCompletion) pullRefreshFeedbackPending = true
        val resetNavigation = id != libraryId || !libraryLoaded
        indexing = true
        updateStatus("正在读取 $name 的目录…")
        ContextCompat.startForegroundService(this, Intent(this, IndexingService::class.java))
        work.execute {
            runCatching {
                val metadata = JSONObject(String(
                    retry(5) { readLibraryFile(id, "metadata.json") }, StandardCharsets.UTF_8
                ))
                val mtime = JSONObject(String(
                    retry(5) { readLibraryFile(id, "mtime.json") }, StandardCharsets.UTF_8
                ))
                val mtimes = buildMap {
                    val keys = mtime.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "all") put(key, mtime.optLong(key, 0))
                    }
                }
                val previous = runCatching { JSONObject(indexCacheFile().readText()) }.getOrNull()
                val unlisted = previous?.optJSONArray("unlisted")
                // ponytail: automatic refresh trusts Eagle's manifest; manual update scans for unlisted additions.
                val itemIds = if (source == LibrarySource.ONEDRIVE && id == libraryId && !save && !fullScan && unlisted != null && mtimes.isNotEmpty())
                    LibraryLogic.refreshItemIds(mtimes.keys, unlisted)
                    else if (source == LibrarySource.WEBDAV && mtimes.isNotEmpty()) mtimes.keys
                    else retry(5) { listAssetIds(id) }
                val folders = parseFolders(metadata.optJSONArray("folders") ?: JSONArray())
                val groups = LibraryLogic.tagGroups(metadata)
                libraryId = id
                libraryName = name
                runCatching { folderCacheFile().writeText(metadata.toString()) }
                if (save) saveLibrary()
                runOnUiThread {
                    folderTree = folders
                    tagGroups = groups
                    if (resetNavigation) {
                        expandedFolderIds = emptySet()
                        folderGroupOpen = false
                        tagManagerOpen = false
                        expandedTagGroups = emptySet()
                        selectedSection = LibrarySection.ALL
                        resetBrowseHistory()
                        selectedFolderId = null
                        selectedFolderName = "全部"
                    }
                    libraryLoaded = true
                    sessionRestored = true
                    indexTotal = itemIds.size
                    status = "已连接 $name。"
                    if (!attemptedTagResume && pendingTagRename() != null && canUpload()) runTagRename()
                }
                buildAssetIndex(itemIds, mtimes)
            }.onFailure {
                runOnUiThread { indexing = false; pullRefreshFeedbackPending = false }
                stopService(Intent(this, IndexingService::class.java))
                showError("无法打开 Eagle 素材库", it)
            }
        }
    }

    private fun buildAssetIndex(itemIds: Set<String>, mtimes: Map<String, Long>) {
        val cached = loadIndexCache().filter { (id, asset) ->
            id in itemIds && (mtimes[id]?.let { it == asset.mtime } ?: true)
        }.toMap().toMutableMap()
        // Previously skipped formats must be read once so they can appear as files or thumbnails.
        val ignored = mutableMapOf<String, Long>()
        publishAssets(cached.values)
        var missing = itemIds.filterNot { it in cached || it in ignored }
        if (missing.isEmpty()) {
            saveIndexCache(cached.values, ignored, itemIds.size, 0, itemIds - mtimes.keys)
            stopService(Intent(this, IndexingService::class.java))
            runOnUiThread { refreshSettings() }
            runOnUiThread {
                indexing = false; indexDone = itemIds.size; indexProgress = 1f
                notifyPullRefresh(0)
            }
            return
        }

        runOnUiThread {
            indexing = true
            indexDone = cached.size + ignored.size
            indexProgress = indexDone.toFloat() / maxOf(1, itemIds.size)
        }
        repeat(3) { round ->
            if (missing.isEmpty() || Thread.currentThread().isInterrupted) return@repeat
            if (pauseIndexForEdit) {
                saveIndexCache(cached.values, ignored, itemIds.size, missing.size, itemIds - mtimes.keys)
                return
            }
            if (round > 0) {
                updateStatus("网络波动，正在重试 ${missing.size} 个未读取素材…")
                repeat(20 * round) { if (!pauseIndexForEdit) Thread.sleep(250) }
            }
            val failures = mutableListOf<String>()
            // Bound outstanding reads so an edit can yield the index without queuing thousands of requests.
            for ((chunkIndex, chunk) in missing.chunked(8).withIndex()) {
                val complete = readIndexChunk(indexWorkers, chunk, { pauseIndexForEdit },
                    { id -> retry(3) { fetchAssetMetadata(id, mtimes[id] ?: 0L) } }, parallelism = if (galleryScrolling) 4 else 8) { id, result ->
                    result.onSuccess { asset ->
                        if (asset == null) ignored[id] = mtimes[id] ?: 0L else cached[id] = asset
                    }.onFailure {
                        failures += id
                        Log.w("MAGLE", "Index metadata failed: ${it.javaClass.simpleName}, HTTP ${(it as? HttpFailure)?.code ?: 0}")
                    }
                }
                if (!complete) {
                    saveIndexCache(cached.values, ignored, itemIds.size,
                        itemIds.size - cached.size - ignored.size, itemIds - mtimes.keys)
                    publishAssets(cached.values)
                    updateStatus("索引已保存进度，正在优先处理编辑…")
                    return
                }
                if (chunkIndex % 12 == 0 || chunkIndex == (missing.size - 1) / 8) {
                    publishAssets(cached.values)
                    saveIndexCache(cached.values, ignored, itemIds.size,
                        itemIds.size - cached.size - ignored.size, itemIds - mtimes.keys)
                }
                val resolved = cached.size + ignored.size
                runOnUiThread {
                    indexDone = resolved.coerceIn(0, itemIds.size)
                    indexProgress = indexDone.toFloat() / maxOf(1, itemIds.size)
                }
            }
            missing = failures
        }
        saveIndexCache(cached.values, ignored, itemIds.size, missing.size, itemIds - mtimes.keys)
        stopService(Intent(this, IndexingService::class.java))
        runOnUiThread {
            indexing = false
            refreshSettings()
            indexDone = itemIds.size - missing.size
            indexProgress = indexDone.toFloat() / maxOf(1, itemIds.size)
            status = if (missing.isEmpty()) {
                "已扫描 ${itemIds.size} 个项目，已收录 ${cached.size} 个素材。"
            } else {
                "已读取 ${cached.size} 个素材，${missing.size} 个项目读取失败，可继续索引。"
            }
            notifyPullRefresh(missing.size)
        }
    }

    private fun notifyPullRefresh(remaining: Int) {
        if (!pullRefreshFeedbackPending) return
        pullRefreshFeedbackPending = false
        Toast.makeText(this, LibraryLogic.refreshResultMessage(remaining), Toast.LENGTH_SHORT).show()
    }

    private fun fetchAssetMetadata(id: String, mtime: Long): AssetMeta? {
        val bytes = try { readAssetFile(id, "metadata.json") } catch (error: HttpFailure) {
            if (source != LibrarySource.ONEDRIVE || error.code != 404) throw error
            // A missing metadata file alone is not proof that the directory is empty.
            val entries = graphJson("/drives/${enc(driveId!!)}/items/${enc(libraryId!!)}:/images/${enc("$id.info")}:/children?\$select=name&\$top=1")
                .getJSONArray("value")
            if (LibraryLogic.isConfirmedEmptyDirectory(error.code, entries.length())) return null
            throw error
        }
        return parseAssetMetadata(id, JSONObject(String(bytes, StandardCharsets.UTF_8)), mtime)
    }

    private fun parseAssetMetadata(id: String, json: JSONObject, mtime: Long): AssetMeta {
        val ext = json.optString("ext").lowercase(Locale.ROOT)
        val folders = json.optJSONArray("folders") ?: JSONArray()
        val tags = json.optJSONArray("tags") ?: JSONArray()
        return AssetMeta(
            id = id,
            name = json.optString("name", id),
            ext = ext,
            folders = buildSet { for (i in 0 until folders.length()) add(folders.getString(i)) },
            noThumbnail = json.optBoolean("noThumbnail", false),
            mtime = if (mtime == 0L) json.optLong("modificationTime") else mtime,
            isDeleted = json.optBoolean("isDeleted", false),
            annotation = json.optString("annotation"),
            url = json.optString("url"),
            tags = buildList { for (i in 0 until tags.length()) add(tags.getString(i)) },
            size = json.optLong("size"),
            width = json.optInt("width"),
            height = json.optInt("height")
        )
    }

    private fun publishAssets(values: Collection<AssetMeta>) {
        val sorted = values.sortedByDescending { it.mtime }
        runOnUiThread { indexedAssets = sorted }
    }

    private fun loadIndexCache(): Map<String, AssetMeta> = runCatching {
        val root = JSONObject(indexCacheFile().readText())
        check(root.optInt("schemaVersion") == 2)
        val items = root.getJSONObject("items")
        buildMap {
            val keys = items.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val json = items.getJSONObject(id)
                val folders = json.optJSONArray("folders") ?: JSONArray()
                val tags = json.optJSONArray("tags") ?: JSONArray()
                put(id, AssetMeta(
                    id = id,
                    name = json.getString("name"),
                    ext = json.getString("ext"),
                    folders = buildSet { for (i in 0 until folders.length()) add(folders.getString(i)) },
                    noThumbnail = json.optBoolean("noThumbnail", false),
                    mtime = json.getLong("mtime"),
                    isDeleted = json.optBoolean("isDeleted", false),
                    annotation = json.optString("annotation"),
                    url = json.optString("url"),
                    tags = buildList { for (i in 0 until tags.length()) add(tags.getString(i)) },
                    size = json.optLong("size"),
                    width = json.optInt("width"),
                    height = json.optInt("height")
                ))
            }
        }
    }.getOrDefault(emptyMap())

    private fun loadIgnoredIndexCache(): Map<String, Long> = runCatching {
        val ignored = JSONObject(indexCacheFile().readText()).optJSONObject("ignored") ?: JSONObject()
        buildMap {
            val keys = ignored.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                put(id, ignored.optLong(id))
            }
        }
    }.getOrDefault(emptyMap())

    private fun saveIndexCache(
        values: Collection<AssetMeta>,
        ignoredValues: Map<String, Long>? = null,
        scanTotal: Int? = null,
        failedCount: Int? = null,
        unlistedValues: Set<String>? = null
    ) {
        runCatching {
            val previous = runCatching { JSONObject(indexCacheFile().readText()) }.getOrNull()
            val items = JSONObject()
            values.forEach { asset ->
                items.put(asset.id, JSONObject()
                    .put("name", asset.name)
                    .put("ext", asset.ext)
                    .put("folders", JSONArray(asset.folders.toList()))
                    .put("noThumbnail", asset.noThumbnail)
                    .put("mtime", asset.mtime)
                    .put("isDeleted", asset.isDeleted)
                    .put("annotation", asset.annotation)
                    .put("url", asset.url)
                    .put("tags", JSONArray(asset.tags))
                    .put("size", asset.size)
                    .put("width", asset.width)
                    .put("height", asset.height))
            }
            val ignored = ignoredValues?.let { entries ->
                JSONObject().apply { entries.forEach { (id, mtime) -> put(id, mtime) } }
            } ?: previous?.optJSONObject("ignored") ?: JSONObject()
            val target = indexCacheFile()
            val temporary = File(target.parentFile, "${target.name}.tmp")
            val cache = JSONObject()
                .put("schemaVersion", 2)
                .put("libraryId", libraryId)
                .put("libraryName", libraryName)
                .put("items", items)
                .put("ignored", ignored)
                .put("scanTotal", scanTotal ?: previous?.optInt("scanTotal", values.size) ?: values.size)
                .put("failedCount", failedCount ?: previous?.optInt("failedCount", 0) ?: 0)
            val unlisted = unlistedValues?.let { JSONArray(it.toList()) } ?: previous?.optJSONArray("unlisted")
            if (unlisted != null) cache.put("unlisted", unlisted)
            temporary.writeText(cache.toString())
            if (!temporary.renameTo(target)) {
                target.delete()
                check(temporary.renameTo(target))
            }
        }
    }

    private fun indexCacheFile() = File(filesDir, "eagle-index-${libraryId.orEmpty().hashCode()}.json")

    private fun folderCacheFile() = File(filesDir, "eagle-folders-${libraryId.orEmpty().hashCode()}.json")

    private fun loadThumbnail(asset: AssetMeta): ByteArray {
        val cached = thumbnailCacheFile(asset)
        synchronized(thumbnailCacheLock) {
            if (cached.isFile) {
                val bytes = cached.readBytes()
                if (validThumbnail(bytes)) { cached.setLastModified(System.currentTimeMillis()); return bytes }
                cached.delete()
            }
        }
        val bytes = fetchThumbnail(asset)
        check(validThumbnail(bytes)) { "缩略图数据无效" }
        synchronized(thumbnailCacheLock) {
            if (!cached.exists() && !(clearCacheOnExit && isFinishing)) {
                cached.parentFile?.mkdirs()
                cached.writeBytes(bytes)
                scheduleThumbnailMaintenance()
            }
        }
        return bytes
    }

    private fun validThumbnail(bytes: ByteArray): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    private fun fetchThumbnail(asset: AssetMeta): ByteArray {
        if (!asset.noThumbnail) {
            try { return readAssetFile(asset.id, asset.thumbnailName) } catch (error: Exception) {
                if (error is CancellationException || error is InterruptedException) throw error
                if (source == LibrarySource.ONEDRIVE && (error !is HttpFailure || error.code != 404)) throw error
            }
        }
        if (asset.ext.equals("svg", true)) {
            check(asset.size <= 8 * 1024 * 1024) { "SVG 文件过大，无法安全预览" }
            val bitmap = SvgPreview.render(readAssetFile(asset.id, asset.fileName), 1024)
            return java.io.ByteArrayOutputStream().use { output ->
                try { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); output.toByteArray() }
                finally { bitmap.recycle() }
            }
        }
        if (source == LibrarySource.ONEDRIVE) {
            try {
                return graphBytes("/drives/${enc(driveId!!)}/items/${enc(libraryId!!)}:/images/" +
                    "${enc("${asset.id}.info")}/${enc(asset.fileName)}:/thumbnails/0/medium/content")
            } catch (error: HttpFailure) {
                if (error.code == 404) throw NoThumbnailException()
                throw error
            }
        }
        if (!LibraryLogic.isViewableImage(asset.ext)) throw NoThumbnailException()
        return readAssetFile(asset.id, asset.fileName)
    }

    private fun thumbnailCacheFile(asset: AssetMeta) = File(
        File(cacheDir, "session-thumbnails"),
        LibraryLogic.thumbnailCacheKey(libraryId.orEmpty(), asset.id, asset.mtime)
    )

    private fun openSettings() {
        if (batchBusy) {
            Toast.makeText(this, "请等待当前批量操作完成", Toast.LENGTH_SHORT).show()
            return
        }
        clearSelection()
        searchOpen = false
        viewPickerOpen = false
        refreshSettings()
        settingsOpen = true
    }

    private fun trimThumbnailCache() = synchronized(thumbnailCacheLock) {
        val epoch = thumbnailCacheEpoch
        val directory = File(cacheDir, "session-thumbnails")
        if (thumbnailCacheLimitEnabled) LibraryLogic.trimThumbnailCache(directory, thumbnailCacheLimitMb.toLong() * 1024 * 1024)
        val files = directory.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".thumb") }
        val bytes = files.sumOf(File::length)
        runOnUiThread { if (epoch == thumbnailCacheEpoch) { thumbnailCacheCount = files.size; thumbnailCacheBytes = bytes } }
    }

    private fun scheduleThumbnailMaintenance() = synchronized(thumbnailCacheLock) {
        if (thumbnailMaintenance?.isActive != true) {
            // ponytail: batch directory maintenance once per second; limit may briefly exceed by in-flight downloads.
            thumbnailMaintenance = lifecycleScope.launch(Dispatchers.IO) {
                delay(1000)
                trimThumbnailCache()
            }
        }
    }

    private fun refreshSettings() {
        settingsRefresh?.cancel()
        val selectedLibrary = libraryId
        val cacheEpoch = thumbnailCacheEpoch
        settingsRefresh = lifecycleScope.launch {
            delay(100) // Coalesce bursts of index/settings notifications before reading disk.
            val (uploads, cacheSize, indexes) = withContext(Dispatchers.IO) {
                val uploads = pendingUploads().size
                val cacheFiles = File(cacheDir, "session-thumbnails").listFiles().orEmpty().filter(File::isFile)
                val cacheSize = cacheFiles.size to cacheFiles.sumOf(File::length)
                val indexes = filesDir.listFiles { file ->
                    file.name.startsWith("eagle-index-") && file.name.endsWith(".json")
                }.orEmpty().mapNotNull { file ->
                    if (!isActive) throw CancellationException()
                    runCatching {
                        val root = JSONObject(file.readText())
                        val id = root.optString("libraryId")
                        LibraryIndexInfo(
                            file = file,
                            libraryId = id,
                            name = root.optString("libraryName").ifBlank { "旧素材库索引" },
                            itemCount = root.optJSONObject("items")?.length() ?: 0,
                            scanTotal = root.optInt("scanTotal", root.optJSONObject("items")?.length() ?: 0),
                            failedCount = root.optInt("failedCount"),
                            bytes = file.length() + (File(filesDir,
                                file.name.replaceFirst("eagle-index-", "eagle-folders-")
                            ).takeIf(File::isFile)?.length() ?: 0L),
                            current = id.isNotBlank() && id == selectedLibrary
                        )
                    }.getOrNull()
                }.sortedWith(compareByDescending<LibraryIndexInfo> { it.current }.thenBy { it.name })
                Triple(uploads, cacheSize, indexes)
            }
            pendingUploadCount = uploads
            if (cacheEpoch == thumbnailCacheEpoch) {
                thumbnailCacheCount = cacheSize.first; thumbnailCacheBytes = cacheSize.second
            }
            if (selectedLibrary == libraryId) savedIndexes = indexes
        }
    }

    private fun clearThumbnailCache(showMessage: Boolean) {
        bitmapCache.evictAll()
        synchronized(thumbnailCacheLock) {
            File(cacheDir, "session-thumbnails").deleteRecursively()
        }
        thumbnailCacheCount = 0
        thumbnailCacheBytes = 0
        thumbnailCacheEpoch += 1
        if (showMessage) Toast.makeText(this, "已清除缩略图缓存", Toast.LENGTH_SHORT).show()
    }

    private fun openAssetDetails(asset: AssetMeta) {
        searchOpen = false
        detailOriginalBitmap = null
        detailOriginalLoading = false
        infoAsset = asset
    }

    private fun closeAssetDetails() {
        infoAsset = null
        detailOriginalBitmap = null
        detailOriginalLoading = false
    }

    private fun loadDetailOriginal(asset: AssetMeta) {
        if (detailOriginalLoading || detailOriginalBitmap != null) return
        detailOriginalLoading = true
        priorityDownloads.execute {
            runCatching {
                val bytes = readAssetFile(asset.id, asset.fileName)
                if (asset.ext.equals("svg", true)) SvgPreview.render(bytes, 2048)
                else decodeSampled(bytes, 4096) ?: error("原图解码失败")
            }
                .onSuccess { bitmap ->
                    runOnUiThread {
                        if (infoAsset?.id == asset.id) {
                            detailOriginalBitmap = bitmap
                            detailOriginalLoading = false
                        }
                    }
                }
                .onFailure {
                    runOnUiThread { if (infoAsset?.id == asset.id) { detailOriginalLoading = false; showError("原图加载失败", it) } }
                }
        }
    }

    private fun openExternalFile(asset: AssetMeta) {
        detailOriginalLoading = true
        priorityDownloads.execute {
            runCatching {
                val directory = File(cacheDir, "external-files/${asset.id.hashCode()}").apply { mkdirs() }
                val file = File(directory, asset.fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_"))
                file.writeBytes(readAssetFile(asset.id, asset.fileName))
                val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
                runOnUiThread {
                    detailOriginalLoading = false
                    runCatching {
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mime(asset.fileName))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "打开文件"))
                    }.onFailure { showError("没有可打开此文件的应用", it) }
                }
            }.onFailure {
                runOnUiThread { detailOriginalLoading = false }
                showError("读取文件失败", it)
            }
        }
    }

    private fun runBatch(assets: List<AssetMeta>, action: String, folderIds: Set<String> = emptySet(), tags: List<String> = emptyList()) {
        if (batchBusy || assets.isEmpty()) return
        batchBusy = true
        Toast.makeText(this, "正在$action ${assets.size} 个素材…", Toast.LENGTH_SHORT).show()
        val job = {
            val librarySnapshot = editSnapshot()
            val succeeded = mutableSetOf<String>()
            val changed = mutableMapOf<String, AssetMeta>()
            var lastError: Throwable? = null
            assets.forEach { asset ->
                if (!Thread.currentThread().isInterrupted) runCatching {
                    if (action == "下载") {
                        saveDownload(asset.fileName, readAssetFile(asset.id, asset.fileName)).getOrThrow()
                    } else {
                        changed[asset.id] = mutateAssetNow(asset) { json ->
                            if (action == "回收站" || action == "还原") LibraryLogic.setRecycled(json, action)
                            else if (action == "添加标签") LibraryLogic.appendTags(json, tags)
                            else LibraryLogic.setMetadataList(json, "folders", folderIds)
                        }
                    }
                    succeeded += asset.id
                }.onFailure { lastError = it }
            }
            if (changed.isNotEmpty()) {
                val updated = (librarySnapshot.associateBy { it.id } + changed).values
                // Cloud writes already succeeded; a failed cache save must not hide those changes.
                runCatching { saveIndexCache(updated) }.onFailure { lastError = it }
                publishAssets(updated)
            }
            runOnUiThread {
                batchBusy = false
                infoAsset?.let { current -> changed[current.id]?.let {
                    if (action == "还原") closeAssetDetails() else infoAsset = it
                } }
                selectedAssetIds = selectedAssetIds - succeeded
                if (!hasSelection() || succeeded.size == assets.size) clearSelection()
                val failed = assets.size - succeeded.size
                val message = if (failed == 0) "$action 完成：${succeeded.size} 个素材"
                    else "$action 完成 ${succeeded.size} 个，失败 $failed 个；未完成的素材仍保持选中。"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                if (lastError != null) Log.w("MAGLE", "Batch $action: $message", lastError)
            }
        }
        if (action == "下载") priorityDownloads.execute { job() } else queueEdit { job() }
    }

    private fun shareAssets(assets: List<AssetMeta>) {
        if (batchBusy || assets.isEmpty()) return
        if (assets.size > 100 || assets.any { it.size > 200L * 1024 * 1024 } || assets.sumOf { it.size } > 500L * 1024 * 1024) {
            Toast.makeText(this, "一次最多分享 100 项，单项不超过 200 MB、合计不超过 500 MB，请减少选择", Toast.LENGTH_LONG).show()
            return
        }
        batchBusy = true
        Toast.makeText(this, "正在准备 ${assets.size} 个原文件…", Toast.LENGTH_SHORT).show()
        priorityDownloads.execute {
            val shareRoot = File(cacheDir, "external-files/shares").apply { mkdirs() }
            // Keep granted files for receivers; remove only our share caches older than 24 hours on the next share.
            shareRoot.listFiles().orEmpty().filter { it.isDirectory && System.currentTimeMillis() - it.lastModified() > 24L * 60 * 60 * 1000 }.forEach { it.deleteRecursively() }
            val directory = File(shareRoot, java.util.UUID.randomUUID().toString()).apply { mkdirs() }
            val result = runCatching {
                val uris = ArrayList<Uri>()
                assets.forEachIndexed { index, asset ->
                    val parent = File(directory, index.toString()).apply { mkdirs() }
                    val safeName = asset.fileName.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
                    val file = File(parent, safeName)
                    file.writeBytes(readAssetFile(asset.id, asset.fileName))
                    uris += FileProvider.getUriForFile(this, "$packageName.files", file)
                }
                val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                    type = LibraryLogic.shareMimeType(assets.map { mime(it.fileName) })
                    if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first()) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    clipData = ClipData.newUri(contentResolver, "MAGLE 原文件", uris.first()).apply {
                        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                intent
            }
            if (result.isFailure) directory.deleteRecursively()
            runOnUiThread {
                batchBusy = false
                result.onSuccess { intent ->
                    runCatching { startActivity(Intent.createChooser(intent, "分享原文件")) }
                        .onFailure { Toast.makeText(this, "无法打开系统分享：${it.message}", Toast.LENGTH_LONG).show() }
                }.onFailure { Toast.makeText(this, "准备分享失败：${it.message}；未发送任何文件", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun mutateAssetNow(
        asset: AssetMeta,
        mutateMetadata: (JSONObject) -> Unit
    ): AssetMeta {
        val now = System.currentTimeMillis()
        val path = "images/${asset.id}.info/metadata.json"
        val change = mutateJsonFile(path) { metadata ->
            if (metadata.optString("name") != asset.name || metadata.optString("ext").lowercase(Locale.ROOT) != asset.ext || metadata.optBoolean("isDeleted") != asset.isDeleted ||
                jsonStrings(metadata, "folders").toSet() != asset.folders || jsonStrings(metadata, "tags").toSet() != asset.tags.toSet()) throw LibraryConflict()
            mutateMetadata(metadata)
            metadata.put("modificationTime", now)
        }
        try { mergeLibraryMtime(asset.id, now) }
        catch (error: Exception) { rollbackJsonFile(path, change, error) }
        return parseAssetMetadata(asset.id, JSONObject(String(change.after, StandardCharsets.UTF_8)), now)
    }

    private fun jsonStrings(json: JSONObject, key: String): List<String> = (json.optJSONArray(key) ?: JSONArray()).let { array ->
        (0 until array.length()).map { array.getString(it) }
    }

    @Composable
    private fun UploadConfirmation() {
        var folderIds by remember { mutableStateOf(setOfNotNull(selectedFolderId)) }
        var tags by remember { mutableStateOf(emptyList<String>()) }
        var folderMenu by remember { mutableStateOf(false) }
        var tagMenu by remember { mutableStateOf(false) }
        val folderNames = flatFolderTree.filter { it.id in folderIds }.map { it.name }
        AlertDialog(
            onDismissRequest = { uploadCandidates = emptyList() },
            title = { Text("上传 ${uploadCandidates.size} 张图片") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("素材库：${libraryName.orEmpty()}")
                    InspectorSection("标签", tags, true) { tagMenu = true }
                    InspectorSection("文件夹", folderNames, true) { folderMenu = true }
                    Text("保留原文件；手机生成缩略图和元数据。仅支持系统可解码的图片，单张最多 200 MB。失败任务保留，可手动重试。", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = canUpload() && !batchBusy, onClick = {
                    val uris = uploadCandidates
                    uploadCandidates = emptyList()
                    runUploads(uris, folderIds, tags)
                }) { Text("上传") }
            },
            dismissButton = { TextButton(onClick = { uploadCandidates = emptyList() }) { Text("取消") } }
        )
        if (folderMenu) FolderPicker(folderIds, true, { folderMenu = false }) { selected ->
            folderIds = selected
            folderMenu = false
        }
        if (tagMenu) TagPicker(tags, { tagMenu = false }) { selected -> tags = selected; tagMenu = false }
    }

    private fun uploadRoot() = File(noBackupFilesDir, "eagle-uploads")

    private fun canUpload() = libraryLoaded && libraryId != null &&
        UploadLogic.canUpload(source.name, connected, localLibraryUri != null &&
            contentResolver.persistedUriPermissions.any { it.uri == localLibraryUri && it.isWritePermission })

    private fun pendingUploads(): List<File> = uploadRoot().listFiles().orEmpty().filter { job ->
        runCatching {
            val state = JSONObject(File(job, "state.json").readText())
            state.optBoolean("ready") && UploadLogic.matchesTarget(state, source.name, libraryId, driveId)
        }.getOrDefault(false)
    }

    private fun saveUploadState(job: File, state: JSONObject) {
        val temporary = File(job, "state.json.tmp")
        temporary.writeText(state.toString())
        check(temporary.renameTo(File(job, "state.json"))) { "无法保存上传进度" }
    }

    private fun stageUpload(uri: Uri, folderIds: Set<String>, tags: List<String>): File {
        val now = System.currentTimeMillis()
        val id = UploadLogic.newId()
        val displayName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: error("无法读取图片名称")
        val fileName = UploadLogic.safeFileName(displayName)
        val job = File(uploadRoot(), id)
        check(job.mkdirs())
        try {
            val original = File(job, "original")
            contentResolver.openInputStream(uri)?.use { input ->
                original.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        if (Thread.currentThread().isInterrupted) throw InterruptedException()
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= 200L * 1024 * 1024) { "单张图片不能超过 200 MB" }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("无法读取选中的图片")
            check(original.length() > 0) { "图片文件为空" }
            var width = 0
            var height = 0
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(original)) { decoder, info, _ ->
                width = info.size.width
                height = info.size.height
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val sample = ((maxOf(width, height) + 599) / 600).coerceAtLeast(1)
                decoder.setTargetSampleSize(sample)
            }
            try {
                File(job, "thumbnail.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
            File(job, "metadata.json").writeText(UploadLogic.metadata(id, fileName, original.length(), width, height, now, folderIds, tags).toString())
            saveUploadState(job, JSONObject().put("ready", true).put("id", id).put("fileName", fileName)
                .put("source", source.name).put("libraryId", libraryId).put("driveId", driveId).put("mtime", now))
            return job
        } catch (error: Throwable) {
            // Only our newly staged local copy is removed; never the selected source file.
            job.deleteRecursively()
            throw error
        }
    }

    private fun runUploads(uris: List<Uri>, folderIds: Set<String> = emptySet(), tags: List<String> = emptyList()) {
        if (!canUpload() || batchBusy) return
        batchBusy = true
        uploadMessage = "正在准备上传…"
        ContextCompat.startForegroundService(this, Intent(this, IndexingService::class.java).putExtra("upload", true))
        queueEdit {
            var succeeded = 0
            var failed = 0
            var lastFailure: Throwable? = null
            try {
                val jobs = if (uris.isEmpty()) pendingUploads() else uris.mapNotNull { uri ->
                    runCatching { stageUpload(uri, folderIds, tags) }.onFailure {
                        if (it is InterruptedException) throw it
                        failed++; lastFailure = it
                    }.getOrNull()
                }
                jobs.forEachIndexed { index, job ->
                    if (Thread.currentThread().isInterrupted) throw InterruptedException()
                    runOnUiThread { uploadMessage = "正在上传 ${index + 1}/${jobs.size}…" }
                    runCatching { uploadJob(job) }.onSuccess { succeeded++ }.onFailure {
                        if (it is InterruptedException) throw it
                        failed++
                        lastFailure = it
                        Log.w("MAGLE", "Upload failed: ${it.javaClass.simpleName}, HTTP ${(it as? HttpFailure)?.code ?: 0}")
                    }
                }
            } catch (error: Throwable) { lastFailure = error } finally {
                stopService(Intent(this, IndexingService::class.java))
                val message = "上传完成 $succeeded 张" + if (failed > 0 || lastFailure != null) "；部分未完成，可重试。${lastFailure?.message.orEmpty().take(180)}" else ""
                runOnUiThread {
                    batchBusy = false
                    uploadMessage = message
                    refreshSettings()
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    if (succeeded > 0) loadLibrary(libraryId!!, libraryName.orEmpty(), false)
                }
            }
        }
    }

    private fun uploadJob(job: File) {
        val state = JSONObject(File(job, "state.json").readText())
        check(UploadLogic.matchesTarget(state, source.name, libraryId, driveId)) { "上传任务不属于当前素材库" }
        val id = state.getString("id")
        check(id.length == 13 || id.length == 36) { "旧上传任务的 ID 不兼容 Eagle，请保留任务并修复后再重试" }
        if (source != LibrarySource.ONEDRIVE) {
            uploadFileJob(job, state)
            return
        }
        val base = "/drives/${enc(driveId!!)}/items/${enc(libraryId!!)}"
        // UUID-based IDs reserve an isolated directory; retries always reuse this same directory.
        val directory = "$base:/images/${enc("$id.info")}:"
        if (!state.has("remoteId")) {
            val existing = try { graphJson(directory + "?\$select=id") } catch (error: HttpFailure) {
                if (error.code != 404) throw error
                null
            }
            val remoteId = existing?.getString("id") ?: retry(3) {
                graphPost("$base:/images:/children", JSONObject().put("name", "$id.info")
                    .put("folder", JSONObject()).put("@microsoft.graph.conflictBehavior", "fail"))
            }.getString("id")
            state.put("remoteId", remoteId)
            saveUploadState(job, state)
        }
        val fileName = state.getString("fileName")
        if (!state.optBoolean("originalDone")) {
            uploadOriginal(job, state)
            state.put("originalDone", true)
            saveUploadState(job, state)
        }
        if (!state.optBoolean("thumbnailDone")) {
            val thumbName = fileName.substringBeforeLast('.') + "_thumbnail.png"
            retry(3) { graphPut(assetFile(id, thumbName), File(job, "thumbnail.png").readBytes()) }
            state.put("thumbnailDone", true)
            saveUploadState(job, state)
        }
        if (!state.optBoolean("metadataDone")) {
            retry(3) { graphPut(assetFile(id, "metadata.json"), File(job, "metadata.json").readBytes()) }
            state.put("metadataDone", true)
            saveUploadState(job, state)
        }
        mergeLibraryMtime(id, state.getLong("mtime"))
        TokenStore.removeSecret(this, "upload-$id")
        check(job.deleteRecursively()) { "云端已上传，本地任务清理失败，可重试确认" }
    }

    private fun uploadFileJob(job: File, state: JSONObject) {
        val id = state.getString("id")
        val fileName = state.getString("fileName")
        retry(3) {
            when (source) {
                LibrarySource.LOCAL -> {
                    val root = localRoot()
                    check(root.canWrite()) { "本地素材库不可写，请重新选择文件夹并授予写入权限" }
                    val images = root.findFile("images") ?: root.createDirectory("images") ?: error("无法创建本地 images 目录")
                    check(images.isDirectory) { "images 不是文件夹" }
                    val directory = images.findFile("$id.info") ?: images.createDirectory("$id.info") ?: error("无法创建本地素材目录")
                    check(directory.isDirectory && directory.name == "$id.info") { "本地素材目录无效" }
                    localAssetDirectories = localAssetDirectories + (id to directory)
                }
                LibrarySource.SMB, LibrarySource.GOOGLE, LibrarySource.DROPBOX -> remote().createAssetDirectory(id, state) { saveUploadState(job, state) }
                LibrarySource.WEBDAV -> webDavCreateDirectory(webDavPath("images/${encPath("$id.info")}/"))
                else -> error("此来源暂不支持上传")
            }
        }
        if (!state.optBoolean("originalDone")) {
            val original = File(job, "original")
            retry(3) {
                uploadStagedFile(id, fileName, original, state, job)
            }
            state.put("originalDone", true)
            saveUploadState(job, state)
        }
        if (!state.optBoolean("thumbnailDone")) {
            retry(3) { uploadStagedFile(id, fileName.substringBeforeLast('.') + "_thumbnail.png", File(job, "thumbnail.png"), state, job) }
            state.put("thumbnailDone", true)
            saveUploadState(job, state)
        }
        if (!state.optBoolean("metadataDone")) {
            retry(3) { uploadStagedFile(id, "metadata.json", File(job, "metadata.json"), state, job) }
            state.put("metadataDone", true)
            saveUploadState(job, state)
        }
        mergeLibraryMtime(id, maxOf(state.getLong("mtime"), System.currentTimeMillis()))
        check(job.deleteRecursively()) { "素材库已写入，本地任务清理失败，可重试确认" }
    }

    private fun uploadStagedFile(id: String, name: String, staged: File, state: JSONObject, job: File) {
        when (source) {
            LibrarySource.LOCAL -> {
                val directory = localAssetDirectories[id] ?: error("本地素材目录不存在")
                val mime = if (name == "metadata.json") "application/json"
                    else java.net.URLConnection.guessContentTypeFromName(name) ?: "application/octet-stream"
                val target = directory.findFile(name) ?: directory.createFile(mime, name) ?: error("无法创建本地素材文件")
                check(target.isFile && target.name == name) { "文件管理器更改了文件名，无法写入 Eagle 素材" }
                contentResolver.openOutputStream(target.uri, "wt")?.use { output ->
                    staged.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (Thread.currentThread().isInterrupted) throw InterruptedException()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                    }
                } ?: error("无法写入本地素材，请检查文件夹权限及剩余空间")
                check(target.length() == staged.length()) { "本地文件大小校验失败，任务已保留，可重试" }
            }
            LibrarySource.SMB, LibrarySource.GOOGLE, LibrarySource.DROPBOX -> remote().uploadFile("images/$id.info/$name", staged, state) { saveUploadState(job, state) }
            LibrarySource.WEBDAV -> webDavUploadFile(webDavPath("images/${encPath("$id.info")}/${encPath(name)}"), staged)
            else -> error("此来源暂不支持上传")
        }
    }

    private fun uploadOriginal(job: File, state: JSONObject) {
        val id = state.getString("id")
        val original = File(job, "original")
        val path = "/drives/${enc(driveId!!)}/items/${enc(state.getString("remoteId"))}:/${enc(state.getString("fileName"))}"
        var sessionUrl = TokenStore.loadSecret(this, "upload-$id")
        var offset = 0L
        if (sessionUrl != null) {
            try {
                offset = UploadLogic.nextOffset(JSONObject(String(readResponse(connection(sessionUrl)), StandardCharsets.UTF_8)), original.length())
            } catch (error: HttpFailure) {
                if (error.code != 404 && error.code != 410) throw error
                // The final response may have been lost after the server committed the file.
                val remoteFile = try { graphJson("$path:?\$select=size") } catch (missing: HttpFailure) {
                    if (missing.code != 404) throw missing
                    null
                }
                if (remoteFile?.optLong("size") == original.length()) return
                sessionUrl = null
            }
        }
        if (sessionUrl == null) {
            sessionUrl = retry(3) { graphPost("$path:/createUploadSession", JSONObject().put("item",
                JSONObject().put("@microsoft.graph.conflictBehavior", "replace"))) }.getString("uploadUrl")
            check(Uri.parse(sessionUrl).scheme == "https")
            TokenStore.saveSecret(this, "upload-$id", sessionUrl)
        }
        val uploadUrl = sessionUrl!!
        RandomAccessFile(original, "r").use { file ->
            // Native Graph chunk size: 5 MiB is a multiple of 320 KiB.
            val buffer = ByteArray(5 * 1024 * 1024)
            while (offset < original.length()) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                file.seek(offset)
                val count = minOf(buffer.size.toLong(), original.length() - offset).toInt()
                file.readFully(buffer, 0, count)
                try {
                    val response = retry(3) {
                        readResponse(connection(uploadUrl).apply {
                            requestMethod = "PUT"
                            doOutput = true
                            setFixedLengthStreamingMode(count)
                            setRequestProperty("Content-Type", "application/octet-stream")
                            setRequestProperty("Content-Range", "bytes $offset-${offset + count - 1}/${original.length()}")
                            // Upload URLs are pre-authorized; do not send the OAuth token here.
                            outputStream.use { it.write(buffer, 0, count) }
                        })
                    }
                    val json = JSONObject(String(response, StandardCharsets.UTF_8))
                    val next = if (json.has("id")) {
                        check(json.optLong("size") == original.length()) { "上传结果的文件大小不一致" }
                        original.length()
                    } else UploadLogic.nextOffset(json, original.length())
                    check(next > offset) { "上传进度未推进，请稍后重试" }
                    offset = next
                } catch (error: HttpFailure) {
                    if (error.code != 416) throw error
                    offset = UploadLogic.nextOffset(JSONObject(String(readResponse(connection(uploadUrl)), StandardCharsets.UTF_8)), original.length())
                }
                val percent = (offset * 100 / original.length()).toInt()
                runOnUiThread { uploadMessage = "原文件上传 $percent%" }
            }
        }
    }

    private fun graphPost(path: String, json: JSONObject): JSONObject {
        val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        return JSONObject(String(readResponse(connection(GRAPH + path).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${graphAccessToken()}")
            setRequestProperty("Content-Type", "application/json")
            setFixedLengthStreamingMode(bytes.size)
            outputStream.use { it.write(bytes) }
        }), StandardCharsets.UTF_8))
    }


    private fun saveDownload(rawName: String, bytes: ByteArray): Result<Unit> =
        runCatching {
            val name = rawName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime(name))
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/MAGLE")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("无法创建下载文件")
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("无法写入下载文件")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            } catch (error: Exception) {
                contentResolver.delete(uri, null, null)
                throw error
            }
        }

    private fun parseFolders(array: JSONArray): List<FolderNode> = buildList {
        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)
            add(FolderNode(
                json.getString("id"),
                json.getString("name"),
                parseFolders(json.optJSONArray("children") ?: JSONArray())
            ))
        }
    }

    private fun flattenVisibleFolders(
        nodes: List<FolderNode>,
        expanded: Set<String>,
        depth: Int = 0
    ): List<FolderRow> = buildList {
        nodes.forEach { node ->
            add(FolderRow(node, depth))
            if (LibraryLogic.shouldShowChildren(node.id, expanded)) {
                addAll(flattenVisibleFolders(node.children, expanded, depth + 1))
            }
        }
    }

    private fun allFolders(nodes: List<FolderNode>): List<FolderNode> = buildList {
        nodes.forEach { node ->
            add(node)
            addAll(allFolders(node.children))
        }
    }

    private fun folderChoices(nodes: List<FolderNode>, parent: String = ""): List<FolderChoice> = buildList {
        nodes.forEach { node ->
            val label = if (parent.isBlank()) node.name else "$parent / ${node.name}"
            add(FolderChoice(node.id, label))
            addAll(folderChoices(node.children, label))
        }
    }

    private fun tokenRequest(values: Map<String, String>): JSONObject {
        val result = postFormAllowError("$LOGIN/token", values)
        if (!result.has("access_token")) throw HttpFailure(400, 0L, result.optString("error_description", "Microsoft 登录失败"))
        return result
    }

    private fun postForm(url: String, values: Map<String, String>): JSONObject {
        val result = postFormAllowError(url, values)
        if (result.has("error")) error(result.optString("error_description", result.getString("error")))
        return result
    }

    private fun postFormAllowError(url: String, values: Map<String, String>): JSONObject {
        val form = values.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, StandardCharsets.UTF_8.name())}=" +
                    URLEncoder.encode(it.value, StandardCharsets.UTF_8.name())
        }
        val connection = connection(url).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        connection.outputStream.use { it.write(form.toByteArray(StandardCharsets.UTF_8)) }
        return JSONObject(String(readResponse(connection), StandardCharsets.UTF_8))
    }

    private fun graphJson(path: String) = JSONObject(String(graphBytes(path), StandardCharsets.UTF_8))

    private fun graphJsonUrl(url: String) = JSONObject(String(graphBytesUrl(url), StandardCharsets.UTF_8))

    private fun graphBytes(path: String): ByteArray {
        return graphBytesUrl(GRAPH + path)
    }

    private fun graphBytesUrl(url: String): ByteArray {
        fun request(token: String): ByteArray = readResponse(connection(url).apply {
            setRequestProperty("Authorization", "Bearer $token")
        })
        val token = graphAccessToken()
        return try { request(token) } catch (error: HttpFailure) {
            if (error.code != 401) throw error
            synchronized(tokenLock) { if (accessToken == token) accessTokenExpires = 0L }
            request(graphAccessToken())
        }
    }

    private fun graphPut(path: String, bytes: ByteArray, etag: String? = null): ByteArray {
        val connection = connection(GRAPH + path).apply {
            requestMethod = "PUT"
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${graphAccessToken()}")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (etag != null) setRequestProperty("If-Match", etag)
            setFixedLengthStreamingMode(bytes.size)
        }
        connection.outputStream.use { it.write(bytes) }
        return readResponse(connection)
    }

    private fun connection(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        check(URL(url).protocol == "https" || source == LibrarySource.WEBDAV &&
            getSharedPreferences("library", MODE_PRIVATE).getBoolean("allowLanWebDavHttp", false) && UploadLogic.isPrivateHttpUrl(url)) { "不允许未加密的连接" }
        connectTimeout = 20_000
        readTimeout = 90_000
        instanceFollowRedirects = true
    }

    private fun readResponse(connection: HttpURLConnection): ByteArray {
        try {
        val code = connection.responseCode
        val input: InputStream? = if (code in 200..399) connection.inputStream else connection.errorStream
        val bytes = ByteArrayOutputStream().also { output ->
            input?.use {
                val buffer = ByteArray(8192)
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
            }
        }.toByteArray()
        if (code !in 200..299) {
            var body = String(bytes, StandardCharsets.UTF_8)
            runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()?.let { body = it }
            throw HttpFailure(code, connection.getHeaderField("Retry-After")?.toLongOrNull()?.times(1000L) ?: 0L,
                "HTTP $code: $body")
        }
        return bytes
        } finally { connection.disconnect() }
    }

    private class HttpFailure(val code: Int, val retryAfterMs: Long, message: String) : java.io.IOException(message)

    private fun <T> retry(attempts: Int, action: () -> T): T {
        var last: Throwable? = null
        repeat(attempts) { attempt ->
            try { return action() } catch (error: Throwable) {
                if (error is InterruptedException || Thread.currentThread().isInterrupted) throw error
                if (error is HttpFailure && error.code != 429 && error.code !in 500..599) throw error
                last = error
                if (attempt + 1 < attempts) Thread.sleep(maxOf(1500L * (attempt + 1),
                    (error as? HttpFailure)?.retryAfterMs ?: 0L).coerceAtMost(60_000L))
            }
        }
        throw last ?: IllegalStateException("重试失败")
    }

    private fun listAssetIds(id: String): Set<String> = when (source) {
        LibrarySource.ONEDRIVE -> buildSet {
            var next: String? = "$GRAPH/drives/${enc(driveId!!)}/items/${enc(id)}:/images:/children" +
                "?\$select=name,folder&\$top=200"
            while (next != null) {
                val page = graphJsonUrl(next)
                val values = page.getJSONArray("value")
                for (i in 0 until values.length()) {
                    val item = values.getJSONObject(i)
                    val name = item.optString("name")
                    if (item.has("folder") && name.endsWith(".info", true)) add(name.dropLast(5))
                }
                next = page.optString("@odata.nextLink").takeIf(String::isNotBlank)
            }
        }
        LibrarySource.LOCAL -> {
            val root = localRoot()
            val directories = root.findFile("images")?.listFiles().orEmpty()
                .filter { it.isDirectory && it.name?.endsWith(".info", true) == true }
                .associateBy { it.name!!.dropLast(5) }
            localAssetDirectories = directories
            directories.keys
        }
        LibrarySource.WEBDAV -> webDavAssetIds()
        LibrarySource.DROPBOX, LibrarySource.GOOGLE, LibrarySource.SMB, LibrarySource.S3 -> remote().assetIds()
    }

    private fun readLibraryFile(id: String, filename: String): ByteArray = when (source) {
        LibrarySource.ONEDRIVE -> graphBytes(libraryFile(id, filename))
        LibrarySource.LOCAL -> readDocument(localRoot().findFile(filename))
        LibrarySource.WEBDAV -> webDavBytes(webDavPath(filename))
        LibrarySource.DROPBOX, LibrarySource.GOOGLE, LibrarySource.SMB, LibrarySource.S3 -> remote().read(filename)
    }

    private fun readAssetFile(id: String, filename: String): ByteArray = when (source) {
        LibrarySource.ONEDRIVE -> graphBytes(assetFile(id, filename))
        LibrarySource.LOCAL -> readDocument(localAssetDirectories[id]?.findFile(filename))
        LibrarySource.WEBDAV -> webDavBytes(webDavPath("images/${encPath("$id.info")}/${encPath(filename)}"))
        LibrarySource.DROPBOX, LibrarySource.GOOGLE, LibrarySource.SMB, LibrarySource.S3 -> remote().read("images/$id.info/$filename")
    }


    private fun remote() = remoteLibrary ?: error("素材库尚未连接")

    private fun localRoot() = DocumentFile.fromTreeUri(
        this,
        localLibraryUri ?: error("本地素材库授权已失效")
    ) ?: error("无法读取本地素材库")

    private fun readDocument(file: DocumentFile?): ByteArray {
        val target = file ?: error("素材库文件不存在")
        return contentResolver.openInputStream(target.uri)?.use(InputStream::readBytes)
            ?: error("无法读取 ${target.name}")
    }

    private fun writeDocument(file: DocumentFile?, bytes: ByteArray) {
        val target = file ?: error("素材库文件不存在")
        contentResolver.openOutputStream(target.uri, "wt")?.use { it.write(bytes) }
            ?: error("无法写入 ${target.name}")
    }

    private fun webDavPath(relative: String) = Uri.parse(webDavUrl ?: error("WebDAV 未配置"))
        .buildUpon().appendEncodedPath(relative.trimStart('/')).build().toString()

    private val webDavClient by lazy {
        okhttp3.OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).writeTimeout(90, TimeUnit.SECONDS).build()
    }

    private fun webDavRequest(url: String, method: String = "GET", body: okhttp3.RequestBody? = null, depth: String? = null, ifMatch: String? = null): okhttp3.Response {
        val request = buildWebDavRequest(url, webDavUser, webDavPassword,
            getSharedPreferences("library", MODE_PRIVATE).getBoolean("allowLanWebDavHttp", false), method, body, depth, ifMatch)
        val response = webDavClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            val retryAfter = response.header("Retry-After")?.toLongOrNull()?.times(1000L) ?: 0L
            response.close()
            throw HttpFailure(code, retryAfter, "WebDAV HTTP $code，请检查路径和权限")
        }
        return response
    }

    private fun webDavBytes(url: String): ByteArray = webDavRequest(url).use { it.body.bytes() }

    private fun webDavCreateDirectory(url: String) {
        try {
            webDavRequest(url, "MKCOL").close()
        } catch (error: HttpFailure) {
            if (error.code != 405) throw error
            val bytes = webDavRequest(url, "PROPFIND", depth = "0").use { it.body.bytes() }
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            check(factory.newDocumentBuilder().parse(bytes.inputStream()).getElementsByTagNameNS("DAV:", "collection").length > 0) { "上传目录不存在或不可写" }
        }
    }

    private fun webDavUploadFile(url: String, file: File) {
        webDavRequest(url, "PUT", file.asRequestBody("application/octet-stream".toMediaType())).close()
        webDavRequest(url, "HEAD").use {
            check(it.header("Content-Length")?.toLongOrNull() == file.length()) { "上传后的文件大小无法确认" }
        }
    }

    private fun webDavAssetIds(): Set<String> {
        val body = "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>"
            .toRequestBody("application/xml; charset=utf-8".toMediaType())
        val xml = webDavRequest(webDavPath("images/"), "PROPFIND", body, "1").use { it.body.bytes() }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val nodes = factory.newDocumentBuilder().parse(xml.inputStream()).getElementsByTagNameNS("*", "href")
        return buildSet {
            for (i in 0 until nodes.length) {
                val name = Uri.decode(nodes.item(i).textContent.trimEnd('/').substringAfterLast('/'))
                if (name.endsWith(".info", true)) add(name.dropLast(5))
            }
        }
    }

    private fun libraryFile(id: String, filename: String) =
        "/drives/${enc(driveId!!)}/items/${enc(id)}:/${enc(filename)}:/content"

    private fun assetFile(id: String, filename: String) =
        "/drives/${enc(driveId!!)}/items/${enc(libraryId!!)}:/images/${enc("$id.info")}/${enc(filename)}:/content"

    private fun encPath(value: String) = Uri.encode(value)

    private fun loadSavedLibrary() {
        getSharedPreferences("library", MODE_PRIVATE).run {
            driveId = getString("driveId", null)
            libraryId = getString("libraryId", null)
            libraryName = getString("libraryName", null)
            source = runCatching {
                LibrarySource.valueOf(getString("source", LibrarySource.ONEDRIVE.name)!!)
            }.getOrDefault(LibrarySource.ONEDRIVE)
            localLibraryUri = getString("localUri", null)?.let(Uri::parse)
            webDavUrl = getString("webDavUrl", null)
            webDavUser = getString("webDavUser", null)
            webDavPassword = TokenStore.loadSecret(this@MainActivity, "webdav-password")
            savedLibraries = runCatching {
                val array = JSONArray(getString("knownLibraries", "[]"))
                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        add(SavedLibrary(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            source = LibrarySource.valueOf(item.getString("source")),
                            driveId = item.optString("driveId").takeIf(String::isNotBlank),
                            localUri = item.optString("localUri").takeIf(String::isNotBlank),
                            webDavUrl = item.optString("webDavUrl").takeIf(String::isNotBlank),
                            webDavUser = item.optString("webDavUser").takeIf(String::isNotBlank)
                        ))
                    }
                }
            }.getOrDefault(emptyList())
            if (savedLibraries.isEmpty() && libraryId != null) {
                savedLibraries = listOf(currentLibrary())
            }
        }
    }

    private fun ensureWritePermissionMigration() {
        getSharedPreferences("auth-migration", MODE_PRIVATE).run {
            if (!getBoolean("files-read-write", false)) {
                TokenStore.clear(this@MainActivity)
                edit().putBoolean("files-read-write", true).apply()
            }
        }
    }

    private fun saveLibrary() {
        val saved = currentLibrary()
        savedLibraries = (savedLibraries.filterNot { it.id == saved.id } + saved)
            .sortedBy { it.name.lowercase(Locale.ROOT) }
        val known = JSONArray().apply {
            savedLibraries.forEach { item -> put(JSONObject()
                .put("id", item.id)
                .put("name", item.name)
                .put("source", item.source.name)
                .put("driveId", item.driveId)
                .put("localUri", item.localUri)
                .put("webDavUrl", item.webDavUrl)
                .put("webDavUser", item.webDavUser)) }
        }
        getSharedPreferences("library", MODE_PRIVATE).edit()
            .putString("driveId", driveId)
            .putString("libraryId", libraryId)
            .putString("libraryName", libraryName)
            .putString("source", source.name)
            .putString("localUri", localLibraryUri?.toString())
            .putString("webDavUrl", webDavUrl)
            .putString("webDavUser", webDavUser)
            .putString("knownLibraries", known.toString())
            .apply()
        if (source == LibrarySource.WEBDAV) {
            webDavPassword?.let {
                runCatching {
                    TokenStore.saveSecret(this, "webdav-password", it)
                    TokenStore.saveSecret(this, "webdav-password-${saved.id.hashCode()}", it)
                }
            }
        }
    }

    private fun currentLibrary() = SavedLibrary(
        id = libraryId ?: error("素材库尚未选择"),
        name = libraryName ?: "Eagle 素材库",
        source = source,
        driveId = driveId,
        localUri = localLibraryUri?.toString(),
        webDavUrl = webDavUrl,
        webDavUser = webDavUser
    )

    @Composable
    private fun RebindLibraryDialog(saved: SavedLibrary) {
        val importing = saved.id.startsWith("backup:")
        AlertDialog(
            onDismissRequest = { if (!remoteConnecting) { rebindLibraryCandidate = null; if (importing) discardImportedIndex() } },
            title = { Text(if (importing) "导入索引 · ${saved.name}" else "切换素材库来源") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请选择同一个 Eagle 素材库在其他来源中的副本。库名称可以不同，但素材 ID、文件夹结构和配套元数据必须对应；仅有相同图片不算同一个库。")
                    if (importing) Text("备份不含原文件或登录凭据。请选择备份对应的已连接库，或连接新来源；确认同库后恢复索引与已有缩略图。换设备可能需要重新授权。")
                    Text("目标来源必须已存有这份库。本功能不上传或搬运原文件；核对后复用未变索引和已有缩略图，补同步少量新增或修改，原连接保留供切回。S3（Beta）暂不支持作为切换目标。")
                    OutlinedButton(enabled = !remoteConnecting && !indexing && !batchBusy && !sessionRestoring,
                        onClick = {
                            newSourceForLibrary = saved
                            rebindLibraryCandidate = null
                            sourceChooserOpen = true
                        }, modifier = Modifier.fillMaxWidth()) { Text("连接新来源（不重新建立完整索引）") }
                    savedLibraries.filter { it.id != saved.id && it.source != LibrarySource.S3 }.forEach { target ->
                        OutlinedButton(enabled = !remoteConnecting && !indexing && !batchBusy && !sessionRestoring,
                            onClick = { rebindLibrary(saved, target) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (importing) "${target.name} · ${target.source.label}" else target.source.label)
                        }
                    }
                    if (remoteConnecting) Text("正在核对目标库…")
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(enabled = !remoteConnecting,
                onClick = { rebindLibraryCandidate = null; if (importing) discardImportedIndex() }) { Text("取消") } }
        )
    }

    private fun finishNewSource(target: SavedLibrary): Boolean {
        val original = newSourceForLibrary ?: return false
        newSourceForLibrary = null
        rebindLibrary(original, target)
        return true
    }

    private fun cancelNewSource() {
        newSourceForLibrary?.let { rebindLibraryCandidate = it }
        newSourceForLibrary = null
    }

    private fun discardImportedIndex() {
        importedIndex?.directory?.deleteRecursively()
        importedIndex = null
    }

    private fun rebindLibrary(saved: SavedLibrary, target: SavedLibrary) {
        if (target.source == LibrarySource.S3) {
            Toast.makeText(this, "S3（Beta）暂不支持切换来源", Toast.LENGTH_LONG).show()
            return
        }
        if (indexing || batchBusy || sessionRestoring || remoteConnecting) return
        // Never redirect resumable writes to a different storage provider.
        if (pendingTagRename() != null || uploadRoot().listFiles().orEmpty().filter(File::isDirectory).any { job ->
                runCatching { JSONObject(File(job, "state.json").readText()).optString("libraryId")
                    .let { it in setOf(saved.id, target.id) } }.getOrDefault(true)
            }) {
            Toast.makeText(this, "请先完成未完成的上传或标签修改", Toast.LENGTH_LONG).show()
            return
        }
        remoteConnecting = true
        val backup = if (saved.id.startsWith("backup:")) importedIndex else null
        work.execute {
            runCatching {
                val original = backup?.index ?: JSONObject(File(filesDir, "eagle-index-${saved.id.hashCode()}.json").readText())
                val reader: (String) -> ByteArray = when (target.source) {
                    LibrarySource.LOCAL -> {
                        val root = DocumentFile.fromTreeUri(this, Uri.parse(target.localUri ?: error("本地授权丢失")))
                            ?: error("本地目录无法访问");
                        { path: String ->
                            var file: DocumentFile? = root
                            path.split('/').forEach { part -> file = file?.findFile(part) }
                            readDocument(file)
                        }
                    }
                    LibrarySource.ONEDRIVE -> { path -> graphBytes("/drives/${enc(target.driveId!!)}/items/${enc(target.id)}:/${enc(path)}:/content") }
                    LibrarySource.WEBDAV -> {
                        val password = TokenStore.loadSecret(this, "webdav-password-${target.id.hashCode()}")
                            ?: error("目标 WebDAV 凭据丢失，请重新连接");
                        { path: String ->
                            val url = Uri.parse(target.webDavUrl!!).buildUpon().appendEncodedPath(enc(path)).build().toString()
                            webDavClient.newCall(buildWebDavRequest(url, target.webDavUser, password,
                                getSharedPreferences("library", MODE_PRIVATE).getBoolean("allowLanWebDavHttp", false))).execute().use {
                                check(it.isSuccessful) { "目标 WebDAV HTTP ${it.code}" }
                                it.body.bytes()
                            }
                        }
                    }
                    else -> {
                        if (target.source == LibrarySource.GOOGLE) {
                            val config = JSONObject(getSharedPreferences("remote-libraries", MODE_PRIVATE).getString(target.id, "{}")!!)
                            check(config.optString("account") == googleAccount) { "请先激活目标 Google 库完成账号授权，再切换来源" }
                        }
                        val client = RemoteLibrary.load(this, target.id, ::googleToken);
                        { path: String -> client.read(path) }
                    }
                }
                val metadata = JSONObject(String(reader("metadata.json"), StandardCharsets.UTF_8))
                val manifest = JSONObject(String(reader("mtime.json"), StandardCharsets.UTF_8))
                val samples = original.getJSONObject("items").keys().asSequence()
                    .filter { manifest.has(it) && it != "all" }.sorted().take(3).associateWith {
                        JSONObject(String(reader("images/$it.info/metadata.json"), StandardCharsets.UTF_8))
                    }
                val targetFile = File(filesDir, "eagle-index-${target.id.hashCode()}.json")
                val existing = runCatching { JSONObject(targetFile.readText()) }.getOrNull()
                val cache = rebindIndex(original, existing, manifest, samples, target.id, target.name)
                Log.i("MAGLE", "${if (backup != null) "索引导入" else "来源切换"} ${saved.source.name} -> ${target.source.name}：复用 ${cache.getJSONObject("items").length()} 项，目标清单 ${cache.optInt("scanTotal")} 项")
                // Cache-only commit: keep the source index and both connection records for recovery.
                val temporary = File(filesDir, "${targetFile.name}.rebind")
                temporary.writeText(cache.toString())
                check(temporary.renameTo(targetFile)) { "无法保存目标索引，原连接未切换" }
                File(filesDir, "eagle-folders-${target.id.hashCode()}.json").writeText(metadata.toString())
                File(cacheDir, "session-thumbnails").mkdirs()
                cache.getJSONObject("items").keys().forEach { assetId ->
                    val item = cache.getJSONObject("items").getJSONObject(assetId)
                    val key = LibraryLogic.thumbnailCacheKey(saved.id, assetId, item.optLong("mtime"))
                    val oldThumbnail = if (backup != null) File(backup.directory, "thumbnails/$assetId.thumb")
                        .takeIf { original.getJSONObject("items").optJSONObject(assetId)?.optLong("mtime") == item.optLong("mtime") }
                        else File(File(cacheDir, "session-thumbnails"), key)
                    if (oldThumbnail?.isFile == true) runCatching {
                        oldThumbnail.copyTo(File(File(cacheDir, "session-thumbnails"), LibraryLogic.thumbnailCacheKey(target.id, assetId, item.optLong("mtime"))), false)
                    }
                }
                runOnUiThread {
                    remoteConnecting = false
                    rebindLibraryCandidate = null
                    if (backup != null) discardImportedIndex()
                    activateLibrary(target)
                    Toast.makeText(this, "${if (backup != null) "已导入" else "已切换来源"}，复用 ${cache.getJSONObject("items").length()} 项索引，正在检查差异", Toast.LENGTH_LONG).show()
                }
            }.onFailure {
                runOnUiThread { remoteConnecting = false }
                showError(if (backup != null) "导入索引失败，原连接保留" else "未切换来源，原库保持不变", it)
            }
        }
    }

    private fun activateLibrary(saved: SavedLibrary) {
        if (indexing || batchBusy || sessionRestoring || remoteConnecting) {
            Toast.makeText(this, "当前库正在索引，请完成后再切换", Toast.LENGTH_SHORT).show()
            return
        }
        clearSelection()
        sessionRestored = false
        sessionNeedsAuthorization = false
        source = saved.source
        driveId = saved.driveId
        libraryId = saved.id
        libraryName = saved.name
        localLibraryUri = saved.localUri?.let(Uri::parse)
        webDavUrl = saved.webDavUrl
        webDavUser = saved.webDavUser
        if (saved.source == LibrarySource.WEBDAV) {
            webDavPassword = TokenStore.loadSecret(this, "webdav-password-${saved.id.hashCode()}")
                ?: TokenStore.loadSecret(this, "webdav-password")
        }
        settingsOpen = false
        saveLibrary()
        if (saved.source in REMOTE_SOURCES) {
            restoreSession()
            return
        }
        if (saved.source == LibrarySource.ONEDRIVE) {
            openOfflineLibrary()
            if (connected) loadLibrary(saved.id, saved.name, false) else restoreSession()
        } else {
            loadLibrary(saved.id, saved.name, false)
        }
    }

    private fun removeLibrary(saved: SavedLibrary) {
        if (indexing || batchBusy) {
            Toast.makeText(this, "请等待当前索引或操作完成", Toast.LENGTH_SHORT).show()
            return
        }
        if (saved.source in REMOTE_SOURCES) {
            getSharedPreferences("remote-libraries", MODE_PRIVATE).edit().remove(saved.id).apply()
            TokenStore.removeSecret(this, "remote-${saved.id}")
        }
        File(filesDir, "eagle-index-${saved.id.hashCode()}.json").delete()
        File(filesDir, "eagle-folders-${saved.id.hashCode()}.json").delete()
        savedLibraries = savedLibraries.filterNot { it.id == saved.id }
        persistKnownLibraries()
        if (libraryId == saved.id) {
            val next = savedLibraries.firstOrNull()
            if (next != null) {
                activateLibrary(next)
            } else {
                libraryId = null
                libraryName = null
                libraryLoaded = false
                resetBrowseHistory()
                indexedAssets = emptyList()
                folderTree = emptyList()
                tagGroups = emptyList()
                getSharedPreferences("library", MODE_PRIVATE).edit()
                    .remove("driveId").remove("libraryId").remove("libraryName")
                    .remove("localUri").remove("webDavUrl").remove("webDavUser").apply()
            }
        }
        refreshSettings()
        Toast.makeText(this, "已移除素材库及本机索引", Toast.LENGTH_SHORT).show()
    }

    private fun renameLibrary(saved: SavedLibrary, name: String) {
        savedLibraries = savedLibraries.map { if (it.id == saved.id) it.copy(name = name) else it }
        persistKnownLibraries()
        if (libraryId == saved.id) {
            libraryName = name
            getSharedPreferences("library", MODE_PRIVATE).edit().putString("libraryName", name).apply()
        }
        File(filesDir, "eagle-index-${saved.id.hashCode()}.json").takeIf(File::isFile)?.let { file ->
            runCatching {
                val root = JSONObject(file.readText()).put("libraryName", name)
                file.writeText(root.toString())
            }
        }
        refreshSettings()
    }

    private fun persistKnownLibraries() {
        val known = JSONArray().apply {
            savedLibraries.forEach { item -> put(JSONObject()
                .put("id", item.id).put("name", item.name).put("source", item.source.name)
                .put("driveId", item.driveId).put("localUri", item.localUri)
                .put("webDavUrl", item.webDavUrl).put("webDavUser", item.webDavUser)) }
        }
        getSharedPreferences("library", MODE_PRIVATE).edit().putString("knownLibraries", known.toString()).apply()
    }

    private fun updateStatus(message: String) = runOnUiThread { status = message }

    private fun showError(title: String, error: Throwable) = runOnUiThread {
        Log.e("MAGLE", title, error)
        status = "$title：${error.message ?: "未知错误"}"
        Toast.makeText(this, status, Toast.LENGTH_LONG).show()
    }

    private data class CloudFolder(val id: String?, val name: String)
    private data class FolderNode(val id: String, val name: String, val children: List<FolderNode>)
    private data class FolderRow(val node: FolderNode, val depth: Int)
    private data class FolderChoice(val id: String, val label: String)
    private data class DevicePrompt(val code: String, val uri: String)
    private enum class LibrarySource(val label: String) {
        ONEDRIVE("OneDrive"), LOCAL("本地"), WEBDAV("WebDAV"),
        DROPBOX("Dropbox"), GOOGLE("Google Drive"), SMB("SMB"), S3("S3 兼容存储")
    }
    private data class LibraryIndexInfo(
        val file: File,
        val libraryId: String,
        val name: String,
        val itemCount: Int,
        val scanTotal: Int,
        val failedCount: Int,
        val bytes: Long,
        val current: Boolean
    )
    private data class SavedLibrary(
        val id: String,
        val name: String,
        val source: LibrarySource,
        val driveId: String?,
        val localUri: String?,
        val webDavUrl: String?,
        val webDavUser: String?
    )
    private data class AssetMeta(
        val id: String,
        val name: String,
        val ext: String,
        val folders: Set<String>,
        val noThumbnail: Boolean,
        val mtime: Long,
        val isDeleted: Boolean,
        val annotation: String,
        val url: String,
        val tags: List<String>,
        val size: Long,
        val width: Int,
        val height: Int
    ) {
        val fileName get() = "$name.$ext"
        val thumbnailName get() = "${name}_thumbnail.png"
    }
}

private fun enc(value: String) = Uri.encode(value)

private fun decodeSampled(bytes: ByteArray, maxEdge: Int, coverTarget: Boolean = false): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = LibraryLogic.bitmapSampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
    // Original-image viewing keeps its existing memory ceiling; only thumbnails must cover their cell.
    if (!coverTarget && maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

private fun mime(name: String): String = when {
    name.endsWith(".png", true) -> "image/png"
    name.endsWith(".webp", true) -> "image/webp"
    name.endsWith(".gif", true) -> "image/gif"
    name.endsWith(".heic", true) || name.endsWith(".heif", true) -> "image/heic"
    name.endsWith(".avif", true) -> "image/avif"
    name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
    name.endsWith(".svg", true) -> "image/svg+xml"
    name.endsWith(".pdf", true) -> "application/pdf"
    name.endsWith(".psd", true) -> "image/vnd.adobe.photoshop"
    else -> android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase(Locale.ROOT)) ?: "application/octet-stream"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(Locale.ROOT, bytes / 1024.0)
    else -> "%.1f MB".format(Locale.ROOT, bytes / (1024.0 * 1024.0))
}

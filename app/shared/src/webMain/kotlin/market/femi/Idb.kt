@file:OptIn(ExperimentalWasmJsInterop::class)

package market.femi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import market.femi.models.Audio
import market.femi.models.XmpItem
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsModule
import kotlin.js.JsName
import kotlin.js.Promise
import kotlin.js.definedExternally
import kotlin.js.get
import kotlin.js.length
import kotlin.js.toJsNumber
import kotlin.js.toJsString

const val VIDEOS: String = "videos"
const val NAME: String = "name"


@JsModule("idb")
external object Idb {
    fun openDB(
        name: String,
        version: Int,
        config: OpenDBConfig = definedExternally
    ): Promise<IDBPDatabase>
}

external interface IDBPDatabase : JsAny {
    fun createObjectStore(name: String, options: StoreOptions = definedExternally): IDBPObjectStore
    fun transaction(storeName: String, mode: String): IDBPTransaction
}

external interface IDBPIndex : JsAny {
    fun openCursor(
        query: JsAny? = definedExternally,
        direction: String = definedExternally
    ): Promise<IDBPCursorWithValue?>
    fun getAll(
        query: JsAny? = definedExternally,
        count: Int = definedExternally
    ): Promise<JsArray<JsAny>>
}

external interface IDBPObjectStore : JsAny {
    fun index(name: String): IDBPIndex
    fun createIndex(name: String, keyPath: String, options: IndexOptions = definedExternally): JsAny
    fun deleteIndex(name: String) // <-- ADD THIS LINE
    fun delete(key: JsAny): Promise<JsAny?>
    fun put(value: JsAny): Promise<JsAny?>
    fun openCursor(
        query: JsAny? = definedExternally,
        direction: String = definedExternally
    ): Promise<IDBPCursorWithValue?>

    fun getAll(
        query: JsAny? = definedExternally,
        count: Int = definedExternally
    ): Promise<JsArray<JsAny>>

    fun getAllKeys(
        query: JsAny? = definedExternally,
        count: Int = definedExternally
    ): Promise<JsArray<JsAny>>
}

external interface IDBPTransaction : JsAny {
    fun objectStore(name: String): IDBPObjectStore
    val done: Promise<JsAny?>
}

external object IDBKeyRange : JsAny {
    // open = true means "strictly greater than" (don't include the boundary key itself)
    fun lowerBound(bound: JsAny, open: Boolean = definedExternally): JsAny
}

external interface IDBPCursorWithValue : JsAny {
    val value: JsAny
    fun advance(count: Int): Promise<IDBPCursorWithValue?>

    @JsName("continue")
    fun continueCursor(): Promise<IDBPCursorWithValue?>
}

@JsName("IDBKeyRange")
external object IDBKeyRangeFactory : JsAny {
    fun bound(
        lower: String,
        upper: String,
        lowerOpen: Boolean = definedExternally,
        upperOpen: Boolean = definedExternally
    ): JsAny

    fun only(value: String): JsAny
    fun only(value: Int): JsAny
}

// --- Configuration Object Interfaces ---
external interface OpenDBConfig : JsAny {
    var upgrade: ((db: IDBPDatabase, oldVersion: Int, newVersion: Int?, transaction: IDBPTransaction) -> Unit)?
}

external interface StoreOptions : JsAny {
    var keyPath: String
}

external interface IndexOptions : JsAny {
    var unique: Boolean
}

// --- @JsFun Factories ---
@JsFun("function() { return {}; }")
external fun createOpenDBConfig(): OpenDBConfig

@JsFun("function(path) { return { keyPath: path }; }")
external fun createStoreOptions(path: String): StoreOptions

@JsFun("function(isUnique) { return { unique: isUnique }; }")
external fun createIndexOptions(isUnique: Boolean): IndexOptions


@JsFun("function(row) { return row.id; }")
external fun getRowId(row: JsAny): String



suspend fun getIdb(): IDBPDatabase {
    val config = createOpenDBConfig()
    val nonUniqueOpts = createIndexOptions(false)
    val uniqueOpts = createIndexOptions(true)
    config.upgrade = { db, oldVersion, _, transaction ->
        if (oldVersion < 1) {
            val storeOpts = createStoreOptions("id")
            // Videos
            val videosStore = db.createObjectStore(VIDEOS, storeOpts)
            videosStore.createIndex(NAME, NAME, uniqueOpts)
            // Audios
            val audiosStore = db.createObjectStore("audios", storeOpts)
            audiosStore.createIndex(NAME, NAME, uniqueOpts)
            // Images
            val imagesStore = db.createObjectStore("images", storeOpts)
            imagesStore.createIndex(NAME, NAME, uniqueOpts)
        }
        if (oldVersion < 2) {
            val audiosStore = transaction.objectStore("audios")
            audiosStore.createIndex("uid", "uid", nonUniqueOpts)
        }
        if (oldVersion < 3) {
            val audiosStore = transaction.objectStore("audios")
            audiosStore.deleteIndex("uid")
            audiosStore.createIndex("uid", "uid", uniqueOpts)
            audiosStore.createIndex("project", "project", nonUniqueOpts)
        }
        if (oldVersion < 4) {
            val audiosStore = transaction.objectStore("audios")
            audiosStore.createIndex("like", "like", nonUniqueOpts)
        }
        if (oldVersion < 5) {
            val audiosStore = transaction.objectStore("audios")
            audiosStore.deleteIndex("like")
            audiosStore.createIndex("like_int", "like_int", nonUniqueOpts)
        }
        if (oldVersion < 9) {
            val store = transaction.objectStore("videos")
            try { store.deleteIndex("project") } catch (_: Throwable) { /* fresh DB: index never existed */ }
            store.createIndex("project", "project", nonUniqueOpts)
        }
        if (oldVersion < 10) {
            val store = transaction.objectStore("images")
            store.createIndex("project","project", nonUniqueOpts)
        }
        if (oldVersion < 11) {
            val storeOpts = createStoreOptions("id")
            val store = db.createObjectStore("matrixs", storeOpts)
            store.createIndex(NAME, NAME, uniqueOpts)
        }
        if (oldVersion < 12) {
            db.createObjectStore("handles", createStoreOptions("id"))
        }
        if (oldVersion < 30) {
            val storeOpts = createStoreOptions("id")
            val xmpFilesStore = db.createObjectStore("files", storeOpts)
            xmpFilesStore.createIndex(NAME, NAME, uniqueOpts)
            xmpFilesStore.createIndex("projectName", "projectName", nonUniqueOpts)
        }
    }

    // Call openDB and await the native JS Promise using your extension
    return Idb.openDB("😎", 30, config).await()
}

fun IDBPDatabase.getReads(storeName: String): IDBPObjectStore {
    val tx = this.transaction(storeName, "readonly")
    return tx.objectStore(storeName)
}

fun IDBPDatabase.getWrites(storeName: String): Pair<IDBPTransaction, IDBPObjectStore> {
    val tx = this.transaction(storeName, "readwrite")
    return Pair(tx, tx.objectStore(storeName))
}

suspend fun <T> IDBPDatabase.getPaginated(
    storeName: String,
    lastSeenId: Int?,
    take: Int,
    serializer: KSerializer<T>
): List<T> {
    // 1. Build the O(1) B-Tree query boundary
    val query: JsAny? = if (lastSeenId != null) {
        IDBKeyRange.lowerBound(lastSeenId.toJsNumber(), open = true)
    } else {
        null
    }

    // 2. Fetch exactly `take` items starting immediately after `lastSeenId`
    val jsArray = this.getReads(storeName).getAll(query = query, count = take).await()

    val results = mutableListOf<T>()
    for (i in 0 until jsArray.length) {
        val jsValue = jsArray[i] ?: continue
        val jsonString = JSJSON.stringify(jsValue)
        results.add(AppJson.decodeFromString(serializer, jsonString))
    }

    return results
}

suspend fun<T> IDBPDatabase.getById(
    id: String,
    storeName: String,
    serializer: KSerializer<T>
): T? {
    val store = this.getReads(storeName)
    val range = IDBKeyRangeFactory.only(id)
    val jsArray = store.getAll(query = range, count = 1).await()
    if (jsArray.length == 0) return null
    val jsValue = jsArray[0] ?: return null
    val jsonString = JSJSON.stringify(jsValue)
    return AppJson.decodeFromString(serializer, jsonString)
}

suspend fun <T> IDBPDatabase.searchByIndex(
    storeName: String,
    indexName: String,
    prefix: String,
    limit: Int,
    serializer: KSerializer<T>
): List<T> {
    val store = this.getReads(storeName)
    val index = store.index(indexName)

    // Create the exact B-Tree slice for the prefix
    val lower = prefix
    val upper = lower + "\uFFFF"
    val range = IDBKeyRangeFactory.bound(lower, upper)

    // Open a cursor explicitly on the Index, restricted by the KeyRange
    val jsArray = index.getAll(query = range, count = limit).await()
    val results = mutableListOf<T>()

    // Sequentially read only the exact matches until the 5k limit is hit
    for (i in 0 until jsArray.length) {
        val jsValue = jsArray[i] ?: continue
        val jsonString = JSJSON.stringify(jsValue)
        results.add(AppJson.decodeFromString(serializer, jsonString))
    }

    return results
}

suspend fun IDBPDatabase.getMaxKey(storeName: String): Int {
    val store = this.getReads(storeName)

    // Instantly jump to the end of the B-Tree to find the highest AlphaRank
    val cursor = store.openCursor(null, "prev").await() ?: return 0
    val jsValue = cursor.value
    val jsonString = JSJSON.stringify(jsValue)
    return AppJson.parseToJsonElement(jsonString).jsonObject["id"]?.jsonPrimitive?.int ?: 0
}



suspend fun IDBPDatabase.getAudioByUid(uid: String): Audio? {
    val store = this.getReads("audios")
    val index = store.index("uid")
    val range = IDBKeyRangeFactory.only(uid)

    // O(log N) direct B-Tree lookup
    val cursor = index.openCursor(range).await() ?: return null
    val jsValue = cursor.value
    val jsonString = JSJSON.stringify(jsValue)
    return AppJson.decodeFromString(Audio.serializer(), jsonString)
}


suspend fun <T> IDBPDatabase.getListByIndexValue(
    storeName:String,
    index:String,
    value:String,
    limit:Int=5000,
    serializer: KSerializer<T>
): List<T> {
    val store = this.getReads(storeName)
    val index = store.index(index)
    val range = IDBKeyRangeFactory.only(value)
    val jsArray = index.getAll(query = range, count = limit).await()
    val results = mutableListOf<T>()
    for (i in 0 until jsArray.length) {
        val jsValue = jsArray[i]
        if (jsValue != null) {
            results.add(AppJson.decodeFromString(serializer, JSJSON.stringify(jsValue)))
        }
    }
    return results
}


suspend fun IDBPDatabase.getLikedAudios(limit: Int): List<Audio> {
    val store = this.getReads("audios")
    val index = store.index("like_int") // <-- Point to the new integer index
    val range = IDBKeyRangeFactory.only(1) // <-- Query for 1

    // Native C++ batch fetch, single Wasm boundary crossing
    val jsArray = index.getAll(query = range, count = limit).await()

    val results = mutableListOf<Audio>()
    for (i in 0 until jsArray.length) {
        val jsValue = jsArray[i]
        if (jsValue != null) {
            results.add(AppJson.decodeFromString(Audio.serializer(), JSJSON.stringify(jsValue)))
        }
    }
    return results
}

suspend fun IDBPDatabase.getLikedFiles(limit: Int): List<XmpItem> {
    val store = this.getReads("files")
    val index = store.index("like_int") // <-- Point to the new integer index
    val range = IDBKeyRangeFactory.only(1) // <-- Query for 1
    // Native C++ batch fetch, single Wasm boundary crossing
    val jsArray = index.getAll(query = range, count = limit).await()
    val results = mutableListOf<XmpItem>()
    for (i in 0 until jsArray.length) {
        val jsValue = jsArray[i]
        if (jsValue != null) {
            results.add(AppJson.decodeFromString(XmpItem.serializer(), JSJSON.stringify(jsValue)))
        }
    }
    return results
}

suspend fun IDBPDatabase.backfillAudioLikeInts() {
    var lastSeenId: String? = null
    val limit = 5000 // Match your established architecture limit
    var totalUpdated = 0

    while (true) {
        val query: JsAny? = if (lastSeenId != null) {
            IDBKeyRange.lowerBound(lastSeenId.toJsString(), open = true)
        } else null

        val jsArray = this.getReads("audios").getAll(query = query, count = limit).await()
        if (jsArray.length == 0) break

        val (tx, storeWrites) = this.getWrites("audios")
        var batchUpdatedCount = 0

        for (i in 0 until jsArray.length) {
            val jsValue = jsArray[i] ?: continue

            // Track the cursor boundary for the next batch
            if (i == jsArray.length - 1) {
                lastSeenId = getRowId(jsValue)
            }

            // Instantly check if it needs an update without serialization
            if (needsLikeIntUpdate(jsValue)) {
                storeWrites.put(applyLikeIntPatch(jsValue))
                batchUpdatedCount++
            }
        }

        tx.done.await()
        totalUpdated += batchUpdatedCount
    }

    if (totalUpdated > 0) {
        println("🧹 Sparse Index Backfill Complete: Optimized $totalUpdated audio records.")
    }
}

@JsFun("function(row) { return (!row.project || row.project.trim() === ''); }")
external fun needsImageProjectUpdate(row: JsAny): Boolean

@JsFun("function(row) { return ((row.like === true && row.like_int !== 1) || (row.like !== true && 'like_int' in row)); }")
external fun needsLikeIntUpdate(row: JsAny): Boolean

@JsFun("function(row) { if (row.like === true) { row.like_int = 1; } else { delete row.like_int; } return row; }")
external fun applyLikeIntPatch(row: JsAny): JsAny

class Dbb {
    var db: IDBPDatabase? by mutableStateOf(null)
        private set
    init {
        MainScope().launch {
            println("trying")
            db = getIdb()
            println("done")
        }
    }
}

val ddd = Dbb()

fun getDbMaybe(): IDBPDatabase? {
    return ddd.db
}

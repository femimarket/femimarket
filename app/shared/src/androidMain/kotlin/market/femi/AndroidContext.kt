package market.femi

import android.content.Context
import android.net.Uri
import java.lang.ref.WeakReference

object AndroidContext {
    private var _contextRef: WeakReference<Context>? = null
    private var _pickTreeHandler: (suspend () -> Uri?)? = null
    private var _pickImagesHandler: (suspend () -> List<MemoryFile>)? = null
    fun init(context: Context) {
        _contextRef = WeakReference(context.applicationContext)
    }
    val context: Context
        get() = _contextRef?.get() ?: error("AppContext is not initialized. Call AppContext.init(this) in your Application class.")


    // --- Tree Picker Backing Field & Non-Nullable Getter ---
    var pickTreeHandler: suspend () -> Uri?
        get() = _pickTreeHandler ?: error("No active picker interface registered from host view context. Ensure MainActivity is initialized.")
        set(value) { _pickTreeHandler = value }

    // --- Image Picker Backing Field & Non-Nullable Getter ---
    var pickImagesHandler: suspend () -> List<MemoryFile>
        get() = _pickImagesHandler ?: error("No active image picker interface registered from host view context. Ensure MainActivity is initialized.")
        set(value) { _pickImagesHandler = value }

}
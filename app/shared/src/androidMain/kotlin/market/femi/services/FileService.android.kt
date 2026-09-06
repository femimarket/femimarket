package market.femi.services

import market.femi.AndroidContext

actual fun createRealFsService(kv: KvService, importDir: String?): FileService =
    RealKotlinxIoFsService(importDir ?: "${AndroidContext.context.filesDir.absolutePath}/femi")

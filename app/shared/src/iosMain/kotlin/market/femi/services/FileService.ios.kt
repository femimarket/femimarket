package market.femi.services

import platform.Foundation.NSHomeDirectory

actual fun createRealFsService(kv: KvService, importDir: String?): FileService =
    RealKotlinxIoFsService(importDir ?: (NSHomeDirectory() + "/Documents"))

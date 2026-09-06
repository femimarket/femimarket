package market.femi.services

actual fun createRealFsService(kv: KvService, importDir: String?): FileService =
    RealKotlinxIoFsService(importDir ?: "${System.getProperty("user.home")}/femi-test")

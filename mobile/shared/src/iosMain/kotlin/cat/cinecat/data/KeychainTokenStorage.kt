package cat.cinecat.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Sessió desada al Keychain d'iOS: el magatzem de credencials del sistema,
 * xifrat pel mateix iOS i aïllat per app.
 *
 * AfterFirstUnlockThisDeviceOnly: accessible un cop desbloquejat el telèfon
 * després d'engegar-lo, i no es copia a altres dispositius ni a còpies de
 * seguretat. Una sessió és d'aquest dispositiu.
 *
 * L'API del Keychain és de C (Security framework). Des de Kotlin/Native s'hi
 * accedeix per "cinterop": les consultes són CFDictionary amb claus com
 * kSecClass (punters de C), per això es construeixen amb CFDictionaryAddValue.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainTokenStorage : TokenStorage {

    override fun load(): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery({ query ->
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        }) { SecItemCopyMatching(it, result.ptr) }
        if (status != errSecSuccess) return null
        // CFBridgingRelease: passa la propietat del resultat a Kotlin (ARC).
        val data = CFBridgingRelease(result.value) as? NSData ?: return null
        NSString.create(data, NSUTF8StringEncoding)?.toString()
    }

    override fun save(value: String?) {
        // Més simple que actualitzar: esborrem l'entrada i, si cal, la tornem a afegir.
        withQuery({}) { SecItemDelete(it) }
        if (value == null) return
        val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val cfData = CFBridgingRetain(data)
        try {
            withQuery({ query ->
                CFDictionaryAddValue(query, kSecValueData, cfData)
                CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
            }) { SecItemAdd(it, null) }
        } finally {
            CFRelease(cfData)
        }
    }

    /**
     * Construeix la consulta base (classe, servei i compte que identifiquen
     * la nostra entrada), hi afegeix el que calgui i l'allibera en acabar.
     */
    private fun <T> withQuery(extra: (CFMutableDictionaryRef?) -> Unit, block: (CFMutableDictionaryRef?) -> T): T {
        // Els callbacks kCFType fan que el diccionari retingui (retain) les
        // claus i els valors que hi afegim, i els alliberi amb ell.
        val query = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        val service: CFTypeRef? = CFBridgingRetain(SERVICE)
        val account: CFTypeRef? = CFBridgingRetain(ACCOUNT)
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, service)
            CFDictionaryAddValue(query, kSecAttrAccount, account)
            extra(query)
            return block(query)
        } finally {
            CFRelease(service)
            CFRelease(account)
            CFRelease(query)
        }
    }

    private companion object {
        const val SERVICE = "cat.cinecat.app"
        const val ACCOUNT = "session"
    }
}

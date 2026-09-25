package cat.cinecat.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Sessió desada a Android, xifrada amb una clau de l'Android Keystore.
 *
 * Com funciona:
 *  1. La primera vegada, es genera una clau AES dins l'Android Keystore. La
 *     clau NO es pot extreure: viu al maquinari segur del dispositiu i només
 *     el sistema la pot fer servir, i només per a aquesta app.
 *  2. El text (token + usuari) es xifra amb AES-GCM i el resultat (IV +
 *     text xifrat) es desa a SharedPreferences.
 * Qui llegeixi les SharedPreferences (una còpia de seguretat, un dispositiu
 * rootejat...) només hi veu bytes xifrats.
 *
 * (La llibreria EncryptedSharedPreferences, que feia això, està obsoleta; fer-ho
 * a mà amb el Keystore són poques línies i s'entén què passa.)
 */
class KeystoreTokenStorage(context: Context) : TokenStorage {
    private val prefs = context.getSharedPreferences("cinecat.session", Context.MODE_PRIVATE)

    override fun load(): String? {
        val stored = prefs.getString(KEY, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            cipher.doFinal(bytes.copyOfRange(IV_SIZE, bytes.size)).decodeToString()
        }.getOrNull() // clau canviada o dades corruptes: com si no hi hagués sessió
    }

    override fun save(value: String?) {
        if (value == null) {
            prefs.edit().remove(KEY).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key()) // el sistema genera un IV aleatori
        val encrypted = cipher.iv + cipher.doFinal(value.encodeToByteArray())
        prefs.edit().putString(KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "cinecat.session.key"
        const val KEY = "session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}

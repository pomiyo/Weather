package dev.local.weatherstudy.backend.encryptor

import javax.inject.Inject

/**
 * Educational reconstruction (stub — role-preserving, per the project's STEP 22).
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.backend.encryptor.BackendEncryptor
 *
 * Original dependency: the values in the bundled backend database are stored obscured,
 * and this class is what reverses that before they reach `SecureKeyProvider`.
 *
 * Why not reconstructed: reconstructing it would be reconstructing a key-recovery
 * routine, whose only purpose is to recover the credentials this project explicitly
 * does not extract. The project's boundaries say to **document the dependency rather
 * than defeat it**, so the class is kept, its role is stated, and the transform is
 * identity.
 *
 * Where used: between [dev.local.weatherstudy.backend.dao.StudyBackendDao] and the
 * typed providers.
 *
 * Replacement behaviour: pass-through. The registry is empty in the reconstruction, so
 * there is nothing to decode.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyBackendEncryptor @Inject constructor() {

    /** Identity. See the class note: the original's transform is deliberately not reconstructed. */
    fun decode(value: String): String = value

    /** Identity, for symmetry with the original's interface. */
    fun encode(value: String): String = value
}

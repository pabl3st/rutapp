package com.pabl3st.rutapp.util

import com.pabl3st.rutapp.data.session.SessionManager

/**
 * Implementación fake de SessionManager para tests unitarios.
 * No requiere Context ni Keystore — almacena en memoria.
 * Añadir nuevas propiedades aquí cuando se añadan a SessionManager.
 */
class FakeSessionManager : SessionManager(
    // SessionManager requiere Context — usamos un mock vacío.
    // La clase FakeSessionManager sobreescribe todos los accesos a prefs.
    context = io.mockk.mockk(relaxed = true)
) {
    // ── Estado en memoria ─────────────────────────────────────
    private var _token: String?          = TestFixtures.TOKEN
    private var _userId: Int             = TestFixtures.USER_ID
    private var _userName: String        = "god"
    private var _userEmail: String       = "god@rutasapp.dev"
    private var _userRole: String        = "owner"
    private var _userDisplayName: String = "God Admin"
    private var _accountId: Int          = TestFixtures.ACCOUNT_ID
    private var _accountType: String     = "individual"
    private var _accountName: String     = "God Admin"
    private var _lastSync: String        = ""
    private var _deviceId: String        = "test-device-001"

    // ── Overrides ─────────────────────────────────────────────
    override var token: String?
        get()      = _token
        set(value) { _token = value }

    override val isLoggedIn: Boolean
        get() = !_token.isNullOrEmpty()

    override var userId: Int
        get()      = _userId
        set(value) { _userId = value }

    override var userName: String
        get()      = _userName
        set(value) { _userName = value }

    override var userEmail: String
        get()      = _userEmail
        set(value) { _userEmail = value }

    override var userRole: String
        get()      = _userRole
        set(value) { _userRole = value }

    override var userDisplayName: String
        get()      = _userDisplayName
        set(value) { _userDisplayName = value }

    override var accountId: Int
        get()      = _accountId
        set(value) { _accountId = value }

    override var accountType: String
        get()      = _accountType
        set(value) { _accountType = value }

    override var accountName: String
        get()      = _accountName
        set(value) { _accountName = value }

    override val isCompany: Boolean
        get() = _accountType == "company"

    override var lastSyncTimestamp: String
        get()      = _lastSync
        set(value) { _lastSync = value }

    override val deviceId: String
        get() = _deviceId

    override fun saveAuth(
        token: String, userId: Int, userName: String, userEmail: String,
        userRole: String, userDisplayName: String, accountId: Int,
        accountType: String, accountName: String,
    ) {
        _token           = token
        _userId          = userId
        _userName        = userName
        _userEmail       = userEmail
        _userRole        = userRole
        _userDisplayName = userDisplayName
        _accountId       = accountId
        _accountType     = accountType
        _accountName     = accountName
    }

    override fun clear() {
        _token           = null
        _userId          = 0
        _userName        = ""
        _userEmail       = ""
        _userRole        = "agent"
        _userDisplayName = ""
        _accountId       = 0
        _accountType     = "individual"
        _accountName     = ""
        _lastSync        = ""
    }

    // ── Helpers para tests ────────────────────────────────────
    fun setNoAuth()  { _token = null }
    fun setLoggedIn() { _token = TestFixtures.TOKEN }
}

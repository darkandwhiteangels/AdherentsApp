package com.antechrist.adherentsapp.ui.screens.config

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.data.remote.ClubConfigRemote
import com.antechrist.adherentsapp.domain.model.admin.ClubConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class ClubConfigUi(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val savedSnack: String? = null,
    val clubName: String = "",
    val clubCity: String = "",
    val presidentName: String = "",
    val secretaryName: String = "",
    val logoUrl: String? = null,
    val signatureUrl: String? = null,              // président
    val secretarySignatureUrl: String? = null      // secrétaire
)

class ClubConfigViewModel : ViewModel() {

    private val _ui = MutableStateFlow(ClubConfigUi())
    val ui = _ui.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        try {
            val cfg = ClubConfigRemote.get()
            if (cfg == null) {
                _ui.value = ClubConfigUi(loading = false)
            } else {
                _ui.value = ClubConfigUi(
                    loading = false,
                    clubName = cfg.clubName,
                    clubCity = cfg.clubCity,
                    presidentName = cfg.presidentName,
                    secretaryName = cfg.secretaryName,
                    logoUrl = cfg.logoUrl,
                    signatureUrl = cfg.presidentSignatureUrl,
                    secretarySignatureUrl = cfg.secretarySignatureUrl

                )
            }
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Erreur de chargement")
        }
    }

    fun setClubName(v: String) { _ui.value = _ui.value.copy(clubName = v) }
    fun setClubCity(v: String) { _ui.value = _ui.value.copy(clubCity = v) }
    fun setPresident(v: String) { _ui.value = _ui.value.copy(presidentName = v) }
    fun setSecretary(v: String) { _ui.value = _ui.value.copy(secretaryName = v) }

    fun save() = viewModelScope.launch {
        _ui.value = _ui.value.copy(saving = true, error = null, savedSnack = null)
        try {
            val cfg = ClubConfig(
                clubName = _ui.value.clubName.trim(),
                clubCity = _ui.value.clubCity.trim(),
                presidentName = _ui.value.presidentName.trim(),
                secretaryName = _ui.value.secretaryName.trim(),
                logoUrl = _ui.value.logoUrl,
                presidentSignatureUrl = _ui.value.signatureUrl,
                secretarySignatureUrl = _ui.value.secretarySignatureUrl
            )
            ClubConfigRemote.upsert(cfg)
            _ui.value = _ui.value.copy(saving = false, savedSnack = "Configuration enregistrée")
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(saving = false, error = e.message ?: "Échec de l’enregistrement")
        }
    }

    fun clearSnack() { _ui.value = _ui.value.copy(savedSnack = null) }

    fun uploadLogo(uri: Uri) = viewModelScope.launch {
        try {
            val url = ClubConfigRemote.uploadLogo(uri)
            _ui.value = _ui.value.copy(logoUrl = url)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message ?: "Échec upload logo")
        }
    }

    fun uploadSignature(uri: Uri) = viewModelScope.launch {
        try {
            val url = ClubConfigRemote.uploadSignature(uri)
            _ui.value = _ui.value.copy(signatureUrl = url)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message ?: "Échec upload signature")
        }
    }

    fun uploadSecretarySignature(uri: Uri) = viewModelScope.launch {
        try {
            val url = ClubConfigRemote.uploadSecretarySignature(uri)
            _ui.value = _ui.value.copy(secretarySignatureUrl = url)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message ?: "Échec upload signature secrétaire")
        }
    }

    /**
     * Upload de la signature manuscrite capturée (Bitmap) en PNG (fond transparent),
     * via le data remote. Nécessite d’ajouter ClubConfigRemote.uploadSignatureBytes(...).
     */
    fun uploadSignatureBitmap(bmp: Bitmap) = viewModelScope.launch {
        try {
            val baos = ByteArrayOutputStream()
            // PNG = sans perte, fond transparent conservé
            bmp.compress(Bitmap.CompressFormat.PNG, /*ignored*/ 100, baos)
            val bytes = baos.toByteArray()

            val url = ClubConfigRemote.uploadSignatureBytes(
                bytes = bytes,
                contentType = "image/png"
            )
            _ui.value = _ui.value.copy(signatureUrl = url)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message ?: "Échec capture signature")
        }
    }

    fun uploadSecretarySignatureBitmap(bmp: Bitmap) = viewModelScope.launch {
        try {
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val bytes = baos.toByteArray()

            val url = ClubConfigRemote.uploadSecretarySignatureBytes(
                bytes = bytes,
                contentType = "image/png"
            )
            _ui.value = _ui.value.copy(secretarySignatureUrl = url)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message ?: "Échec capture signature secrétaire")
        }
    }
}

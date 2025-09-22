package com.example.teost.presentation

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.teost.data.repository.CreditsBackendRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DeepLinkReceiverActivity : ComponentActivity() {
    @Inject lateinit var backendRepo: CreditsBackendRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri: Uri? = intent?.data
        if (uri == null) {
            Toast.makeText(this, getString(com.example.teost.core.ui.R.string.invalid_link), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val requestId = uri.getQueryParameter("id")
        val action = uri.getQueryParameter("action")?.lowercase() ?: "approve"

        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, getString(com.example.teost.core.ui.R.string.missing_request_id), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            // In-app approval dinonaktifkan. Cek status dari backend jika tersedia, lalu beri tahu user.
            val status = runCatching { backendRepo.getStatus(requestId) }.getOrNull()
            val msg = when {
                status != null -> "Request status: ${status}"
                else -> getString(com.example.teost.core.ui.R.string.action_failed_or_unauthorized)
            }
            Toast.makeText(this@DeepLinkReceiverActivity, msg, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}



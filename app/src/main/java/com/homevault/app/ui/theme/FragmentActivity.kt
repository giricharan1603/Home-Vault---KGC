package com.homevault.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.FragmentActivity
import com.homevault.app.worker.WarrantyReminderWorker

class MainActivity : FragmentActivity() {
    private val viewModel: HomeVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        WarrantyReminderWorker.scheduleDailyCheck(this)

        if (savedInstanceState == null) {
            handleShareIntent(intent)
        }

        setContent {
            MaterialTheme {
                HomeVaultApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val sharedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            sharedUri?.let {
                viewModel.processSharedUri(it)
                intent.removeExtra(Intent.EXTRA_STREAM)
            }
        }
    }
}
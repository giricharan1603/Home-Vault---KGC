package com.homevault.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.homevault.app.data.HomeVaultDatabase
import java.util.concurrent.TimeUnit

class WarrantyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val dao = HomeVaultDatabase.getInstance(applicationContext).assetDao()
        val assets = dao.getAllAssetsOnce()
        val milestoneDays = setOf(60L, 30L, 7L, 1L)

        assets.forEach { asset ->
            val daysLeft = asset.daysRemaining()
            if (daysLeft in milestoneDays && asset.lastNotifiedDaysRemaining != daysLeft) {
                sendPrivacySafeNotification(
                    id = asset.id.hashCode(),
                    title = "Warranty Expiring in $daysLeft Days",
                    privateMessage = "${asset.productName} (${asset.modelNumber}) warranty expires in $daysLeft days."
                )
                dao.markNotificationSent(asset.id, daysLeft)
            }
        }
        return Result.success()
    }

    private fun sendPrivacySafeNotification(id: Int, title: String, privateMessage: String) {
        val channelId = "homevault_warranty_alerts"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Warranty & Compliance Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)

        // Redacted notification shown on locked screens to prevent PII exposure
        val publicNotification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("HomeVault Reminder")
            .setContentText("Unlock device to view upcoming appliance warranty expiry.")
            .build()

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(privateMessage)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }

    companion object {
        fun scheduleDailyCheck(context: Context) {
            val request = PeriodicWorkRequestBuilder<WarrantyReminderWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "HomeVaultDailyWarrantyCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
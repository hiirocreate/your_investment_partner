package com.investmentmonitor.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class InvestmentMonitorApp : Application() {

    lateinit var serviceLocator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = ServiceLocator.getInstance(this)
        createNotificationChannels()
    }

    /** Spec section 67: separate channels for critical vs normal news so users can tune each. */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val criticalChannel = NotificationChannel(
            CHANNEL_CRITICAL_NEWS,
            "重要ニュース",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "重要度の高い企業ニュース(決算・M&A・業績修正など)"
        }

        val normalChannel = NotificationChannel(
            CHANNEL_NORMAL_NEWS,
            "通常ニュース",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "その他の企業ニュース"
        }

        manager.createNotificationChannel(criticalChannel)
        manager.createNotificationChannel(normalChannel)
    }

    companion object {
        const val CHANNEL_CRITICAL_NEWS = "critical_news"
        const val CHANNEL_NORMAL_NEWS = "normal_news"
    }
}

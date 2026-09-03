package com.investmentmonitor.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local persistence for a user's watchlist. Kept deliberately small (spec section 45:
 * "必要以上にデータベースを複雑化しない") - richer company data is re-fetched from
 * CompanyProvider by id rather than duplicated here.
 */
@Entity(tableName = "watched_companies")
data class WatchedCompanyEntity(
    @PrimaryKey val companyId: String,
    val companyName: String,
    val addedAtEpochMillis: Long,
    val newsMonitoringEnabled: Boolean = true,
    val relatedCompanyMonitoringEnabled: Boolean = false,
    val priceAlertsEnabled: Boolean = false,
    val importantNewsOnly: Boolean = false,
    val lastNewsSeenAtEpochMillis: Long = 0L
)

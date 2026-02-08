package com.antechrist.adherentsapp.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Stream des compteurs de présences par saison.
 * Map<adherentId, presentCount>
 */
interface PresenceStatsRepository {
    fun streamCountsForSeason(seasonKey: String): Flow<Map<String, Int>>
}

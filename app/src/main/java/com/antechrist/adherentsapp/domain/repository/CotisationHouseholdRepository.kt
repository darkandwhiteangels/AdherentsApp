package com.antechrist.adherentsapp.domain.repository

import com.antechrist.adherentsapp.domain.model.finance.*
import kotlinx.coroutines.flow.Flow

interface CotisationsHouseholdsRepository {
    fun stream(seasonKey: String, guardianId: String): Flow<CotisationHousehold?>
    suspend fun get(seasonKey: String, guardianId: String): CotisationHousehold?
    suspend fun upsert(doc: CotisationHousehold)
    suspend fun setStatus(seasonKey: String, guardianId: String, status: HouseholdStatus)

    /** Miroir: mettre à jour "cotisations/{season}/members/{adherentId}" + bool adherent.cotisationPaid si soldé */
    suspend fun syncMirrorsAndAdherentsPaid(doc: CotisationHousehold, paid: Boolean)
}

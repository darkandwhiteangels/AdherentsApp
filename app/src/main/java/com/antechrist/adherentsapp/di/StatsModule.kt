package com.antechrist.adherentsapp.di

import com.antechrist.adherentsapp.data.firestore.PresenceStatsRepositoryImpl
import com.antechrist.adherentsapp.domain.repository.PresenceStatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StatsModule {

    @Binds
    @Singleton
    abstract fun bindPresenceStatsRepository(
        impl: PresenceStatsRepositoryImpl
    ): PresenceStatsRepository
}

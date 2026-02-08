package com.antechrist.adherentsapp.di

import com.antechrist.adherentsapp.data.seed.KihonSeed
import com.antechrist.adherentsapp.data.seed.LevelSeed
import com.antechrist.adherentsapp.data.seed.LevelSeedProvider
import com.antechrist.adherentsapp.data.seed.MovementSeed
import com.antechrist.adherentsapp.data.seed.MovementSeedProvider
import com.antechrist.adherentsapp.data.seed.OptionSeed
import com.antechrist.adherentsapp.data.seed.OptionSeedProvider
import com.antechrist.adherentsapp.domain.usecase.SeedKihonCatalogIfEmpty // ⬅️ NOTE: même package que dans ton stacktrace
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class KihonBindsModule {

    @Binds
    @Singleton
    abstract fun bindKihonSeedProvider(impl: KihonSeed): SeedKihonCatalogIfEmpty.KihonSeedProvider

    @Binds @Singleton
    abstract fun bindMovementSeedProvider(impl: MovementSeed): MovementSeedProvider

    @Binds @Singleton
    abstract fun bindOptionSeedProvider(impl: OptionSeed): OptionSeedProvider

    @Binds @Singleton
    abstract fun bindLevelSeedProvider(impl: LevelSeed): LevelSeedProvider

}

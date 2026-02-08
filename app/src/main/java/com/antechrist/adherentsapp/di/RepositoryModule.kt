package com.antechrist.adherentsapp.di

import com.antechrist.adherentsapp.data.auth.AuthDataSource
import com.antechrist.adherentsapp.data.auth.AuthRemoteImpl
import com.antechrist.adherentsapp.data.auth.AuthRepositoryImpl
import com.antechrist.adherentsapp.data.firestore.AdherentsRepositoryImpl
import com.antechrist.adherentsapp.data.firestore.CotisationsHouseholdsRepositoryImpl
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import com.antechrist.adherentsapp.domain.repository.AuthRepository
import com.antechrist.adherentsapp.domain.repository.CotisationsHouseholdsRepository
import com.antechrist.adherentsapp.data.firestore.NotificationGroupsRepositoryImpl
import com.antechrist.adherentsapp.domain.repository.NotificationGroupsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthDataSource(impl: AuthRemoteImpl): AuthDataSource

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindAdherentsRepository(impl: AdherentsRepositoryImpl): AdherentsRepository

    @Binds @Singleton
    abstract fun bindCotisationsHouseholdsRepository(
        impl: CotisationsHouseholdsRepositoryImpl
    ): CotisationsHouseholdsRepository

    @Binds
    @Singleton
    abstract fun bindNotificationGroupsRepository(
        impl: NotificationGroupsRepositoryImpl
    ): NotificationGroupsRepository

}
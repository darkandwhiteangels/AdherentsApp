package com.antechrist.adherentsapp.di

import com.antechrist.adherentsapp.data.firestore.InfoMessagesRepositoryImpl
import com.antechrist.adherentsapp.domain.repository.InfoMessagesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InfoMessagesModule {

    @Binds
    @Singleton
    abstract fun bindInfoMessagesRepository(
        impl: InfoMessagesRepositoryImpl
    ): InfoMessagesRepository
}

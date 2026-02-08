package com.antechrist.adherentsapp.di

import com.antechrist.adherentsapp.core.CoroutineDispatchers
import com.antechrist.adherentsapp.ui.screens.presence.IoDispatcher
import com.antechrist.adherentsapp.ui.screens.presence.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers = CoroutineDispatchers()
}
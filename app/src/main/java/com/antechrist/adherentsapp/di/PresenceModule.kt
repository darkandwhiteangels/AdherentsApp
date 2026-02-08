package com.antechrist.adherentsapp.di

import com.antechrist.adherentsapp.data.firestore.PresenceDataSource
import com.antechrist.adherentsapp.data.firestore.PresenceRepositoryImpl
import com.antechrist.adherentsapp.domain.repository.PresenceRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PresenceBindModule {
    @Binds
    @Singleton
    abstract fun bindPresenceRepository(impl: PresenceRepositoryImpl): PresenceRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PresenceProvideModule {
    // ✅ On réutilise le Firestore fourni par FirebaseModule
    @Provides
    @Singleton
    fun providePresenceDataSource(db: FirebaseFirestore): PresenceDataSource =
        PresenceDataSource(db)
}

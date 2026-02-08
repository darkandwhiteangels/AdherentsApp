package com.antechrist.adherentsapp.di

import com.antechrist.adherentsapp.data.firestore.GuardiansRepositoryImpl
import com.antechrist.adherentsapp.domain.repository.GuardiansRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GuardiansModule {
    @Binds
    @Singleton
    abstract fun bindGuardiansRepository(impl: GuardiansRepositoryImpl): GuardiansRepository
}

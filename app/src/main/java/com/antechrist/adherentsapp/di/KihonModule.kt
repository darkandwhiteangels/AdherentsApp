// com/antechrist/adherentsapp/di/KihonModule.kt
package com.antechrist.adherentsapp.di

import com.antechrist.adherentsapp.data.firestore.KihonDataSource
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import com.antechrist.adherentsapp.data.firestore.KihonRepositoryImpl
import com.antechrist.adherentsapp.domain.usecase.DeleteKihonSequence
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KihonModule {

    @Provides
    @Singleton
    fun provideKihonDataSource(
        db: FirebaseFirestore
    ): KihonDataSource = KihonDataSource(db)

    @Provides
    @Singleton
    fun provideKihonRepository(
        ds: KihonDataSource
    ): KihonRepository = KihonRepositoryImpl(ds)

    @Provides
    fun provideDeleteKihonSequence(repo: KihonRepository) = DeleteKihonSequence(repo)

}

package com.maca.tsp.di

import com.maca.tsp.common.appstate.GlobalState
import com.maca.tsp.common.appstate.GlobalStateImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GlobalStateModule {

    @Singleton
    @Provides
    fun provideGlobalState(): GlobalState = GlobalStateImpl()

}
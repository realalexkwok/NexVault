package com.nexvault.wallet.core.datastore.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    // All DataStore classes use @Singleton + @Inject constructor,
    // so they should be auto-provided.
    //
    // If you need to add @Provides methods here for DataStore instances
    // or third-party dependencies, add them here.
}

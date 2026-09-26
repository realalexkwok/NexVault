package com.nexvault.wallet.core.security.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    // All security managers are @Singleton and use constructor injection,
    // so Hilt will automatically provide them.
    //
    // If you need to add interface bindings for testing or alternative implementations,
    // add @Binds methods here.
    //
    // Example:
    // @Binds
    // @IntoSet
    // abstract fun bindSomeInterface(impl: Implementation): Interface
}

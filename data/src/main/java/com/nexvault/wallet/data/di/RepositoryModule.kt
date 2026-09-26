package com.nexvault.wallet.data.di

import com.nexvault.wallet.data.repository.AuthRepositoryImpl
import com.nexvault.wallet.data.repository.ChainRepositoryImpl
import com.nexvault.wallet.data.repository.TokenRepositoryImpl
import com.nexvault.wallet.data.repository.TransactionRepositoryImpl
import com.nexvault.wallet.data.repository.WalletRepositoryImpl
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.repository.ChainRepository
import com.nexvault.wallet.domain.repository.TokenRepository
import com.nexvault.wallet.domain.repository.TransactionRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        impl: WalletRepositoryImpl,
    ): WalletRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChainRepository(
        impl: ChainRepositoryImpl,
    ): ChainRepository

    @Binds
    @Singleton
    abstract fun bindTokenRepository(
        impl: TokenRepositoryImpl,
    ): TokenRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl,
    ): TransactionRepository
}

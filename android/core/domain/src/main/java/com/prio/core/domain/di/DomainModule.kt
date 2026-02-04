package com.prio.core.domain.di

import com.prio.core.domain.parser.NaturalLanguageParser
import com.prio.core.domain.parser.RuleBasedNaturalLanguageParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for domain layer dependency injection.
 * 
 * Provides bindings for:
 * - NaturalLanguageParser (bound to RuleBasedNaturalLanguageParser)
 * - EisenhowerEngine (auto-provided via @Inject constructor)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    
    @Binds
    @Singleton
    abstract fun bindNaturalLanguageParser(
        impl: RuleBasedNaturalLanguageParser
    ): NaturalLanguageParser
}

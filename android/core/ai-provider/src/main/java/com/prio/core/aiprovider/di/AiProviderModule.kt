package com.prio.core.aiprovider.di

import com.prio.core.ai.provider.AiProvider
import com.prio.core.aiprovider.provider.OnDeviceAiProvider
import com.prio.core.aiprovider.provider.RuleBasedFallbackProvider
import com.prio.core.aiprovider.router.AiProviderRouter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for the main AI provider (router).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainAiProvider

/**
 * Qualifier for the rule-based provider.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RuleBasedProvider

/**
 * Qualifier for the on-device LLM provider.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OnDeviceProvider

/**
 * Hilt module providing AI provider dependencies.
 * 
 * Task 2.2.5-2.2.8: AI Provider implementations and routing.
 * 
 * Provides:
 * - RuleBasedFallbackProvider: Primary classifier (75% accuracy, <50ms)
 * - OnDeviceAiProvider: LLM-based classifier (2-3s, higher accuracy for edge cases)
 * - AiProviderRouter: Smart router that chains providers
 */
@Module
@InstallIn(SingletonComponent::class)
object AiProviderModule {
    
    /**
     * Provide the main AI provider (router).
     * This is the primary entry point for AI requests.
     */
    @Provides
    @Singleton
    @MainAiProvider
    fun provideMainAiProvider(
        router: AiProviderRouter
    ): AiProvider = router
    
    /**
     * Provide the rule-based provider.
     */
    @Provides
    @Singleton
    @RuleBasedProvider
    fun provideRuleBasedProvider(
        provider: RuleBasedFallbackProvider
    ): AiProvider = provider
    
    /**
     * Provide the on-device LLM provider.
     */
    @Provides
    @Singleton
    @OnDeviceProvider
    fun provideOnDeviceProvider(
        provider: OnDeviceAiProvider
    ): AiProvider = provider
    
    /**
     * Provide all available providers as a set for enumeration.
     */
    @Provides
    @Singleton
    fun provideAllProviders(
        ruleBasedProvider: RuleBasedFallbackProvider,
        onDeviceProvider: OnDeviceAiProvider
    ): Set<AiProvider> = setOf(ruleBasedProvider, onDeviceProvider)
}

/**
 * Bindings module for interface bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiProviderBindingsModule {
    
    /**
     * Bind RuleBasedFallbackProvider to AiProvider for multi-binding.
     */
    @Binds
    @IntoSet
    abstract fun bindRuleBasedProvider(
        provider: RuleBasedFallbackProvider
    ): AiProvider
    
    /**
     * Bind OnDeviceAiProvider to AiProvider for multi-binding.
     */
    @Binds
    @IntoSet
    abstract fun bindOnDeviceProvider(
        provider: OnDeviceAiProvider
    ): AiProvider
}

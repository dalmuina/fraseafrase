package com.dalmuina.fraseafrase.core.data.di

import javax.inject.Qualifier

/**
 * Calificador del dispatcher de E/S que usan los data sources (CLAUDE.md: los dispatchers se inyectan). Su proveedor
 * vive en `:app`, que es el único módulo que conoce las implementaciones (ADR-0010).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

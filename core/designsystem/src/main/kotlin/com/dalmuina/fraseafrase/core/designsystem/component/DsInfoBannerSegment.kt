package com.dalmuina.fraseafrase.core.designsystem.component

/**
 * Tramo de texto de [DsInfoBanner]: texto normal o en negrita ([emphasized]). Es un parámetro de
 * texto, no `AnnotatedString` libre (design D2), para que el llamador no construya estilos de
 * texto por su cuenta ni el componente necesite tipos del dominio (ADR-0010).
 */
data class DsInfoBannerSegment(val text: String, val emphasized: Boolean = false)

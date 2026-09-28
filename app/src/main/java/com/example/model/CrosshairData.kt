package com.example.model

enum class CrosshairStyle(val displayName: String) {
    CLASSIC_CROSS("Classic Cross"),
    DOT("Center Dot"),
    CIRCLE_DOT("Circle & Dot"),
    T_SHAPE("Tactical T"),
    GAP_CROSS("Gap Cross"),
    BOX_CROSS("Box Cross"),
    CUSTOM_IMAGE("Custom Image")
}

data class CrosshairConfig(
    val isEnabled: Boolean = false,
    val style: CrosshairStyle = CrosshairStyle.GAP_CROSS,
    val colorHex: Long = 0xFF00F0FF, // Electric Cyan default
    val sizeDp: Float = 26f,
    val thicknessDp: Float = 2.5f,
    val gapDp: Float = 6f,
    val opacity: Float = 0.95f,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val customImageUri: String? = null,
    val isLocked: Boolean = false
)

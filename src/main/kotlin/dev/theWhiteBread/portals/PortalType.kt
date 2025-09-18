package dev.theWhiteBread.portals

import kotlinx.serialization.Serializable

@Serializable
enum class PortalType {
    STABLE,
    UNSTABLE

    ;

    fun isStable(): Boolean = this == STABLE

    fun isUnstable(): Boolean = this == UNSTABLE
}
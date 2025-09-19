package dev.theWhiteBread.portals.portal

import dev.theWhiteBread.portals.PortalManager

fun <T : Portal> T.temporaryPortal(): T {
    this.persistence = false
    return this
}

fun <T: Portal> T.registerPortal(render: Boolean = false): T {
    PortalManager.registerPortal(this, render)
    return this
}
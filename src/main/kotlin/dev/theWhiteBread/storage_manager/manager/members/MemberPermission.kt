package dev.theWhiteBread.storage_manager.manager.members

enum class MemberPermission(
    val weight: Int,
    val description: String
) {
    Viewer(0, "<gray><gold>Players</gold> can see your <gold>controller</gold> but <bold>cannot</bold> access anything."),
    User(1, "<gray>Players can utilize the <gold>controller</gold> as if they own it. Players <bold>cannot</bold> configure your controller."),
    Manager(2, "<gray>Player can utilize and manager the <gold>controller</gold> as if they own it.")

    ;

    fun isAtLeast(permissionWeight: MemberPermission): Boolean {
        return this.weight >= permissionWeight.weight
    }
}
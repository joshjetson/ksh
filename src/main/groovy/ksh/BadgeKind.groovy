package ksh

/**
 * Discriminator for a Badge row — the "switch" that decides what kind of
 * collectible it is. Add a value here to introduce a new collectible type
 * (e.g. FRAME, TITLE) without any schema change; GORM stores the name string.
 */
enum BadgeKind {

    BADGE,    // a vanilla achievement pin
    AVATAR    // an unlockable, equippable profile picture

    // Future: FRAME, TITLE, DECORATION, …

    String getLabel() {
        name().toLowerCase().capitalize()
    }
}

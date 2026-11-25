package com.example.uniride.presentation.navigation

import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile

/**
 * Modes the user can switch between with the selector. Driving needs a vehicle, so a
 * `CONDUCTOR` or `AMBOS` profile without one can only be a passenger.
 *
 * Without a profile (until RF01 loads it after login) every mode is available, so the
 * placeholders can be walked.
 */
fun availableModes(profile: Profile?): Set<AppMode> {
    if (profile == null) return AppMode.entries.toSet()
    val canDrive = profile.vehicle != null
    return when (profile.preferredMode) {
        PreferredMode.PASAJERO -> setOf(AppMode.PASAJERO)
        PreferredMode.CONDUCTOR -> if (canDrive) setOf(AppMode.CONDUCTOR) else setOf(AppMode.PASAJERO)
        PreferredMode.AMBOS -> if (canDrive) AppMode.entries.toSet() else setOf(AppMode.PASAJERO)
    }
}

/** [selected] if it is allowed, otherwise the first allowed mode. */
fun effectiveMode(selected: AppMode, available: Set<AppMode>): AppMode =
    if (selected in available) selected else AppMode.entries.first { it in available }

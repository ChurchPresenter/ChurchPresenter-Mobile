package com.church.presenter.churchpresentermobile.ui

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.MoreDestination

/** The More tools that have just arrived, and carry a "New" badge until it has been seen. */
internal val NewFeatures: Set<MoreDestination> = setOf(MoreDestination.CALENDAR, MoreDestination.CANVAS)

/**
 * Which "New" badges to show, and the record of which have been seen.
 *
 * A badge shows for one session: the session it is first seen in. What was seen
 * before this launch is read once, so a badge seen now stays up until the app is
 * next started, rather than vanishing the moment it is drawn.
 *
 * @param seenAtLaunch The badges seen in earlier sessions. Defaults to the
 *   process-wide snapshot, so a screen rebuilt mid-session — an Android rotation —
 *   still shows what this session showed.
 */
class NewBadges(
    private val settings: AppSettings,
    seenAtLaunch: Set<String> = NewBadgeSession.seenAtLaunch(settings),
) {
    /** The badges this session shows. */
    val showing: Set<MoreDestination> = newBadgesToShow(NewFeatures, seenAtLaunch)

    /** Records [destinations] as seen, so the next session leaves them off. */
    fun shown(destinations: Collection<MoreDestination>) {
        val seen = settings.seenNewBadges
        val names = destinations.map { it.name }
        if (!seen.containsAll(names)) settings.seenNewBadges = seen + names
    }
}

/** [features] not yet seen in an earlier session. */
internal fun newBadgesToShow(features: Set<MoreDestination>, seenBefore: Set<String>): Set<MoreDestination> =
    features.filterTo(LinkedHashSet()) { it.name !in seenBefore }

/** The badges seen before this launch, read once per process. */
object NewBadgeSession {
    private var snapshot: Set<String>? = null

    internal fun seenAtLaunch(settings: AppSettings): Set<String> =
        snapshot ?: settings.seenNewBadges.also { snapshot = it }
}

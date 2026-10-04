package dev.local.weatherstudy.widget.notification.view

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the seven builders in
 * com.sec.android.daemonapp.notification.view and the nine classes in
 * `notification/channel`:
 * `NormalNotificationView`, `PanelNotificationView`, `AlertNotificationView`,
 * `NarrativeNotificationView`, `AppUpdateNotificationView`,
 * `RestoreNotificationView`, `EmptyNotificationView`
 *
 * Observed responsibility: one builder per notification shape, over 16
 * `notification_*.xml` custom layouts in the original. The split matters because the
 * shapes are genuinely different — the panel one is an ongoing forecast with a custom
 * RemoteViews body, the alert one carries a severity colour, the narrative one is text
 * only — and each maps to its own channel so the user can silence them independently.
 *
 * The channel ids come from `StudyNotificationType`, which is in the **domain** layer:
 * the notification setting is persisted, so the id has to be shared with the data layer.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyNotificationView {
    val channelId: String
    fun build(context: Context): Notification
}

/** `…notification.view.NormalNotificationView`. */
class StudyNormalNotificationView @Inject constructor() : StudyNotificationView {
    override val channelId = dev.local.weatherstudy.domain.type.StudyNotificationType.NORMAL
    override fun build(context: Context): Notification =
        baseBuilder(context, channelId).setContentTitle("Weather").build()
}

/**
 * `…notification.view.PanelNotificationView`.
 *
 * The ongoing forecast notification — the one with a custom RemoteViews body and 16
 * supporting layouts in the original.
 */
class StudyPanelNotificationView @Inject constructor() : StudyNotificationView {
    override val channelId = dev.local.weatherstudy.domain.type.StudyNotificationType.PANEL
    override fun build(context: Context): Notification =
        baseBuilder(context, channelId).setOngoing(true).build()
}

/** `…notification.view.AlertNotificationView` — severe weather. */
class StudyAlertNotificationView @Inject constructor() : StudyNotificationView {
    override val channelId = dev.local.weatherstudy.domain.type.StudyNotificationType.FORECAST_CHANGE
    override fun build(context: Context): Notification =
        baseBuilder(context, channelId)
            .setCategory(Notification.CATEGORY_EVENT)
            .build()
}

/** `…notification.view.NarrativeNotificationView` — a text-only daily summary. */
class StudyNarrativeNotificationView @Inject constructor() : StudyNotificationView {
    override val channelId = dev.local.weatherstudy.domain.type.StudyNotificationType.NORMAL
    override fun build(context: Context): Notification = baseBuilder(context, channelId).build()
}

/** `…notification.view.AppUpdateNotificationView`. */
class StudyAppUpdateNotificationView @Inject constructor() : StudyNotificationView {
    override val channelId = dev.local.weatherstudy.domain.type.StudyNotificationType.APP_UPDATE
    override fun build(context: Context): Notification = baseBuilder(context, channelId).build()
}

/** `…notification.view.RestoreNotificationView` — shown after a backup restore. */
class StudyRestoreNotificationView @Inject constructor() : StudyNotificationView {
    override val channelId = dev.local.weatherstudy.domain.type.StudyNotificationType.NORMAL
    override fun build(context: Context): Notification = baseBuilder(context, channelId).build()
}

/**
 * `…notification.view.EmptyNotificationView`.
 *
 * A deliberate no-content notification. It exists because a foreground service and an
 * expedited worker both must post *something*, and this is the least intrusive option
 * when there is nothing to say.
 */
class StudyEmptyNotificationView @Inject constructor() : StudyNotificationView {
    override val channelId = dev.local.weatherstudy.domain.type.StudyNotificationType.REFRESH
    override fun build(context: Context): Notification =
        baseBuilder(context, channelId).setOngoing(true).build()
}

/** `notification/channel` — one channel per notification type, so each can be silenced. */
private fun baseBuilder(context: Context, channelId: String): Notification.Builder {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(
        NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_LOW),
    )
    return Notification.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
}

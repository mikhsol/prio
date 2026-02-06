package com.prio.app.feature.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.prio.core.data.repository.MeetingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads events from Android Calendar Provider (read-only).
 *
 * Per CB-002:
 * - READ_CALENDAR permission, sync to Room, multi-calendar support
 * - Color by source calendar
 * - All calendar data stays on device
 * - No calendar write operations in MVP
 *
 * Privacy-first: only reads, never writes. Syncs to local Room for offline access.
 */
@Singleton
class CalendarProviderHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meetingRepository: MeetingRepository
) {

    companion object {
        private const val TAG = "CalendarProvider"

        /** Projection for calendar list query. */
        private val CALENDAR_PROJECTION = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.VISIBLE
        )

        /** Projection for event query. */
        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.CALENDAR_COLOR,
            CalendarContract.Events.DISPLAY_COLOR
        )

        /** Projection for attendees query. */
        private val ATTENDEE_PROJECTION = arrayOf(
            CalendarContract.Attendees.ATTENDEE_NAME,
            CalendarContract.Attendees.ATTENDEE_EMAIL
        )
    }

    /**
     * Check whether READ_CALENDAR permission is granted.
     */
    fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Fetch available device calendars.
     */
    suspend fun getCalendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext emptyList()

        val calendars = mutableListOf<DeviceCalendar>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, CALENDAR_PROJECTION, selection, null, null)
            cursor?.let {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val name = it.getString(1) ?: "Unknown"
                    val color = it.getInt(2)
                    val accountName = it.getString(3) ?: ""
                    val accountType = it.getString(4) ?: ""

                    calendars.add(
                        DeviceCalendar(
                            id = id,
                            displayName = name,
                            color = color,
                            accountName = accountName,
                            accountType = accountType
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Calendar permission denied")
        } catch (e: Exception) {
            Timber.e(e, "Error reading calendars")
        } finally {
            cursor?.close()
        }

        calendars
    }

    /**
     * Sync calendar events for a date range into local Room database.
     *
     * Uses [MeetingRepository.upsertFromCalendar] to preserve user notes/action items.
     *
     * @return number of events synced
     */
    suspend fun syncEventsForRange(
        startMillis: Long,
        endMillis: Long
    ): Int = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext 0

        val contentResolver: ContentResolver = context.contentResolver
        val uri: Uri = CalendarContract.Events.CONTENT_URI
        val selection =
            "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        var synced = 0
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, EVENT_PROJECTION, selection, selectionArgs, sortOrder)
            cursor?.let {
                while (it.moveToNext()) {
                    val eventId = it.getLong(0)
                    val title = it.getString(1) ?: "(No title)"
                    val description = it.getString(2)
                    val dtStart = it.getLong(3)
                    val dtEnd = it.getLong(4).takeIf { end -> end > 0 } ?: (dtStart + 3_600_000)
                    val location = it.getString(5)
                    val calendarColor = it.getInt(8).takeIf { c -> c != 0 } ?: it.getInt(7)

                    val attendees = getAttendeesForEvent(contentResolver, eventId)

                    meetingRepository.upsertFromCalendar(
                        calendarEventId = eventId.toString(),
                        title = title,
                        startTime = Instant.fromEpochMilliseconds(dtStart),
                        endTime = Instant.fromEpochMilliseconds(dtEnd),
                        description = description,
                        location = location,
                        attendees = attendees
                    )
                    synced++
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Calendar permission denied during sync")
        } catch (e: Exception) {
            Timber.e(e, "Error syncing calendar events")
        } finally {
            cursor?.close()
        }

        Timber.d("Synced $synced calendar events")
        synced
    }

    /**
     * Fetch attendees for a specific event.
     */
    private fun getAttendeesForEvent(
        contentResolver: ContentResolver,
        eventId: Long
    ): List<String> {
        val attendees = mutableListOf<String>()
        val uri = CalendarContract.Attendees.CONTENT_URI
        val selection = "${CalendarContract.Attendees.EVENT_ID} = ?"
        val selectionArgs = arrayOf(eventId.toString())

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, ATTENDEE_PROJECTION, selection, selectionArgs, null)
            cursor?.let {
                while (it.moveToNext()) {
                    val name = it.getString(0) ?: it.getString(1) ?: continue
                    attendees.add(name)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error reading attendees for event $eventId")
        } finally {
            cursor?.close()
        }

        return attendees
    }
}

/**
 * Represents a device calendar from Android Calendar Provider.
 */
data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val color: Int,
    val accountName: String,
    val accountType: String
)

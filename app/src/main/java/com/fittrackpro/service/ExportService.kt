package com.fittrackpro.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.TrackPointDao
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.data.local.database.entity.TrackPoint
import com.fittrackpro.data.local.database.entity.TrackStatistics
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao
) {
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    enum class ExportFormat {
        GPX, TCX
    }

    data class ExportResult(
        val success: Boolean,
        val file: File? = null,
        val uri: Uri? = null,
        val error: String? = null
    )

    suspend fun exportTrack(trackId: String, format: ExportFormat): ExportResult {
        return try {
            val track = trackDao.getTrackById(trackId) ?: return ExportResult(
                success = false,
                error = "Track not found"
            )

            val trackPoints = trackPointDao.getTrackPointsByTrackId(trackId)
            val statistics = trackDao.getStatisticsByTrackId(trackId)

            if (trackPoints.isEmpty()) {
                return ExportResult(
                    success = false,
                    error = "No GPS data available for this activity"
                )
            }

            val fileName = generateFileName(track, format)
            val exportDir = File(context.cacheDir, "exports")
            exportDir.mkdirs()

            val exportFile = File(exportDir, fileName)

            val content = when (format) {
                ExportFormat.GPX -> generateGpx(track, trackPoints, statistics)
                ExportFormat.TCX -> generateTcx(track, trackPoints, statistics)
            }

            FileWriter(exportFile).use { writer ->
                writer.write(content)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            ExportResult(success = true, file = exportFile, uri = uri)
        } catch (e: Exception) {
            ExportResult(success = false, error = e.message ?: "Export failed")
        }
    }

    fun createShareIntent(result: ExportResult, format: ExportFormat): Intent? {
        if (!result.success || result.uri == null) return null

        val mimeType = when (format) {
            ExportFormat.GPX -> "application/gpx+xml"
            ExportFormat.TCX -> "application/vnd.garmin.tcx+xml"
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun generateFileName(track: Track, format: ExportFormat): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(track.startTime))
        val activityType = track.activityType.replaceFirstChar { it.uppercase() }
        val extension = format.name.lowercase()
        return "FitTrack_${activityType}_$dateStr.$extension"
    }

    private fun generateGpx(
        track: Track,
        trackPoints: List<TrackPoint>,
        statistics: TrackStatistics?
    ): String {
        val sb = StringBuilder()

        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="FitTrack Pro"""")
        sb.appendLine("""  xmlns="http://www.topografix.com/GPX/1/1"""")
        sb.appendLine("""  xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">""")

        // Metadata
        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>${escapeXml(track.name ?: "Activity")}</name>")
        sb.appendLine("    <time>${isoDateFormat.format(Date(track.startTime))}</time>")
        if (track.description != null) {
            sb.appendLine("    <desc>${escapeXml(track.description)}</desc>")
        }
        sb.appendLine("  </metadata>")

        // Track
        sb.appendLine("  <trk>")
        sb.appendLine("    <name>${escapeXml(track.name ?: track.activityType.replaceFirstChar { it.uppercase() })}</name>")
        sb.appendLine("    <type>${track.activityType}</type>")

        // Track segment
        sb.appendLine("    <trkseg>")
        for (point in trackPoints) {
            sb.appendLine("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
            point.altitude?.let {
                sb.appendLine("        <ele>$it</ele>")
            }
            sb.appendLine("        <time>${isoDateFormat.format(Date(point.timestamp))}</time>")

            // Extensions for heart rate
            if (point.heartRate != null || point.speed != null) {
                sb.appendLine("        <extensions>")
                sb.appendLine("          <gpxtpx:TrackPointExtension>")
                point.heartRate?.let {
                    sb.appendLine("            <gpxtpx:hr>$it</gpxtpx:hr>")
                }
                point.speed?.let {
                    sb.appendLine("            <gpxtpx:speed>$it</gpxtpx:speed>")
                }
                sb.appendLine("          </gpxtpx:TrackPointExtension>")
                sb.appendLine("        </extensions>")
            }

            sb.appendLine("      </trkpt>")
        }
        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")

        return sb.toString()
    }

    private fun generateTcx(
        track: Track,
        trackPoints: List<TrackPoint>,
        statistics: TrackStatistics?
    ): String {
        val sb = StringBuilder()

        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<TrainingCenterDatabase""")
        sb.appendLine("""  xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"""")
        sb.appendLine("""  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">""")

        sb.appendLine("  <Activities>")

        // Map activity type to TCX sport
        val sport = when (track.activityType.lowercase()) {
            "running" -> "Running"
            "cycling" -> "Biking"
            else -> "Other"
        }

        sb.appendLine("    <Activity Sport=\"$sport\">")
        sb.appendLine("      <Id>${isoDateFormat.format(Date(track.startTime))}</Id>")

        // Lap (entire activity as one lap)
        sb.appendLine("      <Lap StartTime=\"${isoDateFormat.format(Date(track.startTime))}\">")
        statistics?.let { stats ->
            sb.appendLine("        <TotalTimeSeconds>${stats.duration / 1000.0}</TotalTimeSeconds>")
            sb.appendLine("        <DistanceMeters>${stats.distance}</DistanceMeters>")
            sb.appendLine("        <Calories>${stats.calories}</Calories>")
            stats.avgHeartRate?.let {
                sb.appendLine("        <AverageHeartRateBpm><Value>$it</Value></AverageHeartRateBpm>")
            }
            stats.maxHeartRate?.let {
                sb.appendLine("        <MaximumHeartRateBpm><Value>$it</Value></MaximumHeartRateBpm>")
            }
        }
        sb.appendLine("        <Intensity>Active</Intensity>")
        sb.appendLine("        <TriggerMethod>Manual</TriggerMethod>")

        // Track
        sb.appendLine("        <Track>")
        for (point in trackPoints) {
            sb.appendLine("          <Trackpoint>")
            sb.appendLine("            <Time>${isoDateFormat.format(Date(point.timestamp))}</Time>")
            sb.appendLine("            <Position>")
            sb.appendLine("              <LatitudeDegrees>${point.latitude}</LatitudeDegrees>")
            sb.appendLine("              <LongitudeDegrees>${point.longitude}</LongitudeDegrees>")
            sb.appendLine("            </Position>")
            point.altitude?.let {
                sb.appendLine("            <AltitudeMeters>$it</AltitudeMeters>")
            }
            point.heartRate?.let {
                sb.appendLine("            <HeartRateBpm><Value>$it</Value></HeartRateBpm>")
            }
            sb.appendLine("          </Trackpoint>")
        }
        sb.appendLine("        </Track>")
        sb.appendLine("      </Lap>")

        sb.appendLine("      <Creator xsi:type=\"Device_t\">")
        sb.appendLine("        <Name>FitTrack Pro</Name>")
        sb.appendLine("      </Creator>")

        sb.appendLine("    </Activity>")
        sb.appendLine("  </Activities>")

        sb.appendLine("  <Author xsi:type=\"Application_t\">")
        sb.appendLine("    <Name>FitTrack Pro</Name>")
        sb.appendLine("  </Author>")

        sb.appendLine("</TrainingCenterDatabase>")

        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

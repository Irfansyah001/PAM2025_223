package com.umy.medremindid.ui.report

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.umy.medremindid.data.local.dao.AdherenceLogDao
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReportExporter {

    fun exportCsv(context: Context, rows: List<AdherenceLogDao.AdherenceLogRow>): Uri {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val zone = ZoneId.systemDefault()

        val file = File(context.cacheDir, "medremind_report_${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { w ->
            w.appendLine("planned_time,status,taken_time,medicine_name,dosage,note")
            rows.forEach { r ->
                val planned = r.plannedTime.atZone(zone).toLocalDateTime().format(fmt)
                val taken = r.takenTime?.atZone(zone)?.toLocalDateTime()?.format(fmt).orEmpty()
                val mn = (r.medicineName ?: "").replace(",", " ")
                val ds = (r.dosage ?: "").replace(",", " ")
                val note = (r.note ?: "").replace("\n", " ").replace(",", " ")
                w.appendLine("$planned,${r.status},$taken,$mn,$ds,$note")
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}

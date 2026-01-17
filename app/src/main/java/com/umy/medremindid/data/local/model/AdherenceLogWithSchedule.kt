package com.umy.medremindid.data.local.model

import androidx.room.Embedded
import com.umy.medremindid.data.local.entity.AdherenceLogEntity

data class AdherenceLogWithSchedule(
    @Embedded val log: AdherenceLogEntity,
    val medicineName: String,
    val dosage: String
)

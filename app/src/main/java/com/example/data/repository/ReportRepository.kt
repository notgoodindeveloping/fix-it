package com.example.data.repository

import com.example.data.local.ReportDao
import com.example.data.model.Report
import com.example.data.network.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ReportRepository(private val reportDao: ReportDao) {

    // Expose all reports reactively from local Room database
    val allReports: Flow<List<Report>> = reportDao.getAllReports()

    fun getReportById(id: Long): Flow<Report?> = reportDao.getReportById(id)

    // Delete report locally first, then sync with Supabase
    suspend fun deleteReport(id: Long): Result<Unit> = withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
        if (SupabaseService.isConfigured) {
            val result = SupabaseService.deleteReport(id)
            if (result.isSuccess) {
                reportDao.deleteReportById(id)
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Gagal menghapus dari Supabase"))
            }
        } else {
            reportDao.deleteReportById(id)
            Result.success(Unit)
        }
    }

    // Refresh reports from Supabase and cache locally
    suspend fun refreshReports(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SupabaseService.isConfigured) {
            // In simulation mode, keep local Room data intact
            return@withContext Result.success(Unit)
        }

        val result = SupabaseService.fetchReports()
        if (result.isSuccess) {
            val remoteReports = result.getOrNull() ?: emptyList()
            reportDao.clearAll()
            for (report in remoteReports) {
                reportDao.insertReport(report)
            }
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Gagal sinkronisasi data"))
        }
    }

    // Add new report locally and upload to Supabase if configured
    suspend fun addReport(title: String, description: String, imageUrl: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        if (SupabaseService.isConfigured) {
            val result = SupabaseService.insertReport(title, description, imageUrl)
            if (result.isSuccess) {
                val supabaseId = result.getOrNull()
                val finalReportId = if (supabaseId != null && supabaseId != -1L) supabaseId else 0L
                val newReport = Report(id = finalReportId, title = title, description = description, imageUrl = imageUrl)
                reportDao.insertReport(newReport)
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Gagal upload ke Supabase"))
            }
        } else {
            // In local simulation, inserting to Room is enough!
            val newReport = Report(title = title, description = description, imageUrl = imageUrl)
            reportDao.insertReport(newReport)
            Result.success(Unit)
        }
    }
}

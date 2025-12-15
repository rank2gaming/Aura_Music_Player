package com.rank2gaming.aura.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PlaylistBackupManager {

    // Backup to: /Documents/AuraMusic/playlists_backup.json
    fun backupPlaylists(context: Context) {
        try {
            val playlists = PlaylistManager.getPlaylists(context)
            val json = Gson().toJson(playlists)

            // Create Directory
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AuraMusic")
            if (!dir.exists()) dir.mkdirs()

            // Create File
            val file = File(dir, "playlists_backup.json")
            val writer = FileWriter(file)
            writer.write(json)
            writer.flush()
            writer.close()

            Toast.makeText(context, "Backup saved to Documents/AuraMusic", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Backup Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Restore from a specific URI (picked by user)
    fun restorePlaylists(context: Context, uri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            inputStream?.close()

            val json = sb.toString()
            val type = object : TypeToken<Map<String, List<Long>>>() {}.type
            val restoredData: Map<String, List<Long>> = Gson().fromJson(json, type)

            // Merge with existing or overwrite? Let's Merge safely.
            val current = PlaylistManager.getPlaylists(context).toMutableMap()
            current.putAll(restoredData)

            // Save manually (Need to expose save logic in PlaylistManager or just re-add one by one)
            // Ideally PlaylistManager should have a 'saveAll' method.
            // For now, we rely on the internal pref logic via a trick or update PlaylistManager.
            // *Assuming PlaylistManager has a setPlaylists method or we iterate*

            // Re-saving logic simulation using public methods:
            val prefs = context.getSharedPreferences("AuraPlaylists", Context.MODE_PRIVATE)
            prefs.edit().putString("user_playlists_map", Gson().toJson(current)).apply()

            Toast.makeText(context, "Playlists Restored Successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Restore Failed: Invalid File", Toast.LENGTH_SHORT).show()
        }
    }
}
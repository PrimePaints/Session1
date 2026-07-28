package app.repeatless.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import app.repeatless.data.db.AppDatabase
import app.repeatless.data.db.BoardEntity
import app.repeatless.data.db.PadEntity
import app.repeatless.ui.theme.PadHexStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class SoundboardRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.soundboardDao()

    private val audioDir: File by lazy {
        File(context.filesDir, "audio_clips").apply { mkdirs() }
    }

    // Free tier caps
    companion object {
        const val FREE_MAX_BOARDS = 2
        const val FREE_MAX_PADS_PER_BOARD = 12
    }

    val boards: Flow<List<BoardEntity>> = dao.getBoards()
    val allPads: Flow<List<PadEntity>> = dao.getAllPads()

    suspend fun renameTriggerTag(oldTag: String, newTag: String) = withContext(Dispatchers.IO) {
        val trimmedNew = newTag.trim()
        val all = dao.getAllPadsSync()
        val updated = all.filter { it.triggerTag?.equals(oldTag, ignoreCase = true) == true }
            .map { it.copy(triggerTag = trimmedNew.ifBlank { null }) }
        if (updated.isNotEmpty()) {
            dao.updatePads(updated)
        }
    }

    suspend fun mergeTriggerTags(sourceTag: String, targetTag: String) = withContext(Dispatchers.IO) {
        val trimmedTarget = targetTag.trim()
        val all = dao.getAllPadsSync()
        val updated = all.filter { it.triggerTag?.equals(sourceTag, ignoreCase = true) == true }
            .map { it.copy(triggerTag = trimmedTarget.ifBlank { null }) }
        if (updated.isNotEmpty()) {
            dao.updatePads(updated)
        }
    }

    suspend fun deleteTriggerTag(tagToDelete: String) = withContext(Dispatchers.IO) {
        val all = dao.getAllPadsSync()
        val updated = all.filter { it.triggerTag?.equals(tagToDelete, ignoreCase = true) == true }
            .map { it.copy(triggerTag = null) }
        if (updated.isNotEmpty()) {
            dao.updatePads(updated)
        }
    }

    fun getPadsForBoard(boardId: String): Flow<List<PadEntity>> {
        return dao.getPadsForBoard(boardId)
    }

    suspend fun ensureInitialData() = withContext(Dispatchers.IO) {
        val count = dao.getBoardCount()
        if (count == 0) {
            val defaultBoard = BoardEntity(
                id = UUID.randomUUID().toString(),
                name = "My Soundboard",
                order = 0
            )
            dao.insertBoard(defaultBoard)
        }
    }

    fun getAudioFileForPad(audioFileName: String): File {
        return File(audioDir, audioFileName)
    }

    fun getRawAudioFileForPad(pad: PadEntity): File {
        val rawFile = File(audioDir, "raw_clip_${pad.id}.m4a")
        if (rawFile.exists() && rawFile.length() > 0) {
            return rawFile
        }
        return getAudioFileForPad(pad.audioFileName)
    }

    suspend fun movePadToBoard(
        padId: String,
        newBoardId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val pad = dao.getPadByIdSync(padId)
            ?: return@withContext Result.failure(IllegalStateException("Pad not found"))

        val targetBoardPadsCount = dao.getPadCountForBoard(newBoardId)
        dao.updatePad(pad.copy(boardId = newBoardId, order = targetBoardPadsCount))
        Result.success(Unit)
    }

    suspend fun createBoard(name: String, isPro: Boolean): Result<BoardEntity> = withContext(Dispatchers.IO) {
        val currentBoardCount = dao.getBoardCount()
        if (!isPro && currentBoardCount >= FREE_MAX_BOARDS) {
            return@withContext Result.failure(
                IllegalStateException("Free tier limit reached ($FREE_MAX_BOARDS boards). Upgrade to Pro for unlimited boards!")
            )
        }

        val newBoard = BoardEntity(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "New Board" },
            order = currentBoardCount
        )
        dao.insertBoard(newBoard)
        Result.success(newBoard)
    }

    suspend fun renameBoard(boardId: String, newName: String) = withContext(Dispatchers.IO) {
        val board = dao.getBoardByIdSync(boardId) ?: return@withContext
        dao.updateBoard(board.copy(name = newName.ifBlank { "Untitled Board" }))
    }

    suspend fun deleteBoard(boardId: String) = withContext(Dispatchers.IO) {
        val totalBoards = dao.getBoardCount()
        if (totalBoards <= 1) {
            throw IllegalStateException("Cannot delete the only remaining board!")
        }

        val pads = dao.getPadsForBoardSync(boardId)
        for (pad in pads) {
            val audioFile = getAudioFileForPad(pad.audioFileName)
            if (audioFile.exists()) {
                audioFile.delete()
            }
        }

        dao.deleteBoardById(boardId)
    }

    suspend fun reorderBoards(orderedBoardIds: List<String>) = withContext(Dispatchers.IO) {
        val boards = dao.getBoardsSync()
        val boardMap = boards.associateBy { it.id }
        orderedBoardIds.forEachIndexed { index, boardId ->
            boardMap[boardId]?.let { board ->
                dao.updateBoard(board.copy(order = index))
            }
        }
    }

    suspend fun addPad(
        boardId: String,
        label: String,
        colorHex: String,
        tempAudioFile: File,
        durationMs: Long,
        isPro: Boolean,
        rawAudioFile: File? = null
    ): Result<PadEntity> = withContext(Dispatchers.IO) {
        val padCount = dao.getPadCountForBoard(boardId)
        if (!isPro && padCount >= FREE_MAX_PADS_PER_BOARD) {
            return@withContext Result.failure(
                IllegalStateException("Free tier limit reached ($FREE_MAX_PADS_PER_BOARD pads per board). Upgrade to Pro for unlimited pads!")
            )
        }

        val padId = UUID.randomUUID().toString()
        val audioFileName = "clip_$padId.m4a"
        val rawFileName = "raw_clip_$padId.m4a"
        val destFile = File(audioDir, audioFileName)
        val rawDestFile = File(audioDir, rawFileName)

        val sourceRaw = rawAudioFile ?: tempAudioFile
        if (sourceRaw.exists()) {
            sourceRaw.copyTo(rawDestFile, overwrite = true)
        }

        tempAudioFile.copyTo(destFile, overwrite = true)
        if (tempAudioFile.exists()) {
            tempAudioFile.delete()
        }
        if (rawAudioFile != null && rawAudioFile.exists()) {
            rawAudioFile.delete()
        }

        val chosenColor = colorHex.ifBlank {
            PadHexStrings[padCount % PadHexStrings.size]
        }

        val newPad = PadEntity(
            id = padId,
            boardId = boardId,
            label = label.ifBlank { "Sound ${padCount + 1}" },
            colorHex = chosenColor,
            order = padCount,
            audioFileName = audioFileName,
            mimeType = "audio/mp4",
            durationMs = durationMs
        )

        dao.insertPad(newPad)
        Result.success(newPad)
    }

    suspend fun reRecordPad(
        padId: String,
        tempAudioFile: File,
        durationMs: Long,
        rawAudioFile: File? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val existingPad = dao.getPadByIdSync(padId)
            ?: return@withContext Result.failure(IllegalStateException("Pad not found"))

        val destFile = File(audioDir, existingPad.audioFileName)
        val rawDestFile = File(audioDir, "raw_clip_${existingPad.id}.m4a")

        val sourceRaw = rawAudioFile ?: tempAudioFile
        if (sourceRaw.exists()) {
            sourceRaw.copyTo(rawDestFile, overwrite = true)
        }

        tempAudioFile.copyTo(destFile, overwrite = true)
        if (tempAudioFile.exists()) {
            tempAudioFile.delete()
        }
        if (rawAudioFile != null && rawAudioFile.exists()) {
            rawAudioFile.delete()
        }

        dao.updatePad(existingPad.copy(durationMs = durationMs))
        Result.success(Unit)
    }

    suspend fun trimAndSavePad(
        padId: String,
        startMs: Long,
        endMs: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val existingPad = dao.getPadByIdSync(padId)
            ?: return@withContext Result.failure(IllegalStateException("Pad not found"))

        val rawFile = File(audioDir, "raw_clip_$padId.m4a")
        val sourceFile = if (rawFile.exists() && rawFile.length() > 0) {
            rawFile
        } else {
            File(audioDir, existingPad.audioFileName)
        }

        val destFile = File(audioDir, existingPad.audioFileName)
        val calculatedDuration = (endMs - startMs).coerceAtLeast(100L)

        val success = app.repeatless.audio.AudioTrimmer.trim(sourceFile, destFile, startMs, endMs)
        if (success && destFile.exists() && destFile.length() > 0) {
            dao.updatePad(existingPad.copy(durationMs = calculatedDuration))
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed to trim audio clip"))
        }
    }

    suspend fun updatePad(padId: String, newLabel: String, newColorHex: String, triggerTag: String? = null) = withContext(Dispatchers.IO) {
        val existingPad = dao.getPadByIdSync(padId) ?: return@withContext
        dao.updatePad(
            existingPad.copy(
                label = newLabel.ifBlank { existingPad.label },
                colorHex = newColorHex.ifBlank { existingPad.colorHex },
                triggerTag = triggerTag
            )
        )
    }

    suspend fun reorderPads(boardId: String, orderedPadIds: List<String>) = withContext(Dispatchers.IO) {
        val pads = dao.getPadsForBoardSync(boardId)
        val padMap = pads.associateBy { it.id }
        val updatedList = mutableListOf<PadEntity>()

        orderedPadIds.forEachIndexed { index, padId ->
            padMap[padId]?.let { pad ->
                updatedList.add(pad.copy(order = index))
            }
        }

        if (updatedList.isNotEmpty()) {
            dao.updatePads(updatedList)
        }
    }

    suspend fun deletePad(padId: String) = withContext(Dispatchers.IO) {
        val pad = dao.getPadByIdSync(padId) ?: return@withContext
        val audioFile = getAudioFileForPad(pad.audioFileName)
        if (audioFile.exists()) {
            audioFile.delete()
        }
        val rawFile = File(audioDir, "raw_clip_$padId.m4a")
        if (rawFile.exists()) {
            rawFile.delete()
        }
        dao.deletePadById(padId)
    }

    // Export Backup to JSON String
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val boards = dao.getBoardsSync()
        val rootJson = JSONObject()
        rootJson.put("app", "soundboard")
        rootJson.put("version", 2)
        rootJson.put("exportedAt", System.currentTimeMillis())

        val boardsArray = JSONArray()
        for (board in boards) {
            val boardObj = JSONObject()
            boardObj.put("id", board.id)
            boardObj.put("name", board.name)
            boardObj.put("order", board.order)

            val pads = dao.getPadsForBoardSync(board.id)
            val padsArray = JSONArray()
            for (pad in pads) {
                val padObj = JSONObject()
                padObj.put("id", pad.id)
                padObj.put("label", pad.label)
                padObj.put("color", pad.colorHex)
                padObj.put("order", pad.order)
                padObj.put("mime", pad.mimeType)
                padObj.put("durationMs", pad.durationMs)

                val audioFile = getAudioFileForPad(pad.audioFileName)
                if (audioFile.exists()) {
                    val bytes = audioFile.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    padObj.put("audio", "data:${pad.mimeType};base64,$base64")
                } else {
                    padObj.put("audio", "")
                }
                padsArray.put(padObj)
            }
            boardObj.put("pads", padsArray)
            boardsArray.put(boardObj)
        }
        rootJson.put("boards", boardsArray)
        rootJson.toString(2)
    }

    // Import Backup from JSON InputStream
    suspend fun importBackupJson(inputStream: InputStream, replaceAll: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            val rootObj = JSONObject(jsonText)

            if (!rootObj.has("app") || rootObj.optString("app") != "soundboard") {
                return@withContext Result.failure(IllegalArgumentException("Invalid soundboard backup file format"))
            }

            val version = rootObj.optInt("version", 1)

            if (replaceAll) {
                // Clear existing
                val allPads = dao.getAllPadsSync()
                for (p in allPads) {
                    val f = getAudioFileForPad(p.audioFileName)
                    if (f.exists()) f.delete()
                }
                val allBoards = dao.getBoardsSync()
                for (b in allBoards) {
                    dao.deleteBoardById(b.id)
                }
            }

            var importedPadsCount = 0

            if (version == 1) {
                // Legacy Web Prototype format (single board)
                val boardTitle = rootObj.optString("title", "Imported Soundboard")
                val boardId = UUID.randomUUID().toString()
                val currentBoardCount = dao.getBoardCount()

                val newBoard = BoardEntity(
                    id = boardId,
                    name = boardTitle,
                    order = currentBoardCount
                )
                dao.insertBoard(newBoard)

                val padsArray = rootObj.optJSONArray("pads") ?: JSONArray()
                for (i in 0 until padsArray.length()) {
                    val padObj = padsArray.getJSONObject(i)
                    val label = padObj.optString("label", "Pad ${i + 1}")
                    val color = padObj.optString("color", "#ff6b61")
                    val order = padObj.optInt("order", i)
                    val mime = padObj.optString("mime", "audio/mp4")
                    val dataUrl = padObj.optString("audio", "")

                    val padId = UUID.randomUUID().toString()
                    val audioFileName = "clip_$padId.m4a"
                    val destFile = File(audioDir, audioFileName)

                    if (dataUrl.isNotBlank() && dataUrl.contains("base64,")) {
                        val base64Data = dataUrl.substringAfter("base64,")
                        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                        FileOutputStream(destFile).use { fos ->
                            fos.write(bytes)
                        }
                    }

                    val padEntity = PadEntity(
                        id = padId,
                        boardId = boardId,
                        label = label,
                        colorHex = color,
                        order = order,
                        audioFileName = audioFileName,
                        mimeType = mime,
                        durationMs = 0L
                    )
                    dao.insertPad(padEntity)
                    importedPadsCount++
                }

            } else {
                // Version 2 format with multi-boards
                val boardsArray = rootObj.optJSONArray("boards") ?: JSONArray()
                for (bIdx in 0 until boardsArray.length()) {
                    val boardObj = boardsArray.getJSONObject(bIdx)
                    val boardName = boardObj.optString("name", "Board ${bIdx + 1}")
                    val boardId = UUID.randomUUID().toString()
                    val currentBoardCount = dao.getBoardCount()

                    val newBoard = BoardEntity(
                        id = boardId,
                        name = boardName,
                        order = currentBoardCount
                    )
                    dao.insertBoard(newBoard)

                    val padsArray = boardObj.optJSONArray("pads") ?: JSONArray()
                    for (pIdx in 0 until padsArray.length()) {
                        val padObj = padsArray.getJSONObject(pIdx)
                        val label = padObj.optString("label", "Pad ${pIdx + 1}")
                        val color = padObj.optString("color", "#ff6b61")
                        val order = padObj.optInt("order", pIdx)
                        val mime = padObj.optString("mime", "audio/mp4")
                        val durationMs = padObj.optLong("durationMs", 0L)
                        val dataUrl = padObj.optString("audio", "")

                        val padId = UUID.randomUUID().toString()
                        val audioFileName = "clip_$padId.m4a"
                        val destFile = File(audioDir, audioFileName)

                        if (dataUrl.isNotBlank() && dataUrl.contains("base64,")) {
                            val base64Data = dataUrl.substringAfter("base64,")
                            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                            FileOutputStream(destFile).use { fos ->
                                fos.write(bytes)
                            }
                        }

                        val padEntity = PadEntity(
                            id = padId,
                            boardId = boardId,
                            label = label,
                            colorHex = color,
                            order = order,
                            audioFileName = audioFileName,
                            mimeType = mime,
                            durationMs = durationMs
                        )
                        dao.insertPad(padEntity)
                        importedPadsCount++
                    }
                }
            }

            Result.success(importedPadsCount)
        } catch (e: Exception) {
            Log.e("SoundboardRepo", "Error importing backup JSON", e)
            Result.failure(e)
        }
    }
}

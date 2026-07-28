package app.repeatless.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "pads",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["boardId"])]
)
data class PadEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val boardId: String,
    val label: String,
    val colorHex: String,
    val order: Int = 0,
    val audioFileName: String,
    val mimeType: String = "audio/mp4",
    val durationMs: Long = 0L,
    val triggerTag: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

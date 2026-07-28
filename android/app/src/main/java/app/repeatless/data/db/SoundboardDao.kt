package app.repeatless.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundboardDao {

    // Board Queries
    @Query("SELECT * FROM boards ORDER BY `order` ASC, createdAt ASC")
    fun getBoards(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards ORDER BY `order` ASC, createdAt ASC")
    suspend fun getBoardsSync(): List<BoardEntity>

    @Query("SELECT * FROM boards WHERE id = :id LIMIT 1")
    fun getBoardById(id: String): Flow<BoardEntity?>

    @Query("SELECT * FROM boards WHERE id = :id LIMIT 1")
    suspend fun getBoardByIdSync(id: String): BoardEntity?

    @Query("SELECT COUNT(*) FROM boards")
    suspend fun getBoardCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: BoardEntity)

    @Update
    suspend fun updateBoard(board: BoardEntity)

    @Query("DELETE FROM boards WHERE id = :id")
    suspend fun deleteBoardById(id: String)

    @Delete
    suspend fun deleteBoard(board: BoardEntity)


    // Pad Queries
    @Query("SELECT * FROM pads WHERE boardId = :boardId ORDER BY `order` ASC, createdAt ASC")
    fun getPadsForBoard(boardId: String): Flow<List<PadEntity>>

    @Query("SELECT * FROM pads WHERE boardId = :boardId ORDER BY `order` ASC, createdAt ASC")
    suspend fun getPadsForBoardSync(boardId: String): List<PadEntity>

    @Query("SELECT * FROM pads")
    fun getAllPads(): Flow<List<PadEntity>>

    @Query("SELECT * FROM pads")
    suspend fun getAllPadsSync(): List<PadEntity>

    @Query("SELECT * FROM pads WHERE id = :id LIMIT 1")
    fun getPadById(id: String): Flow<PadEntity?>

    @Query("SELECT * FROM pads WHERE id = :id LIMIT 1")
    suspend fun getPadByIdSync(id: String): PadEntity?

    @Query("SELECT COUNT(*) FROM pads WHERE boardId = :boardId")
    suspend fun getPadCountForBoard(boardId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPad(pad: PadEntity)

    @Update
    suspend fun updatePad(pad: PadEntity)

    @Update
    suspend fun updatePads(pads: List<PadEntity>)

    @Query("DELETE FROM pads WHERE id = :id")
    suspend fun deletePadById(id: String)

    @Delete
    suspend fun deletePad(pad: PadEntity)
}

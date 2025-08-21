package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import co.touchlab.kermit.Logger
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task

@Dao
abstract class TagDao {
    @Query("UPDATE tags SET name = :name WHERE tag_uid = :tagUid")
    abstract suspend fun rename(tagUid: String, name: String)

    @Insert
    abstract suspend fun insert(tag: Tag)

    @Insert
    abstract suspend fun insert(tags: Iterable<Tag>)

    @Query("DELETE FROM tags WHERE task = :taskId AND tag_uid in (:tagUids)")
    internal abstract suspend fun deleteTags(taskId: Long, tagUids: List<String>)

    @Query("SELECT * FROM tags WHERE tag_uid = :tagUid")
    abstract suspend fun getByTagUid(tagUid: String): List<Tag>

    @Query("SELECT * FROM tags WHERE task = :taskId")
    abstract suspend fun getTagsForTask(taskId: Long): List<Tag>

    @Query("SELECT * FROM tags WHERE task = :taskId AND tag_uid = :tagUid")
    abstract suspend fun getTagByTaskAndTagUid(taskId: Long, tagUid: String): Tag?

    @Delete
    abstract suspend fun delete(tags: List<Tag>)

    @Transaction
    open suspend fun applyTags(task: Task, tagDataDao: TagDataDao, current: Collection<TagData>) {
        Logger.d("TagDao") { "applyTags task=$task current=$current" }
        val taskId = task.id
        val existing = HashSet(tagDataDao.getTagDataForTask(taskId))
        val selected = current.toMutableSet()
        val added = selected subtract existing
        val removed = existing subtract selected
        deleteTags(taskId, removed.map { td -> td.remoteId!! })
        insert(task, added)
    }

    suspend fun insert(task: Task, tags: Collection<TagData>) {
        if (!tags.isEmpty()) {
            insert(
                tags.map {
                    Tag(
                        task = task.id,
                        taskUid = task.uuid,
                        name = it.name,
                        tagUid = it.remoteId
                    )
                }
            )
        }
    }
}
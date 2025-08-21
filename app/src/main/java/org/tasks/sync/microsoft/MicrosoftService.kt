package org.tasks.sync.microsoft

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MicrosoftService(
    private val client: HttpClient
) {
    private val baseUrl: String = "https://graph.microsoft.com/v1.0/me/todo"

    suspend fun getLists(): TaskLists = client.get("$baseUrl/lists").body()

    suspend fun paginateLists(nextPage: String): TaskLists = client.get(nextPage).body()

    suspend fun createList(body: TaskLists.TaskList): TaskLists.TaskList =
        client
            .post("$baseUrl/lists") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            .body()

    suspend fun updateList(listId: String, body: TaskLists.TaskList): TaskLists.TaskList =
        client
            .patch("$baseUrl/lists/$listId") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            .body()

    suspend fun deleteList(listId: String) = client.delete("$baseUrl/lists/$listId")

    suspend fun getTasks(listId: String) = client.get("$baseUrl/lists/$listId/tasks/delta")

    suspend fun paginateTasks(nextPage: String) = client.get(nextPage)

    suspend fun createTask(listId: String, body: Tasks.Task): Tasks.Task =
        client
            .post("$baseUrl/lists/$listId/tasks") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            .body()

    suspend fun updateTask(listId: String, taskId: String, body: Tasks.Task): Tasks.Task =
        client
            .patch("$baseUrl/lists/$listId/tasks/$taskId") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            .body()

    suspend fun deleteTask(listId: String, taskId: String) =
        client.delete("$baseUrl/lists/$listId/tasks/$taskId")

    suspend fun createChecklistItem(listId: String, taskId: String, body: Tasks.Task.ChecklistItem): Tasks.Task.ChecklistItem =
        client
            .post("$baseUrl/lists/$listId/tasks/$taskId/checklistItems") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            .body()

    suspend fun updateChecklistItem(listId: String, taskId: String, body: Tasks.Task.ChecklistItem): Tasks.Task.ChecklistItem =
        client
            .patch("$baseUrl/lists/$listId/tasks/$taskId/checklistItems/${body.id}") {
                contentType(ContentType.Application.Json)
                setBody(body.copy(id = null, createdDateTime = null))
            }
            .body()

    suspend fun deleteChecklistItem(listId: String, taskId: String, checklistItemId: String) =
        client.delete("$baseUrl/lists/$listId/tasks/$taskId/checklistItems/$checklistItemId")

    suspend fun getList(listId: String): TaskLists.TaskList =
        client
            .get("$baseUrl/lists/$listId") {
                contentType(ContentType.Application.Json)
            }
            .body()
}

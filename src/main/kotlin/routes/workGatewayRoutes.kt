package routes

import ServiceConfig
import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import routes.utils.proxyGet

suspend fun forwardMultipartToWorkService(
    client: HttpClient,
    targetUrl: String,
    call: ApplicationCall
) {
    val multipart = call.receiveMultipart()

    var taskId: String? = null
    var firstName: String? = null
    var lastName: String? = null

    var fileBytes: ByteArray? = null
    var fileName: String? = null
    var fileContentType: ContentType? = null

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                when (part.name) {
                    "taskId" -> taskId = part.value
                    "firstName" -> firstName = part.value
                    "lastName" -> lastName = part.value
                }
            }

            is PartData.FileItem -> {
                fileName = part.originalFileName ?: "file"
                fileContentType = part.contentType

                val channel = part.provider()
                fileBytes = channel.readRemaining().readBytes()
            }

            else -> {}
        }
        part.dispose()
    }

    if (taskId == null || firstName == null || lastName == null || fileBytes == null) {
        call.respond(HttpStatusCode.BadRequest, "Missing required fields")
        return
    }

    val response = client.submitFormWithBinaryData(
        url = targetUrl,
        formData = formData {
            append("taskId", taskId)
            append("firstName", firstName)
            append("lastName", lastName)

            append("file", fileBytes, Headers.build {
                append(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.File
                        .withParameter(ContentDisposition.Parameters.FileName, fileName!!)
                        .toString()
                )
                append(HttpHeaders.ContentType, fileContentType?.toString() ?: "application/octet-stream")
            })
        }
    )

    val contentType = response.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
        ?: ContentType.Application.Json

    call.respond(response.status, TextContent(response.bodyAsText(), contentType))
}


fun Route.workGatewayRoutes(
    client: HttpClient,
    services: ServiceConfig
) {
    route("/works") {

        post {
            val target = "${services.worksBaseUrl}/works"
            forwardMultipartToWorkService(client, target, call)
        }

        get {
            val target = "${services.worksBaseUrl}/works"
            proxyGet(client, target, call)
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get
            val target = "${services.worksBaseUrl}/works/$id"
            proxyGet(client, target, call)
        }
    }
}
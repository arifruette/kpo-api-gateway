package routes.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

suspend fun proxyGet(
    client: HttpClient,
    targetUrl: String,
    call: ApplicationCall
) {
    val response: HttpResponse = client.get(targetUrl) {
        url {
            parameters.appendAll(call.request.queryParameters)
        }
    }

    val contentType = response.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
        ?: ContentType.Application.Json

    val body = response.bodyAsText()
    call.respond(response.status, TextContent(body, contentType))
}

suspend fun proxyDelete(
    client: HttpClient,
    targetUrl: String,
    call: ApplicationCall
) {
    val response: HttpResponse = client.delete(targetUrl)

    if (response.status == HttpStatusCode.NoContent) {
        call.respond(HttpStatusCode.NoContent)
    } else {
        val contentType = response.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
            ?: ContentType.Application.Json
        val body = response.bodyAsText()
        call.respond(response.status, TextContent(body, contentType))
    }
}

suspend fun proxyPostJson(
    client: HttpClient,
    targetUrl: String,
    call: ApplicationCall
) {
    val requestBody = call.receiveText()
    val contentType = call.request.contentType().takeIf { !it.match(ContentType.Any) }
        ?: ContentType.Application.Json

    val response: HttpResponse = client.post(targetUrl) {
        headers {
            append(HttpHeaders.ContentType, contentType.toString())
        }
        setBody(requestBody)
    }

    val respContentType = response.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
        ?: ContentType.Application.Json
    val body = response.bodyAsText()
    call.respond(response.status, TextContent(body, respContentType))
}

suspend fun proxyPostNoBody(
    client: HttpClient,
    targetUrl: String,
    call: ApplicationCall
) {
    val response: HttpResponse = client.post(targetUrl)

    val contentType = response.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
        ?: ContentType.Application.Json
    val body = response.bodyAsText()
    call.respond(response.status, TextContent(body, contentType))
}

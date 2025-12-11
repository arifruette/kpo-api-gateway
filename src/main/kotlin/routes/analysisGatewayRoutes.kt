package routes

import ServiceConfig
import io.ktor.client.*
import io.ktor.server.routing.*
import routes.utils.proxyGet
import routes.utils.proxyPostNoBody

fun Route.analysisGatewayRoutes(
    client: HttpClient,
    services: ServiceConfig
) {
    post("/works/{id}/analyze") {
        val id = call.parameters["id"] ?: return@post
        val target = "${services.analysisBaseUrl}/internal/works/$id/analyze"
        proxyPostNoBody(client, target, call)
    }

    get("/works/{id}/reports") {
        val id = call.parameters["id"] ?: return@get
        val target = "${services.analysisBaseUrl}/works/$id/reports"
        proxyGet(client, target, call)
    }

    get("/reports/plagiarism") {
        val target = "${services.analysisBaseUrl}/reports/plagiarism"
        proxyGet(client, target, call)
    }
}

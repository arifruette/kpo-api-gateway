package routes

import ServiceConfig
import io.ktor.client.*
import io.ktor.server.routing.*
import routes.utils.proxyDelete
import routes.utils.proxyGet
import routes.utils.proxyPostJson

fun Route.taskGatewayRoutes(
    client: HttpClient,
    services: ServiceConfig
) {
    route("/tasks") {

        get {
            val target = "${services.tasksBaseUrl}/tasks"
            proxyGet(client, target, call)
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get
            val target = "${services.tasksBaseUrl}/tasks/$id"
            proxyGet(client, target, call)
        }

        post {
            val target = "${services.tasksBaseUrl}/tasks"
            proxyPostJson(client, target, call)
        }

        delete("{id}") {
            val id = call.parameters["id"] ?: return@delete
            val target = "${services.tasksBaseUrl}/tasks/$id"
            proxyDelete(client, target, call)
        }
    }
}

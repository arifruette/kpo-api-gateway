import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as client
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import java.net.ConnectException
import io.ktor.server.plugins.swagger.*
import routes.analysisGatewayRoutes
import routes.taskGatewayRoutes
import routes.workGatewayRoutes

data class ServiceConfig(
    val tasksBaseUrl: String,
    val worksBaseUrl: String,
    val analysisBaseUrl: String
)

fun Application.module() {

    val config = environment.config

    val services = ServiceConfig(
        tasksBaseUrl = config.property("services.tasks.baseUrl").getString(),
        worksBaseUrl = config.property("services.works.baseUrl").getString(),
        analysisBaseUrl = config.property("services.analysis.baseUrl").getString()
    )

    val client = HttpClient(CIO) {
        install(client) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    install(CallLogging)

    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<ConnectException> { call, cause ->
            call.application.environment.log.error("Downstream service unavailable", cause)
            call.respond(
                status = HttpStatusCode.BadGateway,
                message = mapOf(
                    "error" to "Service unavailable",
                    "details" to (cause.message ?: "Cannot connect to downstream service")
                )
            )
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unexpected error in gateway", cause)
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = mapOf(
                    "error" to "Gateway error",
                    "details" to (cause.message ?: "Unexpected error")
                )
            )
        }
    }

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yml")

        taskGatewayRoutes(client, services)
        workGatewayRoutes(client, services)
        analysisGatewayRoutes(client, services)

        get("/") {
            call.respondRedirect("/swagger")
        }
    }
}

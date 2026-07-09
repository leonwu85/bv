package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.util.LogCatcherUtil
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object HttpServer {
    var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun startServer() {
        GlobalScope.launch(Dispatchers.IO) {
            server = embeddedServer(CIO, port = 0) {
                homeModule()
                logsApiModule()
                geetestCompanionModule()
            }
            server?.start(wait = true)
        }
    }

    private fun Application.homeModule() {
        routing {
            get("/") {
                call.respondText("Hello World!")
            }
        }
    }

    private fun Application.logsApiModule() {
        routing {
            get("/api/logs/{filename}") {
                val filename =
                    call.parameters["filename"] ?: return@get call.respondText(
                        text = "filename is null",
                        status = HttpStatusCode.NotFound
                    )
                LogCatcherUtil.updateLogFiles()
                val file = (LogCatcherUtil.crashFiles + LogCatcherUtil.manualFiles)
                    .find { it.name == filename } ?: return@get call.respondText(
                    text = "file not found",
                    status = HttpStatusCode.NotFound
                )
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        file.name
                    ).toString()
                )
                call.respondFile(file)
            }
        }
    }

    private fun Application.geetestCompanionModule() {
        routing {
            get("/geetest/{id}") {
                val id = call.parameters["id"].orEmpty()
                val session = GeetestCompanionService.getSession(id)
                    ?: return@get call.respondText(
                        text = "验证会话不存在或已过期，请在电视上重新打开验证",
                        status = HttpStatusCode.NotFound
                    )
                call.respondText(
                    text = GeetestCompanionService.buildHtmlForSession(session),
                    contentType = ContentType.Text.Html
                )
            }
            post("/geetest/{id}/result") {
                val id = call.parameters["id"].orEmpty()
                val body = call.receiveText()
                val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                    ?: return@post call.respondText(
                        text = """{"ok":false,"message":"invalid json"}""",
                        status = HttpStatusCode.BadRequest,
                        contentType = ContentType.Application.Json
                    )
                val validate = obj["validate"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val seccode = obj["seccode"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val challenge = obj["challenge"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val ok = GeetestCompanionService.completeSession(
                    id = id,
                    challenge = challenge,
                    validate = validate,
                    seccode = seccode
                )
                if (!ok) {
                    return@post call.respondText(
                        text = """{"ok":false,"message":"session not found or empty result"}""",
                        status = HttpStatusCode.BadRequest,
                        contentType = ContentType.Application.Json
                    )
                }
                call.respondText(
                    text = """{"ok":true}""",
                    contentType = ContentType.Application.Json
                )
            }
        }
    }
}

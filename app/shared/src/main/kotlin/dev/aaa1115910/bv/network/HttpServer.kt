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
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.BindException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive

object HttpServer {
    private const val MAX_BIND_ATTEMPTS = 3
    private const val BIND_RETRY_DELAY_MS = 100L

    private val logger = KotlinLogging.logger("HttpServer")
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val starting = AtomicBoolean(false)

    @Volatile
    var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun startServer() {
        if (!starting.compareAndSet(false, true)) return
        if (server != null) {
            starting.set(false)
            return
        }

        serverScope.launch {
            try {
                startServerWithRetry()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                // 本地辅助服务不是应用主流程，构造阶段的异常也不能终止进程。
                logger.error(error) { "Local HTTP server initialization failed" }
            } finally {
                starting.set(false)
            }
        }
    }

    private suspend fun startServerWithRetry() {
        repeat(MAX_BIND_ATTEMPTS) { attempt ->
            val candidate = embeddedServer(CIO, port = 0) {
                homeModule()
                logsApiModule()
                geetestCompanionModule()
            }

            try {
                // CIO waits for connector startup even when wait=false, so bind failures are
                // raised here while the engine keeps running on its own job after success.
                candidate.start(wait = false)
                server = candidate
                return
            } catch (error: Throwable) {
                if (error is CancellationException) throw error

                val isBindFailure = error.causeSequence().any { it is BindException }
                val willRetry = isBindFailure && attempt < MAX_BIND_ATTEMPTS - 1
                if (!willRetry) {
                    logger.error(error) { "Local HTTP server failed to start" }
                    return
                }

                logger.warn(error) {
                    "Local HTTP server port collision; retrying (${attempt + 1}/$MAX_BIND_ATTEMPTS)"
                }
                delay(BIND_RETRY_DELAY_MS * (attempt + 1))
            }
        }
    }

    private fun Throwable.causeSequence(): Sequence<Throwable> =
        generateSequence(this) { it.cause?.takeUnless { cause -> cause === it } }

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
            get("/geetest/{id}/captcha") {
                val id = call.parameters["id"].orEmpty()
                val request = GeetestCompanionService.getSession(id)?.request as? CaptchaRequest.Gaia
                    ?: return@get call.respondText("验证会话已过期", status = HttpStatusCode.NotFound)
                call.respondText(buildGaiaCaptchaHtml(request.voucher, id), ContentType.Text.Html)
            }
            get("/geetest/{id}/gaia/config") {
                val session = GeetestCompanionService.getSession(call.parameters["id"].orEmpty())
                if (session?.request !is CaptchaRequest.Gaia) {
                    return@get call.respondText("验证会话已过期", status = HttpStatusCode.NotFound)
                }
                call.respondText(GaiaCompanionApi.config(), ContentType.Application.Json)
            }
            post("/geetest/{id}/gaia/encrypt") {
                val session = GeetestCompanionService.getSession(call.parameters["id"].orEmpty())
                if (session?.request !is CaptchaRequest.Gaia || session.completed) {
                    return@post call.respondText("验证会话已过期", status = HttpStatusCode.NotFound)
                }
                val body = call.receiveChannel()
                    .readRemaining(GaiaCompanionCrypto.MAX_REQUEST_BYTES + 1).readByteArray()
                if (body.size > GaiaCompanionCrypto.MAX_REQUEST_BYTES) {
                    return@post call.respondText("验证数据过大", status = HttpStatusCode.PayloadTooLarge)
                }
                val encrypted = try {
                    GaiaCompanionCrypto.encrypt(body.decodeToString())
                } catch (_: IllegalArgumentException) {
                    return@post call.respondText("验证加密参数无效", status = HttpStatusCode.BadRequest)
                }
                call.respondText(encrypted, ContentType.Application.Json)
            }
            post("/geetest/{id}/gaia/{action}") {
                val session = GeetestCompanionService.getSession(call.parameters["id"].orEmpty())
                val request = session?.request as? CaptchaRequest.Gaia
                    ?: return@post call.respondText("验证会话已过期", status = HttpStatusCode.NotFound)
                val action = call.parameters["action"]
                val form = call.receiveParameters()
                val response = when (action) {
                    "register" -> {
                        if (form.getAll("v_voucher") != listOf(request.voucher)) {
                            return@post call.respondText("验证凭据不匹配", status = HttpStatusCode.BadRequest)
                        }
                        GaiaCompanionApi.register(form)
                    }
                    "validate" -> GaiaCompanionApi.validate(form)
                    else -> return@post call.respondText("", status = HttpStatusCode.NotFound)
                }
                call.respondText(response, ContentType.Application.Json)
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
                fun field(name: String) = (obj[name] as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
                val answer = if ("vtoken" in obj) CaptchaAnswer.Gaia(field("vtoken"))
                    else CaptchaAnswer.Geetest(field("challenge"), field("validate"), field("seccode"))
                val ok = GeetestCompanionService.completeSession(id, answer)
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

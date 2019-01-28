import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*

class HttpBackend(private val engine: HttpClientEngine) {

    private suspend fun executeCall(call: HttpClientCall) {
        val (request, response) = engine.execute(call, requestData)
        call.request = request
        call.response = response
    }

    class Config {
        lateinit var engine: HttpClientEngine
    }

    companion object Feature : HttpClientFeature<Config, HttpBackend> {
        override val key: AttributeKey<HttpBackend>
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

        override fun prepare(block: Config.() -> Unit): HttpBackend = HttpBackend(Config().apply(block).engine)

        override fun install(feature: HttpBackend, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.Engine) { call ->
                if (call !is HttpClientCall) return@intercept

//                response.coroutineContext[Job]!!.invokeOnCompletion { cause ->
//                    @Suppress("UNCHECKED_CAST")
//                    val childContext = requestData.executionContext as CompletableDeferred<Unit>
//                    if (cause == null) childContext.complete(Unit) else childContext.completeExceptionally(cause)
//                }

//                val receivedCall = receivePipeline.execute(call, call.response).call
//                proceedWith(receivedCall)
            }
        }
    }
}

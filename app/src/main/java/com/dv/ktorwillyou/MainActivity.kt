package com.dv.ktorwillyou

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.chuckerteam.chucker.api.ChuckerInterceptor
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MainActivity : AppCompatActivity() {

    private var response = "empty"
    private lateinit var client: HttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SERVER
        lifecycleScope.launch(Dispatchers.IO) {
            embeddedServer(Netty, 8080) {
                install(ContentNegotiation) {
                    json()
                }
                routing {
                    get("/marry") {
                        call.respond(Message(response))
                    }
                    post("/respond") {
                        val r = call.receive<QuestionResponse>()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "Got response: ${r.response}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }.start(wait = true)
        }

        submitBtn.setOnClickListener {
            response = inputEt.text.toString()
        }


        // CLIENT
        client = HttpClient(OkHttp) {
            engine {
                addInterceptor(ChuckerInterceptor.Builder(this@MainActivity).build())
            }
            install(JsonFeature)
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KTOR", message)
                    }
                }
                level = LogLevel.ALL
            }
            install(UserAgent) {
                agent = "aaa"
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 30000
            }
            defaultRequest {
                header("Content-Type", "application/json")
                host = "localhost"
                port = 8080
            }
        }


        getBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val res = client.get<Message> {
                    url {
                        encodedPath = "/marry"
                    }
                }
                responseLbl.text = res.message
                withContext(Dispatchers.Main) {
                    sayYesBtn.isVisible = true
                }
            }
        }

        sayYesBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                client.post {
                    url {
                        host = "www.infinum.com"
                        encodedPath = "/hello"
                    }
                    url("www.infinum.com/hello")
                    body = QuestionResponse("yes")
                    headersOf("name" to listOf("value"), "name2" to listOf("value2"))
                    headers {
                        append("name", "value")
                    }
                }
            }
        }
    }
}

@Serializable
data class Message(
    @SerialName("message") val message: String
)

@Serializable
data class QuestionResponse(
    @SerialName("response") val response: String
)
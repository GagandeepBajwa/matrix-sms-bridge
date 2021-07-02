package net.folivo.matrix.bridge.sms.provider.voip

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirstOrNull
import net.folivo.matrix.bridge.sms.SmsBridgeProperties
import net.folivo.matrix.bridge.sms.handler.ReceiveSmsService
import net.folivo.matrix.bridge.sms.provider.SmsProvider
import net.folivo.matrix.bridge.sms.provider.voip.VoipInSmsMessagesResponse
import net.folivo.matrix.bridge.sms.provider.voip.VoipSmsProcessed
import net.folivo.matrix.bridge.sms.provider.voip.VoipSmsProvider
import net.folivo.matrix.core.model.events.m.room.message.NoticeMessageEventContent
import net.folivo.matrix.restclient.MatrixClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

class VoipSmsProvider(
    private val receiveSmsService: ReceiveSmsService,
    private val processedRepository: VoipSmsProcessedRepository,
    private val outSmsMessageRepository: VoipOutSmsMessageRepository,
    private val webClient: WebClient,
    private val matrixClient: MatrixClient,
    private val smsBridgeProperties: SmsBridgeProperties
) : SmsProvider {

    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }

    override suspend fun sendSms(receiver: String, body: String) {
        try{
            sendOutMessageRequest(receiver, body)
        }catch(error:Throwable){
            Log.error("could not send message to voip message gateway: ${error.message}");
        }
    }

    suspend fun sendOutFailedMessages() {
        if (outSmsMessageRepository.count() > 0L) {
            outSmsMessageRepository.findAll().collect {
                sendOutSmsMessageRequest(it.receiver, it.body)
                outSmsMessageRepository.delete(it)
            }
            if (smsBridgeProperties.defaultRoomId != null) {
                matrixClient.roomsApi.sendRoomEvent(
                    smsBridgeProperties.defaultRoomId,
                    NoticeMessageEventContent(smsBridgeProperties.templates.providerResendSuccess)
                )
            }
        }
    }

    private suspend fun sendOutMessageRequest(receiver: String, body:String){
        LOG.debug("start send out sms message via android")
        webClient.post().uri("/messages/out").bodyValue(message)
            .retrieve().toBodilessEntity().awaitFirstOrNull()
        LOG.debug("send out sms message via android was successful")
    }

    suspend fun getAndProcessNewMessages() {
        AndroidSmsProvider.LOG.debug("request new messages")
        val lastProcessed = processedRepository.findById(1)
        val response = webClient.get().uri {
            it.apply {
                path("/messages/in")
                if (lastProcessed != null) queryParam("after", lastProcessed.lastProcessedId)
            }.build()
        }.retrieve().awaitBody<AndroidInSmsMessagesResponse>()
        response.messages
            .sortedBy { it.id }
            .fold(lastProcessed, { process, message ->
                val answer = receiveSmsService.receiveSms(
                    message.body,
                    message.sender
                )
                try {
                    if (answer != null) sendSms(message.sender, answer)
                } catch (error: Throwable) {
                    VoipSmsProvider.LOG.error("could not answer ${message.sender} with message $answer. Reason: ${error.message}")
                    VoipSmsProvider.LOG.debug("details:", error)
                }
                processedRepository.save(
                    process?.copy(lastProcessedId = message.id)
                        ?: VoipSmsProcessed(1, message.id)
                )
            })
        VoipSmsProvider.LOG.debug("processed new messages")
    }
}


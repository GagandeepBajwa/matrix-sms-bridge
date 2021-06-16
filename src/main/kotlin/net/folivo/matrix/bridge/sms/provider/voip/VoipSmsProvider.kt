package net.folivo.matrix.bridge.sms.provider.voip

class VoipSmsProvider(
    private val receiveSmsService: ReceiveSmsService,
    private val processedRepository: AndroidSmsProcessedRepository,
    private val outSmsMessageRepository: AndroidOutSmsMessageRepository,
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
            LOg.error("could not send message to voip message gateway: ${error.message}");
        }
    }

    suspend fun sendOutFailedMessages() {
        if (outSmsMessageRepository.count() > 0L) {
            outSmsMessageRepository.findAll().collect {
                sendOutSmsMessageRequest(AndroidOutSmsMessageRequest(it.receiver, it.body))
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

    private suspend fun getAndProcessNewMessages(){
        LOG.debug("request new messages")
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
                    LOG.error("could not answer ${message.sender} with message $answer. Reason: ${error.message}")
                    LOG.debug("details:", error)
                }
                processedRepository.save(
                    process?.copy(lastProcessedId = message.id)
                        ?: AndroidSmsProcessed(1, message.id)
                )
            })
        LOG.debug("processed new messages")
    }
}


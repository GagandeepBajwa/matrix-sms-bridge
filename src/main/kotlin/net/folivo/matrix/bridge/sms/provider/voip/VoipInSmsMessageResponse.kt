package net.folivo.matrix.bridge.sms.provider.voip

import com.fasterxml.jackson.annotation.JsonProperty

data class VoipInSmsMessagesResponse(
    @JsonProperty("nextBatch")
    val nextBatch: Int,
    @JsonProperty("messages")
    val messages: List<VoipInSmsMessage>
)
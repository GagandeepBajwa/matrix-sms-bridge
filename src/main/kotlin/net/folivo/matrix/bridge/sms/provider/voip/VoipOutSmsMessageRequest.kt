package net.folivo.matrix.bridge.sms.provider.voip

import com.fasterxml.jackson.annotation.JsonProperty

data class VoipOutSmsMessageRequest(
    @JsonProperty("recipientPhoneNumber")
    val receiver: String,
    @JsonProperty("message")
    val body: String
)
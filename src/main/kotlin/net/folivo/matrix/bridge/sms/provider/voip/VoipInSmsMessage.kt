package net.folivo.matrix.bridge.sms.provider.voip

import com.fasterxml.jackson.annotation.JsonProperty

data class VoipInSmsMessage(
    @JsonProperty("number")
    val sender: String,
    @JsonProperty("body")
    val body: String,
    @JsonProperty
    val id: Int,
)
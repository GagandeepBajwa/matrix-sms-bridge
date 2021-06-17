package net.folivo.matrix.bridge.sms.provider.voip

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

ConfigurationProperties("matrix.bridge.sms.provider.voip")
@ConstructorBinding
data class VoipSmsProviderProperties(
    val enabled: Boolean = false,
    val baseUrl: String,
    val username: String,
    val password: String,
    val trustStore: TrustStore? = null
) {
    data class TrustStore(
        val path: String,
        val password: String,
        val type: String
    )
}
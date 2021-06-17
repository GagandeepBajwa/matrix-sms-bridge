package net.folivo.matrix.bridge.sms.provider.voip

import org.springframework.http.HttpStatus

class VoipSmsProviderException(message: String?, val status: HttpStatus) : Throwable(message)
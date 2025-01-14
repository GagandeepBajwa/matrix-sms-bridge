package net.folivo.matrix.bridge.sms.provider.voip

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.folivo.matrix.bridge.sms.SmsBridgeProperties
import net.folivo.matrix.core.model.events.m.room.message.NoticeMessageEventContent
import net.folivo.matrix.restclient.MatrixClient
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener

class VoipSmsProviderLauncher(
    private val voipSmsProvider: VoipSmsProvider,
    private val smsBridgeProperties: SmsBridgeProperties,
    private val matrixClient: MatrixClient)
    {
        companion object {
            private val LOG = LoggerFactory.getLogger(this::class.java)
        }

        @EventListener(ApplicationReadyEvent::class)
        fun startReceiveLoop(): Job {
            return GlobalScope.launch {
                while (true) {
                    retry(binaryExponentialBackoff(base = 5000, max = 60000) + logReceiveAttempt()) {
                        voipSmsProvider.getAndProcessNewMessages()
                        delay(5000)
                    }
                }
            }
        }

        @EventListener(ApplicationReadyEvent::class)
        fun startRetrySendLoop(): Job {
            return GlobalScope.launch {
                while (true) {
                    retry(binaryExponentialBackoff(base = 10000, max = 120000) + logSendAttempt()) {
                        voipSmsProvider.sendOutFailedMessages()
                        delay(10000)
                    }
                }
            }
        }

        private fun logReceiveAttempt(): RetryPolicy<Throwable> {
            return {
                LOG.error("could not retrieve messages from voip or process them: ${reason.message}")
                LOG.debug("detailed error", reason)
                try {
                    if (smsBridgeProperties.defaultRoomId != null)
                        matrixClient.roomsApi.sendRoomEvent(
                            smsBridgeProperties.defaultRoomId,
                            NoticeMessageEventContent(
                                smsBridgeProperties.templates.providerReceiveError
                                    .replace("{error}", reason.message ?: "unknown")
                            )
                        )
                } catch (error: Throwable) {
                    LOG.error("could not warn user in default room: ${error.message}")
                }
                ContinueRetrying
            }
        }


        private fun logSendAttempt(): RetryPolicy<Throwable> {
            return {
                LOG.error("could not send messages to voip: ${reason.message}")
                LOG.debug("detailed error", reason)
                ContinueRetrying
            }
        }


    }
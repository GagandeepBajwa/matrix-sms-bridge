package net.folivo.matrix.bridge.sms.provider.voip

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VoipSmsProcessedRepository : CoroutineCrudRepository<VoipSmsProcessed, Long> {
}
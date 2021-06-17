package net.folivo.matrix.bridge.sms.provider.voip

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("voip_sms_processed")
data class VoipSmsProcessed(
    @Id
    @Column("id")
    val id: Long,
    @Column("last_processed_id")
    var lastProcessedId: Int,
    @Version
    @Column("version")
    var version: Int = 0
)
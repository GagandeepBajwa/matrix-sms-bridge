package net.folivo.matrix.bridge.sms.provider.voip

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import kotlinx.coroutines.cancelAndJoin
import net.folivo.matrix.bridge.sms.SmsBridgeProperties
import net.folivo.matrix.core.model.MatrixId.EventId
import net.folivo.matrix.core.model.MatrixId.RoomId
import net.folivo.matrix.core.model.events.m.room.message.NoticeMessageEventContent
import net.folivo.matrix.restclient.MatrixClient

class VoipProviderLauncherTest : DescribeSpec(testBody())

private fun testBody(): DescribeSpec.() -> Unit {
    return {
        val voipSmsProviderMock: VoipSmsProvider = mockk()
        val smsBridgePropertiesMock: SmsBridgeProperties = mockk {
            every { templates.providerReceiveError }.returns("error {error}")
        }
        val matrixClientMock: MatrixClient = mockk {
            coEvery { roomsApi.sendRoomEvent(any(), any(), any(), any(), any()) }
                .returns(EventId("event", "server"))
        }

        val cut = AndroidSmsProviderLauncher(voipSmsProviderMock, smsBridgePropertiesMock, matrixClientMock)

        describe(AndroidSmsProviderLauncher::startReceiveLoop.name) {
            describe("on error") {
                describe("default room given") {
                    val defaultRoom = RoomId("default", "server")
                    beforeTest {
                        every { smsBridgePropertiesMock.defaultRoomId }.returns(defaultRoom)
                    }
                    it("should notify default room") {
                        coEvery { voipSmsProviderMock.getAndProcessNewMessages() }
                            .throws(RuntimeException("meteor"))
                        val job = cut.startReceiveLoop()
                        coVerify(timeout = 100) {
                            matrixClientMock.roomsApi.sendRoomEvent(
                                defaultRoom, match<NoticeMessageEventContent> { it.body == "error meteor" },
                                any(), any(), any()
                            )
                        }
                        job.cancelAndJoin()
                    }
                }
                describe("default room not given") {
                    beforeTest {
                        every { smsBridgePropertiesMock.defaultRoomId }.returns(null)
                    }
                    it("should not notify default room") {
                        coEvery { voipSmsProviderMock.getAndProcessNewMessages() }
                            .throws(RuntimeException("meteor"))
                        val job = cut.startReceiveLoop()
                        coVerify(exactly = 0, timeout = 100) {
                            matrixClientMock.roomsApi.sendRoomEvent(any(), any(), any(), any(), any())
                        }
                        job.cancelAndJoin()
                    }
                }
            }
        }

        afterTest { clearMocks(smsBridgePropertiesMock, voipSmsProviderMock, matrixClientMock) }
    }
}
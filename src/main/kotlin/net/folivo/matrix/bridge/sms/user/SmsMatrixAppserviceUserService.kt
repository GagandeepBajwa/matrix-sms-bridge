package net.folivo.matrix.bridge.sms.user

import net.folivo.matrix.appservice.api.user.CreateUserParameter
import net.folivo.matrix.appservice.api.user.MatrixAppserviceUserService
import net.folivo.matrix.bot.appservice.MatrixAppserviceServiceHelper
import reactor.core.publisher.Mono

class SmsMatrixAppserviceUserService(
        private val helper: MatrixAppserviceServiceHelper,
        private val appserviceUserRepository: AppserviceUserRepository
) : MatrixAppserviceUserService {

    override fun userExistingState(userId: String): Mono<MatrixAppserviceUserService.UserExistingState> {
        return appserviceUserRepository.existsById(userId)
                .flatMap { isInDatabase ->
                    if (isInDatabase) {
                        Mono.just(MatrixAppserviceUserService.UserExistingState.EXISTS)
                    } else {
                        helper.shouldCreateUser(userId)
                                .map { shouldCreateUser ->
                                    if (shouldCreateUser) {
                                        MatrixAppserviceUserService.UserExistingState.CAN_BE_CREATED
                                    } else {
                                        MatrixAppserviceUserService.UserExistingState.DOES_NOT_EXISTS
                                    }
                                }
                    }
                }
    }

    override fun getCreateUserParameter(userId: String): Mono<CreateUserParameter> {
        return Mono.create {
            val telephoneNumber = userId.removePrefix("@sms_").substringBefore(":")
            val displayName = "+$telephoneNumber (SMS)"
            it.success(CreateUserParameter(displayName))
        }
    }

    override fun saveUser(userId: String): Mono<Void> {
        return appserviceUserRepository.save(AppserviceUser(userId))
                .then()
    }
}
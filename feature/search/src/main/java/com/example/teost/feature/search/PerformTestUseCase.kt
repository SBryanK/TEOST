package com.example.teost.feature.search

import com.example.teost.core.domain.model.TestRequest
import com.example.teost.core.domain.model.TestResponse
import com.example.teost.data.repository.ConnectionTestRepository
import com.example.teost.util.AppException
import com.example.teost.util.Result
import com.example.teost.util.UrlValidator
import com.example.teost.util.toAppException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

class PerformTestUseCase @Inject constructor(
    private val repository: ConnectionTestRepository
) {
    suspend operator fun invoke(url: String): Flow<Result<TestResponse>> = flow {
        emit(Result.Loading)
        when (val validation = UrlValidator.validate(url)) {
            is UrlValidator.ValidationResult.Valid -> {
                val request = TestRequest(id = UUID.randomUUID().toString(), url = validation.normalized)
                val repoResult = repository.performTest(request)
                repoResult.onSuccess { response ->
                    emit(Result.Success(response))
                }.onFailure { throwable ->
                    emit(Result.Error(throwable.toAppException()))
                }
            }
            is UrlValidator.ValidationResult.Invalid -> {
                emit(Result.Error(AppException.ValidationException(validation.reason)))
            }
        }
    }
}



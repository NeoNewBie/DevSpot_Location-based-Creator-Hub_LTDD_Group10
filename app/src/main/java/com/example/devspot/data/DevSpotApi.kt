package com.example.devspot.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String,
    val githubUrl: String? = null,
    val companyName: String? = null
)

data class LoginResponse(
    val userId: Int,
    val email: String,
    val fullName: String,
    val role: String
)

interface DevSpotApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>
}

package com.ng.s33986010.medtrack.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// OpenFDA

data class FdaResponse(val results: List<DrugLabelResult> = emptyList())

data class DrugLabelResult(
    val purpose: List<String>? = null,
    val warnings: List<String>? = null,
    @SerializedName("dosage_and_administration") val dosageAndAdministration: List<String>? = null,
    @SerializedName("active_ingredient") val activeIngredient: List<String>? = null,
    @SerializedName("indications_and_usage") val indicationsAndUsage: List<String>? = null,
    @SerializedName("storage_and_handling") val storageAndHandling: List<String>? = null,
    val openfda: OpenFdaMeta? = null
)

data class OpenFdaMeta(
    @SerializedName("brand_name") val brandName: List<String>? = null,
    @SerializedName("generic_name") val genericName: List<String>? = null,
    @SerializedName("manufacturer_name") val manufacturerName: List<String>? = null
)

interface OpenFdaApi {
    @GET("label.json")
    suspend fun searchDrug(
        @Query("search") search: String,
        @Query("limit") limit: Int = 1
    ): FdaResponse
}

// Gemini

data class GeminiRequest(val contents: List<GeminiContent>)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiPart(val text: String)

data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())
data class GeminiCandidate(val content: GeminiContent? = null)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.1-flash-lite:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}

// Retrofit Client

object RetrofitClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val fdaApi: OpenFdaApi = Retrofit.Builder()
        .baseUrl("https://api.fda.gov/drug/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenFdaApi::class.java)

    val geminiApi: GeminiApi = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)
}

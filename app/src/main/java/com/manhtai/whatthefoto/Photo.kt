package com.manhtai.whatthefoto

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

data class Photo(
    var url: String,
)


class DynamicFieldAdapter(private val imagePath: String) : JsonAdapter<Photo?>() {

    @FromJson
    override fun fromJson(reader: JsonReader): Photo? {
        if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
            throw JsonDataException("Expected an object but was ${reader.peek()}")
        }

        reader.beginObject()

        var url = ""

        while (reader.hasNext()) {
            val fieldName = reader.nextName()
            if (fieldName == imagePath) {
                url = reader.nextString()
            } else {
                reader.skipValue()
            }
        }

        reader.endObject()

        if (url == "") {
            return null
        }
        return Photo(url = url)
    }


    @ToJson
    override fun toJson(writer: JsonWriter, value: Photo?) {
        throw UnsupportedOperationException("This adapter only supports deserialization.")
    }
}


interface PhotoApiService {
    @GET
    fun getPhotos(@Url fullUrl: String): Call<List<Photo?>>
}


class PhotoApi(imagePath: String = "url") {
    private val adapter = Moshi.Builder().add(DynamicFieldAdapter(imagePath)).build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://manhtai.com/")
        .addConverterFactory(MoshiConverterFactory.create(adapter))
        .build()

    val service: PhotoApiService = retrofit.create(PhotoApiService::class.java)
}
package com.example.android.politicalpreparedness.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.models.Election
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ElectionRepository(database: ElectionDatabase) {
    val upcomingElections = MutableLiveData<List<Election>>()

    suspend fun fetchUpcomingElections() {
        withContext(Dispatchers.IO) {
            try {
                val electionResponse = CivicsApi.retrofitService.getElections()
                upcomingElections.postValue(electionResponse.elections)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

    }
}
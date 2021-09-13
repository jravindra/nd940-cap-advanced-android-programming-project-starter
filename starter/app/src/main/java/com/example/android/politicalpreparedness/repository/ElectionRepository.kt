package com.example.android.politicalpreparedness.repository

import androidx.lifecycle.MutableLiveData
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.network.models.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ElectionRepository(private val database: ElectionDatabase) {
    val upcomingElections = MutableLiveData<List<Election>>()
    val state = MutableLiveData<State>()

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

    suspend fun fetchVoterInfo(address: String, electionId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val voterResponse = CivicsApi.retrofitService.getVoterInfo(address, electionId)
                state.postValue(voterResponse.state?.get(0))
            } catch (e: java.lang.Exception) {
                Timber.e(e)
            }
        }
    }

    suspend fun followElection(election: Election) {
        withContext(Dispatchers.IO) {
            database.electionDao.insertElection(election)
        }
    }

    suspend fun unfollowElection(election: Election) {
        withContext(Dispatchers.IO) {
            database.electionDao.deleteElection(election.id)
        }
    }

    suspend fun isElectionSaved(election: Election): Boolean {
        var eDB: Election? = null
        withContext(Dispatchers.IO) {
            eDB = database.electionDao.getElection(election.id)
        }
        return eDB == election
    }
}
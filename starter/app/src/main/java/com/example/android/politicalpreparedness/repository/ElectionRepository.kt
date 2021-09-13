package com.example.android.politicalpreparedness.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.network.models.State
import com.example.android.politicalpreparedness.representative.model.Representative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ElectionRepository(private val database: ElectionDatabase) {
    val savedElections: LiveData<List<Election>> = database.electionDao.getElections()
    val upcomingElections = MutableLiveData<List<Election>>()
    val state = MutableLiveData<State>()
    val representatives = MutableLiveData<List<Representative>>()

    suspend fun fetchUpcomingElections() {
        withContext(Dispatchers.IO) {
            val electionResponse = CivicsApi.retrofitService.getElections()
            upcomingElections.postValue(electionResponse.elections)
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

    suspend fun fetchRepresentatives(address: String) {
        withContext(Dispatchers.IO) {
            try {
                val (offices, officials) = CivicsApi.retrofitService.getRepresentatives(address)
                representatives.postValue(offices.flatMap { office -> office.getRepresentatives(officials) })
            } catch (e: Exception) {
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
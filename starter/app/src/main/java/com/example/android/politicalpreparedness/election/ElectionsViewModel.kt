package com.example.android.politicalpreparedness.election

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.repository.ElectionRepository
import com.example.android.politicalpreparedness.utils.isNetworkReachable
import kotlinx.coroutines.launch
import java.io.IOException

//TODO: Construct ViewModel and provide election datasource
class ElectionsViewModel(private val repository: ElectionRepository, application: Application) :
    AndroidViewModel(application) {
    val upcommingElections = repository.upcomingElections

    // List of variables to check network progress and navigation
    private val _isNetworkAvailable = MutableLiveData<Boolean>(false)
    val isNetworkAvailable: LiveData<Boolean>
        get() = _isNetworkAvailable

    private val _isNetworkFetchError = MutableLiveData<Boolean>(true)
    val isNetworkFetchError: LiveData<Boolean>
        get() = _isNetworkFetchError


    private val _navigateToVoterInfo = MutableLiveData<Election>()
    val navigateToVoterInfo: LiveData<Election>
        get() = _navigateToVoterInfo

    init {
        if (isNetworkReachable(application)) {
            true.also { _isNetworkAvailable.value = it }
            fetchUpcomingElections()
        } else {
            false.also { _isNetworkAvailable.value = it }
        }
    }

    fun onElectionClicked(election: Election) {
        _navigateToVoterInfo.value = election
    }

    fun navigateToVoterInfoComplete() {
        _navigateToVoterInfo.value = null
    }


    private fun fetchUpcomingElections() {
        viewModelScope.launch {
            try {
                repository.fetchUpcomingElections()
                _isNetworkFetchError.value = false
            } catch (e: IOException) {
                if (upcommingElections.value.isNullOrEmpty()) {
                    _isNetworkFetchError.value = true
                }
            }
        }
    }


}
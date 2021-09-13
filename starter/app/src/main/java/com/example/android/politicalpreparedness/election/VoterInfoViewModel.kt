package com.example.android.politicalpreparedness.election

import androidx.lifecycle.*
import com.example.android.politicalpreparedness.database.ElectionDao
import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.repository.ElectionRepository
import kotlinx.coroutines.launch
import java.io.IOException

class VoterInfoViewModel(
    private val repository: ElectionRepository,
    private val followString: String,
    private val unfollowString: String,
    private val election: Election
) : ViewModel() {

    private val _voterInfoFetched = MutableLiveData<Boolean>(false)
    val voterInfoFetched: LiveData<Boolean>
        get() = _voterInfoFetched

    private val _errorOnFetchingNetworkData = MutableLiveData<Boolean>(false)
    val errorOnFetchingNetworkData: LiveData<Boolean>
        get() = _errorOnFetchingNetworkData

    private val _electionInfoUrl = MutableLiveData<String>()
    val electionInfoUrl: LiveData<String>
        get() = _electionInfoUrl


    private val _buttonText = MutableLiveData<String>()
    val buttonText: LiveData<String>
        get() = _buttonText


    init {
        viewModelScope.launch {
            toggleButtonText()
            getVoterInfo()
        }
    }

    private val _votingLocationFinderUrl = MutableLiveData<String>()
    val votingLocationFinderUrl: LiveData<String>
        get() = _votingLocationFinderUrl


    fun openElectionInformationUrl() {
        _votingLocationFinderUrl.value = repository.state.value?.electionAdministrationBody?.electionInfoUrl
    }

    fun onOpenElectionInformationUrlComplete() {
        _votingLocationFinderUrl.value = null
    }

    fun openVotingLocationsUrl() {
        _votingLocationFinderUrl.value = repository.state.value?.electionAdministrationBody?.votingLocationFinderUrl
    }

    fun onOpenVotingLocationsUrlComplete() {
        _votingLocationFinderUrl.value = null
    }


    private val _ballotInfoUrl = MutableLiveData<String>()
    val ballotInfoUrl: LiveData<String>
        get() = _ballotInfoUrl

    fun openBallotInfoUrl() {
        _ballotInfoUrl.value = repository.state.value?.electionAdministrationBody?.ballotInfoUrl
    }

    fun onOpenBallotInfoUrlComplete() {
        _ballotInfoUrl.value = null
    }

    fun displayNetworkErrorComplete() {
        _errorOnFetchingNetworkData.value = false
    }

    private suspend fun getVoterInfo() {
        try {
            _voterInfoFetched.value = false
            repository.fetchVoterInfo(election.division.state, election.id)
            _errorOnFetchingNetworkData.value = false
            _voterInfoFetched.value = true
        } catch (networkError: IOException) {
            if (repository.state.value == null) {
                _errorOnFetchingNetworkData.value = true
            }
        }
    }

    private suspend fun toggleButtonText() {
        if (repository.isElectionSaved(election)) {
            _buttonText.value = unfollowString
        } else {
            _buttonText.value = followString
        }
    }

    fun toggleFollowElection() {
        viewModelScope.launch {
            if (repository.isElectionSaved(election)) {
                repository.unfollowElection(election)
            } else {
                repository.followElection(election)
            }
            toggleButtonText()
        }
    }

    val hasElectionInformation = Transformations.map(repository.state) { state ->
        state.electionAdministrationBody.electionInfoUrl != null
    }

    val hasVotingLocationsInformation = Transformations.map(repository.state) { state ->
        state.electionAdministrationBody.votingLocationFinderUrl != null
    }

    val hasBallotInformation = Transformations.map(repository.state) { state ->
        state.electionAdministrationBody.ballotInfoUrl != null
    }

    val hasCorrespondenceInformation = Transformations.map(repository.state) { state ->
        state.electionAdministrationBody.correspondenceAddress != null
    }

    val correspondenceAddress = Transformations.map(repository.state) { state ->
        state.electionAdministrationBody.correspondenceAddress?.toFormattedString()
    }


}
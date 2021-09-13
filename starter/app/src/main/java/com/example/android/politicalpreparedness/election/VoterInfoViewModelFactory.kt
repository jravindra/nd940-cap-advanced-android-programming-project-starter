package com.example.android.politicalpreparedness.election

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.repository.ElectionRepository

class VoterInfoViewModelFactory(
    private val repository: ElectionRepository,
    private val followString: String,
    private val unfollowString: String,
    private val election: Election
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoterInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VoterInfoViewModel(repository, followString, unfollowString, election) as T
        }
        throw IllegalArgumentException("Unable to construct viewmodel")
    }
}
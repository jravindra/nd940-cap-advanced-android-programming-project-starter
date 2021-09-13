package com.example.android.politicalpreparedness.representative

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.repository.ElectionRepository
import com.example.android.politicalpreparedness.utils.isNetworkReachable
import kotlinx.coroutines.launch
import java.io.IOException

class RepresentativeViewModel(private val repository: ElectionRepository, application: Application) :
    AndroidViewModel(application) {

    val representatives = repository.representatives

    var line1 = MutableLiveData<String>()

    var line2 = MutableLiveData<String>()

    var city = MutableLiveData<String>()

    var state = MutableLiveData<String>()

    var zip = MutableLiveData<String>()

    private val _address = MutableLiveData<Address>(Address("", "", "", "", ""))
    val address: LiveData<Address>
        get() = _address

    private val _errorOnFetchingNetworkData = MutableLiveData<Boolean>(false)
    val errorOnFetchingNetworkData: LiveData<Boolean>
        get() = _errorOnFetchingNetworkData

    private val _networkNotAvailable = MutableLiveData<Boolean>()
    val networkNotAvailable: LiveData<Boolean>
        get() = _networkNotAvailable

    private val _representativesFetched = MutableLiveData<Boolean>(true)
    val representativesFetched: LiveData<Boolean>
        get() = _representativesFetched

    init {
        _networkNotAvailable.value = !isNetworkReachable(getApplication())
    }

    private fun getRepresentatives(address: String) {
        if (isNetworkReachable(getApplication())) {
            viewModelScope.launch {
                try {
                    _representativesFetched.value = false
                    repository.fetchRepresentatives(address)
                    _errorOnFetchingNetworkData.value = false
                    _representativesFetched.value = true
                } catch (networkError: IOException) {
                    if (repository.representatives.value == null) {
                        _errorOnFetchingNetworkData.value = true
                    }
                }
            }
        } else {
            _networkNotAvailable.value = true
            _errorOnFetchingNetworkData.value = true
        }
    }

    fun displayNetworkErrorComplete() {
        _errorOnFetchingNetworkData.value = false
    }

    fun onFindMyRepresentativesClicked() {
        val line1 = line1.value
        val line2 = line2.value
        val city = city.value
        val state = address.value?.state
        val zip = zip.value

        val addr = line1?.let {
            city?.let { it1 ->
                state?.let { it2 ->
                    zip?.let { it3 ->
                        Address(
                            it, line2, it1, it2,
                            it3
                        )
                    }
                }
            }
        }
        addr?.let { getRepresentatives(it.toFormattedString()) }
//        _address.value?.let { getRepresentatives(it.toFormattedString()) }
    }

    fun onUseMyLocationClicked(address: Address) {
        line1.value = address.line1
        if (address.line2.isNullOrEmpty())
            line2.value = address.line2!!
        city.value = address.city
        state.value = address.state
        zip.value = address.zip
        _address.value = address
        getRepresentatives(address.toFormattedString())
    }
}

package com.example.android.politicalpreparedness.election

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.databinding.FragmentElectionBinding
import com.example.android.politicalpreparedness.election.adapter.ElectionListAdapter
import com.example.android.politicalpreparedness.repository.ElectionRepository

class ElectionsFragment : Fragment() {

    //Declare ViewModel
    private lateinit var electionsViewModel: ElectionsViewModel
    private lateinit var upcomingElectionsListAdapter: ElectionListAdapter
    private lateinit var savedElectionsListAdapter: ElectionListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireNotNull(this.activity).application
        val database = ElectionDatabase.getInstance(requireContext())
        val repository = ElectionRepository(database)
        val viewModelFactory = ElectionsViewModelFactory(repository, application)
        electionsViewModel = ViewModelProvider(this, viewModelFactory).get(ElectionsViewModel::class.java)

        val binding = FragmentElectionBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.electionViewModel = electionsViewModel

        upcomingElectionsListAdapter = ElectionListAdapter(ElectionListAdapter.ElectionListener { election ->
            electionsViewModel.onElectionClicked(election)
        }, "Upcoming Elections")

        savedElectionsListAdapter = ElectionListAdapter(ElectionListAdapter.ElectionListener { election ->
            electionsViewModel.onElectionClicked(election)
        }, "Saved Elections")


        binding.savedElectionsRecycler.adapter = savedElectionsListAdapter
        binding.upcomingElectionsRecycler.adapter = upcomingElectionsListAdapter

        // observers
        electionsViewModel.upcommingElections.observe(viewLifecycleOwner, Observer {
            it?.apply {
                upcomingElectionsListAdapter.addHeaderAndSubmitList(it)
            }
        })

        // observers
        electionsViewModel.savedElections.observe(viewLifecycleOwner, Observer { savedElections ->
            savedElections?.apply {
                savedElectionsListAdapter.addHeaderAndSubmitList(savedElections)
                if (savedElections.isNotEmpty()) {
                    binding.noDataHint.visibility = View.GONE
                } else {
                    binding.noDataHint.visibility = View.VISIBLE
                }
            }
        })

        electionsViewModel.navigateToVoterInfo.observe(viewLifecycleOwner) { election ->
            election?.let {
                this.findNavController().navigate(
                    ElectionsFragmentDirections.actionElectionsFragmentToVoterInfoFragment(
                        election,
                        election.division
                    )
                )
                electionsViewModel.navigateToVoterInfoComplete()
            }
        }



        return binding.root
    }
}
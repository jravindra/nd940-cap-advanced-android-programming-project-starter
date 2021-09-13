package com.example.android.politicalpreparedness.utils

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.network.models.Election

@BindingAdapter("electionDateText")
fun TextView.setElectionDate(item: Election) {
    item.let {
        text = item.electionDay.toSimpleString()
    }
}

@BindingAdapter("viewVisibility")
fun setVisibility(view: View, isVisible: Boolean) {
    view.visibility = if (isVisible) View.VISIBLE else View.GONE
}

@BindingAdapter("customText")
fun TextView.customText(hasInformation: Boolean) {
    if (!hasInformation) {
        text = resources.getString(R.string.unknown_address)
    }
}
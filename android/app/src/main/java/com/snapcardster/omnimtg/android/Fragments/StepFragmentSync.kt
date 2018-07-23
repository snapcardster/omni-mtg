package com.snapcardster.omnimtg.android.Fragments

import com.stepstone.stepper.VerificationError
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.snapcardster.omnimtg.android.R
import com.stepstone.stepper.Step


class StepFragmentSync : StepFragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        //initialize your UI

        return inflater!!.inflate(R.layout.fragment_step_sync, container, false)
    }

    override fun verifyStep(): VerificationError? {
        //return null if the user can go to the next step, create a new VerificationError instance otherwise
        return null
    }

    override fun onSelected() {
        //update UI when selected
    }

    override fun onError(error: VerificationError) {
        //handle error inside of the fragment, e.g. show error on EditText
    }

    companion object {
        fun newInstance() = StepFragmentSync()
        val tabTitle = "Sync"
    }
}
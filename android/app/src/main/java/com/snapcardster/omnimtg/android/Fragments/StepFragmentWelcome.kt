package com.snapcardster.omnimtg.android.Fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.snapcardster.omnimtg.android.MainActivity
import com.snapcardster.omnimtg.android.MainActivity.Companion.controller
import com.snapcardster.omnimtg.android.MainActivity.Companion.firstRun
import com.snapcardster.omnimtg.android.R
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_step_welcome.view.*


class StepFragmentWelcome : StepFragment() {


    val tabTitle = "Explanation"

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        //initialize your UI
        val view = inflater!!.inflate(R.layout.fragment_step_welcome, container, false)

        view.card_about_text.movementMethod = LinkMovementMethod.getInstance()

        return view
    }

    override fun verifyStep(): VerificationError? {
        //return null if the user can go to the next step, create a new VerificationError instance otherwise
        return null
    }

    override fun onSelected() {
        //update UI when selected
        if ((firstRun || controller.running.value) && !MainActivity.controller.mkmAppToken.value.isNullOrBlank() &&
                !MainActivity.controller.mkmAppSecret.value.isNullOrBlank() &&
                !MainActivity.controller.mkmAccessTokenSecret.value.isNullOrBlank() &&
                !MainActivity.controller.mkmAccessToken.value.isNullOrBlank()) {
            firstRun = true
            activity.stepperLayout.proceed()
        }
    }
}
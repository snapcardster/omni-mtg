package com.snapcardster.omnimtg.android.Fragments

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.snapcardster.omnimtg.android.MainActivity
import com.snapcardster.omnimtg.android.R
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_step_wifi.view.*


class StepFragmentWifi : StepFragment() {

    var position = -1

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //initialize your UI
        val view = inflater!!.inflate(R.layout.fragment_step_wifi, container, false)

        view.card_wifi_btn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS));
        }

        return view
    }

    override fun verifyStep(): VerificationError? {
        return null
    }

    override fun onSelected() {
        if (MainActivity.firstRun && !MainActivity.controller.mkmAppToken.value.isNullOrBlank() &&
                !MainActivity.controller.mkmAppSecret.value.isNullOrBlank() &&
                !MainActivity.controller.mkmAccessTokenSecret.value.isNullOrBlank() &&
                !MainActivity.controller.mkmAccessToken.value.isNullOrBlank()) {
            activity.stepperLayout.proceed()
        }
    }
}
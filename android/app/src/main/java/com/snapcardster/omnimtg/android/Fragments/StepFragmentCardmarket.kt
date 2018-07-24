package com.snapcardster.omnimtg.android.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.snapcardster.omnimtg.android.MainActivity.Companion.controller
import com.snapcardster.omnimtg.android.R
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.fragment_step_cardmarket.view.*
import org.jetbrains.anko.support.v4.toast


class StepFragmentCardmarket : StepFragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        //initialize your UI
        val view = inflater!!.inflate(R.layout.fragment_step_cardmarket, container, false)

        bind(controller.mkmAccessToken, view.card_cardmarket_details_accesstoken)
        bind(controller.mkmAccessTokenSecret, view.card_cardmarket_details_accesstokensecret)
        bind(controller.mkmAppSecret, view.card_cardmarket_details_appsecret)
        bind(controller.mkmAppToken, view.card_cardmarket_details_apptoken)

        return view
    }

    override fun verifyStep(): VerificationError? {
        //return null if the user can go to the next step, create a new VerificationError instance otherwise
        if (controller.mkmAccessToken.value.isNullOrBlank() || controller.mkmAccessTokenSecret.value.isNullOrBlank() || controller.mkmAppSecret.value.isNullOrBlank() || controller.mkmAppToken.value.isNullOrBlank()) {
            toast("MKM Credentials not set")
            return VerificationError("MKM Credentials not set")
        } else {
            return null
        }
    }

    override fun onSelected() {
        //update UI when selected
    }
}
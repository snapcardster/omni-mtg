package com.snapcardster.omnimtg.android.Adapter

import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentManager
import com.snapcardster.omnimtg.android.Fragments.*
import com.stepstone.stepper.Step
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter
import com.stepstone.stepper.viewmodel.StepViewModel


class StepperAdapter(fm: FragmentManager, context: Context) : AbstractFragmentStepAdapter(fm, context) {

    val CURRENT_STEP_POSITION_KEY = "CURRENT_STEP_POSITION_KEY"


    override fun createStep(position: Int): Step {
        val step = when (position) {
            0 -> StepFragmentWelcome()
            1 -> StepFragmentWifi()
            2 -> StepFragmentCardmarket()
            3 -> StepFragmentSnapcardster()
            4 -> StepFragmentSync()
            else -> StepFragmentWelcome()
        }
        val b = Bundle()
        b.putInt(CURRENT_STEP_POSITION_KEY, position)
        step.arguments = b
        return step as Step
    }

    override fun getCount(): Int {
        return 5
    }

    override fun getViewModel(position: Int): StepViewModel {
        //Override this method to set Step title for the Tabs, not necessary for other stepper types
        return StepViewModel.Builder(context)
                .setTitle(when (position) {
                    0 -> "Welcome"
                    1 -> "WiFi"
                    2 -> "CardMarket"
                    3 -> "Snapcardster"
                    4 -> "Sync"
                    else -> "Welcome"
                }) //can be a CharSequence instead
                .setEndButtonLabel(when (position) {
                    4 -> ""
                    else -> "Continue"
                })
                .create()
    }
}
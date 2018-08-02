package com.snapcardster.omnimtg.android.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.snapcardster.omnimtg.android.AndroidIntegerPropertyListener
import com.snapcardster.omnimtg.android.MainActivity.Companion.controller
import com.snapcardster.omnimtg.android.MainActivity.Companion.firstRun
import com.snapcardster.omnimtg.android.R
import com.stepstone.stepper.BlockingStep
import com.stepstone.stepper.StepperLayout
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.fragment_step_sync.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class StepFragmentSync : StepFragment(), BlockingStep {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        //initialize your UI
        val view = inflater!!.inflate(R.layout.fragment_step_sync, container, false)

        controller.interval.addListener(object : AndroidIntegerPropertyListener {
            override fun onChanged(oldValue: Int?, newValue: Int?, callListener: Boolean?) {
                doAsync { uiThread { view.card_sync_settings_txt.setText("Sync every $newValue seconds, ${(24 * 60 * 60) / newValue!!} times per day\nNext Sync in ${controller.getnextSync().value} seconds") } }
            }
        })
        controller.getnextSync().addListener(object : AndroidIntegerPropertyListener {
            override fun onChanged(oldValue: Int?, newValue: Int?, callListener: Boolean?) {
                doAsync { uiThread { controller.interval.value.let { view.card_sync_settings_txt.setText("Sync every ${it} seconds, ${(24 * 60 * 60) / it} times per day\nNext Sync in ${newValue} seconds") } } }
            }
        })

        controller.interval.value = controller.interval.value // Call Listener to set Text
        view.card_sync_settings_slider.progress = controller.interval.value

        view.card_sync_settings_slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2) {
                    controller.interval.value = p1 + 10
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                controller.save(activity)
            }

        })

        bind(controller.output, view.card_sync_output_log)

        return view
    }

    override fun verifyStep(): VerificationError? {
        //return null if the user can go to the next step, create a new VerificationError instance otherwise
        return null
    }

    override fun onSelected() {
        controller.save(activity)
        if (!controller.running.value) {
            controller.start(activity)
        }
        firstRun = false
    }

    override fun onCompleteClicked(callback: StepperLayout.OnCompleteClickedCallback) {
    }

    override fun onBackClicked(callback: StepperLayout.OnBackClickedCallback?) {

    }

    override fun onNextClicked(callback: StepperLayout.OnNextClickedCallback?) {

    }

}
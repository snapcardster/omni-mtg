package com.snapcardster.omnimtg.android.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.snapcardster.omnimtg.android.MainActivity.Companion.controller
import com.snapcardster.omnimtg.android.MainActivity.Companion.firstRun
import com.snapcardster.omnimtg.android.R
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_step_snapcardster.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread


class StepFragmentSnapcardster : StepFragment() {

    var position = -1

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("StepSnapcardster","onCreate")

        //initialize your UI
        val view = inflater!!.inflate(R.layout.fragment_step_snapcardster, container, false)
        bind(controller.snapUser, view.card_snapcardster_login_user, listOf(controller.snapToken))
        bind(controller.snapPassword, view.card_snapcardster_login_password)

        view.card_snapcardster_login_btn.setOnClickListener {
            view.card_snapcardster_login_progress.visibility = View.VISIBLE
            view.card_snapcardster_login_btn.visibility = View.GONE
            doAsync {
                controller.loginSnap()
                uiThread {
                    view.card_snapcardster_login_progress.visibility = View.GONE
                    view.card_snapcardster_login_btn.visibility = View.VISIBLE
                    if (controller.snapToken.value.isNullOrBlank()) {
                        toast("Login failed, please check username and password")
                    } else {
                        view.card_snapcardster_login_btn.text = "Login with different Account"
                        if (activity.stepperLayout.currentStepPosition == position) {
                            activity.stepperLayout.proceed()
                        }
                    }
                }
            }

        }

        return view
    }

    override fun verifyStep(): VerificationError? {
        return if (!controller.snapToken.value.isNullOrBlank()) {
            null
        } else {
            VerificationError("You need to login")
        }
    }

    override fun onSelected() {
        if (firstRun && !controller.snapToken.value.isNullOrBlank() && !controller.snapUser.value.isNullOrBlank()) {
            activity.stepperLayout.proceed()
        } else {
            controller.save(activity)
        }
    }
}
package com.snapcardster.omnimtg.android

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.snapcardster.omnimtg.android.Adapter.StepperAdapter
import kotlinx.android.synthetic.main.activity_main.*
import com.stepstone.stepper.StepperLayout
import com.stepstone.stepper.adapter.StepAdapter


class MainActivity : AppCompatActivity() {

    private var mStepperLayout: StepperLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //val controller = MainControllerWrapper()

        stepperLayout.setAdapter(StepperAdapter(supportFragmentManager,this))
    }
}

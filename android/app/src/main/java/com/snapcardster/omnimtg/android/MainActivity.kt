package com.snapcardster.omnimtg.android

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.snapcardster.omnimtg.android.Adapter.StepperAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stepperLayout.setAdapter(StepperAdapter(supportFragmentManager,this))

        controller.readProperties(this)
    }

    companion object {
        val controller = MainControllerWrapper()
        var firstRun = true
    }
}

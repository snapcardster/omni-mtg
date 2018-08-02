package com.snapcardster.omnimtg.android

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.snapcardster.omnimtg.android.Adapter.StepperAdapter
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stepperLayout.setAdapter(StepperAdapter(supportFragmentManager, this))

        controller.readProperties(this)
    }

    override fun onResume() {
        super.onResume()
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val current = connManager.activeNetworkInfo
        if (current == null) {
            alert {
                title="Connect to WiFi"
                message="You need to connect this device to your network. Press OK to open the settings"
                isCancelable = false
                okButton {
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            }.show()
        }else{
            Log.d("Start",current.typeName)
        }
    }

    companion object {
        val controller = MainControllerWrapper()
        var firstRun = true
    }
}

package com.snapcardster.omnimtg.android

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.snapcardster.omnimtg.android.Adapter.StepperAdapter
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton
import org.jetbrains.anko.wifiManager

class MainActivity : AppCompatActivity() {

    val PERMISSION_WRITE_STORAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stepperLayout.setAdapter(StepperAdapter(supportFragmentManager, this))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_WRITE_STORAGE)
        }

        controller.readProperties(this)
    }

    override fun onResume() {
        super.onResume()
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val current = connManager.activeNetworkInfo
        if (current == null) {
            alert {
                title = "Connect to WiFi"
                message = "You need to connect this device to your network. Press OK to open the settings"
                isCancelable = false
                okButton {
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            }.show()
        } else {
            Log.d("Start", current.typeName)
        }
    }

    companion object {
        val controller = MainControllerWrapper()
        var firstRun = true
    }
}

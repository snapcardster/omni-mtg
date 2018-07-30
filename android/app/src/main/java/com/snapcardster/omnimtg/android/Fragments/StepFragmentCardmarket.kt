package com.snapcardster.omnimtg.android.Fragments

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.snapcardster.omnimtg.android.MainActivity.Companion.controller
import com.snapcardster.omnimtg.android.R
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.fragment_step_cardmarket.*
import kotlinx.android.synthetic.main.fragment_step_cardmarket.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.support.v4.toast
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_HTML
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.util.Log
import com.snapcardster.omnimtg.android.MainActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.support.v4.act


class StepFragmentCardmarket : StepFragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        //initialize your UI
        val view = inflater!!.inflate(R.layout.fragment_step_cardmarket, container, false)

        bind(controller.mkmAccessToken, view.card_cardmarket_details_accesstoken)
        bind(controller.mkmAccessTokenSecret, view.card_cardmarket_details_accesstokensecret)
        bind(controller.mkmAppSecret, view.card_cardmarket_details_appsecret)
        bind(controller.mkmAppToken, view.card_cardmarket_details_apptoken)

        view.card_explanation_text.movementMethod = LinkMovementMethod.getInstance()

        view.card_cardmarket_details_paste.onClick {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            if (clipboard.hasPrimaryClip()) {
                if (clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_HTML) || clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {

                    //since the clipboard contains plain text.
                    val item = clipboard.getPrimaryClip().getItemAt(0)

                    // Gets the clipboard as text.
                    val pasteData = item.getText().toString()

                    controller.insertFromClip("mkm", pasteData)
                }else{
                    Log.d("Clip","desc:" + clipboard.getPrimaryClipDescription().getMimeType(0))
                }
            }else{
                Log.d("Clip","hasPrimaryClip = false")
            }
        }


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
        if (MainActivity.firstRun && !MainActivity.controller.mkmAppToken.value.isNullOrBlank() &&
                !MainActivity.controller.mkmAppSecret.value.isNullOrBlank() &&
                !MainActivity.controller.mkmAccessTokenSecret.value.isNullOrBlank() &&
                !MainActivity.controller.mkmAccessToken.value.isNullOrBlank()){
            activity.stepperLayout.proceed()
        }
    }
}
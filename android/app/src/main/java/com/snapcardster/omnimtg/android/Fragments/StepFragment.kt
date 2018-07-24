package com.snapcardster.omnimtg.android.Fragments

import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.snapcardster.omnimtg.Interfaces.StringProperty
import com.stepstone.stepper.Step
import com.stepstone.stepper.VerificationError
import org.jetbrains.anko.support.v4.toast

abstract class StepFragment : Fragment(), Step {
    fun bind(prop: StringProperty, txt: EditText) {
        // TODO Add Listener to prop
        txt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                prop.value = p0.toString()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

        })
    }

    fun bind(prop: StringProperty, txt: EditText, invalidateOnChange: List<StringProperty>){
        // TODO Add Listener to prop
        txt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                prop.value = p0.toString()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                invalidateOnChange.forEach { it.value = "" }
            }

        })
    }

    override fun onError(error: VerificationError) {
        toast(error.errorMessage)
        //handle error inside of the fragment, e.g. show error on EditText
    }
}

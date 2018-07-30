package com.snapcardster.omnimtg.android.Fragments

import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import com.snapcardster.omnimtg.Interfaces.StringProperty
import com.snapcardster.omnimtg.android.AndroidSrtingPropertyListener
import com.stepstone.stepper.Step
import com.stepstone.stepper.VerificationError
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread

abstract class StepFragment : Fragment(), Step {

    var listeners: HashMap<EditText, TextWatcher> = hashMapOf()

    fun bind(prop: StringProperty, txt: TextView) {
        prop.addListener(object : AndroidSrtingPropertyListener {
            override fun onChanged(oldValue: String?, newValue: String?, callListener: Boolean) {
                if (callListener) {
                    Log.d("PropChanged", oldValue + " -> " + newValue)
                    if (oldValue != newValue) {
                        doAsync { uiThread { txt.setText(newValue) } }
                    }
                }
            }
        })
    }

    fun bind(prop: StringProperty, txt: EditText) {
        if (txt.text.toString() != prop.value) {
            txt.setText(prop.value)
        }
        if (listeners.containsKey(txt)) {
            txt.removeTextChangedListener(listeners.get(txt))
        }
        prop.addListener(object : AndroidSrtingPropertyListener {
            override fun onChanged(oldValue: String?, newValue: String?, callListener: Boolean) {
                if (callListener) {
                    Log.d("PropChanged", oldValue + " -> " + newValue)
                    if (oldValue != newValue) {
                        txt.setText(newValue)
                    }
                }
            }
        })
        val l = object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                prop.setValue(p0.toString(), false)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        }
        listeners.put(txt, l)
        txt.addTextChangedListener(l)
    }

    fun bind(prop: StringProperty, txt: EditText, invalidateOnChange: List<StringProperty>) {
        if (txt.text.toString() != prop.value) {
            txt.setText(prop.value)
        }
        if (listeners.containsKey(txt)) {
            txt.removeTextChangedListener(listeners.get(txt))
        }
        prop.addListener(object : AndroidSrtingPropertyListener {
            override fun onChanged(oldValue: String?, newValue: String?, callListener: Boolean) {
                if (callListener) {
                    Log.d("PropChanged2", oldValue + " -> " + newValue)
                    if (oldValue != newValue) {
                        txt.setText(newValue)
                    }
                }
            }
        })
        val l = object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                Log.d("SetProp", p0.toString())
                prop.setValue(p0.toString(), false)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.toString() != prop.value) {
                    invalidateOnChange.forEach {
                        Log.d("Invalidate", it.value + "; view was set to " + p0)
                        it.value = ""
                    }
                }
            }
        }
        listeners.put(txt, l)
        txt.addTextChangedListener(l)
    }

    override fun onError(error: VerificationError) {
        toast(error.errorMessage)
        //handle error inside of the fragment, e.g. show error on EditText
    }
}

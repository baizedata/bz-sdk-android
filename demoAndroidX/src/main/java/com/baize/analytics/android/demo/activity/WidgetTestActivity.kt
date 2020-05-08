/*
 * Created by zhangwei on 2019/04/17.
 * Copyright 2015－2020 Hong Yu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baize.analytics.android.demo.activity

import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.baize.analytics.android.demo.R
import com.baize.analytics.android.sdk.BzLog
import com.baize.analytics.android.sdk.BaizeAPI
import com.baize.analytics.android.sdk.BaizeTrackViewOnClick
import kotlinx.android.synthetic.main.activity_widget.*

class WidgetTestActivity : BaseActivity(), CompoundButton.OnCheckedChangeListener,
        RadioGroup.OnCheckedChangeListener, RatingBar.OnRatingBarChangeListener, SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {
    private val TAG = "WidgetTestActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget)
        initView()
    }

    private fun initView() {
        imageButton.setOnClickListener {
            BzLog.i(TAG, "image button clicked")
        }
        setOnClickBtn.setOnClickListener {
            BzLog.i(TAG, "${setOnClickBtn.text} button clicked")
        }

        EditText.setOnClickListener { }
        AppCompatEditText.setOnClickListener { }

        checkbox1.setOnCheckedChangeListener(this)
        checkbox2.setOnCheckedChangeListener(this)
        checkbox3.setOnClickListener { }
        radio1.setOnCheckedChangeListener(this)
        radio2.setOnCheckedChangeListener(this)
        radio3.setOnClickListener { }
        radioGroup.setOnCheckedChangeListener(this)
        compatRatingBar.onRatingBarChangeListener = this
        ratingBar.onRatingBarChangeListener = this

        seekBar.setOnSeekBarChangeListener(this)
        compatSeekBar.setOnSeekBarChangeListener(this)

        spinner.onItemSelectedListener = this
        compatSpinner.onItemSelectedListener = this
        switch1.setOnCheckedChangeListener(this)
        switchCompat.setOnCheckedChangeListener(this)
        toggleButton.setOnCheckedChangeListener(this)
        mycardview.setOnClickListener {

        }
        cardview.setOnClickListener {

        }
//        material_cardview.setOnClickListener {
//
//        }
        linearlayout.setOnClickListener { }
        mylinearlayout.setOnClickListener { }
        BaizeAPI.sharedInstance().ignoreView(changeSeekBar)
        BaizeAPI.sharedInstance().ignoreView(changeRatingBar)
        BaizeAPI.sharedInstance().ignoreView(changeRadioBtn)
        BaizeAPI.sharedInstance().ignoreView(changSwitchBtn)
        BaizeAPI.sharedInstance().ignoreView(changeCheckedBtn)
        BaizeAPI.sharedInstance().ignoreView(changeToggleBtn)
        linearlayout.setOnClickListener { }
        mylinearlayout.setOnClickListener { }
        val drawable = this@WidgetTestActivity.getDrawable(R.drawable.abc_btn_check_material)
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)
            val hello = "hello,world"
            val spanString = SpannableString(hello);
            spanString.setSpan(imageSpan, 0, 5, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            textViewClick.setText(spanString)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @BaizeTrackViewOnClick
    fun doOnClick(view: View) {
        when (view.id) {
            R.id.xmOnClickBtn -> {
                BzLog.i(TAG, "${xmOnClickBtn.text} button clicked")
            }
            R.id.imageViewClick -> {
                BzLog.i(TAG, "image view clicked")
            }
            R.id.textViewClick -> {

            }
            R.id.changeCheckedBtn -> {
                BzLog.i(TAG, "text view clicked")
                checkbox1.isChecked = !checkbox1.isChecked
            }
            R.id.checkedTextView -> {
                checkedTextView.isChecked = !checkedTextView.isChecked
                BzLog.i("SpannableString", textViewClick.text.toString())
            }
            R.id.changeRadioBtn -> {
                radioGroup.check(R.id.radio2);
            }
            R.id.changSwitchBtn -> {
                switch1.isChecked = !switch1.isChecked
            }
            R.id.changeToggleBtn -> {
                toggleButton.isChecked = !toggleButton.isChecked
            }
            R.id.changeSeekBar -> {
                seekBar.progress = 10
            }
            R.id.changeRatingBar -> {
                ratingBar.rating = 2f
            }
            else -> {

            }
        }
    }

    override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
    }

    override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {
    }

    override fun onRatingChanged(ratingBar: RatingBar, rating: Float, fromUser: Boolean) {
        BzLog.i(TAG, "rating bar : $rating, $fromUser")
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        BzLog.i(TAG, "seekbar bar : $progress, $fromUser")
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View,
                                position: Int, id: Long) {
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
    }
}
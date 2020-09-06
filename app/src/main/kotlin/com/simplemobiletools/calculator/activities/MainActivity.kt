package com.simplemobiletools.calculator.activities

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import com.simplemobiletools.calculator.BuildConfig
import com.simplemobiletools.calculator.R
import com.simplemobiletools.calculator.extensions.config
import com.simplemobiletools.calculator.extensions.updateViewColors
import com.simplemobiletools.calculator.helpers.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.LICENSE_AUTOFITTEXTVIEW
import com.simplemobiletools.commons.helpers.LICENSE_ESPRESSO
import com.simplemobiletools.commons.helpers.LICENSE_ROBOLECTRIC
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.Release
import kotlinx.android.synthetic.main.activity_main.*
import me.grantland.widget.AutofitHelper
import java.math.BigDecimal


class MainActivity : SimpleActivity(), Calculator {
    private var storedTextColor = 0
    private var vibrateOnButtonPress = true
    private var lastKeyEvent = 0

    lateinit var calc: CalculatorImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)

        calc = CalculatorImpl(this, applicationContext)

        btn_plus.setOnClickListener { calc.handleOperation(PLUS); checkHaptic(it); signLabel.setText(PLUS_SIGN)  }
        btn_minus.setOnClickListener { calc.handleOperation(MINUS); checkHaptic(it); signLabel.setText(MINUS_SIGN) }
        btn_multiply.setOnClickListener { calc.handleOperation(MULTIPLY); checkHaptic(it); signLabel.setText(MULTIPLY_SIGN) }
        btn_divide.setOnClickListener { calc.handleOperation(DIVIDE); checkHaptic(it); signLabel.setText(DIVIDE_SIGN) }
        btn_percent.setOnClickListener { calc.handleOperation(PERCENT); checkHaptic(it); signLabel.setText(PERCENT_SIGN) }
        btn_power.setOnClickListener { calc.handleOperation(POWER); checkHaptic(it); signLabel.setText(POWER_SIGN) }
        btn_root.setOnClickListener { calc.handleOperation(ROOT); checkHaptic(it); signLabel.setText(ROOT_SIGN) }

        btn_clear.setOnClickListener { calc.handleClear(); checkHaptic(it);  }
        btn_clear.setOnLongClickListener { signLabel.setText(BLANK_SIGN); calc.handleReset(); true }

        getButtonIds().forEach {
            it.setOnClickListener { calc.numpadClicked(it.id); checkHaptic(it) }
        }

        btn_equals.setOnClickListener { calc.handleEquals(); checkHaptic(it); signLabel.setText(EQUALS_SIGN) }
        formula.setOnLongClickListener { copyToClipboard(false) }
        result.setOnLongClickListener { copyToClipboard(true) }

        AutofitHelper.create(result)
        AutofitHelper.create(formula)
        storeStateVariables()
        updateViewColors(calculator_holder, config.textColor)
        checkWhatsNewDialog()
        checkAppOnSDCard()
    }

    override fun onResume() {
        super.onResume()
        if (storedTextColor != config.textColor) {
            updateViewColors(calculator_holder, config.textColor)
        }

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        vibrateOnButtonPress = config.vibrateOnButtonPress
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun storeStateVariables() {
        config.apply {
            storedTextColor = textColor
        }
    }

    private fun checkHaptic(view: View) {
        if (vibrateOnButtonPress) {
            view.performHapticFeedback()
        }
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_AUTOFITTEXTVIEW or LICENSE_ROBOLECTRIC or LICENSE_ESPRESSO

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
            FAQItem(R.string.faq_1_title_commons, R.string.faq_1_text_commons),
            FAQItem(R.string.faq_4_title_commons, R.string.faq_4_text_commons),
            FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons),
            FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons)
        )

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Calculator")
        builder.setMessage("Simple calculator app with Blackberry optimizations\nOptmized for hardware keyboard use\n\nBased on Simple-Calculator by SimpleMobileTools - https://www.simplemobiletools.com")
        builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
            // You don't have to do anything here if you just
            // want it dismissed when clicked
        })

        // Create the AlertDialog object and return it

        // Create the AlertDialog object and return it
        builder.show()
        //startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    private fun getButtonIds() = arrayOf(btn_decimal, btn_0, btn_1, btn_2, btn_3, btn_4, btn_5, btn_6, btn_7, btn_8, btn_9)

    private fun copyToClipboard(copyResult: Boolean): Boolean {
        var value = formula.value
        if (copyResult) {
            value = result.value
        }

        return if (value.isEmpty()) {
            false
        } else {
            copyToClipboard(value)
            true
        }
    }

    override fun setValue(value: String, context: Context) {
        result.text = value
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(18, R.string.release_18))
            add(Release(28, R.string.release_28))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }

    // used only by Robolectric
    override fun setValueBigDecimal(d: BigDecimal) {
        calc.setValue(Formatter.bigDecimalToString(d))
        calc.lastKey = DIGIT
    }

    override fun setFormula(value: String, context: Context) {
        formula.append("\n")
        formula.append(value)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if(event.getAction() != KeyEvent.ACTION_UP)
            return super.dispatchKeyEvent(event);

        Log.i("key up", java.lang.String.valueOf(event.getKeyCode()))
        //1=51, 2=33, 3=46, 4=47, 5=32, 6=34, 7=54, 8=52, 9=31, 0=7, plus=43, minus=37, multiply=29, divide=35, percent=45, power=48, root=53, enter=66, backspace=67, space=62
        //btn_decimal, btn_0, btn_1, btn_2, btn_3, btn_4, btn_5, btn_6, btn_7, btn_8, btn_9
        //btn_plus, btn_minus, btn_multiply, btn_divide, btn_percent, btn_power, btn_root, btn_equals.
        when(event.keyCode) {
            51 -> btn_1.callOnClick()
            33 -> btn_2.callOnClick()
            46 -> btn_3.callOnClick()
            47 -> btn_4.callOnClick()
            32 -> btn_5.callOnClick()
            34 -> btn_6.callOnClick()
            54 -> btn_7.callOnClick()
            52 -> btn_8.callOnClick()
            31 -> btn_9.callOnClick()
            7 -> btn_0.callOnClick()
            43 -> btn_plus.callOnClick()
            37 -> btn_minus.callOnClick()
            29 -> btn_multiply.callOnClick()
            35 -> btn_divide.callOnClick()
            45 -> btn_percent.callOnClick()
            48 -> btn_power.callOnClick()
            53 -> btn_root.callOnClick()
            62 -> btn_equals.callOnClick()
            56 -> btn_decimal.callOnClick()
            66 -> btn_equals.callOnClick()
            67 -> {
                if (lastKeyEvent == 67) {
                    btn_clear.performLongClick();
                } else {
                    btn_clear.callOnClick()
                }
            }
        }
        lastKeyEvent = event.getKeyCode();

        return super.dispatchKeyEvent(event)
    }
}

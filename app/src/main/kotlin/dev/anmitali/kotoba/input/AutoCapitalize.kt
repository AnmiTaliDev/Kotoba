package dev.anmitali.kotoba.input

import android.text.InputType

fun allowsAutoCapitalize(inputType: Int): Boolean =
    (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
        (inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0

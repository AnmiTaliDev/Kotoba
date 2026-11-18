package dev.anmitali.kotoba.input

data class SpaceEdit(val deleteBefore: Int, val insert: String)

fun spaceEdit(textBefore: CharSequence, doubleSpace: String): SpaceEdit {
    val afterWordAndSpace = doubleSpace.isNotEmpty() &&
        textBefore.length >= 2 &&
        textBefore.last() == ' ' &&
        textBefore[textBefore.length - 2].isLetterOrDigit()
    return if (afterWordAndSpace) SpaceEdit(1, "$doubleSpace ") else SpaceEdit(0, " ")
}

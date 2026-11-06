package com.ollie.chesscoach

// Board + piece recognition from a screenshot is the hard part.
// This is the interface + a stub that returns null until you plug in
// a real recognizer (see notes below).
object BoardRecognizer {

    data class Position(val fen: String)

    fun recognize(bmp: android.graphics.Bitmap): Position? {
        // TODO(real work): find 8x8 grid, classify 64 squares.
        // Options that actually work:
        //  1. Template matching per piece (rank/file colour + glyph) with
        //     a bundled set of square crops you calibrate once per theme.
        //  2. TFLite model: train a small CNN on labelled board crops.
        //  3. If you control the source (your own analysis app), skip OCR —
        //     feed the position straight from the chess library you render with.
        return null
    }
}
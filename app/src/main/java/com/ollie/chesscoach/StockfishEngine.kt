package com.ollie.chesscoach

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

class StockfishEngine(private val ctx: Context) {

    data class Best(val from: String, val to: String, val score: Int)

    private var proc: Process? = null
    private var out: PrintWriter? = null
    private var reader: BufferedReader? = null
    @Volatile private var ready = false

    fun start() {
        val bin = File(ctx.filesDir, "stockfish")
        if (!bin.exists()) {
            ctx.assets.open("stockfish/stockfish-android").use { input ->
                bin.outputStream().use { input.copyTo(it) }
            }
            bin.setExecutable(true)
        }
        proc = ProcessBuilder(bin.absolutePath).redirectErrorStream(true).start()
        out = PrintWriter(proc!!.outputStream, true)
        reader = BufferedReader(InputStreamReader(proc!!.inputStream))
        out!!.println("uci")
        out!!.println("isready")
        ready = true
    }

    fun analyze(fen: String, cb: (Best) -> Unit) {
        if (!ready) return
        out?.println("position fen $fen")
        out?.println("go depth 18")
        Thread {
            var bestMove = ""
            try {
                while (true) {
                    val line = reader?.readLine() ?: break
                    if (line.startsWith("bestmove")) {
                        bestMove = line.split(" ")[1]
                        break
                    }
                }
            } catch (_: Exception) {}
            if (bestMove.length >= 4) {
                cb(Best(bestMove.substring(0, 2), bestMove.substring(2, 4), 0))
            }
        }.start()
    }

    fun stop() {
        ready = false
        runCatching { out?.println("quit") }
        proc?.destroy()
    }
}
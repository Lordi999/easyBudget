package de.meinbudget.app

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Liest höchstens [maxBytes] als UTF-8-Text; bei größeren Daten null. Schließt den Stream. */
fun readLimitedText(input: InputStream, maxBytes: Int): String? = input.use { stream ->
    val buffer = ByteArrayOutputStream()
    val chunk = ByteArray(8 * 1024)
    while (true) {
        val read = stream.read(chunk)
        if (read < 0) break
        buffer.write(chunk, 0, read)
        if (buffer.size() > maxBytes) return null
    }
    buffer.toString(Charsets.UTF_8.name())
}

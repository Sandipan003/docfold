package com.docfold.scanner.data

/** Picks a document name from its recognised text: the first line that reads like a title. */
fun nameFromText(text: String): String? = text.lineSequence()
    .map { line -> line.trim().replace(Regex("\\s+"), " ").trim(' ', '.', ',', ':', ';', '-', '_', '|') }
    .firstOrNull { line -> line.count(Char::isLetter) >= 3 }
    ?.take(60)?.trim()

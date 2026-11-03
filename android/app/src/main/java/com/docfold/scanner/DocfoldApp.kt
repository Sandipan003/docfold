package com.docfold.scanner

import android.app.Application
import com.docfold.scanner.data.DocStore
import com.docfold.scanner.imaging.DeviceProfile
import com.docfold.scanner.pdf.PdfExporter

class DocfoldApp : Application() {
    lateinit var store: DocStore
        private set
    lateinit var exporter: PdfExporter
        private set

    override fun onCreate() {
        super.onCreate()
        DeviceProfile.init(this)
        store = DocStore(this)
        exporter = PdfExporter(store)
    }
}

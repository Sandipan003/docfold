package com.docfold.scanner.ui

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccessTest {
    @Test fun androidNineAndOlderAskToWrite() {
        assertEquals(Manifest.permission.WRITE_EXTERNAL_STORAGE, storagePermission(24))
        assertEquals(Manifest.permission.WRITE_EXTERNAL_STORAGE, storagePermission(28))
    }

    @Test fun android10To12AskToRead() {
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, storagePermission(29))
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, storagePermission(32))
    }

    @Test fun android13AndNewerHaveNoStoragePermission() {
        assertNull(storagePermission(33))
        assertNull(storagePermission(36))
    }
}

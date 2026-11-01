package com.vaishnavs.nexoslauncher.data.files

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileUris {
    fun authority(context: Context): String = "${context.packageName}.files"

    fun uriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, authority(context), file)
    }
}

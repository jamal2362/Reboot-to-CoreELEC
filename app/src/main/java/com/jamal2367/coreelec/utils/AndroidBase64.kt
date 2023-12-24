package com.jamal2367.coreelec.utils

import android.util.Base64
import com.tananaev.adblib.AdbBase64

class AndroidBase64 : AdbBase64 {
    override fun encodeToString(bArr: ByteArray): String {
        return Base64.encodeToString(bArr, 2)
    }
}

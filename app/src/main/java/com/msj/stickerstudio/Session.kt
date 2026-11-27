package com.msj.stickerstudio

import android.net.Uri

/** In-memory hand-off between the editor and the converter screens. */
object Session {
    var inputUri: Uri? = null
    var inputName: String = "video"
    var settings: ConvertSettings = ConvertSettings()
    var lastOutput: String? = null
    var lastOutputFormat: OutputFormat = OutputFormat.ANIMATED_WEBP
}

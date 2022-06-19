package com.example.pracalicencjacka.interfaces

import java.io.File

interface AsyncResponse {
    fun processFinish(output: String?, folder: File)
}
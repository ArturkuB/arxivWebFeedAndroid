package com.example.pracalicencjacka.network

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.widget.Toast
import com.example.pracalicencjacka.interfaces.AsyncResponse
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class DownloadFile(private var context: Context) :
    AsyncTask<String?, Int?, String?>() {

    private var folder: File? = null
    private var mProgressDialog: ProgressDialog = ProgressDialog(context)
    var delegate: AsyncResponse? = null

    init {
        mProgressDialog.setMessage("Downloading PDF. Please Wait...")
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mProgressDialog.setCancelable(true)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        mProgressDialog.show()
    }

    override fun doInBackground(vararg strings: String?): String? {
        var input: InputStream? = null
        var output: OutputStream? = null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(strings.get(0))
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.getResponseCode() !== HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.responseCode
                    .toString() + " " + connection.responseMessage
            }

            val fileLength: Int = connection.contentLength
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val file = File(context.getExternalFilesDir(null).toString() + "/" + "PDFs")
                if (!file.exists()) {
                    file.mkdir()
                }
                folder = file
            } else {
                val extStorageDirectory: String =
                    Environment.getExternalStorageDirectory().toString()

                folder = File(extStorageDirectory, "PDFs")
                folder?.mkdir()
            }

            val imageFile = File(folder, strings[1])
            try {
                imageFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            input = connection.inputStream
            output = FileOutputStream(imageFile.absolutePath)
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also(
                    { count = it }) != -1
            ) {
                if (isCancelled) {
                    input.close()
                    return null
                }
                total += count.toLong()
                if (fileLength > 0)
                    publishProgress((total * 100 / fileLength).toInt())
                output.write(data, 0, count)
            }
        } catch (e: Exception) {
            return e.toString()
        } finally {
            try {
                if (output != null) output.close()
                if (input != null) input.close()
            } catch (ignored: IOException) {
            }
            if (connection != null) connection.disconnect()
        }
        return null
    }


    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        mProgressDialog.setIndeterminate(false)
        mProgressDialog.setMax(100)
        mProgressDialog.setProgress(values[0]!!)
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        mProgressDialog.dismiss()
        if (result != null) {
            if (folder != null)
                delegate?.processFinish("0", folder!!)
            Toast.makeText(context, "Failed to download", Toast.LENGTH_LONG).show()
        } else {
            delegate?.processFinish("1", folder!!)
        }
    }

}

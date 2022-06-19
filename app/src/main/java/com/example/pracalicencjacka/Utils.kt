package com.example.pracalicencjacka

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat

class Utils {
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }

    fun showAlertDialog(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(
            "prefs",
            AppCompatActivity.MODE_PRIVATE
        )
        val prefsEdit: SharedPreferences.Editor = prefs.edit()
        val titleSelector = prefs.getString("selectorTitle", ".list-title")
        val authorsSelector = prefs.getString("selectorAuthors", ".list-authors")
        val pdfLinksSelector =
            prefs.getString("selectorPdfLink", ".list-identifier a:nth-child(2)")

        val titleTextField = EditText(context)
        val authorsTextField = EditText(context)
        val pdfLinkTextField = EditText(context)
        titleTextField.hint = "$titleSelector <- Title"
        authorsTextField.hint = "$authorsSelector <- Authors"
        pdfLinkTextField.hint = "$pdfLinksSelector <- Link"

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayoutCompat.LayoutParams.MATCH_PARENT
        )
        params.setMargins(40, 3, 40, 3)
        titleTextField.layoutParams = params
        authorsTextField.layoutParams = params
        pdfLinkTextField.layoutParams = params
        layout.addView(titleTextField)
        layout.addView(authorsTextField)
        layout.addView(pdfLinkTextField)

        AlertDialog.Builder(context)
            .setTitle("Change selectors")
            .setView(layout)
            .setNeutralButton("Restore defaults") { _, _ ->
                prefsEdit.putString("selectorTitle", ".list-title")
                prefsEdit.putString("selectorAuthors", ".list-authors")
                prefsEdit.putString("selectorPdfLink", ".list-identifier a:nth-child(2)")
                prefsEdit.apply()
                val titleSelector = prefs.getString("selectorTitle", ".list-title")
                val authorsSelector = prefs.getString("selectorAuthors", ".list-authors")
                val pdfLinksSelector =
                    prefs.getString("selectorPdfLink", ".list-identifier a:nth-child(2)")
                titleTextField.hint = "$titleSelector <- Title"
                authorsTextField.hint = "$authorsSelector <- Authors"
                pdfLinkTextField.hint = "$pdfLinksSelector <- Link"
            }
            .setPositiveButton(
                "Save"
            ) { dialog, _ ->
                val title = titleTextField.text.toString()
                val authors = authorsTextField.text.toString()
                val pdfLink = pdfLinkTextField.text.toString()
                if (title.isNotEmpty())
                    prefsEdit.putString("selectorTitle", title)
                if (authors.isNotEmpty())
                    prefsEdit.putString("selectorAuthors", authors)
                if (pdfLink.isNotEmpty())
                    prefsEdit.putString(
                        "selectorPdfLink",
                        pdfLink
                    )
                prefsEdit.apply()
                dialog.dismiss()
            }
            .setNegativeButton(
                "Cancel"
            ) { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

package com.example.pracalicencjacka

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.pracalicencjacka.datamodel.Category
import com.example.pracalicencjacka.datamodel.Publication
import com.example.pracalicencjacka.datamodel.Subcategory
import com.example.pracalicencjacka.network.HTTpsTrustManager
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.query
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.lang.Exception
import java.util.*

class parseWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    var context = context
    private var realm: Realm? = null

    override fun doWork(): Result {
        run()
        return Result.success()
    }

    private fun run() {
        setRealm()
        if(Utils().isOnline(context)){
            querySubscribedSubcategories {
                var count = 0
                runBlocking {
                    val job = CoroutineScope(IO).launch {
                        it.forEach { e ->
                            parse(e) {
                                count += it
                            }
                        }
                    }
                    job.join()
                }

                if (Looper.myLooper() == null)
                    Looper.prepare()
                Toast.makeText(
                    context,
                    "Background Worker: Added $count new publications!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        else
        {
            if (Looper.myLooper() == null)
                Looper.prepare()
            Toast.makeText(
                context,
                "Background Worker: Connection Problem",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    suspend fun parse(subcategory: Subcategory, complete: (Int) -> Unit) {
        //  try {
        parsePublications(
            subcategory.link,
            subcategory.title
        ) { publicationsList ->
            var count = 0
            runBlocking {
                val job = CoroutineScope(Main).launch {
                    publicationsList.forEach { e ->
                        if (savePublication(e, subcategory)) {
                            count += 1
                        }
                    }
                }
                job.join()
            }
            complete(count)
        }
        // } catch (e: Exception) {
        if (Looper.myLooper() == null)
            Looper.prepare()
//        Toast.makeText(
//            context,
//            "Background, parsing error",
//            Toast.LENGTH_LONG
//        ).show()
//    }
    }

    private suspend fun savePublication(
        publication: Publication,
        subcategory: Subcategory
    ): Boolean {
        var added = false
        val pub =
            realm!!.query<Publication>("publicationTag == $0", publication.publicationTag).first()
                .find()
        if (pub != null) {
            Log.i("savePublicationVM", "Duplikat w bazie")
        } else {
            realm!!.write {
                Log.i("**SAVE**", "Dodawanie")
                findLatest(subcategory)?.publications!!.add(publication)
                added = true
            }
        }
        return added
    }

    private suspend fun parsePublications(
        path: String,
        title: String,
        completion: (ArrayList<Publication>) -> Unit
    ) {
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(
                "prefs",
                AppCompatActivity.MODE_PRIVATE
            )
            val titleSelector = prefs.getString("selectorTitle", ".list-title")
            val authorsSelector = prefs.getString("selectorAuthors", ".list-authors")
            val pdfLinksSelector =
                prefs.getString("selectorPdfLink", ".list-identifier a:nth-child(2)")

            HTTpsTrustManager.allowAllSSL()
            val doc: org.jsoup.nodes.Document = Jsoup.connect(path).get()
            val titles: Elements = doc.select(titleSelector)
            val authors: Elements = doc.select(authorsSelector)
            val pdfLinks: Elements = doc.select(pdfLinksSelector)

            val tempList = ArrayList<Publication>()
            if (titles.isNotEmpty()) {
                for (i in 0 until titles.count()) {
                    val publication = Publication()
                    publication.title = titles[i].text().drop(7)
                    publication.author = authors[i].text()
                    publication.link = pdfLinks[i].attr("href")
                    publication.publicationTag = publication.link + title
                    tempList.add(publication)
                }
            }
            completion(tempList)
        } catch (e: Exception) {
            if (Looper.myLooper() == null)
                Looper.prepare()
            Toast.makeText(context, "Parsing subcategory error", Toast.LENGTH_LONG).show()
        }
    }

    fun setRealm() {
        val config =
            RealmConfiguration.Builder(
                setOf(
                    Category::class,
                    Subcategory::class,
                    Publication::class
                )
            )
                .build()
        realm = Realm.open(config)
    }

    fun querySubscribedSubcategories(complete: (RealmResults<Subcategory>) -> Unit) {
        val subcategories = realm!!.query<Subcategory>("subscribed = true").find()
        complete(subcategories)
    }


}
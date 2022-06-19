package com.example.pracalicencjacka.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pracalicencjacka.Utils
import com.example.pracalicencjacka.datamodel.Category
import com.example.pracalicencjacka.datamodel.Publication
import com.example.pracalicencjacka.datamodel.Subcategory
import com.example.pracalicencjacka.network.HTTpsTrustManager
import com.hadilq.liveevent.LiveEvent
import io.realm.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.File
import kotlin.system.measureTimeMillis

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var realm: Realm? = null

    private val _categories: MutableLiveData<RealmResults<Category>?> = MutableLiveData()
    val categories: LiveData<RealmResults<Category>?> get() = _categories

    val _currentCategory: MutableLiveData<Category?> = MutableLiveData()
    val currentCategory: LiveData<Category?> get() = _currentCategory

    val _currentSubcategory: MutableLiveData<Subcategory?> = MutableLiveData()
    val currentSubcategory: LiveData<Subcategory?> get() = _currentSubcategory

    val _currentCategoryPosition: MutableLiveData<Int> = MutableLiveData()
    val currentCategoryPosition: LiveData<Int> get() = _currentCategoryPosition

    val _currentSubcategoryPosition: MutableLiveData<Int> = MutableLiveData()
    val currentSubcategoryPosition: LiveData<Int> get() = _currentSubcategoryPosition

    val _currentPublicationPosition: MutableLiveData<Int> = MutableLiveData()
    val currentPublicationPosition: LiveData<Int> get() = _currentPublicationPosition

    var publicationsArray: ArrayList<Publication>? = null

    var subscribedCurrentSubcategory = false
    private var parsingDone: Boolean = false
    private var noInternetFlag: Boolean = false

    val app: Application = application

    private val _loadingState: LiveEvent<Boolean> = LiveEvent()
    val loadingState: LiveData<Boolean> get() = _loadingState

    fun setRealm() {
        viewModelScope.launch(Main) {
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
    }

    fun parseMainSite() {
        try {
            viewModelScope.launch(IO) {
                withContext(Main)
                {
                    _loadingState.value = true
                }
                HTTpsTrustManager.allowAllSSL()
                val doc: org.jsoup.nodes.Document =
                    Jsoup.connect("https://arxiv.org/").get()

                val elementsForRemoval: Elements = doc.select(
                    "ul:nth-child(20), .columns, #details-econ, #details-eess, #details-stat, #details-q-fin, #details-q-bio, #details-cs, #details-math, a:nth-child(3), a:nth-child(4), a:nth-child(5)"
                )
                elementsForRemoval.forEach { e -> e.remove() }
                parseSection("ul:nth-child(2) a", doc, "Physics")
                parseSection("ul:nth-child(4) a", doc, "Mathematics")
                parseSection("ul:nth-child(6) a", doc, "Computer Science")
                parseSection("ul:nth-child(8) a", doc, "Quantitative Biology")
                parseSection("ul:nth-child(10) a", doc, "Quantitative Finance")
                parseSection("ul:nth-child(12) a", doc, "Statistics")
                parseSection(
                    "ul:nth-child(14) a",
                    doc,
                    "Electrical Engineering and Systems Science"
                )
                parseSection("ul:nth-child(16) a", doc, "Economics")
                withContext(Main)
                {
                    _loadingState.value = false
                    queryAndSet()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(app.applicationContext, "Parsing error", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun parseSection(
        selektor: String,
        document: org.jsoup.nodes.Document,
        categoryTitle: String
    ) {
        try {
            val subcategories: Elements = document.select(selektor)
            val category = Category()
            category.title = categoryTitle
            subcategories.forEach { e ->
                if (e.attr("href").contains("archive")) {
                    val subcategory = Subcategory()
                    subcategory.title = e.text()
                    subcategory.link =
                        "https://arxiv.org" + e.attr("href").replace("archive", "list") + "/recent"
                    category.subcategories.add(subcategory)
                } else {
                     if (e.attr("href").contains("corr")) {
                    val subcategory = Subcategory()
                    subcategory.title = e.text()
                    subcategory.link = "https://arxiv.org/list/cs/recent"
                    category.subcategories.add(subcategory)
                      } else {
                        val subcategory = Subcategory()
                        subcategory.title = e.text()
                        subcategory.link = "https://arxiv.org" + e.attr("href")
                        category.subcategories.add(subcategory)
                      }
                }
            }
            withContext(Main) {
                saveCategory(category)
            }
        } catch (e: Exception) {
            if (Looper.myLooper() == null)
                Looper.prepare()
            Toast.makeText(app.applicationContext, "Parsing error", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun saveCategory(category: Category) {
        realm!!.write {
            this.copyToRealm(category)
        }
    }

    fun manageSubcategoryCellClick() {
        viewModelScope.launch(IO) {
            withContext(Main)
            {
                _loadingState.value = true
            }
            if (Utils().isOnline(app.applicationContext)) {
                val tStart = System.currentTimeMillis()
                parsePublications(
                    currentSubcategory.value!!.link,
                    currentSubcategory.value!!.title
                ) { publicationsList ->
                    if (currentSubcategory.value!!.subscribed) {
                        subscribedCurrentSubcategory = true
                        var count = 0
                        runBlocking {
                            val job = viewModelScope.launch(Main) {
                                publicationsList.forEach { e ->
                                    if (savePublication(e)) {
                                        count += 1
                                    }
                                }
                            }
                            job.join()
                            val tEnd = System.currentTimeMillis()
                            val tDelta = tEnd - tStart
                            val elapsedSeconds = tDelta / 1000.0
                            Log.println(Log.VERBOSE, "Wraz z dodaniem", "$elapsedSeconds")
                            parsingDone = true
                            loadPublications()
                        }
                        if (Looper.myLooper() == null)
                            Looper.prepare()
                        Toast.makeText(
                            app.applicationContext,
                            "Added $count new publications!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        subscribedCurrentSubcategory = false
                        viewModelScope.launch(Main) {
                            publicationsArray = publicationsList
                        }
                        parsingDone = true
                    }
                }
            } else {
                noInternetFlag = true
                subscribedCurrentSubcategory = true
                loadPublications()
            }
            withContext(Main)
            {
                _loadingState.value = false
            }
        }
    }

    fun countSubscribedSubcategories(complete: (Int) -> Unit) {
        viewModelScope.launch(Main) {
            val count = realm!!.query<Subcategory>("subscribed = true").find().count()
            complete(count)
        }
    }

    fun querySubscribedSubcategories(complete: (RealmResults<Subcategory>) -> Unit) {
        viewModelScope.launch(Main) {
            val subcategories = realm!!.query<Subcategory>("subscribed = true").find()
            complete(subcategories)
        }
    }

    fun queryAndSet() {
        _categories.value = realm!!.query<Category>().find()
    }

    fun subscribeItem(position: Int, callback: () -> Unit) {
        viewModelScope.launch(Main) {
            realm!!.write {
                findLatest(currentCategory.value!!)?.subcategories?.get(position)?.subscribed = true
            }
            withContext(Main) {
                _currentCategory.value =
                    realm!!.query<Category>().find()[currentCategoryPosition.value!!]
                _currentSubcategory.value = currentCategory.value!!.subcategories[position]
                callback()
            }
        }
    }

    fun unsubscribeItem(position: Int, callback: () -> Unit) {
        viewModelScope.launch(Main) {
            realm!!.write {
                findLatest(currentCategory.value!!)?.subcategories?.get(position)?.subscribed =
                    false
                currentSubcategory.value!!.publications.forEach {
                    findLatest(it)?.let { it1 -> delete(it1) }
                }
            }
            withContext(Main) {
                _currentCategory.value =
                    realm!!.query<Category>().find()[currentCategoryPosition.value!!]
                _currentSubcategory.value = currentCategory.value!!.subcategories[position]
                callback()
            }

        }
    }

    fun unsubscribeSubcategory(subcategory: Subcategory) {
        _currentSubcategory.value = subcategory
        viewModelScope.launch(Main) {
            realm!!.write {
                findLatest(subcategory)?.subscribed = false
                currentSubcategory.value!!.publications.forEach {
                    findLatest(it)?.let { it1 -> delete(it1) }
                }
            }
            withContext(Main) {
                _currentSubcategory.setValue(
                    realm!!.query<Subcategory>(
                        "title == $0",
                        subcategory.title
                    ).find().first()
                )
            }

        }
    }

    private fun loadPublications() {
        viewModelScope.launch(Main) {
            _currentSubcategory.value =
                realm!!.query<Subcategory>("title == $0", currentSubcategory.value!!.title).find()
                    .first()
        }
    }

    private suspend fun savePublication(publication: Publication): Boolean {
        var added = false
        val pub =
            realm!!.query<Publication>("publicationTag == $0", publication.publicationTag).first()
                .find()
        if (pub != null) {
        } else {
            realm!!.write {
                try {
                    findLatest(currentSubcategory.value!!)?.publications!!.add(publication)
                    added = true
                } catch (e: IllegalArgumentException) {
                    Log.i("Catch", "$e")
                }
            }
        }
        return added
    }

    fun parseSubscribedSubcategory(subcategory: Subcategory) {
        viewModelScope.launch(Default) {
            withContext(Main)
            {
                _loadingState.value = true
                _currentSubcategory.value = subcategory
            }
            if (Utils().isOnline(app.applicationContext)) {
                try {
                    parsePublications(
                        subcategory.link,
                        subcategory.title
                    ) { publicationsList ->
                        subscribedCurrentSubcategory = true
                        var count = 0
                        runBlocking {
                            val job = viewModelScope.launch(Main) {
                                publicationsList.forEach { e ->
                                    if (savePublication(e)) {
                                        count += 1
                                    }
                                }
                            }
                            job.join()
                            parsingDone = true
                            loadPublications()
                        }
                        if (Looper.myLooper() == null)
                            Looper.prepare()
                        Toast.makeText(
                            app.applicationContext,
                            "Added $count new publications!",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        app.applicationContext,
                        "Timeout, parsing error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                noInternetFlag = true
                subscribedCurrentSubcategory = true
                loadPublications()
            }
            withContext(Main)
            {
                _loadingState.value = false
            }
        }
    }

    private suspend fun parsePublications(
        path: String,
        title: String,
        completion: (ArrayList<Publication>) -> Unit
    ) {
            try {
                val prefs: SharedPreferences = app.getSharedPreferences(
                    "prefs",
                    AppCompatActivity.MODE_PRIVATE
                )
                val titleSelector = prefs.getString("selectorTitle", ".list-title")
                val authorsSelector = prefs.getString("selectorAuthors", ".list-authors")
                val pdfLinksSelector =
                    prefs.getString("selectorPdfLink", ".list-identifier a:nth-child(2)")

                HTTpsTrustManager.allowAllSSL()
                val tStart = System.currentTimeMillis()
                val doc: org.jsoup.nodes.Document = Jsoup.connect(path).get()
                val titles: Elements = doc.select(titleSelector)
                val authors: Elements = doc.select(authorsSelector)
                val pdfLinks: Elements = doc.select(pdfLinksSelector)

                val tempList = ArrayList<Publication>()
                if (titles.isNotEmpty()) {
                    for (i in 0 until titles.count()) {
                        val publication = Publication()
                        publication.title = titles[i].text().replace("Title: ", "")
                        publication.author = authors[i].text()
                        publication.link = pdfLinks[i].attr("href")
                        publication.publicationTag = publication.link + title
                        tempList.add(publication)
                    }
                }
                val tEnd = System.currentTimeMillis()
                val tDelta = tEnd - tStart
                val elapsedSeconds = tDelta / 1000.0
                Log.println(Log.VERBOSE, "Czyste parsowanie ", "$elapsedSeconds")
                completion(tempList)
            } catch (e: Exception) {
                if (Looper.myLooper() == null)
                    Looper.prepare()
                Toast.makeText(app.applicationContext, "Parsing error", Toast.LENGTH_LONG).show()
            }
    }

    fun managePdfDirectory(context: Context): File {
        val folder: File
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
            folder.mkdir()
        }
        return folder
    }

    fun checkIfPdfExists(context: Context): File {
        val pdfTitle = if (subscribedCurrentSubcategory) {
            currentSubcategory.value!!.publications[currentPublicationPosition.value!!].title
        } else {
            publicationsArray!![currentPublicationPosition.value!!].title
        }
        val folder = managePdfDirectory(context)

        val path = "$folder/$pdfTitle.pdf"
        return File(path)
    }

    fun setCurrentSubcategory(position: Int) {
        viewModelScope.launch(Main) {
                _currentSubcategory.value =
                    realm!!.query<Subcategory>("title == $0", currentCategory.value!!.subcategories[position].title).find()
                        .first()
        }
    }
}


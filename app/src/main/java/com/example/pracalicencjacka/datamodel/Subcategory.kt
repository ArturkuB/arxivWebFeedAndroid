package com.example.pracalicencjacka.datamodel

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.realmListOf

class Subcategory : RealmObject {
    var title: String = ""
    var link: String = ""
    var subscribed: Boolean = false
    var publications: RealmList<Publication> = realmListOf()
}
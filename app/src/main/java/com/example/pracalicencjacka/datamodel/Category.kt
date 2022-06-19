package com.example.pracalicencjacka.datamodel

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.realmListOf

class Category : RealmObject {
    var title: String = ""
    var subcategories: RealmList<Subcategory> = realmListOf()
}
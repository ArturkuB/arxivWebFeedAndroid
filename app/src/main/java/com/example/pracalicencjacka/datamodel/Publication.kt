package com.example.pracalicencjacka.datamodel

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

class Publication : RealmObject{
    @PrimaryKey
    var publicationTag: String = ""
    var title: String = ""
    var author: String = ""
    var link: String = ""
}
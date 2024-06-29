package com.aprz.gdsaveeditor

import com.google.gson.annotations.SerializedName

class SaveBean(
    @SerializedName(value = "VERSION") val version: String,
    val actors: List<Actor>
)

class Actor(
    @SerializedName(value = "ID") val id: Int,
    @SerializedName(value = "diy_skills") var diySkills: String,
)
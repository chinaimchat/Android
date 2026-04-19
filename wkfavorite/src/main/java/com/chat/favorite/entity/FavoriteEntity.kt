package com.chat.favorite.entity

import com.chad.library.adapter.base.entity.MultiItemEntity

class FavoriteEntity() : MultiItemEntity {
    var type = 0
    val no: String? = null
    var unique_key: String? = null
    var author_uid: String? = null
    var author_avatar: String? = null
    var author_name: String? = null
    var created_at: String? = null
    var payload: Map<*, *>? = null
    override val itemType: Int
        get() = type

}
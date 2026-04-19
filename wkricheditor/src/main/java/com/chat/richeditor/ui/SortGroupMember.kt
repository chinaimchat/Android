package com.chat.richeditor.ui

import com.chat.richeditor.entity.GroupMember

class SortGroupMember {

    fun sort(list: MutableList<GroupMember>) {
        for (i in 0 until list.size - 1) {
            for (j in 0 until list.size - 1 - i) {
                exchangeNameOrderBasic1(j, list)
            }
        }
    }

    private fun exchangeNameOrderBasic1(
        j: Int,
        list: MutableList<GroupMember>
    ) {
        val namePinYin1: String = list[j].pying
        val namePinYin2: String = list[j + 1].pying
        val size = namePinYin1.length.coerceAtMost(namePinYin2.length)
        for (i in 0 until size) {
            val jc = namePinYin1[i]
            val jcNext = namePinYin2[i]
            if (jc < jcNext) {
                break
            }
            if (jc > jcNext) {
                val nameBean: GroupMember = list[j]
                list[j] = list[j + 1]
                list[j + 1] = nameBean
                break
            }
        }
    }


    /**
     * 判断首字母是否为字母
     */
    fun isStartLetter(str: String): Boolean {
        val temp = str.substring(0, 1)
        return Character.isLetter(temp[0])
    }

    /**
     * 判断首字母是否数字
     */
    fun isStartNum(str: String): Boolean {
        val temp = str.substring(0, 1)
        return Character.isDigit(temp[0])
    }
}
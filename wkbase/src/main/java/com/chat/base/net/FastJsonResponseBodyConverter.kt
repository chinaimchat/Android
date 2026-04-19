package com.chat.base.net

import com.alibaba.fastjson.JSON
import okhttp3.ResponseBody
import retrofit2.Converter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class FastJsonResponseBodyConverter<T>(
    private val type: Type
) : Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T? {
        return value.use { body ->
            @Suppress("UNCHECKED_CAST")
            parseResponse(body.string(), type) as T?
        }
    }

    companion object {
        /**
         * List&lt;Bean&gt; 使用 JSON.parseObject(text, parameterizedType) 时，FastJSON 可能把元素落成
         * JSONObject 等类型，后续业务里会 ClassCastException。对「List + 单 Class 元素」显式走 parseArray。
         */
        internal fun parseResponse(text: String, type: Type): Any? {
            if (type is ParameterizedType) {
                val raw = type.rawType
                if (raw is Class<*> && java.util.List::class.java.isAssignableFrom(raw)) {
                    val args = type.actualTypeArguments
                    if (args.size == 1 && args[0] is Class<*>) {
                        return JSON.parseArray(text, args[0] as Class<*>)
                    }
                }
            }
            return JSON.parseObject(text, type)
        }
    }
}

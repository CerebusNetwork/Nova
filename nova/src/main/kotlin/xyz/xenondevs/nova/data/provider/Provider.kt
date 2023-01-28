@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.data.provider

import kotlin.reflect.KProperty

fun <T : Any, R> Provider<T>.map(transform: (T) -> R): Provider<R> {
    return MapEverythingProvider(this, transform).also(::addChild)
}

@JvmName("map1")
fun <T, R> Provider<T?>.map(transform: (T & Any) -> R): Provider<R?> {
    return MapOrNullProvider(this, transform).also(::addChild)
}

fun <T> Provider<T?>.orElse(value: T & Any): Provider<T & Any> {
    return FallbackValueProvider(this, value).also(::addChild)
}

fun <T> Provider<T?>.orElseNullable(value: T?): Provider<T?> {
    return NullableFallbackValueProvider(this, value).also(::addChild)
}

fun <T> Provider<T?>.orElse(provider: Provider<T & Any>): Provider<T & Any> {
    return FallbackProviderProvider(this, provider).also(::addChild)
}

@JvmName("else1")
fun <T> Provider<T?>.orElse(provider: Provider<T?>): Provider<T?> {
    return NullableFallbackProviderProvider(this, provider).also(::addChild)
}

fun <T> Provider<T?>.requireNonNull(message: String): Provider<T & Any> {
    return MapEverythingProvider(this) {
        check(it != null) { message }
        it
    }.also(::addChild)
}

fun <T, R> Provider<List<T>>.flatMap(transform: (T) -> List<R>): Provider<List<R>> {
    return FlatMapProvider(this, transform).also(::addChild)
}

fun <T> Provider<List<List<T>>>.flatten(): Provider<List<T>> {
    return FlatMapProvider(this) { it }.also(::addChild)
}

fun <K, V> Provider<List<Map<K, V>>>.merged(): Provider<Map<K, V>> {
    return MergeMapsProvider(this, ::LinkedHashMap).also(::addChild)
}

fun <K, V> Provider<List<Map<K, V>>>.merged(createMap: () -> MutableMap<K, V>): Provider<Map<K, V>> {
    return MergeMapsProvider(this, createMap).also(::addChild)
}

abstract class Provider<T> {
    
    private var children: ArrayList<Provider<*>>? = null
    
    private var initialized = false
    private var _value: T? = null
    
    open var value: T
        get() {
            if (!initialized) {
                _value = loadValue()
                initialized = true
            }
            
            return _value as T
        }
        protected set(value) {
            initialized = true
            _value = value
        }
    
    fun update() {
        _value = loadValue()
        children?.forEach(Provider<*>::update)
    }
    
    fun addChild(provider: Provider<*>) {
        if (children == null)
            children = ArrayList(1)
        
        children!!.add(provider)
    }
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>?): T = value
    
    protected abstract fun loadValue(): T
    
}

private class MapEverythingProvider<T, R>(
    private val provider: Provider<T>,
    private val transform: (T) -> R
) : Provider<R>() {
    override fun loadValue(): R {
        return transform(provider.value)
    }
}

private class MapOrNullProvider<T, R>(
    private val provider: Provider<T>,
    private val transform: (T & Any) -> R
) : Provider<R?>() {
    override fun loadValue(): R? {
        return provider.value?.let(transform)
    }
}

private class FallbackValueProvider<T>(
    private val provider: Provider<T>,
    private val fallback: T & Any
) : Provider<T & Any>() {
    override fun loadValue(): T & Any {
        return provider.value ?: fallback
    }
}

private class NullableFallbackValueProvider<T>(
    private val provider: Provider<T>,
    private val fallback: T?
) : Provider<T?>() {
    override fun loadValue(): T? {
        return provider.value ?: fallback
    }
}

private class FallbackProviderProvider<T>(
    private val provider: Provider<T?>,
    private val fallback: Provider<T & Any>
) : Provider<T & Any>() {
    override fun loadValue(): T & Any {
        return (provider.value ?: fallback.value)
    }
}

private class NullableFallbackProviderProvider<T>(
    private val provider: Provider<T?>,
    private val fallback: Provider<T?>
) : Provider<T?>() {
    override fun loadValue(): T? {
        return (provider.value) ?: fallback.value
    }
}

private class FlatMapProvider<T, R>(
    private val provider: Provider<List<T>>,
    private val transform: (T) -> List<R>
) : Provider<List<R>>() {
    override fun loadValue(): List<R> {
        return provider.value.flatMap(transform)
    }
}

private class MergeMapsProvider<K, V>(
    private val provider: Provider<List<Map<K, V>>>,
    private val createMap: () -> MutableMap<K, V>
) : Provider<Map<K, V>>() {
    override fun loadValue(): Map<K, V> {
        val newMap = createMap()
        provider.value.forEach { newMap.putAll(it) }
        return newMap
    }
}
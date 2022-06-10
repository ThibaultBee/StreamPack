package io.github.thibaultbee.streampack.internal.utils.extensions

fun <T> Iterable<List<T>>.unzip(): List<List<T>> {
    val expectedSize = this.first().size
    val result = MutableList(expectedSize) { mutableListOf<T>() }
    this.forEach { list ->
        if (list.size != expectedSize) {
            throw IndexOutOfBoundsException("List size must be the same")
        }
        list.forEachIndexed { index, element ->
            result[index].add(element)
        }
    }
    return result
}

fun main() {
    val color = androidx.compose.ui.graphics.Color.Red
    println(color.value.toString())
    try {
        println(color.value.toString().toLong())
    } catch(e: Exception) {
        println(e.message)
    }
}

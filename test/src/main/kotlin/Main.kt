import net.ririfa.yacla.yaml.YamlParser

fun main() {
    val input = object {}.javaClass.getResourceAsStream("/config.yml")
    if (input == null) error("Resource not found!")
    val map = YamlParser().parse(input)
    for ((k, v) in map) {
        println("$k -> ${if (v == "") "<empty string>" else v} (${v?.javaClass?.simpleName})")
    }
}

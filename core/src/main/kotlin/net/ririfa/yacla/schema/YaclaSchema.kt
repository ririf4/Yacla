package net.ririfa.yacla.schema

interface YaclaSchema<T : Any> {
    fun configure(def: FieldDefBuilder<T>)
}

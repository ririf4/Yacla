package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.annotation.*
import net.ririfa.yacla.defaults.DefaultHandlers
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.loader.util.UpdateContext
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/**
 * Default implementation of [ConfigLoader] that loads, validates, and updates a configuration object
 * from a file using a [ConfigParser].
 */
class DefaultConfigLoader<T : Any>(
    private val clazz: Class<T>,
    private val parser: ConfigParser,
    private val file: Path,
    private val logger: YaclaLogger?,
    private val resourcePath: String
) : ConfigLoader<T> {

    override var config: T = loadFromFile()
        private set

    override fun reload(): ConfigLoader<T> = apply {
        logger?.info("Reloading config from $file")
        config = loadFromFile()
    }

    override fun validate(): ConfigLoader<T> = apply {
        logger?.info("Validating config class: ${clazz.simpleName}")

        val kClazz = clazz.kotlin
        val ctorParams = kClazz.primaryConstructor?.parameters ?: emptyList()

        clazz.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .forEach { field ->
                field.isAccessible = true
                val fieldName = field.name
                val value = field.get(config)

                val kParam: KParameter? = ctorParams.firstOrNull { it.name == fieldName }
                val kProp: KProperty1<out Any, *>? = kClazz.memberProperties.firstOrNull { it.name == fieldName }

                getAnnotation(field.getAnnotation(Required::class.java), kParam, kProp, Required::class.java)
                    ?.let { req ->
                        val blank = value is String && value.isBlank()
                        if (value == null || blank) {
                            if (req.soft) {
                                logger?.warn("Soft required field '$fieldName' is not set.")
                            } else {
                                logger?.error("Required field '$fieldName' is missing or blank!")
                            }
                        }
                    }

                getAnnotation(field.getAnnotation(IfNullEvenRequired::class.java), kParam, kProp, IfNullEvenRequired::class.java)
                    ?.takeIf { value == null || (value is String && value.isBlank()) }
                    ?.let { ann ->
                        runCatching {
                            val handler = instantiateKClass(ann.handler)
                            handler.handle(value, config)
                            logger?.info("Executed handler ${ann.handler.simpleName} for '$fieldName'")
                        }.onFailure { e ->
                            logger?.error("Exception in handler '${ann.handler.simpleName}' for '$fieldName'", e)
                        }
                    }

                getAnnotation(field.getAnnotation(CustomValidateHandler::class.java), kParam, kProp, CustomValidateHandler::class.java)
                    ?.let { ann ->
                        runCatching {
                            val validator = instantiateKClass(ann.handler)
                            validator.validate(value, config)
                            logger?.info("CustomValidator '${ann.handler.simpleName}' executed for '$fieldName'")
                        }.onFailure { e ->
                            logger?.error("Failed to execute CustomValidator for '$fieldName'", e)
                        }
                    }

                getAnnotation(field.getAnnotation(Range::class.java), kParam, kProp, Range::class.java)
                    ?.let { range ->
                        if (value is Number) {
                            val v = value.toLong()
                            if (v !in range.min..range.max) {
                                logger?.error("Field '$fieldName' out of range (${range.min}..${range.max}): $v")
                            }
                        }
                    }
            }
    }

    override fun updateConfig(): Boolean {
        val strategy = UpdateStrategyRegistry.strategyFor(parser)
        return if (strategy != null) {
            val ctx = UpdateContext(parser, file, resourcePath, logger)
            val updated = strategy.updateIfNeeded(ctx)
            if (!updated) {
                logger?.info("Config already up-to-date.")
            }
            updated
        } else {
            logger?.warn("No UpdateStrategy registered for ${parser::class.java.simpleName}")
            false
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> constructConfig(clazz: Class<T>, rawMap: Map<String, Any?>): T {
        val kClazz = clazz.kotlin
        val raw = rawMap.toMutableMap()

        val keyMap = raw.keys.associateBy { it.lowercase() }
        fun resolveKey(name: String): String = keyMap[name.lowercase()] ?: name

        fun injectDefault(key: String, type: KClass<*>, defaultAnn: Default?) {
            if (defaultAnn == null) return
            if (raw[key] != null && !(raw[key] is String && raw[key].toString().isBlank())) return
            DefaultHandlers.get(type)?.let { handler ->
                runCatching {
                    raw[key] = handler.parse(defaultAnn.value, type)
                    logger?.info("Injected @Default for '$key' : ${raw[key]}")
                }.onFailure { logger?.error("Default parse failed for '$key'", it) }
            }
        }

        fun applyCustomLoader(key: String, loaderAnn: CustomLoader?) {
            if (loaderAnn == null || !raw.containsKey(key)) return
            runCatching {
                val loader = instantiate(loaderAnn.loader.java)
                raw[key] = loader.load(raw[key])
                logger?.info("Applied CustomLoader for '$key'")
            }.onFailure { logger?.error("CustomLoader failed for '$key'", it) }
        }

        kClazz.primaryConstructor?.let { ctor ->
            ctor.parameters.forEach { p ->
                val key = resolveKey(p.name ?: return@forEach)
                injectDefault(key, p.type.jvmErasure, p.findDefault(clazz))
                applyCustomLoader(key, p.findCustomLoader(clazz))
            }
            val args = ctor.parameters.associateWith { p -> raw[resolveKey(p.name ?: "")] }
            return ctor.callBy(args)
        }

        if (clazz.isRecord) {
            val components = clazz.recordComponents
            val ctor = clazz.declaredConstructors.first()
            components.forEach { comp ->
                val key = resolveKey(comp.name)
                injectDefault(key, comp.type.kotlin, comp.getAnnotation(Default::class.java))
                applyCustomLoader(key, comp.getAnnotation(CustomLoader::class.java))
            }
            val args = components.map { comp -> raw[resolveKey(comp.name)] }.toTypedArray()
            return ctor.newInstance(*args) as T
        }

        throw IllegalArgumentException("Class ${clazz.simpleName} must have a primary constructor or be a record")
    }

    private fun <A : Annotation> getAnnotation(
        fieldAnn: A?,
        kParam: KParameter?,
        kProp: KProperty1<out Any, *>?,
        annCls: Class<A>
    ): A? = fieldAnn ?: kParam.findAnnotationByClass(annCls) ?: kProp.findAnnotationByClass(annCls)

    private fun <A : Annotation> KAnnotatedElement?.findAnnotationByClass(annCls: Class<A>): A? =
        this?.annotations?.firstOrNull { it.annotationClass.java == annCls } as? A

    private fun <R : Any> instantiate(kClass: Class<out R>): R {
        return kClass.kotlin.objectInstance
            ?: kClass.kotlin.primaryConstructor?.takeIf { it.parameters.isEmpty() }?.call()
            ?: kClass.kotlin.createInstance()
    }

    private inline fun <reified R : Any> instantiateKClass(kClass: KClass<out R>): R {
        return kClass.objectInstance
            ?: kClass.primaryConstructor?.takeIf { it.parameters.isEmpty() }?.call()
            ?: kClass.createInstance()
    }

    private fun KParameter.findCustomLoader(parent: Class<*>): CustomLoader? =
        parent.declaredFields.firstNotNullOfOrNull { it.getAnnotation(CustomLoader::class.java) } ?: annotations.filterIsInstance<CustomLoader>().firstOrNull()
        ?: parent.kotlin.memberProperties.firstOrNull { it.name == name }?.annotations?.filterIsInstance<CustomLoader>()?.firstOrNull()

    private fun KParameter.findDefault(parent: Class<*>): Default? =
        parent.declaredFields.firstNotNullOfOrNull { it.getAnnotation(Default::class.java) } ?: annotations.filterIsInstance<Default>().firstOrNull()
        ?: parent.kotlin.memberProperties.firstOrNull { it.name == name }?.annotations?.filterIsInstance<Default>()?.firstOrNull()

    private fun loadFromFile(): T {
        val rawMap = Files.newInputStream(file).use { parser.parse(it) }
        return constructConfig(clazz, rawMap)
    }
}

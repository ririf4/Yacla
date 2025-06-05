package net.ririfa.yacla.annotation;

import net.ririfa.yacla.loader.FieldLoader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a custom {@link FieldLoader} to transform the raw parsed value
 * into the final value assigned to the annotated field.
 * <p>
 * This is useful when the configuration format provides generic structures (e.g., lists, strings),
 * and you need to convert them into specific types like enums, wrappers, or complex objects.
 * <p>
 * The loader is instantiated via its no-arg constructor and invoked during config loading.
 * <p>
 * Example:
 * <pre>{@code
 * public class EnumListLoader implements FieldLoader {
 *     public Object load(Object raw) {
 *         List<?> list = (List<?>) raw;
 *         return list.stream()
 *             .map(e -> MyEnum.valueOf(e.toString()))
 *             .collect(Collectors.toList());
 *     }
 * }
 *
 * public record AppConfig(
 *     @CustomLoader(loader = EnumListLoader.class)
 *     List<MyEnum> modes
 * ) {}
 * }</pre>
 *
 * @see FieldLoader
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomLoader {
    /**
     * Specifies the loader class responsible for transforming the raw value.
     *
     * @return the class implementing {@link FieldLoader}
     */
    Class<? extends FieldLoader> loader();
}

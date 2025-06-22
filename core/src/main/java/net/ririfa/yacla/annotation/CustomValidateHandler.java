package net.ririfa.yacla.annotation;

import net.ririfa.yacla.loader.FieldValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attaches a custom {@link FieldValidator} to the annotated field,
 * allowing advanced validation logic beyond built-in annotations like {@code @Required} or {@code @Range}.
 * <p>
 * This is useful when the field needs domain-specific checks, cross-field conditions,
 * or dynamic rules that cannot be expressed declaratively.
 * <p>
 * The validator is instantiated via no-arg constructor and executed during {@code loader.validate()}.
 * <p>
 * Example:
 * <pre>{@code
 * public class PortValidator implements FieldValidator {
 *     public void validate(Object value, Object configInstance) {
 *         int port = (int) value;
 *         if (port < 1024 || port > 65535) {
 *             throw new IllegalArgumentException("Port must be between 1024 and 65535");
 *         }
 *     }
 * }
 *
 * public record ServerConfig(
 *     @CustomValidateHandler(handler = PortValidator.class)
 *     int port
 * ) {}
 * }</pre>
 *
 * @see FieldValidator
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomValidateHandler {
    /**
     * Specifies the validator class that will be invoked on this field during validation.
     *
     * @return the class implementing {@link FieldValidator}
     */
    Class<? extends FieldValidator> handler();
}

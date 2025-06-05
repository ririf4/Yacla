package net.ririfa.yacla.annotation;

import net.ririfa.yacla.loader.ErrorHandlerWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that if the annotated field is found to be {@code null} or blank during config loading,
 * the specified {@link ErrorHandlerWith} should be invoked instead of failing immediately.
 * <p>
 * This is especially useful for recovering from partially missing configuration,
 * or for performing side-effects (e.g., logging, fallback mutation) when required fields are not present.
 * <p>
 * Typically used in combination with {@link net.ririfa.yacla.annotation.Required}.
 * This annotation does not override validation failure unless {@link ErrorHandlerWith} handles it explicitly.
 * <p>
 * Example:
 * <pre>{@code
 * public class FallbackHandler implements ErrorHandlerWith {
 *     public void handle(Object fieldValue) {
 *         System.out.println("Field was null! Recovering...");
 *     }
 * }
 *
 * public record AppConfig(
 *     @IfNullEvenRequired(handler = FallbackHandler.class)
 *     @Required
 *     String importantToken
 * ) {}
 * }</pre>
 *
 * @see ErrorHandlerWith
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IfNullEvenRequired {
    /**
     * Specifies the handler class to invoke if the field value is null or blank.
     *
     * @return the class implementing {@link ErrorHandlerWith}
     */
    Class<? extends ErrorHandlerWith> handler();
}

package net.ririfa.yacla.annotation;

import net.ririfa.yacla.loader.FieldValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomValidateHandler {
    Class<? extends FieldValidator> handler();
}
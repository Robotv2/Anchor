package fr.robotv2.anchor.api.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    String value();

    boolean nullable() default true;

    String rawType() default ""; // Raw type if needed
}

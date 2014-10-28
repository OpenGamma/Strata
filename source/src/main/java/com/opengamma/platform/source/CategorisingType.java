package com.opengamma.platform.source;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation declaring an interface that should be
 * stored when indexing objects implementing the
 * interface.
 * <p>
 * This means that the data will be availble to
 * use when searching for objects. For example,
 * search for all objects implementing the
 * {@code SpecialTrade} interface, where
 * {@code SpecialTrade} has been annotated as a
 * CategorisingType.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CategorisingType {
}

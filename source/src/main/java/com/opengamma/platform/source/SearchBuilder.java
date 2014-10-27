/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * Helper class to allow easier construction of {@link Search}
 * instances. Specifically, it automatically handles the
 * {@code Optional}s used for specifying type matches.
 */
public class SearchBuilder {

  /**
   * The scheme (as specified by {@link StandardId}) to
   * be searched for.
   */
  private final String scheme;

  /**
   * Optional class that specifies whether to search for
   * instances that are the same type or subclasses of the
   * specified type. This is useful if the specific
   * implementation type is unknown. Note that if the
   * specified type is too broad, the search is likely
   * to be slow.
   * <p>
   * If both this and {@link #specificType} are specified,
   * then the super type will not be used in the search as
   * it is unnecessary. However, the specified supertype
   * must be a parent of the specific type else an exception
   * will be thrown when the {@code Search} is built.
   */
  private Optional<Class<?>> superType = Optional.empty();

  /**
   * Optional class that specifies whether to search for
   * instances that are of the same type as the specified
   * type.
   * <p>
   * If both this and {@link #superType} are specified,
   * then the super type will not be used in the search as
   * it is unnecessary. However, the specified supertype
   * must be a parent of this type else an exception
   * will be thrown when the {@code Search} is built.
   */
  private Optional<Class<? extends IdentifiableBean>> specificType = Optional.empty();

  /**
   * Specifies attributes to be searched for. All attributes
   * must match exactly for an object to be matched. Note that
   * attributes that have been indexed will likely perform much
   * better than those that have not.
   */
  private Map<String, String> attributes = new HashMap<>();

  /**
   * Create the builder with the specified scheme.
   *
   * @param scheme  the scheme (as specified by {@link StandardId})
   *   to be searched for
   */
  public SearchBuilder(String scheme) {
    this.scheme = ArgChecker.notNull(scheme, "scheme");
  }

  /**
   * Specifies to search for instances that are the same type
   * of subclasses of the specified type. This is useful if
   * the specific implementation type is unknown. Note that
   * if the specified type is too broad, the search is likely
   * to be slow.
   * <p>
   * If both this method and {@link #withSpecificType(Class)}
   * are specified, then the super type will not be used in
   * the search as it is unnecessary. However, the specified
   * supertype must be a parent of the specific type else an
   * exception will be thrown.
   *
   * @param type  the supertype to search for
   * @return the builder
   * @throws IllegalStateException if specified superType is
   *   incompatible with previously specified specific type
   */
  public SearchBuilder withSuperType(Class<?> type) {
    superType = Optional.ofNullable(type);
    return this;
  }

  /**
   * Specifies to seach for instances that are the the same type
   * as the specified
   * type.
   * <p>
   * If both this and {@link #withSuperType} are specified,
   * then the super type will not be used in the search as
   * it is unnecessary. However, the specified supertype
   * must be a parent of this type else an exception
   * will be thrown.
   *
   * @param type  the type to search for
   * @returnthe builder
   * @throws IllegalStateException if specified type is
   *   incompatible with previously specified super type
   */
  public SearchBuilder withSpecificType(Class<? extends IdentifiableBean> type) {
    specificType = Optional.ofNullable(type);
    return this;
  }

  /**
   * Adds an attribute to the search criteria. All attributes must match
   * exactly for an object to be matched. Note that attributes that have
   * been indexed will likely perform much better than those that haven't.
   *
   * @param attribute  the attribute name
   * @param value  the attribute value
   * @return the builder
   */
  public SearchBuilder withAttribute(String attribute, String value) {
    attributes.put(ArgChecker.notNull(attribute, "attribute"), ArgChecker.notNull(value, "value"));
    return this;
  }

  /**
   * Builds the {@code Search} object.
   *
   * @return the {@code Search} object
   */
  public Search build() {
    return Search.builder()
        .scheme(scheme)
        .superType(superType)
        .specificType(specificType)
        .attributes(attributes)
        .build();
  }
}

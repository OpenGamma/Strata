/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Supplier;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A supplier of object identifiers.
 * <p>
 * An object identifier consists of a scheme and value.
 * This class creates object identifiers for a fixed scheme name, where each
 * value is an incrementing number. The values are created in a thread-safe way.
 * <p>
 * This class is thread-safe and not externally mutable.
 */
public class ObjectIdSupplier
    implements Supplier<ObjectId> {

  /**
   * The scheme.
   */
  private final String scheme;
  /**
   * The generator of identifiers.
   */
  private final AtomicLong generator = new AtomicLong();

  /**
   * Creates an instance specifying the scheme.
   * <p>
   * The supplier returns identifiers within this scheme.
   * 
   * @param scheme  the base scheme, not empty
   */
  public ObjectIdSupplier(String scheme) {
    this.scheme = ArgChecker.matches(UniqueId.REGEX_SCHEME, scheme, "scheme");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the scheme in use.
   * 
   * @return the scheme
   */
  public String getScheme() {
    return scheme;
  }

  //-------------------------------------------------------------------------
  /**
   * Generates the next object identifier.
   * 
   * @return the next object identifier
   */
  @Override
  public ObjectId get() {
    long id = generator.incrementAndGet();
    return ObjectId.of(scheme, Long.toString(id));
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "ObjectIdSupplier[" + scheme + "]";
  }

}

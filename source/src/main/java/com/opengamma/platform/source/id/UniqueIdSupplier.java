/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Supplier;
import com.opengamma.collect.ArgChecker;

/**
 * A supplier of unique identifiers with different version numbers.
 * <p>
 * A unique identifier consists of an object identifier and value.
 * This class creates unique identifiers for a fixed object identifier, where each
 * version is an incrementing number. The versions are created in a thread-safe way.
 * <p>
 * This class is thread-safe and not externally mutable.
 */
public class UniqueIdSupplier
    implements Supplier<UniqueId> {

  /**
   * The object identifier.
   */
  private final ObjectId objectId;
  /**
   * The generator of versions.
   */
  private final AtomicLong generator = new AtomicLong();

  /**
   * Creates an instance specifying the object identifier.
   * <p>
   * The supplier returns versions of this object identifier.
   * 
   * @param objectId  the base object identifier
   */
  public UniqueIdSupplier(ObjectId objectId) {
    this.objectId = ArgChecker.notNull(objectId, "objectId");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the object identifier in use.
   * 
   * @return the object identifier
   */
  public ObjectId getObjectId() {
    return objectId;
  }

  //-------------------------------------------------------------------------
  /**
   * Generates the next unique identifier.
   * 
   * @return the next unique identifier
   */
  @Override
  public UniqueId get() {
    long version = generator.incrementAndGet();
    return objectId.atVersion(Long.toString(version));
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "UniqueIdSupplier[" + objectId + "]";
  }

}

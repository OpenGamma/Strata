/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

/**
 * TODO does this need to exist any more?
 */
public final class EngineFunctionUtils {

  private EngineFunctionUtils() {
  }

  /**
   * Returns metadata for the outputs a type can produce.
   * The type's public methods are scanned looking for {@link Output} annotations
   * @param type A function or class that can produce output values for the engine
   * @return Metadata for each of the methods that can produce an output
   * TODO this is a big problem
   * we can't possibly know the implementations of an interface at this point and therefore can't provide a
   * constructor to FunctionMetadata. which is a bit of a snag
   */

}

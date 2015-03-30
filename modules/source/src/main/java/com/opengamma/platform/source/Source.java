/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.Result;

/**
 * The Source interface describes an internal source of data.
 * <p>
 * This is intended to be an API (as opposed to an SPI) used within the calculation server.
 * Data can only be retrieved using its standard identifier.
 * <p>
 * No assumption is made about where the data comes from.
 * It may be completely in-memory or be backed by a {@link SourceProvider}.
 */
public interface Source {

  /**
   * Gets an item using its standard identifier.
   * <p>
   * The identifier uniquely identifies a single entity.
   * <p>
   * The type is expressed as a standard {@link Class} object.
   *
   * @param <T>  the expected type of the item
   * @param identifier  the identifier for the item
   * @param targetType  the type of the result
   * @return a {@code Result} containing the item if it exists and is of the correct type,
   *  else the reason why it cannot be returned
   */
  public default <T extends IdentifiableBean> Result<T> get(StandardId identifier, Class<T> targetType) {
    return get(identifier, TypeToken.of(targetType));
  }

  /**
   * Gets an item using its standard identifier.
   * <p>
   * The identifier uniquely identifies a single entity.
   * <p>
   * A {@code TypeToken} is used to express generic parameterized types, such as {@code Trade<Swap>}:
   * <p>
   * <pre>{@code
   *  new TypeToken<Trade<Swap>>() {};
   * }</pre>
   *
   * @param <T>  the expected type of the item
   * @param identifier  the identifier for the item
   * @param targetType  the type of the result
   * @return a {@code Result} containing the item if it exists and is of the correct type,
   *  else the reason why it cannot be returned
   */
  public abstract <T extends IdentifiableBean> Result<T> get(StandardId identifier, TypeToken<T> targetType);

}

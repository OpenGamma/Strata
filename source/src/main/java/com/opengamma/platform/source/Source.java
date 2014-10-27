/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import com.opengamma.collect.result.Result;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * The Source interface describes an internal source of data.
 * <p>
 * This is intended to be an API (as opposed to an SPI) used
 * within the calculation server. Data can only be retrieved
 * using its standard id. No assumption is made about where
 * the data comes from - it may be completely in-memory or
 * be backed by a {@link SourceProvider}.
 */
public interface Source {

  /**
   * Gets an item using its standard identifier.
   *
   * @param id  the identifier for the item
   * @param type  the expected type of the item
   * @param <T>  the expected type of the item
   * @return a {@code Result} containing the item if
   *   it exists and is of the correct type, else the
   *   reason why it cannot be returned
   */
  public abstract <T extends IdentifiableBean> Result<T> get(StandardId id, Class<T> type);

}

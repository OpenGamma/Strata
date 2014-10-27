/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import java.util.Collection;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * A source that implements methods that allow records to
 * be located by criteria other than just an id.
 * <p>
 * This is intended to be an API (as opposed to an SPI) used
 * within the calculation server. Data can be retrieved either
 * by providing a collection of {@code StandardId}s, or by
 * providing a {@link Search} object.
 * using its standard id. No assumption is made about where
 * the data comes from - it may be completely in-memory or
 * be backed by a {@link SourceProvider}.
 */
public interface SearchableSource extends Source {

  /**
   * Execute a search using the provided criteria and return
   * the matching data. If no data matches the criteria then
   * the returned set will be empty.
   *
   * @param search  the search to be undertaken
   * @return the set of data that matches the search
   *   criteria, may be empty
   */
  public abstract ImmutableSet<IdentifiableBean> search(Search search);

  /**
   * Execute a search using the supplied set of identifiers,
   * returning data with matching ids. If no matching data
   * is found then the returned set will be empty.
   *
   * @param ids  the collection of ids to search for
   * @return the set of data that matches the search
   *   criteria, may be empty
   */
  public abstract ImmutableSet<IdentifiableBean> search(Collection<StandardId> ids);

  /**
   * Cross id lookups.
   // TODO - implement when API is stabilized
   */
  public abstract Optional<StandardId> convertId();
}



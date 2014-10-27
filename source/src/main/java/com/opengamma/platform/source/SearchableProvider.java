/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

/**
 *
 * The SearchableProvider interface is used to provide
 * searches for a data store.
 * <p>
 * This is intended to be an SPI (service provider interface)
 * and for this reason no assumptions are made about how the
 * search will be performed.
 * <p>
 * Whilst implementations of this class may also be
 * {@link SourceProvider}s, this does not have to be the case.
 * For instance, indexing into a key-value store may be provided
 * by an external indexing component.
 */
public interface SearchableProvider {

  /**
   * Performs a search returning SearchResults indicating the ids
   * of matching components. Note that the search result does not
   * hold the matching data, just the identifiers. The identifiers
   * can then be used with {@link SourceProvider#bulkGet(Iterable)}
   * to return the required data. Doing this enables some of the
   * data to be returned from a cache.
   *
   * @param search  the search to be performed
   * @return the result of the search
   */
  public abstract SearchResult search(Search search);
}

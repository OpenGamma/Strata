/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import static com.opengamma.platform.source.SearchResultStatus.FULL;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.Guavate;
import com.opengamma.collect.result.FailureReason;
import com.opengamma.collect.result.Result;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * A searchable source which wraps a {@link SearchableProvider}
 * and a {@link SourceProvider} to implement the
 * {@link SearchableSource} interface.
 * <p>
 * The {@code SourceProvider} and {@code SearchProvider} may
 * both be provided by the same instance. However, they are split
 * apart to allow for cases where search and indexing
 * capabilities are provided separately.
 */
public class DefaultSearchableSource implements SearchableSource {

  private static final Logger log = LoggerFactory.getLogger(DefaultSearchableSource.class);

  private final SourceProvider sourceProvider;

  private final SearchableProvider searchProvider;

  /**
   * Creates the {@code SearchableSource}. The {@code SourceProvider}
   * is used get the actual data. The {@code SearchableProvider} is
   * used to lookup standard ids for data, which will be subsequently
   * retrieved using the {@code SourceProvider}.
   *
   * @param sourceProvider  the source provider
   * @param searchProvider  the search provider
   */
  public DefaultSearchableSource(SourceProvider sourceProvider, SearchableProvider searchProvider) {
    this.sourceProvider = ArgChecker.notNull(sourceProvider, "sourceProvider");
    this.searchProvider = ArgChecker.notNull(searchProvider, "searchProvider");
  }

  @Override
  public ImmutableSet<IdentifiableBean> search(Iterable<StandardId> ids) {
    return ImmutableSet.copyOf(sourceProvider.bulkGet(ids).values());
  }

  @Override
  public ImmutableSet<IdentifiableBean> search(Search search) {

    SearchResult searchResult = searchProvider.search(search);
    Collection<StandardId> matchingIds = searchResult.getMatchingIds();
    Collection<IdentifiableBean> beans =
        sourceProvider.bulkGet(matchingIds).values();

    if (beans.size() != matchingIds.size()) {
      log.warn("Received {} ids from search provider but source provider only returned {} of them",
          matchingIds.size(), beans.size());
    }

    return searchResult.getMatchStatus() == FULL ?
        ImmutableSet.copyOf(beans) :
        beans.stream()
            .filter(search::matches)
            .collect(Guavate.<IdentifiableBean>toImmutableSet());
  }


/**
 * Gets an item using its standard identifier.
 * <p>
 * This makes a call to the {@link SourceProvider} but adds a
 * type check ensuring that the returned object is of the
 * expected type.
 * <p>
 * If no object with a matching id can be found, or if one
 * can be found but it has the wrong type, then a failure
 * {@code Result} will be returned.
 *
 * @param id  the identifier for the item
 * @param type  the expected type of the item
 * @param <T>  the expected type of the item
 * @return a {@code Result} containing the item if
 *   it exists and is of the correct type, else the
 *   reason why it cannot be returned
 */
 @Override
  public <T extends IdentifiableBean> Result<T> get(StandardId id, Class<T> type) {

    Optional<IdentifiableBean> opt = sourceProvider.get(id);
    return opt
        .map(b -> attemptTypeConversion(id, type, b))
        .orElse(createMissingDataFailure(id));
  }

  private <T extends IdentifiableBean> Result<T> attemptTypeConversion(
      StandardId id, Class<T> type, IdentifiableBean bean) {

    Class<? extends IdentifiableBean> receivedType = bean.getClass();
    return type.isAssignableFrom(receivedType) ?
        Result.success(type.cast(bean)) :
        createIncorrectTypeFailure(id, type, receivedType);
  }

  private <T extends IdentifiableBean> Result<T> createMissingDataFailure(StandardId id) {
    return Result.<T>failure(FailureReason.MISSING_DATA, "Unable to find data with id: {}", id);
  }

  private <T extends IdentifiableBean> Result<T> createIncorrectTypeFailure(
      StandardId id, Class<T> type, Class<? extends IdentifiableBean> receivedType) {
    return Result.<T>failure(FailureReason.MISSING_DATA,
        "Found data with id: {} of type: {} but expected type was: {}",
        id, receivedType.getSimpleName(), type.getSimpleName());
  }

}

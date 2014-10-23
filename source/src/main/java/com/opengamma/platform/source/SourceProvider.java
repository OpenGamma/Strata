/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.opengamma.collect.Guavate;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;
import com.opengamma.platform.source.id.StandardIdentifiable;

/**
 * The SourceProvider interface is used to expose a data store.
 * Minimal assumptions are made about the form of the data store
 * (e.g. a key value data store, a relational database etc) and
 * the only method that needs to be implemented is
 * {@link #get(StandardId)}.
 * <p>
 * This interface is analogous to the {@link Source} interface.
 * The Source interface is provided for use by callers inside
 * the calculation server, whereas this one is for use by
 * developers integrating a data store. This different focus
 * explains the slightly different apis.
 */
public interface SourceProvider {

  /**
   * Get an item using one of its external identifiers.
   *
   * @param id  the identifier for the item
   * @return an <code>Optional</code> containing the item if it exists
   */
  public abstract Optional<IdentifiableBean> get(StandardId id);

  /**
   * Retrieve a collection of items from a collection of ids.
   * Only ids which are found will be in the returned collection.
   * <p>
   * The default implementation makes multiple calls to the
   * {@link #get(StandardId)} method. This should be overridden
   * if the underlying data store has a more efficient way of
   * performing the operation.
   *
   * @param ids  the collection of ids to get
   * @return the collection of matching items
   */
  public default Map<StandardId, IdentifiableBean> bulkGet(Iterable<? extends StandardId> ids) {
    return Guavate.stream(ids)
        .map(this::get)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toMap(StandardIdentifiable::getStandardId, i -> i));
  }

  /**
   * Returns the subset of data items which have been updated since
   * a particular point in time. This allows clients to check whether
   * data items held locally (e.g. in a cache) are stale and replace
   * those items which are.
   * <p>
   * The default implementation does no check and returns the input list
   * of ids. This is safe, in that no data will be stale, however it is
   * inefficient as data which has not actually changed will get
   * refreshed. This should be overridden if the underlying data store
   * has a more efficient way of performing the operation. Alternatively,
   * if the underlying data store content is static, this should be
   * overridden to always return an empty collection.
   *
   * @param ids  the set of ids to check for staleness
   * @param checkpoint  the time to check against
   * @return the collection of data items from the initial set of ids
   *   which have been updated since the supplied check point
   */
  public default Collection<StandardId> changedSince(Collection<StandardId> ids, LocalDateTime checkpoint) {
    return ids;
  }

  public default void registerForUpdates(UpdateNotificationListener listener) {
    // do nothing by default
  }
}

/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import java.time.Instant;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.id.StandardIdentifiable;

/**
 * The SourceProvider interface is used to expose a data store.
 * <p>
 * This is intended to be an SPI (service provider interface)
 * and for this reason minimal assumptions are made about the
 * form of the data store (for example, a key value data store,
 * a relational database and so on) and the only method that
 * needs to be implemented is {@link #get(StandardId)}. Note
 * that overriding other methods may improve performance,
 * specifically the {@link #bulkGet(Iterable)} and
 * {@link #changedSince(Iterable, Instant)} methods.
 * <p>
 * This interface is analogous to the {@link Source} interface.
 * The Source interface is provided for use by callers inside
 * the calculation server, whereas this one is for use by
 * developers integrating a data store. This different focus
 * explains the slightly different apis.
 */
public interface SourceProvider {

  /**
   * Gets an item using its standard identifier.
   *
   * @param id  the identifier for the item
   * @return an {@code Optional} containing the item if it exists
   */
  public abstract Optional<IdentifiableBean> get(StandardId id);

  /**
   * Retrieve a collection of items from a collection of identifiers.
   * Only identifiers which are found will be in the returned collection.
   * <p>
   * The default implementation makes multiple calls to the
   * {@link #get(StandardId)} method. This should be overridden
   * if the underlying data store has a more efficient way of
   * performing the operation.
   *
   * @param ids  the collection of identifiers to get
   * @return the collection of matching items
   */
  public default ImmutableMap<StandardId, IdentifiableBean> bulkGet(Iterable<StandardId> ids) {
    return Guavate.stream(ids)
        .map(this::get)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Guavate.toImmutableMap(StandardIdentifiable::getStandardId, i -> i));
  }

  /**
   * Returns the subset of data items which have been updated since
   * a particular point in time. This allows clients to check whether
   * data items held locally (e.g. in a cache) are stale and replace
   * those items which are.
   * <p>
   * The default implementation does no check and returns the input list
   * of identifiers. This is safe, in that no data will be stale, however it is
   * inefficient as data which has not actually changed will get
   * refreshed. This should be overridden if the underlying data store
   * has a more efficient way of performing the operation. Alternatively,
   * if the underlying data store content is static, this should be
   * overridden to always return an empty collection.
   *
   * @param ids  the set of identifiers to check for staleness
   * @param checkpoint  the time to check against
   * @return the collection of data items from the initial set of identifiers
   *   which have been updated since the supplied check point
   */
  public default ImmutableSet<StandardId> changedSince(Iterable<StandardId> ids, Instant checkpoint) {
    return ImmutableSet.copyOf(ids);
  }

  /**
   * Registers a listener to receive update events.
   * 
   * @param listener  the listener
   */
  public default void registerForUpdates(UpdateNotificationListener listener) {
    // do nothing by default
  }

}

/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import java.time.Instant;
import java.util.Collection;

import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * Interface allowing a provider of data to notify clients that
 * data has been changed.
 * <p>
 * Two methods are provided and a user of the class may call
 * either depending on which is most appropriate:
 * <ul>
 *   <li>
 *     {@link #idsUpdated(Collection)} is intended for use when
 *     a process is checking for changes (for example, via the
 *     {@link SourceProvider#changedSince(Iterable, Instant)}
 *     method and therefore only knows about the ids. In this
 *     case, the listener will need to go and get the data for
 *     the ids.
 *   </li>
 *   <li>
 *     {@link #dataUpdated(Collection)} is intended for use when
 *     an updated data item is received by a data provider. As well
 *     as pushing the updated data into the data store, they can
 *     supply the data directly to the Source, avoiding the need
 *     for the Source to query the source for the item.
 *   </li>
 * </ul>
 */
public interface UpdateNotificationListener {

  /**
   * Notify that data has been updated, providing the ids
   * of the changed values.
   * <p>
   * Intended for use when a process is checking for changes
   * for example, via the
   * {@link SourceProvider#changedSince(Iterable, Instant)}
   * method and therefore only knows about the ids. In this case,
   * the listener will need to go and get the data for the ids.
   *
   * @param updates  the ids of the updated data
   */
  public abstract void idsUpdated(Collection<StandardId> updates);

  /**
   * Notify that data has been updated, providing the new values
   * for the data.
   * <p>
   * Intended for use when an updated data item is received by a
   * data provider. As well as pushing the updated data into the
   * data store, they can supply the data directly to the Source,
   * avoiding the need for the Source to query the source for
   * the item.
   *
   * @param updates  the updated data
   */
  public abstract void dataUpdated(Collection<IdentifiableBean> updates);
}

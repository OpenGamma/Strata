/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import java.util.Collection;

import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * Interface allowing a provider of data to notify clients that
 * data has been changed.
 */
public interface UpdateNotificationListener {

  /**
   * Notify that data has been updated, providing the new
   * values for the data.
   *
   * @param updates  the updated data
   */
  public abstract void notifyDataUpdates(Collection<IdentifiableBean> updates);

  /**
   * Notify that data has been updated, providing the ids
   * of the changed values.
   *
   * @param updates  the ids of the updated data
   */
  public abstract void notifyIdsUpdated(Collection<StandardId> updates);
}

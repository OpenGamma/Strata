/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

import org.joda.beans.Bean;

/**
 * Provides uniform access to beans that can supply a standard identifier.
 * <p>
 * Joda-Bean domain objects that can be identified using a {@link StandardId} should implement this interface.
 * <p>
 * This interface makes no guarantees about the thread-safety of implementations.
 * However, wherever possible implementations should be immutable.
 */
public interface IdentifiableBean extends StandardIdentifiable, Bean {
  // see inherited interfaces
}

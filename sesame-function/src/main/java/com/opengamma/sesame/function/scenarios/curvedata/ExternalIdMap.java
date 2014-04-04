/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;

/**
 * Adaptor for using an {@link ExternalId} to look up values in a map keyed by {@link ExternalIdBundle}.
 * TODO this is generally useful, move to a different package
 */
public class ExternalIdMap<T> {

  /** Index of IDs to ID bundles. */
  private final Map<ExternalId, ExternalIdBundle> _index = new HashMap<>();

  /** The values. */
  private final Map<ExternalIdBundle, T> _values;

  public ExternalIdMap(Map<ExternalIdBundle, T> values) {
    _values = ImmutableMap.copyOf(ArgumentChecker.notNull(values, "values"));

    for (ExternalIdBundle idBundle : values.keySet()) {
      for (ExternalId id : idBundle) {
        ExternalIdBundle existingValue = _index.put(id, idBundle);

        if (existingValue != null) {
          throw new IllegalArgumentException("Bundles found with overlapping ID sets. This isn't supported. " +
                                                 idBundle + ", " + existingValue);
        }
      }
    }
  }

  /**
   * Returns a value keyed by a bundle containing an ID.
   *
   * @param id an ID
   * @return the value keyed by a bundle containing the ID, null if not found
   */
  public T get(ExternalId id) {
    ExternalIdBundle idBundle = _index.get(id);

    if (idBundle == null) {
      return null;
    }
    return get(idBundle);
  }

  /**
   * Returns a value keyed by an ID bundle.
   *
   * @param idBundle an ID bundle
   * @return the value keyed by the bundle, null if not found
   */
  public T get(ExternalIdBundle idBundle) {
    return _values.get(idBundle);
  }

  /**
   * Returns the ID bundle containing an ID.
   *
   * @param id an ID
   * @return the bundle containing the ID, null if not found
   */
  public ExternalIdBundle getBundle(ExternalId id) {
    return _index.get(id);
  }
}

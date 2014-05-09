/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Simple market data source backed by an immutable map. This class
 * will generally only be useful for test scenarios.
 */
public final class MapMarketDataSource implements MarketDataSource {

  /**
   * Field to be returned if not specified in request..
   */
  public static final FieldName DEFAULT_FIELD = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);

  /**
   * The fixed set of values to be returned on request.
   */
  private final Map<Pair<ExternalIdBundle, FieldName>, Object> _values;

  /**
   * Private constructor to create the map - used only by the builder.
   *
   * @param values values to be held for this source
   */
  private MapMarketDataSource(Map<Pair<ExternalIdBundle, FieldName>, Object> values) {
    _values = ImmutableMap.copyOf(values);
  }

  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    Object value = _values.get(Pairs.of(id, fieldName));
    return value != null ?
           Result.success(value) :
           Result.failure(FailureStatus.MISSING_DATA, "No value for {}/{}", id, fieldName);
  }

  /**
   * Creates a builder for populating the source.
   *
   * @return a builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MapMarketDataSource other = (MapMarketDataSource) obj;
    return Objects.equals(this._values, other._values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_values);
  }

  /**
   * Builder for the data source.
   */
  public static class Builder {

    /**
     * The set of values being built up.
     */
    private final Map<Pair<ExternalIdBundle, FieldName>, Object> _values = new HashMap<>();

    /**
     * Private constructor
     */
    private Builder() {
    }

    /**
     * Adds a value to the data source.
     *
     * @param id the value's ID
     * @param fieldName the value's field name
     * @param value the value
     * @return this builder
     */
    public Builder add(ExternalIdBundle id, FieldName fieldName, Object value) {
      ArgumentChecker.notNull(id, "id");
      ArgumentChecker.notNull(fieldName, "fieldName");
      ArgumentChecker.notNull(value, "value");

      _values.put(Pairs.of(id, fieldName), value);
      return this;
    }

    /**
     * Adds a value to the data source.
     *
     * @param id the value's ID
     * @param fieldName the value's field name
     * @param value the value
     * @return this builder
     */
    public Builder add(ExternalId id, FieldName fieldName, Object value) {
      return add(id.toBundle(), fieldName, value);
    }

    /**
     * Adds a value to the data source using the field name {@link #DEFAULT_FIELD}.
     *
     * @param id the value's ID
     * @param value the value
     * @return this builder
     */
    public Builder add(ExternalIdBundle id, Object value) {
      return add(id, DEFAULT_FIELD, value);
    }

    /**
     * Adds a value to the data source using the field name {@link #DEFAULT_FIELD}.
     *
     * @param id the value's ID
     * @param value the value
     * @return this builder
     */
    public Builder add(ExternalId id, Object value) {
      return add(id.toBundle(), value);
    }

    /**
     * Build the MarketDataSource using the values which have been
     * added to the builder.
     * 
     * @return a data source built from this builder's data
     */
    public MarketDataSource build() {
      return new MapMarketDataSource(_values);
    }
  }
}

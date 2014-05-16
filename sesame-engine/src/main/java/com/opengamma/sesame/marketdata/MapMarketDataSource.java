/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.Map;

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
 * Simple market data source backed by an immutable map.
 * <p>
 * This is primarily intended for use in test cases.
 */
public final class MapMarketDataSource implements MarketDataSource {

  /**
   * Field to be returned if not specified in request.
   */
  public static final FieldName DEFAULT_FIELD = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);

  /**
   * The fixed set of values to be returned on request.
   */
  private final ImmutableMap<Pair<ExternalIdBundle, FieldName>, Object> _values;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty data source with no market data values.
   *
   * @return the empty market data source
   */
  public static MarketDataSource of() {
    return new MapMarketDataSource(ImmutableMap.<Pair<ExternalIdBundle, FieldName>, Object>of());
  }

  /**
   * Obtains a simple single element data source using the field name {@link #DEFAULT_FIELD}.
   *
   * @param id  the external identifiers of the value
   * @param value  the value
   * @return the single element market data source
   */
  public static MarketDataSource of(ExternalIdBundle id, Object value) {
    return builder().add(id, value).build();
  }

  /**
   * Obtains a simple single element data source using the field name {@link #DEFAULT_FIELD}.
   *
   * @param id  the external identifier of the value
   * @param value  the value
   * @return the single element market data source
   */
  public static MarketDataSource of(ExternalId id, Object value) {
    return builder().add(id, value).build();
  }

  /**
   * Creates a builder for populating the source.
   *
   * @return a builder
   */
  public static Builder builder() {
    return new Builder();
  }

  //-------------------------------------------------------------------------
  /**
   * Private constructor to create the map - used only by the builder.
   *
   * @param values values to be held for this source
   */
  private MapMarketDataSource(Map<Pair<ExternalIdBundle, FieldName>, Object> values) {
    _values = ImmutableMap.copyOf(values);
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    Object value = _values.get(Pairs.of(id, fieldName));
    return value != null ?
           Result.success(value) :
           Result.failure(FailureStatus.MISSING_DATA, "No value for {}/{}", id, fieldName);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MapMarketDataSource) {
      final MapMarketDataSource other = (MapMarketDataSource) obj;
      return _values.equals(other._values);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return _values.hashCode();
  }

  @Override
  public String toString() {
    return _values.toString();
  }

  //-------------------------------------------------------------------------
  /**
   * Builder for the data source.
   */
  public static final class Builder {

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
     * @param id  the external identifiers of the value
     * @param fieldName  the field name of the value
     * @param value  the value
     * @return this builder, for method chaining
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
     * @param id  the external identifier of the value
     * @param fieldName  the field name of the value
     * @param value  the value
     * @return this builder, for method chaining
     */
    public Builder add(ExternalId id, FieldName fieldName, Object value) {
      return add(id.toBundle(), fieldName, value);
    }

    /**
     * Adds a value to the data source using the field name {@link #DEFAULT_FIELD}.
     *
     * @param id  the external identifiers of the value
     * @param value  the value
     * @return this builder, for method chaining
     */
    public Builder add(ExternalIdBundle id, Object value) {
      return add(id, DEFAULT_FIELD, value);
    }

    /**
     * Adds a value to the data source using the field name {@link #DEFAULT_FIELD}.
     *
     * @param id  the external identifier of the value
     * @param value  the value
     * @return this builder, for method chaining
     */
    public Builder add(ExternalId id, Object value) {
      return add(id.toBundle(), value);
    }
    
    /**
     * Adds all of the pairs in the given map.
     * @param valueMap the map of values
     * @return this builder
     */
    public Builder addAll(Map<ExternalIdBundle, ? extends Object> valueMap) {
      for (Map.Entry<ExternalIdBundle, ? extends Object> entry : valueMap.entrySet()) {
        add(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * Build the data source using the values which have been added to the builder.
     * 
     * @return a data source built from this builder's data
     */
    public MarketDataSource build() {
      return new MapMarketDataSource(_values);
    }
  }

}

/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId;

/**
 * The swaption lookup, used to select volatilities for pricing.
 * <p>
 * This provides swaption volatilities by index.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultSwaptionMarketDataLookup
    implements SwaptionMarketDataLookup, ImmutableBean, Serializable {

  /**
   * The volatility identifiers, keyed by index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<IborIndex, SwaptionVolatilitiesId> volatilityIds;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a single mapping from index to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified index.
   *
   * @param index  the Ibor index
   * @param volatilityId  the volatility identifier
   * @return the swaption lookup containing the specified mapping
   */
  public static DefaultSwaptionMarketDataLookup of(IborIndex index, SwaptionVolatilitiesId volatilityId) {
    return new DefaultSwaptionMarketDataLookup(ImmutableMap.of(index, volatilityId));
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each index.
   *
   * @param volatilityIds  the volatility identifiers, keyed by index
   * @return the swaption lookup containing the specified volatilities
   */
  public static DefaultSwaptionMarketDataLookup of(Map<IborIndex, SwaptionVolatilitiesId> volatilityIds) {
    return new DefaultSwaptionMarketDataLookup(volatilityIds);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<IborIndex> getVolatilityIndices() {
    return volatilityIds.keySet();
  }

  @Override
  public ImmutableSet<MarketDataId<?>> getVolatilityIds(IborIndex index) {
    SwaptionVolatilitiesId id = volatilityIds.get(index);
    if (id == null) {
      throw new IllegalArgumentException(msgIndexNotFound(index));
    }
    return ImmutableSet.of(id);
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(Set<IborIndex> indices) {
    for (Index index : indices) {
      if (!volatilityIds.keySet().contains(index)) {
        throw new IllegalArgumentException(msgIndexNotFound(index));
      }
    }
    return FunctionRequirements.builder()
        .valueRequirements(ImmutableSet.copyOf(volatilityIds.values()))
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public SwaptionVolatilities volatilities(IborIndex index, MarketData marketData) {
    SwaptionVolatilitiesId volatilityId = volatilityIds.get(index);
    if (volatilityId == null) {
      throw new MarketDataNotFoundException(msgIndexNotFound(index));
    }
    return marketData.getValue(volatilityId);
  }

  //-------------------------------------------------------------------------
  private String msgIndexNotFound(Index index) {
    return Messages.format("Swaption lookup has no volatilities defined for index '{}'", index);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultSwaptionMarketDataLookup}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(DefaultSwaptionMarketDataLookup.class);

  /**
   * The meta-bean for {@code DefaultSwaptionMarketDataLookup}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private DefaultSwaptionMarketDataLookup(
      Map<IborIndex, SwaptionVolatilitiesId> volatilityIds) {
    JodaBeanUtils.notNull(volatilityIds, "volatilityIds");
    this.volatilityIds = ImmutableMap.copyOf(volatilityIds);
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the volatility identifiers, keyed by index.
   * @return the value of the property, not null
   */
  public ImmutableMap<IborIndex, SwaptionVolatilitiesId> getVolatilityIds() {
    return volatilityIds;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultSwaptionMarketDataLookup other = (DefaultSwaptionMarketDataLookup) obj;
      return JodaBeanUtils.equal(volatilityIds, other.volatilityIds);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(volatilityIds);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("DefaultSwaptionMarketDataLookup{");
    buf.append("volatilityIds").append('=').append(JodaBeanUtils.toString(volatilityIds));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

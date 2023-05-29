/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.pricer.index.OvernightFutureOptionVolatilities;
import com.opengamma.strata.pricer.index.OvernightFutureOptionVolatilitiesId;

/**
 * The Overnight future option lookup, used to select volatilities for pricing.
 * <p>
 * This provides Overnight future option volatilities by index.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultOvernightFutureOptionMarketDataLookup
    implements OvernightFutureOptionMarketDataLookup, ImmutableBean, Serializable {

  /**
   * The volatility identifiers, keyed by index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<OvernightIndex, OvernightFutureOptionVolatilitiesId> volatilityIds;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a single mapping from index to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified index.
   *
   * @param index  the Overnight index
   * @param volatilityId  the volatility identifier
   * @return the Overnight future option lookup containing the specified mapping
   */
  public static DefaultOvernightFutureOptionMarketDataLookup of(OvernightIndex index, OvernightFutureOptionVolatilitiesId volatilityId) {
    return new DefaultOvernightFutureOptionMarketDataLookup(ImmutableMap.of(index, volatilityId));
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each index.
   *
   * @param volatilityIds  the volatility identifiers, keyed by index
   * @return the Overnight future option lookup containing the specified volatilities
   */
  public static DefaultOvernightFutureOptionMarketDataLookup of(Map<OvernightIndex, OvernightFutureOptionVolatilitiesId> volatilityIds) {
    return new DefaultOvernightFutureOptionMarketDataLookup(volatilityIds);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<OvernightIndex> getVolatilityIndices() {
    return volatilityIds.keySet();
  }

  @Override
  public ImmutableSet<MarketDataId<?>> getVolatilityIds(OvernightIndex index) {
    OvernightFutureOptionVolatilitiesId id = volatilityIds.get(index);
    if (id == null) {
      throw new IllegalArgumentException(msgIndexNotFound(index));
    }
    return ImmutableSet.of(id);
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(Set<OvernightIndex> indices) {
    Set<OvernightFutureOptionVolatilitiesId> volIds = new HashSet<>();
    for (Index index : indices) {
      if (!volatilityIds.keySet().contains(index)) {
        throw new IllegalArgumentException(msgIndexNotFound(index));
      }
      volIds.add(volatilityIds.get(index));
    }
    return FunctionRequirements.builder().valueRequirements(volIds).build();
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightFutureOptionVolatilities volatilities(OvernightIndex index, MarketData marketData) {
    OvernightFutureOptionVolatilitiesId volatilityId = volatilityIds.get(index);
    if (volatilityId == null) {
      throw new MarketDataNotFoundException(msgIndexNotFound(index));
    }
    return marketData.getValue(volatilityId);
  }

  //-------------------------------------------------------------------------
  private String msgIndexNotFound(Index index) {
    return Messages.format("OvernightFutureOption lookup has no volatilities defined for index '{}'", index);
  }
  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DefaultOvernightFutureOptionMarketDataLookup}.
   */
  private static final TypedMetaBean<DefaultOvernightFutureOptionMarketDataLookup> META_BEAN =
      LightMetaBean.of(
          DefaultOvernightFutureOptionMarketDataLookup.class,
          MethodHandles.lookup(),
          new String[] {
              "volatilityIds"},
          ImmutableMap.of());

  /**
   * The meta-bean for {@code DefaultOvernightFutureOptionMarketDataLookup}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<DefaultOvernightFutureOptionMarketDataLookup> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private DefaultOvernightFutureOptionMarketDataLookup(
      Map<OvernightIndex, OvernightFutureOptionVolatilitiesId> volatilityIds) {
    JodaBeanUtils.notNull(volatilityIds, "volatilityIds");
    this.volatilityIds = ImmutableMap.copyOf(volatilityIds);
  }

  @Override
  public TypedMetaBean<DefaultOvernightFutureOptionMarketDataLookup> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the volatility identifiers, keyed by index.
   * @return the value of the property, not null
   */
  public ImmutableMap<OvernightIndex, OvernightFutureOptionVolatilitiesId> getVolatilityIds() {
    return volatilityIds;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultOvernightFutureOptionMarketDataLookup other = (DefaultOvernightFutureOptionMarketDataLookup) obj;
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
    buf.append("DefaultOvernightFutureOptionMarketDataLookup{");
    buf.append("volatilityIds").append('=').append(JodaBeanUtils.toString(volatilityIds));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

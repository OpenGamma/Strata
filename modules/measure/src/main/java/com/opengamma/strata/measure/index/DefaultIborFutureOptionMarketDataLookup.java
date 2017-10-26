/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.pricer.index.IborFutureOptionVolatilities;
import com.opengamma.strata.pricer.index.IborFutureOptionVolatilitiesId;

/**
 * The Ibor future option lookup, used to select volatilities for pricing.
 * <p>
 * This provides Ibor future option volatilities by index.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultIborFutureOptionMarketDataLookup
    implements IborFutureOptionMarketDataLookup, ImmutableBean, Serializable {

  /**
   * The volatility identifiers, keyed by index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<IborIndex, IborFutureOptionVolatilitiesId> volatilityIds;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a single mapping from index to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified index.
   *
   * @param index  the Ibor index
   * @param volatilityId  the volatility identifier
   * @return the Ibor future option lookup containing the specified mapping
   */
  public static DefaultIborFutureOptionMarketDataLookup of(IborIndex index, IborFutureOptionVolatilitiesId volatilityId) {
    return new DefaultIborFutureOptionMarketDataLookup(ImmutableMap.of(index, volatilityId));
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each index.
   *
   * @param volatilityIds  the volatility identifiers, keyed by index
   * @return the Ibor future option lookup containing the specified volatilities
   */
  public static DefaultIborFutureOptionMarketDataLookup of(Map<IborIndex, IborFutureOptionVolatilitiesId> volatilityIds) {
    return new DefaultIborFutureOptionMarketDataLookup(volatilityIds);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<IborIndex> getVolatilityIndices() {
    return volatilityIds.keySet();
  }

  @Override
  public ImmutableSet<MarketDataId<?>> getVolatilityIds(IborIndex index) {
    IborFutureOptionVolatilitiesId id = volatilityIds.get(index);
    if (id == null) {
      throw new IllegalArgumentException(msgIndexNotFound(index));
    }
    return ImmutableSet.of(id);
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(Set<IborIndex> indices) {
    Set<IborFutureOptionVolatilitiesId> volIds = new HashSet<>();
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
  public IborFutureOptionVolatilities volatilities(IborIndex index, MarketData marketData) {
    IborFutureOptionVolatilitiesId volatilityId = volatilityIds.get(index);
    if (volatilityId == null) {
      throw new MarketDataNotFoundException(msgIndexNotFound(index));
    }
    return marketData.getValue(volatilityId);
  }

  //-------------------------------------------------------------------------
  private String msgIndexNotFound(Index index) {
    return Messages.format("IborFutureOption lookup has no volatilities defined for index '{}'", index);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DefaultIborFutureOptionMarketDataLookup}.
   */
  private static final TypedMetaBean<DefaultIborFutureOptionMarketDataLookup> META_BEAN =
      LightMetaBean.of(
          DefaultIborFutureOptionMarketDataLookup.class,
          MethodHandles.lookup(),
          new String[] {
              "volatilityIds"},
          ImmutableMap.of());

  /**
   * The meta-bean for {@code DefaultIborFutureOptionMarketDataLookup}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<DefaultIborFutureOptionMarketDataLookup> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private DefaultIborFutureOptionMarketDataLookup(
      Map<IborIndex, IborFutureOptionVolatilitiesId> volatilityIds) {
    JodaBeanUtils.notNull(volatilityIds, "volatilityIds");
    this.volatilityIds = ImmutableMap.copyOf(volatilityIds);
  }

  @Override
  public TypedMetaBean<DefaultIborFutureOptionMarketDataLookup> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the volatility identifiers, keyed by index.
   * @return the value of the property, not null
   */
  public ImmutableMap<IborIndex, IborFutureOptionVolatilitiesId> getVolatilityIds() {
    return volatilityIds;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultIborFutureOptionMarketDataLookup other = (DefaultIborFutureOptionMarketDataLookup) obj;
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
    buf.append("DefaultIborFutureOptionMarketDataLookup{");
    buf.append("volatilityIds").append('=').append(JodaBeanUtils.toString(volatilityIds));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import java.io.Serializable;
import java.util.HashSet;
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
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.pricer.bond.BondFutureVolatilities;
import com.opengamma.strata.pricer.bond.BondFutureVolatilitiesId;
import com.opengamma.strata.product.SecurityId;

/**
 * The bond future options lookup, used to select volatilities for pricing.
 * <p>
 * This provides bond future volatilities by security ID.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultBondFutureOptionMarketDataLookup
    implements BondFutureOptionMarketDataLookup, ImmutableBean, Serializable {

  /**
   * The volatility identifiers, keyed by security ID.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<SecurityId, BondFutureVolatilitiesId> volatilityIds;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a single mapping from security ID to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified security ID.
   *
   * @param securityId  the security ID
   * @param volatilityId  the volatility identifier
   * @return the bond future options lookup containing the specified mapping
   */
  public static DefaultBondFutureOptionMarketDataLookup of(
      SecurityId securityId,
      BondFutureVolatilitiesId volatilityId) {

    return new DefaultBondFutureOptionMarketDataLookup(ImmutableMap.of(securityId, volatilityId));
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each security ID.
   *
   * @param volatilityIds  the volatility identifiers, keyed by security ID
   * @return the bond future options lookup containing the specified volatilities
   */
  public static DefaultBondFutureOptionMarketDataLookup of(
      Map<SecurityId, BondFutureVolatilitiesId> volatilityIds) {

    return new DefaultBondFutureOptionMarketDataLookup(volatilityIds);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<SecurityId> getVolatilitySecurityIds() {
    return volatilityIds.keySet();
  }

  @Override
  public ImmutableSet<MarketDataId<?>> getVolatilityIds(SecurityId securityId) {
    BondFutureVolatilitiesId id = volatilityIds.get(securityId);
    if (id == null) {
      throw new IllegalArgumentException(msgSecurityNotFound(securityId));
    }
    return ImmutableSet.of(id);
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(Set<SecurityId> securityIds) {
    Set<BondFutureVolatilitiesId> volIds = new HashSet<>();
    for (SecurityId securityId : securityIds) {
      if (!volatilityIds.keySet().contains(securityId)) {
        throw new IllegalArgumentException(msgSecurityNotFound(securityId));
      }
      volIds.add(volatilityIds.get(securityId));
    }
    return FunctionRequirements.builder().valueRequirements(volIds).build();
  }

  //-------------------------------------------------------------------------
  @Override
  public BondFutureVolatilities volatilities(SecurityId securityId, MarketData marketData) {
    BondFutureVolatilitiesId volatilityId = volatilityIds.get(securityId);
    if (volatilityId == null) {
      throw new MarketDataNotFoundException(msgSecurityNotFound(securityId));
    }
    return marketData.getValue(volatilityId);
  }

  //-------------------------------------------------------------------------
  private String msgSecurityNotFound(SecurityId securityId) {
    return Messages.format("BondFutureOption lookup has no volatilities defined for security ID '{}'", securityId);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultBondFutureOptionMarketDataLookup}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(DefaultBondFutureOptionMarketDataLookup.class);

  /**
   * The meta-bean for {@code DefaultBondFutureOptionMarketDataLookup}.
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

  private DefaultBondFutureOptionMarketDataLookup(
      Map<SecurityId, BondFutureVolatilitiesId> volatilityIds) {
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
   * Gets the volatility identifiers, keyed by security ID.
   * @return the value of the property, not null
   */
  public ImmutableMap<SecurityId, BondFutureVolatilitiesId> getVolatilityIds() {
    return volatilityIds;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultBondFutureOptionMarketDataLookup other = (DefaultBondFutureOptionMarketDataLookup) obj;
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
    buf.append("DefaultBondFutureOptionMarketDataLookup{");
    buf.append("volatilityIds").append('=').append(JodaBeanUtils.toString(volatilityIds));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

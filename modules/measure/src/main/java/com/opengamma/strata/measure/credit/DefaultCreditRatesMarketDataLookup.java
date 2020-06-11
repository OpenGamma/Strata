/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;

/**
 * The credit rates lookup, used to select curves for pricing.
 * <p>
 * This provides access to credit, discount and recovery rate curves.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultCreditRatesMarketDataLookup
    implements CreditRatesMarketDataLookup, ImmutableBean, Serializable {

  /**
   * The credit curves, keyed by standard ID and currency.
   * The curve data, predicting the future, associated with each standard ID and currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Pair<StandardId, Currency>, CurveId> creditCurveIds;
  /**
   * The discount curves, keyed by currency.
   * The curve data, predicting the future, associated with each currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Currency, CurveId> discountCurveIds;
  /**
   * The recovery rate curves, keyed by standard ID.
   * The curve data, predicting the future, associated with each standard ID.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<StandardId, CurveId> recoveryRateCurveIds;
  /**
   * The source of market data for quotes and other observable market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableSource observableSource;

  //-------------------------------------------------------------------------
  static DefaultCreditRatesMarketDataLookup of(
      Map<Pair<StandardId, Currency>, CurveId> creditCurveIds,
      Map<Currency, CurveId> discountCurveIds,
      Map<StandardId, CurveId> recoveryRateCurveIds,
      ObservableSource observableSource) {

    return new DefaultCreditRatesMarketDataLookup(
        creditCurveIds, discountCurveIds, recoveryRateCurveIds, observableSource);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<Currency> getDiscountCurrencies() {
    return discountCurveIds.keySet();
  }

  @Override
  public ImmutableSet<MarketDataId<?>> getDiscountMarketDataIds(Currency currency) {
    CurveId id = discountCurveIds.get(currency);
    if (id == null) {
      throw new IllegalArgumentException(Messages.format(
          "Credit rates lookup has no discount curve defined for currency '{}'", currency));
    }
    return ImmutableSet.of(id);
  }

  @Override
  public ImmutableSet<Pair<StandardId, Currency>> getCreditLegalEntities() {
    return creditCurveIds.keySet();
  }

  @Override
  public ImmutableSet<MarketDataId<?>> getCreditMarketDataIds(StandardId legalEntityId, Currency currency) {
    CurveId id = creditCurveIds.get(Pair.of(legalEntityId, currency));
    if (id == null) {
      throw new IllegalArgumentException(Messages.format(
          "Credit rates lookup has no credit curve defined for legal entity ID '{}' and currency '{}'",
          legalEntityId,
          currency));
    }
    return ImmutableSet.of(id);
  }

  @Override
  public ImmutableSet<StandardId> getRecoveryRateLegalEntities() {
    return recoveryRateCurveIds.keySet();
  }

  @Override
  public ImmutableSet<MarketDataId<?>> getRecoveryRateMarketDataIds(StandardId legalEntityId) {
    CurveId id = recoveryRateCurveIds.get(legalEntityId);
    if (id == null) {
      throw new IllegalArgumentException(Messages.format(
          "Credit rates lookup has no recovery rate curve defined for legal entity ID '{}'", legalEntityId));
    }
    return ImmutableSet.of(id);
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(StandardId legalEntityId, Currency currency) {

    CurveId creditCurveId = creditCurveIds.get(Pair.of(legalEntityId, currency));
    if (creditCurveId == null) {
      throw new IllegalArgumentException(Messages.format(
          "Credit rates lookup has no credit curve defined for '{}' and '{}'", legalEntityId, currency));
    }
    CurveId discountCurveId = discountCurveIds.get(currency);
    if (discountCurveId == null) {
      throw new IllegalArgumentException(Messages.format(
          "Credit rates lookup has no discount curve defined for '{}'", currency));
    }
    CurveId recoveryRateCurveId = recoveryRateCurveIds.get(legalEntityId);
    if (recoveryRateCurveId == null) {
      throw new IllegalArgumentException(Messages.format(
          "Credit rates lookup has no recovery rate curve defined for '{}'", legalEntityId));
    }

    return FunctionRequirements.builder()
        .valueRequirements(ImmutableSet.of(creditCurveId, discountCurveId, recoveryRateCurveId))
        .outputCurrencies(currency)
        .observableSource(observableSource)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public CreditRatesProvider creditRatesProvider(MarketData marketData) {
    return DefaultLookupCreditRatesProvider.of(this, marketData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DefaultCreditRatesMarketDataLookup}.
   */
  private static final TypedMetaBean<DefaultCreditRatesMarketDataLookup> META_BEAN =
      LightMetaBean.of(
          DefaultCreditRatesMarketDataLookup.class,
          MethodHandles.lookup(),
          new String[] {
              "creditCurveIds",
              "discountCurveIds",
              "recoveryRateCurveIds",
              "observableSource"},
          ImmutableMap.of(),
          ImmutableMap.of(),
          ImmutableMap.of(),
          null);

  /**
   * The meta-bean for {@code DefaultCreditRatesMarketDataLookup}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<DefaultCreditRatesMarketDataLookup> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private DefaultCreditRatesMarketDataLookup(
      Map<Pair<StandardId, Currency>, CurveId> creditCurveIds,
      Map<Currency, CurveId> discountCurveIds,
      Map<StandardId, CurveId> recoveryRateCurveIds,
      ObservableSource observableSource) {
    JodaBeanUtils.notNull(creditCurveIds, "creditCurveIds");
    JodaBeanUtils.notNull(discountCurveIds, "discountCurveIds");
    JodaBeanUtils.notNull(recoveryRateCurveIds, "recoveryRateCurveIds");
    JodaBeanUtils.notNull(observableSource, "observableSource");
    this.creditCurveIds = ImmutableMap.copyOf(creditCurveIds);
    this.discountCurveIds = ImmutableMap.copyOf(discountCurveIds);
    this.recoveryRateCurveIds = ImmutableMap.copyOf(recoveryRateCurveIds);
    this.observableSource = observableSource;
  }

  @Override
  public TypedMetaBean<DefaultCreditRatesMarketDataLookup> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the credit curves, keyed by standard ID and currency.
   * The curve data, predicting the future, associated with each standard ID and currency.
   * @return the value of the property, not null
   */
  public ImmutableMap<Pair<StandardId, Currency>, CurveId> getCreditCurveIds() {
    return creditCurveIds;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount curves, keyed by currency.
   * The curve data, predicting the future, associated with each currency.
   * @return the value of the property, not null
   */
  public ImmutableMap<Currency, CurveId> getDiscountCurveIds() {
    return discountCurveIds;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the recovery rate curves, keyed by standard ID.
   * The curve data, predicting the future, associated with each standard ID.
   * @return the value of the property, not null
   */
  public ImmutableMap<StandardId, CurveId> getRecoveryRateCurveIds() {
    return recoveryRateCurveIds;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the source of market data for quotes and other observable market data.
   * @return the value of the property, not null
   */
  public ObservableSource getObservableSource() {
    return observableSource;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultCreditRatesMarketDataLookup other = (DefaultCreditRatesMarketDataLookup) obj;
      return JodaBeanUtils.equal(creditCurveIds, other.creditCurveIds) &&
          JodaBeanUtils.equal(discountCurveIds, other.discountCurveIds) &&
          JodaBeanUtils.equal(recoveryRateCurveIds, other.recoveryRateCurveIds) &&
          JodaBeanUtils.equal(observableSource, other.observableSource);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(creditCurveIds);
    hash = hash * 31 + JodaBeanUtils.hashCode(discountCurveIds);
    hash = hash * 31 + JodaBeanUtils.hashCode(recoveryRateCurveIds);
    hash = hash * 31 + JodaBeanUtils.hashCode(observableSource);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("DefaultCreditRatesMarketDataLookup{");
    buf.append("creditCurveIds").append('=').append(JodaBeanUtils.toString(creditCurveIds)).append(',').append(' ');
    buf.append("discountCurveIds").append('=').append(JodaBeanUtils.toString(discountCurveIds)).append(',').append(' ');
    buf.append("recoveryRateCurveIds").append('=').append(JodaBeanUtils.toString(recoveryRateCurveIds)).append(',').append(' ');
    buf.append("observableSource").append('=').append(JodaBeanUtils.toString(observableSource));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

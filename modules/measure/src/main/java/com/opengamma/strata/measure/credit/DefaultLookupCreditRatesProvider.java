/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.credit.CreditCurveZeroRateSensitivity;
import com.opengamma.strata.pricer.credit.CreditDiscountFactors;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;
import com.opengamma.strata.pricer.credit.ImmutableCreditRatesProvider;
import com.opengamma.strata.pricer.credit.LegalEntitySurvivalProbabilities;
import com.opengamma.strata.pricer.credit.RecoveryRates;

/**
 * A credit rates provider based on a credit rates lookup.
 * <p>
 * This uses a {@link DefaultCreditRatesMarketDataLookup} to provide a view on {@link MarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultLookupCreditRatesProvider
    implements CreditRatesProvider, ImmutableBean, Serializable {

  /**
   * The lookup.
   */
  @PropertyDefinition(validate = "notNull")
  private final DefaultCreditRatesMarketDataLookup lookup;
  /**
   * The market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketData marketData;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a lookup and market data.
   *
   * @param lookup  the lookup
   * @param marketData  the market data
   * @return the credit rates provider
   */
  public static DefaultLookupCreditRatesProvider of(
      DefaultCreditRatesMarketDataLookup lookup,
      MarketData marketData) {

    return new DefaultLookupCreditRatesProvider(lookup, marketData);
  }

  @ImmutableConstructor
  private DefaultLookupCreditRatesProvider(
      DefaultCreditRatesMarketDataLookup lookup,
      MarketData marketData) {

    this.lookup = ArgChecker.notNull(lookup, "lookup");
    this.marketData = ArgChecker.notNull(marketData, "marketData");
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new DefaultLookupCreditRatesProvider(lookup, marketData);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDate();
  }

  //-------------------------------------------------------------------------
  @Override
  public LegalEntitySurvivalProbabilities survivalProbabilities(StandardId legalEntityId, Currency currency) {
    CurveId curveId = lookup.getCreditCurveIds().get(Pair.of(legalEntityId, currency));
    if (curveId == null) {
      throw new MarketDataNotFoundException("Unable to find credit curve: " + legalEntityId + ", " + currency);
    }
    Curve curve = marketData.getValue(curveId);
    CreditDiscountFactors survivalProbabilities = CreditDiscountFactors.of(currency, getValuationDate(), curve);
    return LegalEntitySurvivalProbabilities.of(legalEntityId, survivalProbabilities);
  }

  @Override
  public CreditDiscountFactors discountFactors(Currency currency) {
    CurveId curveId = lookup.getDiscountCurveIds().get(currency);
    if (curveId == null) {
      throw new MarketDataNotFoundException("Unable to find discount curve: " + currency);
    }
    Curve curve = marketData.getValue(curveId);
    return CreditDiscountFactors.of(currency, getValuationDate(), curve);
  }

  @Override
  public RecoveryRates recoveryRates(StandardId legalEntityId) {
    CurveId curveId = lookup.getRecoveryRateCurveIds().get(legalEntityId);
    if (curveId == null) {
      throw new MarketDataNotFoundException("Unable to find recovery rate curve: " + legalEntityId);
    }
    Curve curve = marketData.getValue(curveId);
    return RecoveryRates.of(legalEntityId, getValuationDate(), curve);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof CreditCurveZeroRateSensitivity) {
        CreditCurveZeroRateSensitivity pt = (CreditCurveZeroRateSensitivity) point;
        LegalEntitySurvivalProbabilities factors = survivalProbabilities(pt.getLegalEntityId(), pt.getCurveCurrency());
        sens = sens.combinedWith(factors.parameterSensitivity(pt));
      } else if (point instanceof ZeroRateSensitivity) {
        ZeroRateSensitivity pt = (ZeroRateSensitivity) point;
        CreditDiscountFactors factors = discountFactors(pt.getCurveCurrency());
        sens = sens.combinedWith(factors.parameterSensitivity(pt));
      }
    }
    return sens;
  }

  @Override
  public CurrencyParameterSensitivity singleCreditCurveParameterSensitivity(PointSensitivities pointSensitivities,
      StandardId legalEntityId, Currency currency) {

    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof CreditCurveZeroRateSensitivity) {
        CreditCurveZeroRateSensitivity pt = (CreditCurveZeroRateSensitivity) point;
        if (pt.getLegalEntityId().equals(legalEntityId) && pt.getCurrency().equals(currency)) {
          LegalEntitySurvivalProbabilities factors = survivalProbabilities(pt.getLegalEntityId(), pt.getCurveCurrency());
          sens = sens.combinedWith(factors.parameterSensitivity(pt));
        }
      }
    }
    ArgChecker.isTrue(sens.size() == 1, "sensitivity must be unique");
    return sens.getSensitivities().get(0);
  }

  @Override
  public CurrencyParameterSensitivity singleDiscountCurveParameterSensitivity(PointSensitivities pointSensitivities,
      Currency currency) {

    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof ZeroRateSensitivity) {
        ZeroRateSensitivity pt = (ZeroRateSensitivity) point;
        if (pt.getCurrency().equals(currency)) {
          CreditDiscountFactors factors = discountFactors(pt.getCurveCurrency());
          sens = sens.combinedWith(factors.parameterSensitivity(pt));
        }
      }
    }
    ArgChecker.isTrue(sens.size() == 1, "sensitivity must be unique");
    return sens.getSensitivities().get(0);
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (name instanceof CurveName) {
      return Stream
          .concat(
              lookup.getRecoveryRateCurveIds().values().stream(),
              Stream.concat(
                  lookup.getCreditCurveIds().values().stream(),
                  lookup.getDiscountCurveIds().values().stream()))
          .filter(id -> id.getMarketDataName().equals(name))
          .findFirst()
          .flatMap(id -> marketData.findValue(id))
          .map(v -> name.getMarketDataType().cast(v));
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableCreditRatesProvider toImmutableCreditRatesProvider() {

    LocalDate valuationDate = getValuationDate();
    // credit curves
    Map<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> creditCurves = new HashMap<>();
    for (Pair<StandardId, Currency> pair : lookup.getCreditCurveIds().keySet()) {
      CurveId curveId = lookup.getCreditCurveIds().get(pair);
      if (marketData.containsValue(curveId)) {
        Curve curve = marketData.getValue(curveId);
        CreditDiscountFactors survivalProbabilities = CreditDiscountFactors.of(pair.getSecond(), valuationDate, curve);
        creditCurves.put(pair, LegalEntitySurvivalProbabilities.of(pair.getFirst(), survivalProbabilities));
      }
    }
    // discount curves
    Map<Currency, CreditDiscountFactors> discountCurves = new HashMap<>();
    for (Currency currency : lookup.getDiscountCurveIds().keySet()) {
      CurveId curveId = lookup.getDiscountCurveIds().get(currency);
      if (marketData.containsValue(curveId)) {
        Curve curve = marketData.getValue(curveId);
        discountCurves.put(currency, CreditDiscountFactors.of(currency, valuationDate, curve));
      }
    }
    // recovery rate curves
    Map<StandardId, RecoveryRates> recoveryRateCurves = new HashMap<>();
    for (StandardId legalEntityId : lookup.getRecoveryRateCurveIds().keySet()) {
      CurveId curveId = lookup.getRecoveryRateCurveIds().get(legalEntityId);
      if (marketData.containsValue(curveId)) {
        Curve curve = marketData.getValue(curveId);
        RecoveryRates recoveryRate = RecoveryRates.of(legalEntityId, valuationDate, curve);
        recoveryRateCurves.put(legalEntityId, recoveryRate);
      }
    }
    // build result
    return ImmutableCreditRatesProvider.builder()
        .valuationDate(valuationDate)
        .creditCurves(creditCurves)
        .discountCurves(discountCurves)
        .recoveryRateCurves(recoveryRateCurves)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultLookupCreditRatesProvider}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(DefaultLookupCreditRatesProvider.class);

  /**
   * The meta-bean for {@code DefaultLookupCreditRatesProvider}.
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
   * Gets the lookup.
   * @return the value of the property, not null
   */
  public DefaultCreditRatesMarketDataLookup getLookup() {
    return lookup;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market data.
   * @return the value of the property, not null
   */
  public MarketData getMarketData() {
    return marketData;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultLookupCreditRatesProvider other = (DefaultLookupCreditRatesProvider) obj;
      return JodaBeanUtils.equal(lookup, other.lookup) &&
          JodaBeanUtils.equal(marketData, other.marketData);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(lookup);
    hash = hash * 31 + JodaBeanUtils.hashCode(marketData);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("DefaultLookupCreditRatesProvider{");
    buf.append("lookup").append('=').append(lookup).append(',').append(' ');
    buf.append("marketData").append('=').append(JodaBeanUtils.toString(marketData));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

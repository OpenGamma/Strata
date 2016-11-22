/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

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
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.IssuerCurveDiscountFactors;
import com.opengamma.strata.pricer.bond.IssuerCurveZeroRateSensitivity;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityGroup;
import com.opengamma.strata.pricer.bond.RepoCurveDiscountFactors;
import com.opengamma.strata.pricer.bond.RepoCurveZeroRateSensitivity;
import com.opengamma.strata.pricer.bond.RepoGroup;
import com.opengamma.strata.product.SecurityId;

/**
 * A legal entity discounting provider based on a discounting lookup.
 * <p>
 * This uses a {@link DefaultLegalEntityDiscountingMarketDataLookup} to provide a view on {@link MarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultLookupLegalEntityDiscountingProvider
    implements LegalEntityDiscountingProvider, ImmutableBean, Serializable {

  /**
   * The lookup.
   */
  @PropertyDefinition(validate = "notNull")
  private final DefaultLegalEntityDiscountingMarketDataLookup lookup;
  /**
   * The market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketData marketData;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a lookup and market data.
   * <p>
   * The lookup provides the mapping to repo and issuer curve IDs.
   * The curves are in the market data.
   *
   * @param lookup  the lookup
   * @param marketData  the market data
   * @return the rates provider
   */
  public static DefaultLookupLegalEntityDiscountingProvider of(
      DefaultLegalEntityDiscountingMarketDataLookup lookup,
      MarketData marketData) {

    return new DefaultLookupLegalEntityDiscountingProvider(lookup, marketData);
  }

  @ImmutableConstructor
  private DefaultLookupLegalEntityDiscountingProvider(
      DefaultLegalEntityDiscountingMarketDataLookup lookup,
      MarketData marketData) {

    this.lookup = ArgChecker.notNull(lookup, "lookup");
    this.marketData = ArgChecker.notNull(marketData, "marketData");
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDate();
  }

  //-------------------------------------------------------------------------
  @Override
  public RepoCurveDiscountFactors repoCurveDiscountFactors(SecurityId securityId, StandardId issuerId, Currency currency) {
    RepoGroup repoGroup = lookup.getRepoCurveGroups().get(securityId.getStandardId());
    if (repoGroup == null) {
      repoGroup = lookup.getRepoCurveGroups().get(issuerId);
      if (repoGroup == null) {
        throw new MarketDataNotFoundException("Unable to find repo curve mapping for ID: " + securityId + ", " + issuerId);
      }
    }
    return repoCurveDiscountFactors(repoGroup, currency);
  }

  // lookup the discount factors for the repo group
  private RepoCurveDiscountFactors repoCurveDiscountFactors(RepoGroup repoGroup, Currency currency) {
    CurveId curveId = lookup.getRepoCurves().get(Pair.of(repoGroup, currency));
    if (curveId == null) {
      throw new MarketDataNotFoundException("Unable to find repo curve: " + repoGroup + ", " + currency);
    }
    Curve curve = marketData.getValue(curveId);
    DiscountFactors df = DiscountFactors.of(currency, getValuationDate(), curve);
    return RepoCurveDiscountFactors.of(df, repoGroup);
  }

  //-------------------------------------------------------------------------
  @Override
  public IssuerCurveDiscountFactors issuerCurveDiscountFactors(StandardId issuerId, Currency currency) {
    LegalEntityGroup legalEntityGroup = lookup.getIssuerCurveGroups().get(issuerId);
    if (legalEntityGroup == null) {
      throw new MarketDataNotFoundException("Unable to find issuer curve mapping for ID: " + issuerId);
    }
    return issuerCurveDiscountFactors(legalEntityGroup, currency);
  }

  // lookup the discount factors for the legal entity group
  private IssuerCurveDiscountFactors issuerCurveDiscountFactors(LegalEntityGroup legalEntityGroup, Currency currency) {
    CurveId curveId = lookup.getIssuerCurves().get(Pair.of(legalEntityGroup, currency));
    if (curveId == null) {
      throw new MarketDataNotFoundException("Unable to find issuer curve: " + legalEntityGroup + ", " + currency);
    }
    Curve curve = marketData.getValue(curveId);
    DiscountFactors df = DiscountFactors.of(currency, getValuationDate(), curve);
    return IssuerCurveDiscountFactors.of(df, legalEntityGroup);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof RepoCurveZeroRateSensitivity) {
        RepoCurveZeroRateSensitivity pt = (RepoCurveZeroRateSensitivity) point;
        RepoCurveDiscountFactors factors = repoCurveDiscountFactors(pt.getRepoGroup(), pt.getCurveCurrency());
        sens = sens.combinedWith(factors.parameterSensitivity(pt));
      } else if (point instanceof IssuerCurveZeroRateSensitivity) {
        IssuerCurveZeroRateSensitivity pt = (IssuerCurveZeroRateSensitivity) point;
        IssuerCurveDiscountFactors factors = issuerCurveDiscountFactors(pt.getLegalEntityGroup(), pt.getCurveCurrency());
        sens = sens.combinedWith(factors.parameterSensitivity(pt));
      }
    }
    return sens;
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(MarketDataId<T> key) {
    return marketData.getValue(key);
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    return Stream.concat(lookup.getRepoCurves().values().stream(), lookup.getIssuerCurves().values().stream())
        .filter(id -> id.getMarketDataName().equals(name))
        .findFirst()
        .flatMap(id -> marketData.findValue(id))
        .map(v -> name.getMarketDataType().cast(v));
  }

  @Override
  public ImmutableLegalEntityDiscountingProvider toImmutableLegalEntityDiscountingProvider() {
    // repo curves
    Map<Pair<RepoGroup, Currency>, DiscountFactors> repoCurves = new HashMap<>();
    for (Pair<RepoGroup, Currency> pair : lookup.getRepoCurves().keySet()) {
      CurveId curveId = lookup.getRepoCurves().get(pair);
      if (marketData.containsValue(curveId)) {
        Curve curve = marketData.getValue(curveId);
        repoCurves.put(pair, DiscountFactors.of(pair.getSecond(), getValuationDate(), curve));
      }
    }
    // issuer curves
    Map<Pair<LegalEntityGroup, Currency>, DiscountFactors> issuerCurves = new HashMap<>();
    for (Pair<LegalEntityGroup, Currency> pair : lookup.getIssuerCurves().keySet()) {
      CurveId curveId = lookup.getIssuerCurves().get(pair);
      if (marketData.containsValue(curveId)) {
        Curve curve = marketData.getValue(curveId);
        issuerCurves.put(pair, DiscountFactors.of(pair.getSecond(), getValuationDate(), curve));
      }
    }
    // build result
    return ImmutableLegalEntityDiscountingProvider.builder()
        .valuationDate(getValuationDate())
        .repoCurveGroups(lookup.getRepoCurveGroups())
        .repoCurves(repoCurves)
        .issuerCurveGroups(lookup.getIssuerCurveGroups())
        .issuerCurves(issuerCurves)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultLookupLegalEntityDiscountingProvider}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(DefaultLookupLegalEntityDiscountingProvider.class);

  /**
   * The meta-bean for {@code DefaultLookupLegalEntityDiscountingProvider}.
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
  public DefaultLegalEntityDiscountingMarketDataLookup getLookup() {
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
      DefaultLookupLegalEntityDiscountingProvider other = (DefaultLookupLegalEntityDiscountingProvider) obj;
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
    buf.append("DefaultLookupLegalEntityDiscountingProvider{");
    buf.append("lookup").append('=').append(lookup).append(',').append(' ');
    buf.append("marketData").append('=').append(JodaBeanUtils.toString(marketData));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

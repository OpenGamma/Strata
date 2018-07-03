/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.LegalEntityCurveGroup;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;

/**
 * The lookup that provides access to legal entity discounting in market data.
 * <p>
 * The legal entity discounting market lookup provides access to repo and issuer curves.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface LegalEntityDiscountingMarketDataLookup extends CalculationParameter {

  /**
   * Obtains an instance based on a maps for repo and issuer curves.
   * <p>
   * Both the repo and issuer curves are defined in two parts.
   * The first part maps the issuer ID to a group, and the second part maps the
   * group and currency to the identifier of the curve.
   * 
   * @param repoCurveSecurityGroups  the per security repo curve groups overrides, mapping security ID to group
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @param repoCurveIds  the repo curve identifiers
   * @param issuerCurveGroups  the issuer curve groups, mapping issuer ID to group
   * @param issuerCurveIds  the issuer curves identifiers, keyed by issuer ID and currency
   * @return the rates lookup containing the specified curves
   */
  public static LegalEntityDiscountingMarketDataLookup of(
      Map<SecurityId, RepoGroup> repoCurveSecurityGroups,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      Map<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurveIds) {

    return DefaultLegalEntityDiscountingMarketDataLookup.of(
        repoCurveSecurityGroups, repoCurveGroups, repoCurveIds, issuerCurveGroups, issuerCurveIds, ObservableSource.NONE);
  }

  /**
   * Obtains an instance based on a maps for repo and issuer curves.
   * <p>
   * Both the repo and issuer curves are defined in two parts.
   * The first part maps the issuer ID to a group, and the second part maps the
   * group and currency to the identifier of the curve.
   * 
   * @param repoCurveSecurityGroups  the per security repo curve groups overrides, mapping security ID to group
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @param repoCurveIds  the repo curve identifiers
   * @param issuerCurveGroups  the issuer curve groups, mapping issuer ID to group
   * @param issuerCurveIds  the issuer curves identifiers, keyed by issuer ID and currency
   * @param obsSource  the source of market data for quotes and other observable market data
   * @return the rates lookup containing the specified curves
   */
  public static LegalEntityDiscountingMarketDataLookup of(
      Map<SecurityId, RepoGroup> repoCurveSecurityGroups,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      Map<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurveIds,
      ObservableSource obsSource) {

    return DefaultLegalEntityDiscountingMarketDataLookup.of(
        repoCurveSecurityGroups, repoCurveGroups, repoCurveIds, issuerCurveGroups, issuerCurveIds, obsSource);
  }

  /**
   * Obtains an instance based on a maps for repo and issuer curves.
   * <p>
   * Both the repo and issuer curves are defined in two parts.
   * The first part maps the issuer ID to a group, and the second part maps the
   * group and currency to the identifier of the curve.
   * 
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @param repoCurveIds  the repo curve identifiers
   * @param issuerCurveGroups  the issuer curve groups, mapping issuer ID to group
   * @param issuerCurveIds  the issuer curves identifiers, keyed by issuer ID and currency
   * @return the rates lookup containing the specified curves
   */
  public static LegalEntityDiscountingMarketDataLookup of(
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      Map<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurveIds) {

    return DefaultLegalEntityDiscountingMarketDataLookup.of(
        ImmutableMap.of(), repoCurveGroups, repoCurveIds, issuerCurveGroups, issuerCurveIds, ObservableSource.NONE);
  }

  /**
   * Obtains an instance based on a maps for repo and issuer curves.
   * <p>
   * Both the repo and issuer curves are defined in two parts.
   * The first part maps the issuer ID to a group, and the second part maps the
   * group and currency to the identifier of the curve.
   * 
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @param repoCurveIds  the repo curve identifiers
   * @param issuerCurveGroups  the issuer curve groups, mapping issuer ID to group
   * @param issuerCurveIds  the issuer curves identifiers, keyed by issuer ID and currency
   * @param obsSource  the source of market data for quotes and other observable market data
   * @return the rates lookup containing the specified curves
   */
  public static LegalEntityDiscountingMarketDataLookup of(
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      Map<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurveIds,
      ObservableSource obsSource) {

    return DefaultLegalEntityDiscountingMarketDataLookup.of(
        ImmutableMap.of(), repoCurveGroups, repoCurveIds, issuerCurveGroups, issuerCurveIds, obsSource);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a curve group and group maps.
   * <p>
   * The two maps define mapping from the issuer ID to a group.
   * 
   * @param curveGroup  the curve group to base the lookup on
   * @param repoCurveSecurityGroups  the per security repo curve groups overrides, mapping security ID to group
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @param issuerCurveGroups  the issuer curve groups, mapping issuer ID to group
   * @return the rates lookup containing the specified curves 
   */
  public static LegalEntityDiscountingMarketDataLookup of(
      LegalEntityCurveGroup curveGroup,
      Map<SecurityId, RepoGroup> repoCurveSecurityGroups,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups) {

    CurveGroupName groupName = curveGroup.getName();
    Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds = MapStream.of(curveGroup.getRepoCurves())
        .mapValues(c -> CurveId.of(groupName, c.getName()))
        .toMap();
    Map<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurveIds = MapStream.of(curveGroup.getIssuerCurves())
        .mapValues(c -> CurveId.of(groupName, c.getName()))
        .toMap();
    return DefaultLegalEntityDiscountingMarketDataLookup.of(
        repoCurveSecurityGroups, repoCurveGroups, repoCurveIds, issuerCurveGroups, issuerCurveIds, ObservableSource.NONE);
  }

  /**
   * Obtains an instance based on a curve group and group maps.
   * <p>
   * The two maps define mapping from the issuer ID to a group.
   * 
   * @param curveGroup  the curve group to base the lookup on
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @param issuerCurveGroups  the issuer curve groups, mapping issuer ID to group
   * @return the rates lookup containing the specified curves 
   */
  public static LegalEntityDiscountingMarketDataLookup of(
      LegalEntityCurveGroup curveGroup,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups) {

    return of(curveGroup, ImmutableMap.of(), repoCurveGroups, issuerCurveGroups);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on maps for repo curves.
   * <p>
   * The repo curves are defined in two parts.
   * The first part maps the issuer ID to a group, and the second part maps the
   * group and currency to the identifier of the curve.
   * <p>
   * Issuer curves are not defined in the instance.
   * 
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @param repoCurveIds  the repo curve identifiers, keyed by repo group and currency
   * @return the rates lookup containing the specified curves
   */
  public static LegalEntityDiscountingMarketDataLookup of(
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds) {

    return LegalEntityDiscountingMarketDataLookup.of(repoCurveGroups, repoCurveIds, ObservableSource.NONE);
  }

  /**
   * Obtains an instance based on maps for repo curves.
   * <p>
   * The repo curves are defined in two parts.
   * The first part maps the issuer ID to a group, and the second part maps the
   * group and currency to the identifier of the curve.
   * <p>
   * Issuer curves are not defined in the instance.
   * 
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @param repoCurveIds  the repo curve identifiers, keyed by repo group and currency
   * @param obsSource  the source of market data for quotes and other observable market data
   * @return the rates lookup containing the specified curves
   */
  public static LegalEntityDiscountingMarketDataLookup of(
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds,
      ObservableSource obsSource) {

    return DefaultLegalEntityDiscountingMarketDataLookup.of(repoCurveGroups, repoCurveIds, obsSource);
  }

  /**
   * Obtains an instance based on a curve group and group map.
   * <p>
   * The two maps define mapping from the issuer ID to a group.
   * <p>
   * Issuer curves are not defined in the instance.
   * 
   * @param curveGroup  the curve group to base the lookup on
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @return the rates lookup containing the specified curves 
   */
  public static LegalEntityDiscountingMarketDataLookup of(
      LegalEntityCurveGroup curveGroup,
      Map<LegalEntityId, RepoGroup> repoCurveGroups) {

    CurveGroupName groupName = curveGroup.getName();
    Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds = MapStream.of(curveGroup.getRepoCurves())
        .mapValues(c -> CurveId.of(groupName, c.getName()))
        .toMap();
    return LegalEntityDiscountingMarketDataLookup.of(repoCurveGroups, repoCurveIds, ObservableSource.NONE);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type that the lookup will be queried by.
   * <p>
   * This returns {@code LegalEntityDiscountingMarketDataLookup.class}.
   * When querying parameters using {@link CalculationParameters#findParameter(Class)},
   * {@code LegalEntityDiscountingMarketDataLookup.class} must be passed in to find the instance.
   * 
   * @return the type of the parameter implementation
   */
  @Override
  public default Class<? extends CalculationParameter> queryType() {
    return LegalEntityDiscountingMarketDataLookup.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates market data requirements for the specified security and issuer.
   * 
   * @param securityId  the security ID
   * @param issuerId  the legal entity issuer ID
   * @param currency  the currency of the security
   * @return the requirements
   * @throws IllegalArgumentException if unable to create requirements
   */
  public abstract FunctionRequirements requirements(SecurityId securityId, LegalEntityId issuerId, Currency currency);

  /**
   * Creates market data requirements for the specified issuer.
   * 
   * @param issuerId  the legal entity issuer ID
   * @param currency  the currency of the security
   * @return the requirements
   * @throws IllegalArgumentException if unable to create requirements
   */
  public abstract FunctionRequirements requirements(LegalEntityId issuerId, Currency currency);

  //-------------------------------------------------------------------------
  /**
   * Obtains a filtered view of the complete set of market data.
   * <p>
   * This method returns an instance that binds the lookup to the market data.
   * The input is {@link ScenarioMarketData}, which contains market data for all scenarios.
   * 
   * @param marketData  the complete set of market data for all scenarios
   * @return the filtered market data
   */
  public default LegalEntityDiscountingScenarioMarketData marketDataView(ScenarioMarketData marketData) {
    return DefaultLegalEntityDiscountingScenarioMarketData.of(this, marketData);
  }

  /**
   * Obtains a filtered view of the complete set of market data.
   * <p>
   * This method returns an instance that binds the lookup to the market data.
   * The input is {@link MarketData}, which contains market data for one scenario.
   * 
   * @param marketData  the complete set of market data for one scenario
   * @return the filtered market data
   */
  public default LegalEntityDiscountingMarketData marketDataView(MarketData marketData) {
    return DefaultLegalEntityDiscountingMarketData.of(this, marketData);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a discounting provider based on the specified market data.
   * <p>
   * This provides a {@link LegalEntityDiscountingProvider} suitable for pricing a product.
   * Although this method can be used directly, it is typically invoked indirectly
   * via {@link LegalEntityDiscountingMarketData}:
   * <pre>
   *  // bind the baseData to this lookup
   *  LegalEntityDiscountingMarketData view = lookup.marketView(baseData);
   *  
   *  // pass around RatesMarketData within the function to use in pricing
   *  LegalEntityDiscountingProvider provider = view.discountingProvider();
   * </pre>
   * 
   * @param marketData  the complete set of market data for one scenario
   * @return the discounting provider
   */
  public abstract LegalEntityDiscountingProvider discountingProvider(MarketData marketData);

}

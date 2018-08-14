/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

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
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.NamedMarketDataId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;

/**
 * The legal entity discounting lookup, used to select curves for pricing.
 * <p>
 * This provides access to repo and issuer curves.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultLegalEntityDiscountingMarketDataLookup
    implements LegalEntityDiscountingMarketDataLookup, ImmutableBean, Serializable {

  /**
   * The groups used to find a repo curve by security.
   * <p>
   * This maps the security ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<SecurityId, RepoGroup> repoCurveSecurityGroups;
  /**
   * The groups used to find a repo curve by legal entity.
   * <p>
   * This maps the legal entity ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<LegalEntityId, RepoGroup> repoCurveGroups;
  /**
   * The repo curves, keyed by group and currency.
   * The curve data, predicting the future, associated with each repo group and currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves;
  /**
   * The groups used to find an issuer curve by legal entity.
   * <p>
   * This maps the legal entity ID to a group.
   * The group is used to find the curve in {@code issuerCurves}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<LegalEntityId, LegalEntityGroup> issuerCurveGroups;
  /**
   * The issuer curves, keyed by group and currency.
   * The curve data, predicting the future, associated with each legal entity group and currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves;
  /**
   * The source of market data for quotes and other observable market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableSource observableSource;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a maps for repo and issuer curves.
   * <p>
   * Both the repo and issuer curves are defined in two parts.
   * The first part maps the issuer ID to a group, and the second part maps the
   * group and currency to the identifier of the curve.
   * 
   * @param repoCurveSecurityGroups  the per security repo curve group overrides, mapping security ID to group
   * @param repoCurveGroups  the repo curve groups, mapping issuer ID to group
   * @param repoCurveIds  the repo curve identifiers, keyed by security ID or issuer ID and currency
   * @param issuerCurveGroups  the issuer curve groups, mapping issuer ID to group
   * @param issuerCurveIds  the issuer curves identifiers, keyed by issuer ID and currency
   * @param obsSource  the source of market data for quotes and other observable market data
   * @return the rates lookup containing the specified curves
   */
  public static <T extends NamedMarketDataId<Curve>> DefaultLegalEntityDiscountingMarketDataLookup of(
      Map<SecurityId, RepoGroup> repoCurveSecurityGroups,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      Map<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurveIds,
      ObservableSource obsSource) {

    return new DefaultLegalEntityDiscountingMarketDataLookup(
        repoCurveSecurityGroups, repoCurveGroups, repoCurveIds, issuerCurveGroups, issuerCurveIds, obsSource);
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
  public static DefaultLegalEntityDiscountingMarketDataLookup of(
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds,
      ObservableSource obsSource) {

    return new DefaultLegalEntityDiscountingMarketDataLookup(
        ImmutableMap.of(), repoCurveGroups, repoCurveIds, ImmutableMap.of(), ImmutableMap.of(), obsSource);
  }

  @ImmutableValidator
  private void validate() {
    Set<RepoGroup> uniqueRepoGroups = new HashSet<>(repoCurveGroups.values());
    uniqueRepoGroups.addAll(repoCurveSecurityGroups.values());
    Set<RepoGroup> uniqueRepoCurves = repoCurves.keySet().stream().map(p -> p.getFirst()).collect(toImmutableSet());
    if (!uniqueRepoCurves.containsAll(uniqueRepoGroups)) {
      throw new IllegalArgumentException(
          "Repo curve groups defined without matching curve mappings: " +
              Sets.difference(uniqueRepoGroups, uniqueRepoCurves));
    }
    Set<LegalEntityGroup> uniqueIssuerGroups = new HashSet<>(issuerCurveGroups.values());
    Set<LegalEntityGroup> uniqueIssuerCurves = issuerCurves.keySet().stream().map(p -> p.getFirst()).collect(toImmutableSet());
    if (!uniqueIssuerCurves.containsAll(uniqueIssuerGroups)) {
      throw new IllegalArgumentException(
          "Issuer curve groups defined without matching curve mappings: " +
              Sets.difference(uniqueIssuerGroups, uniqueIssuerCurves));
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(SecurityId securityId, LegalEntityId issuerId, Currency currency) {
    // repo
    RepoGroup repoKey = repoCurveSecurityGroups.get(securityId);
    if (repoKey == null) {
      repoKey = repoCurveGroups.get(issuerId);
    }
    if (repoKey == null) {
      throw new IllegalArgumentException(Messages.format(
          "Legal entity discounting lookup has no repo curve defined for '{}' and '{}'", securityId, issuerId));
    }
    CurveId repoCurveId = repoCurves.get(Pair.of(repoKey, currency));
    if (repoCurveId == null) {
      throw new IllegalArgumentException(Messages.format(
          "Legal entity discounting lookup has no repo curve defined for '{}' and '{}'", securityId, issuerId));
    }
    // issuer
    LegalEntityGroup issuerKey = issuerCurveGroups.get(issuerId);
    if (issuerKey == null) {
      throw new IllegalArgumentException(Messages.format(
          "Legal entity discounting lookup has no issuer curve defined for '{}'", issuerId));
    }
    CurveId issuerCurveId = issuerCurves.get(Pair.of(issuerKey, currency));
    if (issuerCurveId == null) {
      throw new IllegalArgumentException(Messages.format(
          "Legal entity discounting lookup has no issuer curve defined for '{}'", issuerId));
    }
    // result
    return FunctionRequirements.builder()
        .valueRequirements(ImmutableSet.of(repoCurveId, issuerCurveId))
        .outputCurrencies(currency)
        .observableSource(observableSource)
        .build();
  }

  @Override
  public FunctionRequirements requirements(LegalEntityId issuerId, Currency currency) {
    // repo
    RepoGroup repoKey = repoCurveGroups.get(issuerId);
    if (repoKey == null) {
      throw new IllegalArgumentException(Messages.format(
          "Legal entity discounting lookup has no repo curve defined for '{}'", issuerId));
    }
    CurveId repoCurveId = repoCurves.get(Pair.of(repoKey, currency));
    if (repoCurveId == null) {
      throw new IllegalArgumentException(Messages.format(
          "Legal entity discounting lookup has no repo curve defined for '{}'", issuerId));
    }
    // result
    return FunctionRequirements.builder()
        .valueRequirements(ImmutableSet.of(repoCurveId))
        .outputCurrencies(currency)
        .observableSource(observableSource)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public LegalEntityDiscountingProvider discountingProvider(MarketData marketData) {
    return DefaultLookupLegalEntityDiscountingProvider.of(this, marketData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DefaultLegalEntityDiscountingMarketDataLookup}.
   */
  private static final TypedMetaBean<DefaultLegalEntityDiscountingMarketDataLookup> META_BEAN =
      LightMetaBean.of(
          DefaultLegalEntityDiscountingMarketDataLookup.class,
          MethodHandles.lookup(),
          new String[] {
              "repoCurveSecurityGroups",
              "repoCurveGroups",
              "repoCurves",
              "issuerCurveGroups",
              "issuerCurves",
              "observableSource"},
          ImmutableMap.of(),
          ImmutableMap.of(),
          ImmutableMap.of(),
          ImmutableMap.of(),
          ImmutableMap.of(),
          null);

  /**
   * The meta-bean for {@code DefaultLegalEntityDiscountingMarketDataLookup}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<DefaultLegalEntityDiscountingMarketDataLookup> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private DefaultLegalEntityDiscountingMarketDataLookup(
      Map<SecurityId, RepoGroup> repoCurveSecurityGroups,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurves,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      Map<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves,
      ObservableSource observableSource) {
    JodaBeanUtils.notNull(repoCurveSecurityGroups, "repoCurveSecurityGroups");
    JodaBeanUtils.notNull(repoCurveGroups, "repoCurveGroups");
    JodaBeanUtils.notNull(repoCurves, "repoCurves");
    JodaBeanUtils.notNull(issuerCurveGroups, "issuerCurveGroups");
    JodaBeanUtils.notNull(issuerCurves, "issuerCurves");
    JodaBeanUtils.notNull(observableSource, "observableSource");
    this.repoCurveSecurityGroups = ImmutableMap.copyOf(repoCurveSecurityGroups);
    this.repoCurveGroups = ImmutableMap.copyOf(repoCurveGroups);
    this.repoCurves = ImmutableMap.copyOf(repoCurves);
    this.issuerCurveGroups = ImmutableMap.copyOf(issuerCurveGroups);
    this.issuerCurves = ImmutableMap.copyOf(issuerCurves);
    this.observableSource = observableSource;
    validate();
  }

  @Override
  public TypedMetaBean<DefaultLegalEntityDiscountingMarketDataLookup> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the groups used to find a repo curve by security.
   * <p>
   * This maps the security ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   * @return the value of the property, not null
   */
  public ImmutableMap<SecurityId, RepoGroup> getRepoCurveSecurityGroups() {
    return repoCurveSecurityGroups;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the groups used to find a repo curve by legal entity.
   * <p>
   * This maps the legal entity ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   * @return the value of the property, not null
   */
  public ImmutableMap<LegalEntityId, RepoGroup> getRepoCurveGroups() {
    return repoCurveGroups;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the repo curves, keyed by group and currency.
   * The curve data, predicting the future, associated with each repo group and currency.
   * @return the value of the property, not null
   */
  public ImmutableMap<Pair<RepoGroup, Currency>, CurveId> getRepoCurves() {
    return repoCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the groups used to find an issuer curve by legal entity.
   * <p>
   * This maps the legal entity ID to a group.
   * The group is used to find the curve in {@code issuerCurves}.
   * @return the value of the property, not null
   */
  public ImmutableMap<LegalEntityId, LegalEntityGroup> getIssuerCurveGroups() {
    return issuerCurveGroups;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the issuer curves, keyed by group and currency.
   * The curve data, predicting the future, associated with each legal entity group and currency.
   * @return the value of the property, not null
   */
  public ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> getIssuerCurves() {
    return issuerCurves;
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
      DefaultLegalEntityDiscountingMarketDataLookup other = (DefaultLegalEntityDiscountingMarketDataLookup) obj;
      return JodaBeanUtils.equal(repoCurveSecurityGroups, other.repoCurveSecurityGroups) &&
          JodaBeanUtils.equal(repoCurveGroups, other.repoCurveGroups) &&
          JodaBeanUtils.equal(repoCurves, other.repoCurves) &&
          JodaBeanUtils.equal(issuerCurveGroups, other.issuerCurveGroups) &&
          JodaBeanUtils.equal(issuerCurves, other.issuerCurves) &&
          JodaBeanUtils.equal(observableSource, other.observableSource);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurveSecurityGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurveGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurveGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(observableSource);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("DefaultLegalEntityDiscountingMarketDataLookup{");
    buf.append("repoCurveSecurityGroups").append('=').append(repoCurveSecurityGroups).append(',').append(' ');
    buf.append("repoCurveGroups").append('=').append(repoCurveGroups).append(',').append(' ');
    buf.append("repoCurves").append('=').append(repoCurves).append(',').append(' ');
    buf.append("issuerCurveGroups").append('=').append(issuerCurveGroups).append(',').append(' ');
    buf.append("issuerCurves").append('=').append(issuerCurves).append(',').append(' ');
    buf.append("observableSource").append('=').append(JodaBeanUtils.toString(observableSource));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

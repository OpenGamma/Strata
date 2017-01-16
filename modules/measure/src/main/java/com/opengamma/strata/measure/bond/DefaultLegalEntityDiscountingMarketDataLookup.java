/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityGroup;
import com.opengamma.strata.pricer.bond.RepoGroup;
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
   * The groups used to find a repo curve.
   * <p>
   * This maps either the security ID or the legal entity ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<StandardId, RepoGroup> repoCurveGroups;
  /**
   * The repo curves, keyed by group and currency.
   * The curve data, predicting the future, associated with each repo group and currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves;
  /**
   * The groups used to find an issuer curve.
   * <p>
   * This maps the legal entity ID to a group.
   * The group is used to find the curve in {@code issuerCurves}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<StandardId, LegalEntityGroup> issuerCurveGroups;
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
   * @param repoCurveGroups  the repo curve groups, mapping security or issuer ID to group
   * @param repoCurveIds  the repo curve identifiers, keyed by security ID or issuer ID and currency
   * @param issuerCurveGroups  the issuer curve groups, mapping issuer ID to group
   * @param issuerCurveIds  the issuer curves identifiers, keyed by issuer ID and currency
   * @param obsSource  the source of market data for quotes and other observable market data
   * @return the rates lookup containing the specified curves
   */
  public static DefaultLegalEntityDiscountingMarketDataLookup of(
      Map<StandardId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurveIds,
      Map<StandardId, LegalEntityGroup> issuerCurveGroups,
      Map<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurveIds,
      ObservableSource obsSource) {

    return new DefaultLegalEntityDiscountingMarketDataLookup(
        repoCurveGroups, repoCurveIds, issuerCurveGroups, issuerCurveIds, obsSource);
  }

  @ImmutableValidator
  private void validate() {
    Set<RepoGroup> uniqueRepoGroups = new HashSet<>(repoCurveGroups.values());
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
  public FunctionRequirements requirements(SecurityId securityId, StandardId issuerId, Currency currency) {
    // repo
    RepoGroup repoKey = repoCurveGroups.get(securityId.getStandardId());
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

  //-------------------------------------------------------------------------
  @Override
  public LegalEntityDiscountingProvider discountingProvider(MarketData marketData) {
    return DefaultLookupLegalEntityDiscountingProvider.of(this, marketData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultLegalEntityDiscountingMarketDataLookup}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(DefaultLegalEntityDiscountingMarketDataLookup.class);

  /**
   * The meta-bean for {@code DefaultLegalEntityDiscountingMarketDataLookup}.
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

  private DefaultLegalEntityDiscountingMarketDataLookup(
      Map<StandardId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, CurveId> repoCurves,
      Map<StandardId, LegalEntityGroup> issuerCurveGroups,
      Map<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves,
      ObservableSource observableSource) {
    JodaBeanUtils.notNull(repoCurveGroups, "repoCurveGroups");
    JodaBeanUtils.notNull(repoCurves, "repoCurves");
    JodaBeanUtils.notNull(issuerCurveGroups, "issuerCurveGroups");
    JodaBeanUtils.notNull(issuerCurves, "issuerCurves");
    JodaBeanUtils.notNull(observableSource, "observableSource");
    this.repoCurveGroups = ImmutableMap.copyOf(repoCurveGroups);
    this.repoCurves = ImmutableMap.copyOf(repoCurves);
    this.issuerCurveGroups = ImmutableMap.copyOf(issuerCurveGroups);
    this.issuerCurves = ImmutableMap.copyOf(issuerCurves);
    this.observableSource = observableSource;
    validate();
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
   * Gets the groups used to find a repo curve.
   * <p>
   * This maps either the security ID or the legal entity ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   * @return the value of the property, not null
   */
  public ImmutableMap<StandardId, RepoGroup> getRepoCurveGroups() {
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
   * Gets the groups used to find an issuer curve.
   * <p>
   * This maps the legal entity ID to a group.
   * The group is used to find the curve in {@code issuerCurves}.
   * @return the value of the property, not null
   */
  public ImmutableMap<StandardId, LegalEntityGroup> getIssuerCurveGroups() {
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
      return JodaBeanUtils.equal(repoCurveGroups, other.repoCurveGroups) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurveGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurveGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(observableSource);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("DefaultLegalEntityDiscountingMarketDataLookup{");
    buf.append("repoCurveGroups").append('=').append(repoCurveGroups).append(',').append(' ');
    buf.append("repoCurves").append('=').append(repoCurves).append(',').append(' ');
    buf.append("issuerCurveGroups").append('=').append(issuerCurveGroups).append(',').append(' ');
    buf.append("issuerCurves").append('=').append(issuerCurves).append(',').append(' ');
    buf.append("observableSource").append('=').append(JodaBeanUtils.toString(observableSource));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

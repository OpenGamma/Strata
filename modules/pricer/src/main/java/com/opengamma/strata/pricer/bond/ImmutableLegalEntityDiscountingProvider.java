/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.SecurityId;

/**
 * An immutable provider of data for bond pricing, based on repo and issuer discounting.
 * <p>
 * This used to price bonds issued by a legal entity.
 * The data to do this includes discount factors of repo curves and issuer curves.
 * If the bond is inflation linked, the price index data is obtained from {@link RatesProvider}.
 * <p>
 * Two types of discount factors are provided by this class.
 * Repo curves are looked up using either the security ID of the bond, or the issuer (legal entity).
 * Issuer curves are only looked up using the issuer (legal entity).
 */
@BeanDefinition
public final class ImmutableLegalEntityDiscountingProvider
    implements LegalEntityDiscountingProvider, ImmutableBean, Serializable {

  /**
   * The valuation date.
   * All curves and other data items in this provider are calibrated for this date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The groups used to find a repo curve.
   * <p>
   * This maps either the security ID or the legal entity ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   * <p>
   * This property was renamed in version 1.1 of Strata from {@code bondMap}.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<StandardId, RepoGroup> repoCurveGroups;
  /**
   * The repo curves, keyed by group and currency.
   * The curve data, predicting the future, associated with each repo group and currency.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Pair<RepoGroup, Currency>, DiscountFactors> repoCurves;
  /**
   * The groups used to find an issuer curve.
   * <p>
   * This maps the legal entity ID to a group.
   * The group is used to find the curve in {@code issuerCurves}.
   * <p>
   * This property was renamed in version 1.1 of Strata from {@code legalEntityMap}.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<StandardId, LegalEntityGroup> issuerCurveGroups;
  /**
   * The issuer curves, keyed by group and currency.
   * The curve data, predicting the future, associated with each legal entity group and currency.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors> issuerCurves;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.valuationDate == null && !builder.issuerCurves.isEmpty()) {
      builder.valuationDate = builder.issuerCurves.values().iterator().next().getValuationDate();
    }
  }

  @ImmutableValidator
  private void validate() {
    for (Entry<Pair<RepoGroup, Currency>, DiscountFactors> entry : repoCurves.entrySet()) {
      if (!entry.getValue().getValuationDate().isEqual(valuationDate)) {
        throw new IllegalArgumentException("Invalid valuation date for the repo curve: " + entry.getValue());
      }
      if (!repoCurveGroups.containsValue(entry.getKey().getFirst())) {
        throw new IllegalArgumentException("No map to the repo group from ID: " + entry.getKey().getFirst());
      }
    }
    for (Entry<Pair<LegalEntityGroup, Currency>, DiscountFactors> entry : issuerCurves.entrySet()) {
      if (!entry.getValue().getValuationDate().isEqual(valuationDate)) {
        throw new IllegalArgumentException("Invalid valuation date for the issuer curve: " + entry.getValue());
      }
      if (!issuerCurveGroups.containsValue(entry.getKey().getFirst())) {
        throw new IllegalArgumentException("No map to the legal entity group from ID: " + entry.getKey().getFirst());
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public RepoCurveDiscountFactors repoCurveDiscountFactors(SecurityId securityId, StandardId issuerId, Currency currency) {
    RepoGroup repoGroup = repoCurveGroups.get(securityId.getStandardId());
    if (repoGroup == null) {
      repoGroup = repoCurveGroups.get(issuerId);
      if (repoGroup == null) {
        throw new IllegalArgumentException("Unable to find map for ID: " + securityId + ", " + issuerId);
      }
    }
    return repoCurveDiscountFactors(repoGroup, currency);
  }

  // lookup the discount factors for the repo group
  private RepoCurveDiscountFactors repoCurveDiscountFactors(RepoGroup repoGroup, Currency currency) {
    DiscountFactors discountFactors = repoCurves.get(Pair.of(repoGroup, currency));
    if (discountFactors == null) {
      throw new IllegalArgumentException("Unable to find repo curve: " + repoGroup + ", " + currency);
    }
    return RepoCurveDiscountFactors.of(discountFactors, repoGroup);
  }

  //-------------------------------------------------------------------------
  @Override
  public IssuerCurveDiscountFactors issuerCurveDiscountFactors(StandardId issuerId, Currency currency) {
    LegalEntityGroup legalEntityGroup = issuerCurveGroups.get(issuerId);
    if (legalEntityGroup == null) {
      throw new IllegalArgumentException("Unable to find map for ID: " + issuerId);
    }
    return issuerCurveDiscountFactors(legalEntityGroup, currency);
  }

  // lookup the discount factors for the legal entity group
  private IssuerCurveDiscountFactors issuerCurveDiscountFactors(LegalEntityGroup legalEntityGroup, Currency currency) {
    DiscountFactors discountFactors = issuerCurves.get(Pair.of(legalEntityGroup, currency));
    if (discountFactors == null) {
      throw new IllegalArgumentException("Unable to find issuer curve: " + legalEntityGroup + ", " + currency);
    }
    return IssuerCurveDiscountFactors.of(discountFactors, legalEntityGroup);
  }

  //-------------------------------------------------------------------------
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
  public <T> T data(MarketDataId<T> id) {
    throw new IllegalArgumentException("Unknown identifier: " + id.toString());
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (name instanceof CurveName) {
      return Stream.concat(repoCurves.values().stream(), issuerCurves.values().stream())
          .map(df -> df.findData(name))
          .filter(opt -> opt.isPresent())
          .map(opt -> opt.get())
          .findFirst();
    }
    return Optional.empty();
  }

  @Override
  public ImmutableLegalEntityDiscountingProvider toImmutableLegalEntityDiscountingProvider() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableLegalEntityDiscountingProvider}.
   * @return the meta-bean, not null
   */
  public static ImmutableLegalEntityDiscountingProvider.Meta meta() {
    return ImmutableLegalEntityDiscountingProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableLegalEntityDiscountingProvider.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableLegalEntityDiscountingProvider.Builder builder() {
    return new ImmutableLegalEntityDiscountingProvider.Builder();
  }

  private ImmutableLegalEntityDiscountingProvider(
      LocalDate valuationDate,
      Map<StandardId, RepoGroup> repoCurveGroups,
      Map<Pair<RepoGroup, Currency>, DiscountFactors> repoCurves,
      Map<StandardId, LegalEntityGroup> issuerCurveGroups,
      Map<Pair<LegalEntityGroup, Currency>, DiscountFactors> issuerCurves) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(repoCurveGroups, "repoCurveGroups");
    JodaBeanUtils.notNull(repoCurves, "repoCurves");
    JodaBeanUtils.notNull(issuerCurveGroups, "issuerCurveGroups");
    JodaBeanUtils.notNull(issuerCurves, "issuerCurves");
    this.valuationDate = valuationDate;
    this.repoCurveGroups = ImmutableMap.copyOf(repoCurveGroups);
    this.repoCurves = ImmutableMap.copyOf(repoCurves);
    this.issuerCurveGroups = ImmutableMap.copyOf(issuerCurveGroups);
    this.issuerCurves = ImmutableMap.copyOf(issuerCurves);
    validate();
  }

  @Override
  public ImmutableLegalEntityDiscountingProvider.Meta metaBean() {
    return ImmutableLegalEntityDiscountingProvider.Meta.INSTANCE;
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
   * Gets the valuation date.
   * All curves and other data items in this provider are calibrated for this date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the groups used to find a repo curve.
   * <p>
   * This maps either the security ID or the legal entity ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   * <p>
   * This property was renamed in version 1.1 of Strata from {@code bondMap}.
   * @return the value of the property, not null
   */
  private ImmutableMap<StandardId, RepoGroup> getRepoCurveGroups() {
    return repoCurveGroups;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the repo curves, keyed by group and currency.
   * The curve data, predicting the future, associated with each repo group and currency.
   * @return the value of the property, not null
   */
  private ImmutableMap<Pair<RepoGroup, Currency>, DiscountFactors> getRepoCurves() {
    return repoCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the groups used to find an issuer curve.
   * <p>
   * This maps the legal entity ID to a group.
   * The group is used to find the curve in {@code issuerCurves}.
   * <p>
   * This property was renamed in version 1.1 of Strata from {@code legalEntityMap}.
   * @return the value of the property, not null
   */
  private ImmutableMap<StandardId, LegalEntityGroup> getIssuerCurveGroups() {
    return issuerCurveGroups;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the issuer curves, keyed by group and currency.
   * The curve data, predicting the future, associated with each legal entity group and currency.
   * @return the value of the property, not null
   */
  private ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors> getIssuerCurves() {
    return issuerCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ImmutableLegalEntityDiscountingProvider other = (ImmutableLegalEntityDiscountingProvider) obj;
      return JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(repoCurveGroups, other.repoCurveGroups) &&
          JodaBeanUtils.equal(repoCurves, other.repoCurves) &&
          JodaBeanUtils.equal(issuerCurveGroups, other.issuerCurveGroups) &&
          JodaBeanUtils.equal(issuerCurves, other.issuerCurves);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurveGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurveGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurves);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("ImmutableLegalEntityDiscountingProvider{");
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("repoCurveGroups").append('=').append(repoCurveGroups).append(',').append(' ');
    buf.append("repoCurves").append('=').append(repoCurves).append(',').append(' ');
    buf.append("issuerCurveGroups").append('=').append(issuerCurveGroups).append(',').append(' ');
    buf.append("issuerCurves").append('=').append(JodaBeanUtils.toString(issuerCurves));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableLegalEntityDiscountingProvider}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", ImmutableLegalEntityDiscountingProvider.class, LocalDate.class);
    /**
     * The meta-property for the {@code repoCurveGroups} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<StandardId, RepoGroup>> repoCurveGroups = DirectMetaProperty.ofImmutable(
        this, "repoCurveGroups", ImmutableLegalEntityDiscountingProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code repoCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Pair<RepoGroup, Currency>, DiscountFactors>> repoCurves = DirectMetaProperty.ofImmutable(
        this, "repoCurves", ImmutableLegalEntityDiscountingProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code issuerCurveGroups} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<StandardId, LegalEntityGroup>> issuerCurveGroups = DirectMetaProperty.ofImmutable(
        this, "issuerCurveGroups", ImmutableLegalEntityDiscountingProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code issuerCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors>> issuerCurves = DirectMetaProperty.ofImmutable(
        this, "issuerCurves", ImmutableLegalEntityDiscountingProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "repoCurveGroups",
        "repoCurves",
        "issuerCurveGroups",
        "issuerCurves");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -1279842095:  // repoCurveGroups
          return repoCurveGroups;
        case 587630454:  // repoCurves
          return repoCurves;
        case 1830129450:  // issuerCurveGroups
          return issuerCurveGroups;
        case -1909076611:  // issuerCurves
          return issuerCurves;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableLegalEntityDiscountingProvider.Builder builder() {
      return new ImmutableLegalEntityDiscountingProvider.Builder();
    }

    @Override
    public Class<? extends ImmutableLegalEntityDiscountingProvider> beanType() {
      return ImmutableLegalEntityDiscountingProvider.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code repoCurveGroups} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<StandardId, RepoGroup>> repoCurveGroups() {
      return repoCurveGroups;
    }

    /**
     * The meta-property for the {@code repoCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Pair<RepoGroup, Currency>, DiscountFactors>> repoCurves() {
      return repoCurves;
    }

    /**
     * The meta-property for the {@code issuerCurveGroups} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<StandardId, LegalEntityGroup>> issuerCurveGroups() {
      return issuerCurveGroups;
    }

    /**
     * The meta-property for the {@code issuerCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors>> issuerCurves() {
      return issuerCurves;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((ImmutableLegalEntityDiscountingProvider) bean).getValuationDate();
        case -1279842095:  // repoCurveGroups
          return ((ImmutableLegalEntityDiscountingProvider) bean).getRepoCurveGroups();
        case 587630454:  // repoCurves
          return ((ImmutableLegalEntityDiscountingProvider) bean).getRepoCurves();
        case 1830129450:  // issuerCurveGroups
          return ((ImmutableLegalEntityDiscountingProvider) bean).getIssuerCurveGroups();
        case -1909076611:  // issuerCurves
          return ((ImmutableLegalEntityDiscountingProvider) bean).getIssuerCurves();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code ImmutableLegalEntityDiscountingProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableLegalEntityDiscountingProvider> {

    private LocalDate valuationDate;
    private Map<StandardId, RepoGroup> repoCurveGroups = ImmutableMap.of();
    private Map<Pair<RepoGroup, Currency>, DiscountFactors> repoCurves = ImmutableMap.of();
    private Map<StandardId, LegalEntityGroup> issuerCurveGroups = ImmutableMap.of();
    private Map<Pair<LegalEntityGroup, Currency>, DiscountFactors> issuerCurves = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableLegalEntityDiscountingProvider beanToCopy) {
      this.valuationDate = beanToCopy.getValuationDate();
      this.repoCurveGroups = beanToCopy.getRepoCurveGroups();
      this.repoCurves = beanToCopy.getRepoCurves();
      this.issuerCurveGroups = beanToCopy.getIssuerCurveGroups();
      this.issuerCurves = beanToCopy.getIssuerCurves();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -1279842095:  // repoCurveGroups
          return repoCurveGroups;
        case 587630454:  // repoCurves
          return repoCurves;
        case 1830129450:  // issuerCurveGroups
          return issuerCurveGroups;
        case -1909076611:  // issuerCurves
          return issuerCurves;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case -1279842095:  // repoCurveGroups
          this.repoCurveGroups = (Map<StandardId, RepoGroup>) newValue;
          break;
        case 587630454:  // repoCurves
          this.repoCurves = (Map<Pair<RepoGroup, Currency>, DiscountFactors>) newValue;
          break;
        case 1830129450:  // issuerCurveGroups
          this.issuerCurveGroups = (Map<StandardId, LegalEntityGroup>) newValue;
          break;
        case -1909076611:  // issuerCurves
          this.issuerCurves = (Map<Pair<LegalEntityGroup, Currency>, DiscountFactors>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    /**
     * @deprecated Loop in application code
     */
    @Override
    @Deprecated
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public ImmutableLegalEntityDiscountingProvider build() {
      preBuild(this);
      return new ImmutableLegalEntityDiscountingProvider(
          valuationDate,
          repoCurveGroups,
          repoCurves,
          issuerCurveGroups,
          issuerCurves);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the valuation date.
     * All curves and other data items in this provider are calibrated for this date.
     * @param valuationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDate(LocalDate valuationDate) {
      JodaBeanUtils.notNull(valuationDate, "valuationDate");
      this.valuationDate = valuationDate;
      return this;
    }

    /**
     * Sets the groups used to find a repo curve.
     * <p>
     * This maps either the security ID or the legal entity ID to a group.
     * The group is used to find the curve in {@code repoCurves}.
     * <p>
     * This property was renamed in version 1.1 of Strata from {@code bondMap}.
     * @param repoCurveGroups  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder repoCurveGroups(Map<StandardId, RepoGroup> repoCurveGroups) {
      JodaBeanUtils.notNull(repoCurveGroups, "repoCurveGroups");
      this.repoCurveGroups = repoCurveGroups;
      return this;
    }

    /**
     * Sets the repo curves, keyed by group and currency.
     * The curve data, predicting the future, associated with each repo group and currency.
     * @param repoCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder repoCurves(Map<Pair<RepoGroup, Currency>, DiscountFactors> repoCurves) {
      JodaBeanUtils.notNull(repoCurves, "repoCurves");
      this.repoCurves = repoCurves;
      return this;
    }

    /**
     * Sets the groups used to find an issuer curve.
     * <p>
     * This maps the legal entity ID to a group.
     * The group is used to find the curve in {@code issuerCurves}.
     * <p>
     * This property was renamed in version 1.1 of Strata from {@code legalEntityMap}.
     * @param issuerCurveGroups  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder issuerCurveGroups(Map<StandardId, LegalEntityGroup> issuerCurveGroups) {
      JodaBeanUtils.notNull(issuerCurveGroups, "issuerCurveGroups");
      this.issuerCurveGroups = issuerCurveGroups;
      return this;
    }

    /**
     * Sets the issuer curves, keyed by group and currency.
     * The curve data, predicting the future, associated with each legal entity group and currency.
     * @param issuerCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder issuerCurves(Map<Pair<LegalEntityGroup, Currency>, DiscountFactors> issuerCurves) {
      JodaBeanUtils.notNull(issuerCurves, "issuerCurves");
      this.issuerCurves = issuerCurves;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ImmutableLegalEntityDiscountingProvider.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("repoCurveGroups").append('=').append(JodaBeanUtils.toString(repoCurveGroups)).append(',').append(' ');
      buf.append("repoCurves").append('=').append(JodaBeanUtils.toString(repoCurves)).append(',').append(' ');
      buf.append("issuerCurveGroups").append('=').append(JodaBeanUtils.toString(issuerCurveGroups)).append(',').append(' ');
      buf.append("issuerCurves").append('=').append(JodaBeanUtils.toString(issuerCurves));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

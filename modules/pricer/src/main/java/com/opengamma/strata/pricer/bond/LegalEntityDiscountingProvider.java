/**
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
import java.util.Set;

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
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.SecurityId;

/**
 * The discounting factors provider, used to calculate analytic measures.
 * <p>
 * The primary usage of this provider is to price bonds issued by a legal entity.
 * This includes discount factors of repo curves and issuer curves.
 */
@BeanDefinition
public final class LegalEntityDiscountingProvider
    implements ImmutableBean, Serializable {

  /**
   * The valuation date.
   * All curves and other data items in this provider are calibrated for this date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate valuationDate;

  /**
   * The bond group map.
   * <p>
   * This map is used to convert the {@link StandardId} that identifies the bond to
   * the associated bond group in order to lookup a repo curve.
   * <p>
   * See {@link LegalEntityDiscountingProvider#repoCurveDiscountFactors(SecurityId, StandardId, Currency)}.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<StandardId, BondGroup> bondMap;
  /**
   * The repo curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each bond group and currency.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Pair<BondGroup, Currency>, DiscountFactors> repoCurves;

  /**
   * The legal entity group map.
   * <p>
   * This map is used to convert the {@link StandardId} that identifies the legal entity to
   * the associated legal entity group in order to lookup an issuer curve.
   * <p>
   * See {@link LegalEntityDiscountingProvider#issuerCurveDiscountFactors(StandardId, Currency)}.
   */
  @PropertyDefinition(validate = "notEmpty", get = "private")
  private final ImmutableMap<StandardId, LegalEntityGroup> legalEntityMap;
  /**
   * The issuer curves.
   * The curve data, predicting the future, associated with each legal entity group and currency.
   */
  @PropertyDefinition(validate = "notEmpty", get = "private")
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
    for (Entry<Pair<BondGroup, Currency>, DiscountFactors> entry : repoCurves.entrySet()) {
      if (!entry.getValue().getValuationDate().isEqual(valuationDate)) {
        throw new IllegalArgumentException("Invalid valuation date for the repo curve: " + entry.getValue());
      }
      if (!bondMap.containsValue(entry.getKey().getFirst())) {
        throw new IllegalArgumentException("No map to the bond group from ID: " + entry.getKey().getFirst());
      }
    }
    for (Entry<Pair<LegalEntityGroup, Currency>, DiscountFactors> entry : issuerCurves.entrySet()) {
      if (!entry.getValue().getValuationDate().isEqual(valuationDate)) {
        throw new IllegalArgumentException("Invalid valuation date for the issuer curve: " + entry.getValue());
      }
      if (!legalEntityMap.containsValue(entry.getKey().getFirst())) {
        throw new IllegalArgumentException("No map to the legal entity group from ID: " + entry.getKey().getFirst());
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the discount factors of a repo curve for standard IDs and a currency.
   * <p>
   * If the bond standard ID is matched in a BondGroup, the relevant DiscountFactors is returned, 
   * if not the issuer standard ID is checked and the relevant DiscountFactors is returned; 
   * if both the bond and the issuer ID are not in any BondGroup, an error is thrown.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param securityId  the standard ID of security to get the discount factors for
   * @param issuerId  the standard ID of legal entity to get the discount factors for
   * @param currency  the currency to get the discount factors for
   * @return the discount factors 
   * @throws IllegalArgumentException if the discount factors are not available
   */
  public RepoCurveDiscountFactors repoCurveDiscountFactors(SecurityId securityId, StandardId issuerId, Currency currency) {
    BondGroup bondGroup = bondMap.get(securityId.getStandardId());
    if (bondGroup == null) {
      bondGroup = bondMap.get(issuerId);
      if (bondGroup == null) {
        throw new IllegalArgumentException("Unable to find map for ID: " + securityId + ", " + issuerId);
      }
    }
    return repoCurveDiscountFactors(bondGroup, currency);
  }

  // lookup the discount factors for the bond group
  private RepoCurveDiscountFactors repoCurveDiscountFactors(BondGroup bondGroup, Currency currency) {
    DiscountFactors discountFactors = repoCurves.get(Pair.of(bondGroup, currency));
    if (discountFactors == null) {
      throw new IllegalArgumentException("Unable to find repo curve: " + bondGroup + ", " + currency);
    }
    return RepoCurveDiscountFactors.of(discountFactors, bondGroup);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the discount factors of an issuer curve for a standard ID and a currency.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param issuerId  the standard ID to get the discount factors for
   * @param currency  the currency to get the discount factors for
   * @return the discount factors 
   * @throws IllegalArgumentException if the discount factors are not available
   */
  public IssuerCurveDiscountFactors issuerCurveDiscountFactors(StandardId issuerId, Currency currency) {
    LegalEntityGroup legalEntityGroup = legalEntityMap.get(issuerId);
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
  /**
   * Computes the parameter sensitivity.
   * <p>
   * This computes the {@link CurrencyParameterSensitivities} associated with the {@link PointSensitivities}.
   * This corresponds to the projection of the point sensitivity to the curve internal parameters representation.
   * <p>
   * The sensitivities handled here are {@link RepoCurveZeroRateSensitivity} and {@link IssuerCurveZeroRateSensitivity}. 
   * For the other sensitivity objects, use {@link RatesProvider} instead.
   * 
   * @param pointSensitivities  the point sensitivity
   * @return the sensitivity to the curve parameters
   */
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof RepoCurveZeroRateSensitivity) {
        RepoCurveZeroRateSensitivity pt = (RepoCurveZeroRateSensitivity) point;
        RepoCurveDiscountFactors factors = repoCurveDiscountFactors(pt.getBondGroup(), pt.getCurveCurrency());
        sens = sens.combinedWith(factors.parameterSensitivity(pt));
      } else if (point instanceof IssuerCurveZeroRateSensitivity) {
        IssuerCurveZeroRateSensitivity pt = (IssuerCurveZeroRateSensitivity) point;
        IssuerCurveDiscountFactors factors = issuerCurveDiscountFactors(pt.getLegalEntityGroup(), pt.getCurveCurrency());
        sens = sens.combinedWith(factors.parameterSensitivity(pt));
      }
    }
    return sens;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code LegalEntityDiscountingProvider}.
   * @return the meta-bean, not null
   */
  public static LegalEntityDiscountingProvider.Meta meta() {
    return LegalEntityDiscountingProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(LegalEntityDiscountingProvider.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static LegalEntityDiscountingProvider.Builder builder() {
    return new LegalEntityDiscountingProvider.Builder();
  }

  private LegalEntityDiscountingProvider(
      LocalDate valuationDate,
      Map<StandardId, BondGroup> bondMap,
      Map<Pair<BondGroup, Currency>, DiscountFactors> repoCurves,
      Map<StandardId, LegalEntityGroup> legalEntityMap,
      Map<Pair<LegalEntityGroup, Currency>, DiscountFactors> issuerCurves) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(bondMap, "bondMap");
    JodaBeanUtils.notNull(repoCurves, "repoCurves");
    JodaBeanUtils.notEmpty(legalEntityMap, "legalEntityMap");
    JodaBeanUtils.notEmpty(issuerCurves, "issuerCurves");
    this.valuationDate = valuationDate;
    this.bondMap = ImmutableMap.copyOf(bondMap);
    this.repoCurves = ImmutableMap.copyOf(repoCurves);
    this.legalEntityMap = ImmutableMap.copyOf(legalEntityMap);
    this.issuerCurves = ImmutableMap.copyOf(issuerCurves);
    validate();
  }

  @Override
  public LegalEntityDiscountingProvider.Meta metaBean() {
    return LegalEntityDiscountingProvider.Meta.INSTANCE;
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
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the bond group map.
   * <p>
   * This map is used to convert the {@link StandardId} that identifies the bond to
   * the associated bond group in order to lookup a repo curve.
   * <p>
   * See {@link LegalEntityDiscountingProvider#repoCurveDiscountFactors(SecurityId, StandardId, Currency)}.
   * @return the value of the property, not null
   */
  private ImmutableMap<StandardId, BondGroup> getBondMap() {
    return bondMap;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the repo curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each bond group and currency.
   * @return the value of the property, not null
   */
  private ImmutableMap<Pair<BondGroup, Currency>, DiscountFactors> getRepoCurves() {
    return repoCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity group map.
   * <p>
   * This map is used to convert the {@link StandardId} that identifies the legal entity to
   * the associated legal entity group in order to lookup an issuer curve.
   * <p>
   * See {@link LegalEntityDiscountingProvider#issuerCurveDiscountFactors(StandardId, Currency)}.
   * @return the value of the property, not empty
   */
  private ImmutableMap<StandardId, LegalEntityGroup> getLegalEntityMap() {
    return legalEntityMap;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the issuer curves.
   * The curve data, predicting the future, associated with each legal entity group and currency.
   * @return the value of the property, not empty
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
      LegalEntityDiscountingProvider other = (LegalEntityDiscountingProvider) obj;
      return JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(bondMap, other.bondMap) &&
          JodaBeanUtils.equal(repoCurves, other.repoCurves) &&
          JodaBeanUtils.equal(legalEntityMap, other.legalEntityMap) &&
          JodaBeanUtils.equal(issuerCurves, other.issuerCurves);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(bondMap);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityMap);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurves);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("LegalEntityDiscountingProvider{");
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("bondMap").append('=').append(bondMap).append(',').append(' ');
    buf.append("repoCurves").append('=').append(repoCurves).append(',').append(' ');
    buf.append("legalEntityMap").append('=').append(legalEntityMap).append(',').append(' ');
    buf.append("issuerCurves").append('=').append(JodaBeanUtils.toString(issuerCurves));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code LegalEntityDiscountingProvider}.
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
        this, "valuationDate", LegalEntityDiscountingProvider.class, LocalDate.class);
    /**
     * The meta-property for the {@code bondMap} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<StandardId, BondGroup>> bondMap = DirectMetaProperty.ofImmutable(
        this, "bondMap", LegalEntityDiscountingProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code repoCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Pair<BondGroup, Currency>, DiscountFactors>> repoCurves = DirectMetaProperty.ofImmutable(
        this, "repoCurves", LegalEntityDiscountingProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code legalEntityMap} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<StandardId, LegalEntityGroup>> legalEntityMap = DirectMetaProperty.ofImmutable(
        this, "legalEntityMap", LegalEntityDiscountingProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code issuerCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors>> issuerCurves = DirectMetaProperty.ofImmutable(
        this, "issuerCurves", LegalEntityDiscountingProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "bondMap",
        "repoCurves",
        "legalEntityMap",
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
        case 63526809:  // bondMap
          return bondMap;
        case 587630454:  // repoCurves
          return repoCurves;
        case 1085102016:  // legalEntityMap
          return legalEntityMap;
        case -1909076611:  // issuerCurves
          return issuerCurves;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public LegalEntityDiscountingProvider.Builder builder() {
      return new LegalEntityDiscountingProvider.Builder();
    }

    @Override
    public Class<? extends LegalEntityDiscountingProvider> beanType() {
      return LegalEntityDiscountingProvider.class;
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
     * The meta-property for the {@code bondMap} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<StandardId, BondGroup>> bondMap() {
      return bondMap;
    }

    /**
     * The meta-property for the {@code repoCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Pair<BondGroup, Currency>, DiscountFactors>> repoCurves() {
      return repoCurves;
    }

    /**
     * The meta-property for the {@code legalEntityMap} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<StandardId, LegalEntityGroup>> legalEntityMap() {
      return legalEntityMap;
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
          return ((LegalEntityDiscountingProvider) bean).getValuationDate();
        case 63526809:  // bondMap
          return ((LegalEntityDiscountingProvider) bean).getBondMap();
        case 587630454:  // repoCurves
          return ((LegalEntityDiscountingProvider) bean).getRepoCurves();
        case 1085102016:  // legalEntityMap
          return ((LegalEntityDiscountingProvider) bean).getLegalEntityMap();
        case -1909076611:  // issuerCurves
          return ((LegalEntityDiscountingProvider) bean).getIssuerCurves();
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
   * The bean-builder for {@code LegalEntityDiscountingProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<LegalEntityDiscountingProvider> {

    private LocalDate valuationDate;
    private Map<StandardId, BondGroup> bondMap = ImmutableMap.of();
    private Map<Pair<BondGroup, Currency>, DiscountFactors> repoCurves = ImmutableMap.of();
    private Map<StandardId, LegalEntityGroup> legalEntityMap = ImmutableMap.of();
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
    private Builder(LegalEntityDiscountingProvider beanToCopy) {
      this.valuationDate = beanToCopy.getValuationDate();
      this.bondMap = beanToCopy.getBondMap();
      this.repoCurves = beanToCopy.getRepoCurves();
      this.legalEntityMap = beanToCopy.getLegalEntityMap();
      this.issuerCurves = beanToCopy.getIssuerCurves();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case 63526809:  // bondMap
          return bondMap;
        case 587630454:  // repoCurves
          return repoCurves;
        case 1085102016:  // legalEntityMap
          return legalEntityMap;
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
        case 63526809:  // bondMap
          this.bondMap = (Map<StandardId, BondGroup>) newValue;
          break;
        case 587630454:  // repoCurves
          this.repoCurves = (Map<Pair<BondGroup, Currency>, DiscountFactors>) newValue;
          break;
        case 1085102016:  // legalEntityMap
          this.legalEntityMap = (Map<StandardId, LegalEntityGroup>) newValue;
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

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public LegalEntityDiscountingProvider build() {
      preBuild(this);
      return new LegalEntityDiscountingProvider(
          valuationDate,
          bondMap,
          repoCurves,
          legalEntityMap,
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
     * Sets the bond group map.
     * <p>
     * This map is used to convert the {@link StandardId} that identifies the bond to
     * the associated bond group in order to lookup a repo curve.
     * <p>
     * See {@link LegalEntityDiscountingProvider#repoCurveDiscountFactors(SecurityId, StandardId, Currency)}.
     * @param bondMap  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder bondMap(Map<StandardId, BondGroup> bondMap) {
      JodaBeanUtils.notNull(bondMap, "bondMap");
      this.bondMap = bondMap;
      return this;
    }

    /**
     * Sets the repo curves, defaulted to an empty map.
     * The curve data, predicting the future, associated with each bond group and currency.
     * @param repoCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder repoCurves(Map<Pair<BondGroup, Currency>, DiscountFactors> repoCurves) {
      JodaBeanUtils.notNull(repoCurves, "repoCurves");
      this.repoCurves = repoCurves;
      return this;
    }

    /**
     * Sets the legal entity group map.
     * <p>
     * This map is used to convert the {@link StandardId} that identifies the legal entity to
     * the associated legal entity group in order to lookup an issuer curve.
     * <p>
     * See {@link LegalEntityDiscountingProvider#issuerCurveDiscountFactors(StandardId, Currency)}.
     * @param legalEntityMap  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder legalEntityMap(Map<StandardId, LegalEntityGroup> legalEntityMap) {
      JodaBeanUtils.notEmpty(legalEntityMap, "legalEntityMap");
      this.legalEntityMap = legalEntityMap;
      return this;
    }

    /**
     * Sets the issuer curves.
     * The curve data, predicting the future, associated with each legal entity group and currency.
     * @param issuerCurves  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder issuerCurves(Map<Pair<LegalEntityGroup, Currency>, DiscountFactors> issuerCurves) {
      JodaBeanUtils.notEmpty(issuerCurves, "issuerCurves");
      this.issuerCurves = issuerCurves;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("LegalEntityDiscountingProvider.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("bondMap").append('=').append(JodaBeanUtils.toString(bondMap)).append(',').append(' ');
      buf.append("repoCurves").append('=').append(JodaBeanUtils.toString(repoCurves)).append(',').append(' ');
      buf.append("legalEntityMap").append('=').append(JodaBeanUtils.toString(legalEntityMap)).append(',').append(' ');
      buf.append("issuerCurves").append('=').append(JodaBeanUtils.toString(issuerCurves));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

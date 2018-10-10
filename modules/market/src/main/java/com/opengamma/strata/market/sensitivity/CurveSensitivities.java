/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static java.util.stream.Collectors.joining;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.product.PortfolioItem;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;

/**
 * Sensitivity to a set of curves, used to pass risk into calculations.
 * <p>
 * Sometimes it is useful to pass in a representation of risk rather than explicitly
 * listing the current portfolio of trades and/or positions.
 * This target is designed to allow this.
 * <p>
 * A map of sensitivities is provided, allowing both delta and gamma to be included if desired.
 */
@BeanDefinition(builderScope = "private")
public final class CurveSensitivities
    implements PortfolioItem, FxConvertible<CurveSensitivities>, ImmutableBean, Serializable {

  /**
   * The additional information.
   * <p>
   * This allows additional information to be attached to the sensitivities.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PortfolioItemInfo info;
  /**
   * The sensitivities, keyed by type.
   * <p>
   * The map allows sensitivity to different types to be expressed.
   * For example, there might be both delta and gamma sensitivity.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<CurveSensitivitiesType, CurrencyParameterSensitivities> typedSensitivities;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty instance.
   * 
   * @return the empty sensitivities instance
   */
  public static CurveSensitivities empty() {
    return new CurveSensitivities(PortfolioItemInfo.empty(), ImmutableMap.of());
  }

  /**
   * Obtains an instance from a single set of sensitivities.
   * 
   * @param info  the additional information
   * @param type  the type of the sensitivities
   * @param sensitivities  the sensitivities
   * @return the sensitivities instance
   */
  public static CurveSensitivities of(
      PortfolioItemInfo info,
      CurveSensitivitiesType type,
      CurrencyParameterSensitivities sensitivities) {

    return new CurveSensitivities(info, ImmutableMap.of(type, sensitivities));
  }

  /**
   * Obtains an instance from a map of sensitivities.
   * 
   * @param info  the additional information
   * @param typedSensitivities  the map of sensitivities by type
   * @return the sensitivities instance
   */
  public static CurveSensitivities of(
      PortfolioItemInfo info,
      Map<CurveSensitivitiesType, CurrencyParameterSensitivities> typedSensitivities) {

    return new CurveSensitivities(info, ImmutableMap.copyOf(typedSensitivities));
  }

  //-----------------------------------------------------------------------
  /**
   * Combines this set of sensitivities with another set.
   * <p>
   * This returns a new curve sensitivities with a combined map of typed sensitivities.
   * Any sensitivities of the same type will be combined using
   * {@link CurrencyParameterSensitivities#combinedWith(CurrencyParameterSensitivities)}
   * 
   * @param other  the other parameter sensitivities
   * @return an instance based on this one, with the other instance added
   */
  public CurveSensitivities combinedWith(Map<CurveSensitivitiesType, CurrencyParameterSensitivities> other) {
    ImmutableMap<CurveSensitivitiesType, CurrencyParameterSensitivities> combinedSens =
        MapStream.concat(MapStream.of(typedSensitivities), MapStream.of(other))
            .toMap(CurrencyParameterSensitivities::combinedWith);
    return new CurveSensitivities(info, combinedSens);
  }

  /**
   * Converts the sensitivities in this instance to an equivalent in the specified currency.
   * <p>
   * Any FX conversion that is required will use rates from the provider.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the sensitivity object expressed in terms of the result currency
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public CurveSensitivities convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return new CurveSensitivities(
        info,
        MapStream.of(typedSensitivities)
            .mapValues(v -> v.convertedTo(resultCurrency, rateProvider))
            .toMap());
  }

  @Override
  public PortfolioItemSummary summarize() {
    String typesStr = typedSensitivities.keySet().stream()
        .map(CurveSensitivitiesType::toString)
        .sorted()
        .collect(joining(", ", "CurveSensitivities[", "]"));
    return PortfolioItemSummary.of(
        getId().orElse(null),
        PortfolioItemType.SENSITIVITIES,
        ProductType.SENSITIVITIES,
        typedSensitivities.values().stream()
            .flatMap(s -> s.getSensitivities().stream())
            .map(s -> s.getCurrency())
            .collect(toImmutableSet()),
        typesStr);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code CurveSensitivities}.
   * @return the meta-bean, not null
   */
  public static CurveSensitivities.Meta meta() {
    return CurveSensitivities.Meta.INSTANCE;
  }

  static {
    MetaBean.register(CurveSensitivities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CurveSensitivities(
      PortfolioItemInfo info,
      Map<CurveSensitivitiesType, CurrencyParameterSensitivities> typedSensitivities) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(typedSensitivities, "typedSensitivities");
    this.info = info;
    this.typedSensitivities = ImmutableMap.copyOf(typedSensitivities);
  }

  @Override
  public CurveSensitivities.Meta metaBean() {
    return CurveSensitivities.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional information.
   * <p>
   * This allows additional information to be attached to the sensitivities.
   * @return the value of the property, not null
   */
  @Override
  public PortfolioItemInfo getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the sensitivities, keyed by type.
   * <p>
   * The map allows sensitivity to different types to be expressed.
   * For example, there might be both delta and gamma sensitivity.
   * @return the value of the property, not null
   */
  public ImmutableMap<CurveSensitivitiesType, CurrencyParameterSensitivities> getTypedSensitivities() {
    return typedSensitivities;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurveSensitivities other = (CurveSensitivities) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(typedSensitivities, other.typedSensitivities);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(typedSensitivities);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("CurveSensitivities{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("typedSensitivities").append('=').append(JodaBeanUtils.toString(typedSensitivities));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurveSensitivities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<PortfolioItemInfo> info = DirectMetaProperty.ofImmutable(
        this, "info", CurveSensitivities.class, PortfolioItemInfo.class);
    /**
     * The meta-property for the {@code typedSensitivities} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<CurveSensitivitiesType, CurrencyParameterSensitivities>> typedSensitivities = DirectMetaProperty.ofImmutable(
        this, "typedSensitivities", CurveSensitivities.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "typedSensitivities");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 153032499:  // typedSensitivities
          return typedSensitivities;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CurveSensitivities> builder() {
      return new CurveSensitivities.Builder();
    }

    @Override
    public Class<? extends CurveSensitivities> beanType() {
      return CurveSensitivities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PortfolioItemInfo> info() {
      return info;
    }

    /**
     * The meta-property for the {@code typedSensitivities} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<CurveSensitivitiesType, CurrencyParameterSensitivities>> typedSensitivities() {
      return typedSensitivities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((CurveSensitivities) bean).getInfo();
        case 153032499:  // typedSensitivities
          return ((CurveSensitivities) bean).getTypedSensitivities();
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
   * The bean-builder for {@code CurveSensitivities}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<CurveSensitivities> {

    private PortfolioItemInfo info;
    private Map<CurveSensitivitiesType, CurrencyParameterSensitivities> typedSensitivities = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 153032499:  // typedSensitivities
          return typedSensitivities;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (PortfolioItemInfo) newValue;
          break;
        case 153032499:  // typedSensitivities
          this.typedSensitivities = (Map<CurveSensitivitiesType, CurrencyParameterSensitivities>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public CurveSensitivities build() {
      return new CurveSensitivities(
          info,
          typedSensitivities);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("CurveSensitivities.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("typedSensitivities").append('=').append(JodaBeanUtils.toString(typedSensitivities));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

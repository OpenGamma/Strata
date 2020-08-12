/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.option.Strike;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Surface node metadata for a surface node with a specific time to expiry and strike.
 */
@BeanDefinition(builderScope = "private")
public final class FxVolatilitySurfaceYearFractionParameterMetadata
    implements ParameterMetadata, ImmutableBean, Serializable {

  /**
   * The year fraction of the surface node.
   * <p>
   * This is the time to expiry that the node on the surface is defined as.
   * There is not necessarily a direct relationship with a date from an underlying instrument.
   */
  @PropertyDefinition
  private final double yearFraction;
  /**
   * The tenor associated with the year fraction.
   */
  @PropertyDefinition(get = "optional")
  private final Tenor yearFractionTenor;
  /**
   * The strike of the surface node.
   * <p>
   * This is the strike that the node on the surface is defined as.
   */
  @PropertyDefinition(validate = "notNull")
  private final Strike strike;
  /**
   * The currency pair that describes the node.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyPair currencyPair;
  /**
   * The label that describes the node.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String label;

  //-------------------------------------------------------------------------
  /**
   * Creates node metadata using year fraction, strike and currency pair.
   * 
   * @param yearFraction  the year fraction
   * @param strike  the strike
   * @param currencyPair  the currency pair
   * @return node metadata 
   */
  public static FxVolatilitySurfaceYearFractionParameterMetadata of(
      double yearFraction,
      Strike strike,
      CurrencyPair currencyPair) {

    String label = Pair.of(yearFraction, strike.getLabel()).toString();
    return new FxVolatilitySurfaceYearFractionParameterMetadata(yearFraction, null, strike, currencyPair, label);
  }

  /**
   * Creates node metadata using year fraction, associated tenor, strike and currency pair.
   * 
   * @param yearFraction  the year fraction
   * @param yearFractionTenor  the tenor associated with year fraction
   * @param strike  the strike
   * @param currencyPair  the currency pair
   * @return node metadata 
   */
  public static FxVolatilitySurfaceYearFractionParameterMetadata of(
      double yearFraction,
      Tenor yearFractionTenor,
      Strike strike,
      CurrencyPair currencyPair) {

    String label = Pair.of(yearFractionTenor, strike.getLabel()).toString();
    return new FxVolatilitySurfaceYearFractionParameterMetadata(yearFraction, yearFractionTenor, strike, currencyPair, label);
  }

  /**
   * Creates node using year fraction, strike, label and currency pair.
   * 
   * @param yearFraction  the year fraction
   * @param strike  the strike
   * @param label  the label to use
   * @param currencyPair  the currency pair
   * @return the metadata
   */
  public static FxVolatilitySurfaceYearFractionParameterMetadata of(
      double yearFraction,
      Strike strike,
      String label,
      CurrencyPair currencyPair) {

    return new FxVolatilitySurfaceYearFractionParameterMetadata(yearFraction, null, strike, currencyPair, label);
  }

  /**
   * Creates node using year fraction, associated tenor, strike, label and currency pair.
   * 
   * @param yearFraction  the year fraction
   * @param yearFractionTenor  the tenor associated with year fraction
   * @param strike  the strike
   * @param label  the label to use
   * @param currencyPair  the currency pair
   * @return the metadata
   */
  public static FxVolatilitySurfaceYearFractionParameterMetadata of(
      double yearFraction,
      Tenor yearFractionTenor,
      Strike strike,
      String label,
      CurrencyPair currencyPair) {

    return new FxVolatilitySurfaceYearFractionParameterMetadata(yearFraction, yearFractionTenor, strike, currencyPair, label);
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null && builder.strike != null) {
      builder.label = Pair.of(builder.yearFraction, builder.strike.getLabel()).toString();
    }
  }

  @Override
  public Pair<Double, Strike> getIdentifier() {
    return Pair.of(yearFraction, strike);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FxVolatilitySurfaceYearFractionParameterMetadata}.
   * @return the meta-bean, not null
   */
  public static FxVolatilitySurfaceYearFractionParameterMetadata.Meta meta() {
    return FxVolatilitySurfaceYearFractionParameterMetadata.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FxVolatilitySurfaceYearFractionParameterMetadata.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxVolatilitySurfaceYearFractionParameterMetadata(
      double yearFraction,
      Tenor yearFractionTenor,
      Strike strike,
      CurrencyPair currencyPair,
      String label) {
    JodaBeanUtils.notNull(strike, "strike");
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notEmpty(label, "label");
    this.yearFraction = yearFraction;
    this.yearFractionTenor = yearFractionTenor;
    this.strike = strike;
    this.currencyPair = currencyPair;
    this.label = label;
  }

  @Override
  public FxVolatilitySurfaceYearFractionParameterMetadata.Meta metaBean() {
    return FxVolatilitySurfaceYearFractionParameterMetadata.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction of the surface node.
   * <p>
   * This is the time to expiry that the node on the surface is defined as.
   * There is not necessarily a direct relationship with a date from an underlying instrument.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the tenor associated with the year fraction.
   * @return the optional value of the property, not null
   */
  public Optional<Tenor> getYearFractionTenor() {
    return Optional.ofNullable(yearFractionTenor);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike of the surface node.
   * <p>
   * This is the strike that the node on the surface is defined as.
   * @return the value of the property, not null
   */
  public Strike getStrike() {
    return strike;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency pair that describes the node.
   * @return the value of the property, not null
   */
  public CurrencyPair getCurrencyPair() {
    return currencyPair;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label that describes the node.
   * @return the value of the property, not empty
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxVolatilitySurfaceYearFractionParameterMetadata other = (FxVolatilitySurfaceYearFractionParameterMetadata) obj;
      return JodaBeanUtils.equal(yearFraction, other.yearFraction) &&
          JodaBeanUtils.equal(yearFractionTenor, other.yearFractionTenor) &&
          JodaBeanUtils.equal(strike, other.strike) &&
          JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(label, other.label);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFractionTenor);
    hash = hash * 31 + JodaBeanUtils.hashCode(strike);
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("FxVolatilitySurfaceYearFractionParameterMetadata{");
    buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
    buf.append("yearFractionTenor").append('=').append(JodaBeanUtils.toString(yearFractionTenor)).append(',').append(' ');
    buf.append("strike").append('=').append(JodaBeanUtils.toString(strike)).append(',').append(' ');
    buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
    buf.append("label").append('=').append(JodaBeanUtils.toString(label));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxVolatilitySurfaceYearFractionParameterMetadata}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", FxVolatilitySurfaceYearFractionParameterMetadata.class, Double.TYPE);
    /**
     * The meta-property for the {@code yearFractionTenor} property.
     */
    private final MetaProperty<Tenor> yearFractionTenor = DirectMetaProperty.ofImmutable(
        this, "yearFractionTenor", FxVolatilitySurfaceYearFractionParameterMetadata.class, Tenor.class);
    /**
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<Strike> strike = DirectMetaProperty.ofImmutable(
        this, "strike", FxVolatilitySurfaceYearFractionParameterMetadata.class, Strike.class);
    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", FxVolatilitySurfaceYearFractionParameterMetadata.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", FxVolatilitySurfaceYearFractionParameterMetadata.class, String.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "yearFraction",
        "yearFractionTenor",
        "strike",
        "currencyPair",
        "label");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1731780257:  // yearFraction
          return yearFraction;
        case -1032770399:  // yearFractionTenor
          return yearFractionTenor;
        case -891985998:  // strike
          return strike;
        case 1005147787:  // currencyPair
          return currencyPair;
        case 102727412:  // label
          return label;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxVolatilitySurfaceYearFractionParameterMetadata> builder() {
      return new FxVolatilitySurfaceYearFractionParameterMetadata.Builder();
    }

    @Override
    public Class<? extends FxVolatilitySurfaceYearFractionParameterMetadata> beanType() {
      return FxVolatilitySurfaceYearFractionParameterMetadata.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    /**
     * The meta-property for the {@code yearFractionTenor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Tenor> yearFractionTenor() {
      return yearFractionTenor;
    }

    /**
     * The meta-property for the {@code strike} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Strike> strike() {
      return strike;
    }

    /**
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return currencyPair;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1731780257:  // yearFraction
          return ((FxVolatilitySurfaceYearFractionParameterMetadata) bean).getYearFraction();
        case -1032770399:  // yearFractionTenor
          return ((FxVolatilitySurfaceYearFractionParameterMetadata) bean).yearFractionTenor;
        case -891985998:  // strike
          return ((FxVolatilitySurfaceYearFractionParameterMetadata) bean).getStrike();
        case 1005147787:  // currencyPair
          return ((FxVolatilitySurfaceYearFractionParameterMetadata) bean).getCurrencyPair();
        case 102727412:  // label
          return ((FxVolatilitySurfaceYearFractionParameterMetadata) bean).getLabel();
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
   * The bean-builder for {@code FxVolatilitySurfaceYearFractionParameterMetadata}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<FxVolatilitySurfaceYearFractionParameterMetadata> {

    private double yearFraction;
    private Tenor yearFractionTenor;
    private Strike strike;
    private CurrencyPair currencyPair;
    private String label;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1731780257:  // yearFraction
          return yearFraction;
        case -1032770399:  // yearFractionTenor
          return yearFractionTenor;
        case -891985998:  // strike
          return strike;
        case 1005147787:  // currencyPair
          return currencyPair;
        case 102727412:  // label
          return label;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
          break;
        case -1032770399:  // yearFractionTenor
          this.yearFractionTenor = (Tenor) newValue;
          break;
        case -891985998:  // strike
          this.strike = (Strike) newValue;
          break;
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public FxVolatilitySurfaceYearFractionParameterMetadata build() {
      preBuild(this);
      return new FxVolatilitySurfaceYearFractionParameterMetadata(
          yearFraction,
          yearFractionTenor,
          strike,
          currencyPair,
          label);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("FxVolatilitySurfaceYearFractionParameterMetadata.Builder{");
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("yearFractionTenor").append('=').append(JodaBeanUtils.toString(yearFractionTenor)).append(',').append(' ');
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike)).append(',').append(' ');
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

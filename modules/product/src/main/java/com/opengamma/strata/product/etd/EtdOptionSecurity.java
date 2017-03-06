/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PutCall;

/**
 * An instrument representing an exchange traded derivative (ETD) option.
 * <p>
 * A security representing a standardized contract that gives the buyer the right but not the obligation to
 * buy or sell an underlying asset at an agreed price.
 */
@BeanDefinition
public final class EtdOptionSecurity
    implements Security, SecuritizedProduct, ImmutableBean, Serializable {

  /**
   * The standard security information.
   * <p>
   * This includes the security identifier.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityInfo info;
  /**
   * The ID of the contract specification from which this security is derived.
   */
  @PropertyDefinition(validate = "notNull")
  private final EtdContractSpecId contractSpecId;
  /**
   * The expiry month.
   * <p>
   * This is used to describe the instance of the option.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth expiryMonth;
  /**
   * The style of ETD.
   * <p>
   * This captures the style of the ETD. The most common style is 'Monthly'.
   * Other styles are 'Weekly', 'Daily' and 'Flex'.
   * <p>
   * When building, this defaults to 'Monthly'.
   */
  @PropertyDefinition(validate = "notNull")
  private final EtdStyle style;
  /**
   * The version of the option, defaulted to zero.
   * <p>
   * Some options can have multiple versions, representing some kind of change over time.
   * Version zero is the baseline, version one and later indicates some kind of change occurred.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final int version;
  /**
   * Whether the option is a put or call.
   */
  @PropertyDefinition(validate = "notNull")
  private final PutCall putCall;
  /**
   * The strike price, in decimal form, may be negative.
   */
  @PropertyDefinition
  private final double strikePrice;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a contract specification, expiry year-month, style, version, put/call and strike price.
   * <p>
   * The security identifier will be automatically created using {@link EtdIdUtils}.
   * The specification must be for an option.
   *
   * @param spec  the option contract specification
   * @param expiryMonth  the expiry month of the future
   * @param style  the style of the ETD, such as 'Monthly', 'Weekly, 'Daily' or 'Flex.
   * @param version  the non-negative version, zero if versioning does not apply
   * @param putCall  whether the option is a put or call
   * @param strikePrice  the strike price of the option
   * @return an option security based on this contract specification
   * @throws IllegalStateException if the product type of the contract specification is not {@code OPTION}
   */
  public static EtdOptionSecurity of(
      EtdContractSpec spec,
      YearMonth expiryMonth,
      EtdStyle style,
      int version,
      PutCall putCall,
      double strikePrice) {

    if (spec.getType() != EtdType.OPTION) {
      throw new IllegalStateException(
          Messages.format("Cannot create an EtdOptionSecurity from a contract specification of type '{}'", spec.getType()));
    }
    SecurityId securityId =
        EtdIdUtils.optionId(spec.getExchangeId(), spec.getContractCode(), expiryMonth, style, version, putCall, strikePrice);
    return EtdOptionSecurity.builder()
        .info(SecurityInfo.of(securityId, spec.getPriceInfo()))
        .contractSpecId(spec.getId())
        .expiryMonth(expiryMonth)
        .style(style)
        .version(version)
        .putCall(putCall)
        .strikePrice(strikePrice)
        .build();
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.style = EtdStyle.MONTHLY;
  }

  //-------------------------------------------------------------------------
  @Override
  public SecurityId getSecurityId() {
    return Security.super.getSecurityId();
  }

  @Override
  public Currency getCurrency() {
    return Security.super.getCurrency();
  }

  @Override
  public ImmutableSet<SecurityId> getUnderlyingIds() {
    return ImmutableSet.of();
  }

  @Override
  public SecuritizedProduct createProduct(ReferenceData refData) {
    return this;
  }

  @Override
  public Trade createTrade(TradeInfo tradeInfo, double quantity, double tradePrice, ReferenceData refData) {
    return EtdOptionTrade.builder()
        .info(tradeInfo)
        .quantity(quantity)
        .price(tradePrice)
        .security(this)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code EtdOptionSecurity}.
   * @return the meta-bean, not null
   */
  public static EtdOptionSecurity.Meta meta() {
    return EtdOptionSecurity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(EtdOptionSecurity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static EtdOptionSecurity.Builder builder() {
    return new EtdOptionSecurity.Builder();
  }

  private EtdOptionSecurity(
      SecurityInfo info,
      EtdContractSpecId contractSpecId,
      YearMonth expiryMonth,
      EtdStyle style,
      int version,
      PutCall putCall,
      double strikePrice) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(contractSpecId, "contractSpecId");
    JodaBeanUtils.notNull(expiryMonth, "expiryMonth");
    JodaBeanUtils.notNull(style, "style");
    ArgChecker.notNegative(version, "version");
    JodaBeanUtils.notNull(putCall, "putCall");
    this.info = info;
    this.contractSpecId = contractSpecId;
    this.expiryMonth = expiryMonth;
    this.style = style;
    this.version = version;
    this.putCall = putCall;
    this.strikePrice = strikePrice;
  }

  @Override
  public EtdOptionSecurity.Meta metaBean() {
    return EtdOptionSecurity.Meta.INSTANCE;
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
   * Gets the standard security information.
   * <p>
   * This includes the security identifier.
   * @return the value of the property, not null
   */
  @Override
  public SecurityInfo getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the ID of the contract specification from which this security is derived.
   * @return the value of the property, not null
   */
  public EtdContractSpecId getContractSpecId() {
    return contractSpecId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry month.
   * <p>
   * This is used to describe the instance of the option.
   * @return the value of the property, not null
   */
  public YearMonth getExpiryMonth() {
    return expiryMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the style of ETD.
   * <p>
   * This captures the style of the ETD. The most common style is 'Monthly'.
   * Other styles are 'Weekly', 'Daily' and 'Flex'.
   * <p>
   * When building, this defaults to 'Monthly'.
   * @return the value of the property, not null
   */
  public EtdStyle getStyle() {
    return style;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the version of the option, defaulted to zero.
   * <p>
   * Some options can have multiple versions, representing some kind of change over time.
   * Version zero is the baseline, version one and later indicates some kind of change occurred.
   * @return the value of the property
   */
  public int getVersion() {
    return version;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the option is a put or call.
   * @return the value of the property, not null
   */
  public PutCall getPutCall() {
    return putCall;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike price, in decimal form, may be negative.
   * @return the value of the property
   */
  public double getStrikePrice() {
    return strikePrice;
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
      EtdOptionSecurity other = (EtdOptionSecurity) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(contractSpecId, other.contractSpecId) &&
          JodaBeanUtils.equal(expiryMonth, other.expiryMonth) &&
          JodaBeanUtils.equal(style, other.style) &&
          (version == other.version) &&
          JodaBeanUtils.equal(putCall, other.putCall) &&
          JodaBeanUtils.equal(strikePrice, other.strikePrice);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(contractSpecId);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryMonth);
    hash = hash * 31 + JodaBeanUtils.hashCode(style);
    hash = hash * 31 + JodaBeanUtils.hashCode(version);
    hash = hash * 31 + JodaBeanUtils.hashCode(putCall);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikePrice);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("EtdOptionSecurity{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("contractSpecId").append('=').append(contractSpecId).append(',').append(' ');
    buf.append("expiryMonth").append('=').append(expiryMonth).append(',').append(' ');
    buf.append("style").append('=').append(style).append(',').append(' ');
    buf.append("version").append('=').append(version).append(',').append(' ');
    buf.append("putCall").append('=').append(putCall).append(',').append(' ');
    buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code EtdOptionSecurity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<SecurityInfo> info = DirectMetaProperty.ofImmutable(
        this, "info", EtdOptionSecurity.class, SecurityInfo.class);
    /**
     * The meta-property for the {@code contractSpecId} property.
     */
    private final MetaProperty<EtdContractSpecId> contractSpecId = DirectMetaProperty.ofImmutable(
        this, "contractSpecId", EtdOptionSecurity.class, EtdContractSpecId.class);
    /**
     * The meta-property for the {@code expiryMonth} property.
     */
    private final MetaProperty<YearMonth> expiryMonth = DirectMetaProperty.ofImmutable(
        this, "expiryMonth", EtdOptionSecurity.class, YearMonth.class);
    /**
     * The meta-property for the {@code style} property.
     */
    private final MetaProperty<EtdStyle> style = DirectMetaProperty.ofImmutable(
        this, "style", EtdOptionSecurity.class, EtdStyle.class);
    /**
     * The meta-property for the {@code version} property.
     */
    private final MetaProperty<Integer> version = DirectMetaProperty.ofImmutable(
        this, "version", EtdOptionSecurity.class, Integer.TYPE);
    /**
     * The meta-property for the {@code putCall} property.
     */
    private final MetaProperty<PutCall> putCall = DirectMetaProperty.ofImmutable(
        this, "putCall", EtdOptionSecurity.class, PutCall.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", EtdOptionSecurity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "contractSpecId",
        "expiryMonth",
        "style",
        "version",
        "putCall",
        "strikePrice");

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
        case 948987368:  // contractSpecId
          return contractSpecId;
        case 459635981:  // expiryMonth
          return expiryMonth;
        case 109780401:  // style
          return style;
        case 351608024:  // version
          return version;
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public EtdOptionSecurity.Builder builder() {
      return new EtdOptionSecurity.Builder();
    }

    @Override
    public Class<? extends EtdOptionSecurity> beanType() {
      return EtdOptionSecurity.class;
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
    public MetaProperty<SecurityInfo> info() {
      return info;
    }

    /**
     * The meta-property for the {@code contractSpecId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<EtdContractSpecId> contractSpecId() {
      return contractSpecId;
    }

    /**
     * The meta-property for the {@code expiryMonth} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YearMonth> expiryMonth() {
      return expiryMonth;
    }

    /**
     * The meta-property for the {@code style} property.
     * @return the meta-property, not null
     */
    public MetaProperty<EtdStyle> style() {
      return style;
    }

    /**
     * The meta-property for the {@code version} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> version() {
      return version;
    }

    /**
     * The meta-property for the {@code putCall} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PutCall> putCall() {
      return putCall;
    }

    /**
     * The meta-property for the {@code strikePrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strikePrice() {
      return strikePrice;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((EtdOptionSecurity) bean).getInfo();
        case 948987368:  // contractSpecId
          return ((EtdOptionSecurity) bean).getContractSpecId();
        case 459635981:  // expiryMonth
          return ((EtdOptionSecurity) bean).getExpiryMonth();
        case 109780401:  // style
          return ((EtdOptionSecurity) bean).getStyle();
        case 351608024:  // version
          return ((EtdOptionSecurity) bean).getVersion();
        case -219971059:  // putCall
          return ((EtdOptionSecurity) bean).getPutCall();
        case 50946231:  // strikePrice
          return ((EtdOptionSecurity) bean).getStrikePrice();
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
   * The bean-builder for {@code EtdOptionSecurity}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<EtdOptionSecurity> {

    private SecurityInfo info;
    private EtdContractSpecId contractSpecId;
    private YearMonth expiryMonth;
    private EtdStyle style;
    private int version;
    private PutCall putCall;
    private double strikePrice;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(EtdOptionSecurity beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.contractSpecId = beanToCopy.getContractSpecId();
      this.expiryMonth = beanToCopy.getExpiryMonth();
      this.style = beanToCopy.getStyle();
      this.version = beanToCopy.getVersion();
      this.putCall = beanToCopy.getPutCall();
      this.strikePrice = beanToCopy.getStrikePrice();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 948987368:  // contractSpecId
          return contractSpecId;
        case 459635981:  // expiryMonth
          return expiryMonth;
        case 109780401:  // style
          return style;
        case 351608024:  // version
          return version;
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (SecurityInfo) newValue;
          break;
        case 948987368:  // contractSpecId
          this.contractSpecId = (EtdContractSpecId) newValue;
          break;
        case 459635981:  // expiryMonth
          this.expiryMonth = (YearMonth) newValue;
          break;
        case 109780401:  // style
          this.style = (EtdStyle) newValue;
          break;
        case 351608024:  // version
          this.version = (Integer) newValue;
          break;
        case -219971059:  // putCall
          this.putCall = (PutCall) newValue;
          break;
        case 50946231:  // strikePrice
          this.strikePrice = (Double) newValue;
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
    public EtdOptionSecurity build() {
      return new EtdOptionSecurity(
          info,
          contractSpecId,
          expiryMonth,
          style,
          version,
          putCall,
          strikePrice);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the standard security information.
     * <p>
     * This includes the security identifier.
     * @param info  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder info(SecurityInfo info) {
      JodaBeanUtils.notNull(info, "info");
      this.info = info;
      return this;
    }

    /**
     * Sets the ID of the contract specification from which this security is derived.
     * @param contractSpecId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder contractSpecId(EtdContractSpecId contractSpecId) {
      JodaBeanUtils.notNull(contractSpecId, "contractSpecId");
      this.contractSpecId = contractSpecId;
      return this;
    }

    /**
     * Sets the expiry month.
     * <p>
     * This is used to describe the instance of the option.
     * @param expiryMonth  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryMonth(YearMonth expiryMonth) {
      JodaBeanUtils.notNull(expiryMonth, "expiryMonth");
      this.expiryMonth = expiryMonth;
      return this;
    }

    /**
     * Sets the style of ETD.
     * <p>
     * This captures the style of the ETD. The most common style is 'Monthly'.
     * Other styles are 'Weekly', 'Daily' and 'Flex'.
     * <p>
     * When building, this defaults to 'Monthly'.
     * @param style  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder style(EtdStyle style) {
      JodaBeanUtils.notNull(style, "style");
      this.style = style;
      return this;
    }

    /**
     * Sets the version of the option, defaulted to zero.
     * <p>
     * Some options can have multiple versions, representing some kind of change over time.
     * Version zero is the baseline, version one and later indicates some kind of change occurred.
     * @param version  the new value
     * @return this, for chaining, not null
     */
    public Builder version(int version) {
      ArgChecker.notNegative(version, "version");
      this.version = version;
      return this;
    }

    /**
     * Sets whether the option is a put or call.
     * @param putCall  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder putCall(PutCall putCall) {
      JodaBeanUtils.notNull(putCall, "putCall");
      this.putCall = putCall;
      return this;
    }

    /**
     * Sets the strike price, in decimal form, may be negative.
     * @param strikePrice  the new value
     * @return this, for chaining, not null
     */
    public Builder strikePrice(double strikePrice) {
      this.strikePrice = strikePrice;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("EtdOptionSecurity.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("contractSpecId").append('=').append(JodaBeanUtils.toString(contractSpecId)).append(',').append(' ');
      buf.append("expiryMonth").append('=').append(JodaBeanUtils.toString(expiryMonth)).append(',').append(' ');
      buf.append("style").append('=').append(JodaBeanUtils.toString(style)).append(',').append(' ');
      buf.append("version").append('=').append(JodaBeanUtils.toString(version)).append(',').append(' ');
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

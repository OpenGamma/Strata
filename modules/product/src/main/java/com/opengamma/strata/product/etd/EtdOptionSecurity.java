/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.io.Serializable;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
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
    implements EtdSecurity, ImmutableBean, Serializable {

  /** YearMonth format. */
  private static final DateTimeFormatter YM_FORMAT = DateTimeFormatter.ofPattern("MMMuu", Locale.UK);

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
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final EtdContractSpecId contractSpecId;
  /**
   * The year-month of the expiry.
   * <p>
   * Expiry will occur on a date implied by the variant of the ETD.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final YearMonth expiry;
  /**
   * The variant of ETD.
   * <p>
   * This captures the variant of the ETD. The most common variant is 'Monthly'.
   * Other variants are 'Weekly', 'Daily' and 'Flex'.
   * <p>
   * When building, this defaults to 'Monthly'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final EtdVariant variant;
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
  /**
   * The expiry year-month of the underlying instrument.
   * <p>
   * If an option has an underlying instrument, the expiry of that instrument can be specified here.
   * For example, you can have an option expiring in March on the underlying March future, or on the underlying June future.
   * Not all options have an underlying instrument, thus the property is optional.
   * <p>
   * In many cases, the expiry of the underlying instrument is the same as the expiry of the option.
   * In this case, the expiry is often omitted, even though it probably should not be.
   */
  @PropertyDefinition(get = "optional")
  private final YearMonth underlyingExpiryMonth;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a contract specification, expiry year-month, variant, version, put/call and strike price.
   * <p>
   * The security identifier will be automatically created using {@link EtdIdUtils}.
   * The specification must be for an option.
   *
   * @param spec  the option contract specification
   * @param expiry  the expiry year-month of the option
   * @param variant  the variant of the ETD, such as 'Monthly', 'Weekly, 'Daily' or 'Flex'
   * @param version  the non-negative version, zero if versioning does not apply
   * @param putCall  whether the option is a put or call
   * @param strikePrice  the strike price of the option
   * @return an option security based on this contract specification
   * @throws IllegalStateException if the product type of the contract specification is not {@code OPTION}
   */
  public static EtdOptionSecurity of(
      EtdContractSpec spec,
      YearMonth expiry,
      EtdVariant variant,
      int version,
      PutCall putCall,
      double strikePrice) {

    return of(spec, expiry, variant, version, putCall, strikePrice, null);
  }

  /**
   * Obtains an instance from a contract specification, expiry year-month, variant,
   * version, put/call, strike price and underlying expiry.
   * <p>
   * The security identifier will be automatically created using {@link EtdIdUtils}.
   * The specification must be for an option.
   *
   * @param spec  the option contract specification
   * @param expiry  the expiry year-month of the option
   * @param variant  the variant of the ETD, such as 'Monthly', 'Weekly, 'Daily' or 'Flex'
   * @param version  the non-negative version, zero if versioning does not apply
   * @param putCall  whether the option is a put or call
   * @param strikePrice  the strike price of the option
   * @param underlyingExpiryMonth  the expiry of the underlying instrument, such as a future, may be null
   * @return an option security based on this contract specification
   * @throws IllegalStateException if the product type of the contract specification is not {@code OPTION}
   */
  public static EtdOptionSecurity of(
      EtdContractSpec spec,
      YearMonth expiry,
      EtdVariant variant,
      int version,
      PutCall putCall,
      double strikePrice,
      YearMonth underlyingExpiryMonth) {

    if (spec.getType() != EtdType.OPTION) {
      throw new IllegalStateException(
          Messages.format("Cannot create an EtdOptionSecurity from a contract specification of type '{}'", spec.getType()));
    }
    SecurityId securityId = EtdIdUtils.optionId(
        spec.getExchangeId(), spec.getContractCode(), expiry, variant, version, putCall, strikePrice, underlyingExpiryMonth);
    return EtdOptionSecurity.builder()
        .info(SecurityInfo.of(securityId, spec.getPriceInfo()))
        .contractSpecId(spec.getId())
        .expiry(expiry)
        .variant(variant)
        .version(version)
        .putCall(putCall)
        .strikePrice(strikePrice)
        .underlyingExpiryMonth(underlyingExpiryMonth)
        .build();
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.variant = EtdVariant.MONTHLY;
  }

  //-------------------------------------------------------------------------
  @Override
  public EtdType getType() {
    return EtdType.OPTION;
  }

  @Override
  public EtdOptionSecurity withInfo(SecurityInfo info) {
    return toBuilder().info(info).build();
  }

  @Override
  public EtdOptionSecurity createProduct(ReferenceData refData) {
    return this;
  }

  @Override
  public EtdOptionTrade createTrade(TradeInfo tradeInfo, double quantity, double tradePrice, ReferenceData refData) {
    return EtdOptionTrade.builder()
        .info(tradeInfo)
        .quantity(quantity)
        .price(tradePrice)
        .security(this)
        .build();
  }

  @Override
  public EtdOptionPosition createPosition(PositionInfo positionInfo, double quantity, ReferenceData refData) {
    return EtdOptionPosition.ofNet(positionInfo, this, quantity);
  }

  @Override
  public EtdOptionPosition createPosition(
      PositionInfo positionInfo,
      double longQuantity,
      double shortQuantity,
      ReferenceData refData) {

    return EtdOptionPosition.ofLongShort(positionInfo, this, longQuantity, shortQuantity);
  }

  /**
   * Summarizes this ETD option into string form.
   *
   * @return the summary description
   */
  public String summaryDescription() {
    String putCallStr = putCall == PutCall.PUT ? "P" : "C";
    String versionCode = version > 0 ? "V" + version + " " : "";

    NumberFormat f = NumberFormat.getIntegerInstance(Locale.ENGLISH);
    f.setGroupingUsed(false);
    f.setMaximumFractionDigits(8);
    String strikeStr = f.format(strikePrice).replace('-', 'M');

    return expiry.format(YM_FORMAT) +
        variant.getCode() + " " +
        versionCode +
        putCallStr +
        strikeStr;
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code EtdOptionSecurity}.
   * @return the meta-bean, not null
   */
  public static EtdOptionSecurity.Meta meta() {
    return EtdOptionSecurity.Meta.INSTANCE;
  }

  static {
    MetaBean.register(EtdOptionSecurity.Meta.INSTANCE);
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
      YearMonth expiry,
      EtdVariant variant,
      int version,
      PutCall putCall,
      double strikePrice,
      YearMonth underlyingExpiryMonth) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(contractSpecId, "contractSpecId");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(variant, "variant");
    ArgChecker.notNegative(version, "version");
    JodaBeanUtils.notNull(putCall, "putCall");
    this.info = info;
    this.contractSpecId = contractSpecId;
    this.expiry = expiry;
    this.variant = variant;
    this.version = version;
    this.putCall = putCall;
    this.strikePrice = strikePrice;
    this.underlyingExpiryMonth = underlyingExpiryMonth;
  }

  @Override
  public EtdOptionSecurity.Meta metaBean() {
    return EtdOptionSecurity.Meta.INSTANCE;
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
  @Override
  public EtdContractSpecId getContractSpecId() {
    return contractSpecId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year-month of the expiry.
   * <p>
   * Expiry will occur on a date implied by the variant of the ETD.
   * @return the value of the property, not null
   */
  @Override
  public YearMonth getExpiry() {
    return expiry;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the variant of ETD.
   * <p>
   * This captures the variant of the ETD. The most common variant is 'Monthly'.
   * Other variants are 'Weekly', 'Daily' and 'Flex'.
   * <p>
   * When building, this defaults to 'Monthly'.
   * @return the value of the property, not null
   */
  @Override
  public EtdVariant getVariant() {
    return variant;
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
   * Gets the expiry year-month of the underlying instrument.
   * <p>
   * If an option has an underlying instrument, the expiry of that instrument can be specified here.
   * For example, you can have an option expiring in March on the underlying March future, or on the underlying June future.
   * Not all options have an underlying instrument, thus the property is optional.
   * <p>
   * In many cases, the expiry of the underlying instrument is the same as the expiry of the option.
   * In this case, the expiry is often omitted, even though it probably should not be.
   * @return the optional value of the property, not null
   */
  public Optional<YearMonth> getUnderlyingExpiryMonth() {
    return Optional.ofNullable(underlyingExpiryMonth);
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
          JodaBeanUtils.equal(expiry, other.expiry) &&
          JodaBeanUtils.equal(variant, other.variant) &&
          (version == other.version) &&
          JodaBeanUtils.equal(putCall, other.putCall) &&
          JodaBeanUtils.equal(strikePrice, other.strikePrice) &&
          JodaBeanUtils.equal(underlyingExpiryMonth, other.underlyingExpiryMonth);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(contractSpecId);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(variant);
    hash = hash * 31 + JodaBeanUtils.hashCode(version);
    hash = hash * 31 + JodaBeanUtils.hashCode(putCall);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikePrice);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingExpiryMonth);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("EtdOptionSecurity{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("contractSpecId").append('=').append(contractSpecId).append(',').append(' ');
    buf.append("expiry").append('=').append(expiry).append(',').append(' ');
    buf.append("variant").append('=').append(variant).append(',').append(' ');
    buf.append("version").append('=').append(version).append(',').append(' ');
    buf.append("putCall").append('=').append(putCall).append(',').append(' ');
    buf.append("strikePrice").append('=').append(strikePrice).append(',').append(' ');
    buf.append("underlyingExpiryMonth").append('=').append(JodaBeanUtils.toString(underlyingExpiryMonth));
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
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<YearMonth> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", EtdOptionSecurity.class, YearMonth.class);
    /**
     * The meta-property for the {@code variant} property.
     */
    private final MetaProperty<EtdVariant> variant = DirectMetaProperty.ofImmutable(
        this, "variant", EtdOptionSecurity.class, EtdVariant.class);
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
     * The meta-property for the {@code underlyingExpiryMonth} property.
     */
    private final MetaProperty<YearMonth> underlyingExpiryMonth = DirectMetaProperty.ofImmutable(
        this, "underlyingExpiryMonth", EtdOptionSecurity.class, YearMonth.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "contractSpecId",
        "expiry",
        "variant",
        "version",
        "putCall",
        "strikePrice",
        "underlyingExpiryMonth");

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
        case -1289159373:  // expiry
          return expiry;
        case 236785797:  // variant
          return variant;
        case 351608024:  // version
          return version;
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
        case 1929351536:  // underlyingExpiryMonth
          return underlyingExpiryMonth;
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
     * The meta-property for the {@code expiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YearMonth> expiry() {
      return expiry;
    }

    /**
     * The meta-property for the {@code variant} property.
     * @return the meta-property, not null
     */
    public MetaProperty<EtdVariant> variant() {
      return variant;
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

    /**
     * The meta-property for the {@code underlyingExpiryMonth} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YearMonth> underlyingExpiryMonth() {
      return underlyingExpiryMonth;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((EtdOptionSecurity) bean).getInfo();
        case 948987368:  // contractSpecId
          return ((EtdOptionSecurity) bean).getContractSpecId();
        case -1289159373:  // expiry
          return ((EtdOptionSecurity) bean).getExpiry();
        case 236785797:  // variant
          return ((EtdOptionSecurity) bean).getVariant();
        case 351608024:  // version
          return ((EtdOptionSecurity) bean).getVersion();
        case -219971059:  // putCall
          return ((EtdOptionSecurity) bean).getPutCall();
        case 50946231:  // strikePrice
          return ((EtdOptionSecurity) bean).getStrikePrice();
        case 1929351536:  // underlyingExpiryMonth
          return ((EtdOptionSecurity) bean).underlyingExpiryMonth;
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
    private YearMonth expiry;
    private EtdVariant variant;
    private int version;
    private PutCall putCall;
    private double strikePrice;
    private YearMonth underlyingExpiryMonth;

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
      this.expiry = beanToCopy.getExpiry();
      this.variant = beanToCopy.getVariant();
      this.version = beanToCopy.getVersion();
      this.putCall = beanToCopy.getPutCall();
      this.strikePrice = beanToCopy.getStrikePrice();
      this.underlyingExpiryMonth = beanToCopy.underlyingExpiryMonth;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 948987368:  // contractSpecId
          return contractSpecId;
        case -1289159373:  // expiry
          return expiry;
        case 236785797:  // variant
          return variant;
        case 351608024:  // version
          return version;
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
        case 1929351536:  // underlyingExpiryMonth
          return underlyingExpiryMonth;
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
        case -1289159373:  // expiry
          this.expiry = (YearMonth) newValue;
          break;
        case 236785797:  // variant
          this.variant = (EtdVariant) newValue;
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
        case 1929351536:  // underlyingExpiryMonth
          this.underlyingExpiryMonth = (YearMonth) newValue;
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
    public EtdOptionSecurity build() {
      return new EtdOptionSecurity(
          info,
          contractSpecId,
          expiry,
          variant,
          version,
          putCall,
          strikePrice,
          underlyingExpiryMonth);
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
     * Sets the year-month of the expiry.
     * <p>
     * Expiry will occur on a date implied by the variant of the ETD.
     * @param expiry  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiry(YearMonth expiry) {
      JodaBeanUtils.notNull(expiry, "expiry");
      this.expiry = expiry;
      return this;
    }

    /**
     * Sets the variant of ETD.
     * <p>
     * This captures the variant of the ETD. The most common variant is 'Monthly'.
     * Other variants are 'Weekly', 'Daily' and 'Flex'.
     * <p>
     * When building, this defaults to 'Monthly'.
     * @param variant  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder variant(EtdVariant variant) {
      JodaBeanUtils.notNull(variant, "variant");
      this.variant = variant;
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

    /**
     * Sets the expiry year-month of the underlying instrument.
     * <p>
     * If an option has an underlying instrument, the expiry of that instrument can be specified here.
     * For example, you can have an option expiring in March on the underlying March future, or on the underlying June future.
     * Not all options have an underlying instrument, thus the property is optional.
     * <p>
     * In many cases, the expiry of the underlying instrument is the same as the expiry of the option.
     * In this case, the expiry is often omitted, even though it probably should not be.
     * @param underlyingExpiryMonth  the new value
     * @return this, for chaining, not null
     */
    public Builder underlyingExpiryMonth(YearMonth underlyingExpiryMonth) {
      this.underlyingExpiryMonth = underlyingExpiryMonth;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("EtdOptionSecurity.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("contractSpecId").append('=').append(JodaBeanUtils.toString(contractSpecId)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("variant").append('=').append(JodaBeanUtils.toString(variant)).append(',').append(' ');
      buf.append("version").append('=').append(JodaBeanUtils.toString(version)).append(',').append(' ');
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
      buf.append("underlyingExpiryMonth").append('=').append(JodaBeanUtils.toString(underlyingExpiryMonth));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.io.Serializable;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

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
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.TradeInfo;

/**
 * An instrument representing an exchange traded derivative (ETD) future.
 * <p>
 * A security representing a standardized contact between two parties to buy or sell an asset at a
 * future date for an agreed price.
 */
@BeanDefinition
public final class EtdFutureSecurity
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

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a contract specification, expiry year-month and variant.
   * <p>
   * The security identifier will be automatically created using {@link EtdIdUtils}.
   * The specification must be for a future.
   *
   * @param spec  the future contract specification
   * @param expiry  the expiry year-month of the future
   * @param variant  the variant of the ETD, such as 'Monthly', 'Weekly, 'Daily' or 'Flex'
   * @return a future security based on this contract specification
   * @throws IllegalStateException if the product type of the contract specification is not {@code FUTURE}
   */
  public static EtdFutureSecurity of(EtdContractSpec spec, YearMonth expiry, EtdVariant variant) {
    if (spec.getType() != EtdType.FUTURE) {
      throw new IllegalStateException(
          Messages.format("Cannot create an EtdFutureSecurity from a contract specification of type '{}'", spec.getType()));
    }
    SecurityId securityId = EtdIdUtils.futureId(spec.getExchangeId(), spec.getContractCode(), expiry, variant);
    return EtdFutureSecurity.builder()
        .info(SecurityInfo.of(securityId, spec.getPriceInfo()))
        .contractSpecId(spec.getId())
        .expiry(expiry)
        .variant(variant)
        .build();
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.variant = EtdVariant.MONTHLY;
  }

  //-------------------------------------------------------------------------
  @Override
  public EtdType getType() {
    return EtdType.FUTURE;
  }

  @Override
  public EtdFutureSecurity withInfo(SecurityInfo info) {
    return toBuilder().info(info).build();
  }

  @Override
  public EtdFutureSecurity createProduct(ReferenceData refData) {
    return this;
  }

  @Override
  public EtdFutureTrade createTrade(TradeInfo tradeInfo, double quantity, double tradePrice, ReferenceData refData) {
    return EtdFutureTrade.builder()
        .info(tradeInfo)
        .quantity(quantity)
        .price(tradePrice)
        .security(this)
        .build();
  }

  @Override
  public EtdFuturePosition createPosition(PositionInfo positionInfo, double quantity, ReferenceData refData) {
    return EtdFuturePosition.ofNet(positionInfo, this, quantity);
  }

  @Override
  public EtdFuturePosition createPosition(
      PositionInfo positionInfo,
      double longQuantity,
      double shortQuantity,
      ReferenceData refData) {

    return EtdFuturePosition.ofLongShort(positionInfo, this, longQuantity, shortQuantity);
  }

  /**
   * Summarizes this ETD future into string form.
   *
   * @return the summary description
   */
  public String summaryDescription() {
    return variant.getCode() + expiry.format(YM_FORMAT);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code EtdFutureSecurity}.
   * @return the meta-bean, not null
   */
  public static EtdFutureSecurity.Meta meta() {
    return EtdFutureSecurity.Meta.INSTANCE;
  }

  static {
    MetaBean.register(EtdFutureSecurity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static EtdFutureSecurity.Builder builder() {
    return new EtdFutureSecurity.Builder();
  }

  private EtdFutureSecurity(
      SecurityInfo info,
      EtdContractSpecId contractSpecId,
      YearMonth expiry,
      EtdVariant variant) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(contractSpecId, "contractSpecId");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(variant, "variant");
    this.info = info;
    this.contractSpecId = contractSpecId;
    this.expiry = expiry;
    this.variant = variant;
  }

  @Override
  public EtdFutureSecurity.Meta metaBean() {
    return EtdFutureSecurity.Meta.INSTANCE;
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
      EtdFutureSecurity other = (EtdFutureSecurity) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(contractSpecId, other.contractSpecId) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
          JodaBeanUtils.equal(variant, other.variant);
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
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("EtdFutureSecurity{");
    buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
    buf.append("contractSpecId").append('=').append(JodaBeanUtils.toString(contractSpecId)).append(',').append(' ');
    buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
    buf.append("variant").append('=').append(JodaBeanUtils.toString(variant));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code EtdFutureSecurity}.
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
        this, "info", EtdFutureSecurity.class, SecurityInfo.class);
    /**
     * The meta-property for the {@code contractSpecId} property.
     */
    private final MetaProperty<EtdContractSpecId> contractSpecId = DirectMetaProperty.ofImmutable(
        this, "contractSpecId", EtdFutureSecurity.class, EtdContractSpecId.class);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<YearMonth> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", EtdFutureSecurity.class, YearMonth.class);
    /**
     * The meta-property for the {@code variant} property.
     */
    private final MetaProperty<EtdVariant> variant = DirectMetaProperty.ofImmutable(
        this, "variant", EtdFutureSecurity.class, EtdVariant.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "contractSpecId",
        "expiry",
        "variant");

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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public EtdFutureSecurity.Builder builder() {
      return new EtdFutureSecurity.Builder();
    }

    @Override
    public Class<? extends EtdFutureSecurity> beanType() {
      return EtdFutureSecurity.class;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((EtdFutureSecurity) bean).getInfo();
        case 948987368:  // contractSpecId
          return ((EtdFutureSecurity) bean).getContractSpecId();
        case -1289159373:  // expiry
          return ((EtdFutureSecurity) bean).getExpiry();
        case 236785797:  // variant
          return ((EtdFutureSecurity) bean).getVariant();
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
   * The bean-builder for {@code EtdFutureSecurity}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<EtdFutureSecurity> {

    private SecurityInfo info;
    private EtdContractSpecId contractSpecId;
    private YearMonth expiry;
    private EtdVariant variant;

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
    private Builder(EtdFutureSecurity beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.contractSpecId = beanToCopy.getContractSpecId();
      this.expiry = beanToCopy.getExpiry();
      this.variant = beanToCopy.getVariant();
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
    public EtdFutureSecurity build() {
      return new EtdFutureSecurity(
          info,
          contractSpecId,
          expiry,
          variant);
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

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("EtdFutureSecurity.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("contractSpecId").append('=').append(JodaBeanUtils.toString(contractSpecId)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("variant").append('=').append(JodaBeanUtils.toString(variant));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

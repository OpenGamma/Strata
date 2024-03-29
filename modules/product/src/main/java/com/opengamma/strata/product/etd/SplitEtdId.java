/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.time.YearMonth;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.MinimalMetaBean;

import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.common.ExchangeId;

/**
 * An OG-ETD identifier that has been split into its constituent parts.
 */
@BeanDefinition(style = "minimal")
public final class SplitEtdId implements ImmutableBean {

  /**
   * The security ID that was split.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityId securityId;
  /**
   * The type of the contract - future or option.
   */
  @PropertyDefinition(validate = "notNull")
  private final EtdType type;
  /**
   * The ID of the exchange where the instruments derived from the product are traded.
   */
  @PropertyDefinition(validate = "notNull")
  private final ExchangeId exchangeId;
  /**
   * The code supplied by the exchange for use in clearing and margining, such as in SPAN.
   */
  @PropertyDefinition(validate = "notNull")
  private final EtdContractCode contractCode;
  /**
   * The year-month of the expiry.
   * <p>
   * Expiry will occur on a date implied by the variant of the ETD.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth expiry;
  /**
   * The variant of ETD.
   * <p>
   * This captures the variant of the ETD. The most common variant is 'Monthly'.
   * Other variants are 'Weekly', 'Daily' and 'Flex'.
   * <p>
   * When building, this defaults to 'Monthly'.
   */
  @PropertyDefinition(validate = "notNull")
  private final EtdVariant variant;
  /**
   * The additional information if the ID is an option.
   */
  @PropertyDefinition(get = "optional")
  private final SplitEtdOption option;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    builder.type = builder.option != null ? EtdType.OPTION : EtdType.FUTURE;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a security identifier.
   *
   * @param securityId  the OG-ETD security identifier
   * @return the split identifier
   * @throws IllegalArgumentException if the identifier cannot be split
   */
  public static SplitEtdId from(SecurityId securityId) {
    return EtdIdUtils.splitId(securityId);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SplitEtdId}.
   */
  private static final TypedMetaBean<SplitEtdId> META_BEAN =
      MinimalMetaBean.of(
          SplitEtdId.class,
          new String[] {
              "securityId",
              "type",
              "exchangeId",
              "contractCode",
              "expiry",
              "variant",
              "option"},
          () -> new SplitEtdId.Builder(),
          b -> b.getSecurityId(),
          b -> b.getType(),
          b -> b.getExchangeId(),
          b -> b.getContractCode(),
          b -> b.getExpiry(),
          b -> b.getVariant(),
          b -> b.option);

  /**
   * The meta-bean for {@code SplitEtdId}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<SplitEtdId> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SplitEtdId.Builder builder() {
    return new SplitEtdId.Builder();
  }

  private SplitEtdId(
      SecurityId securityId,
      EtdType type,
      ExchangeId exchangeId,
      EtdContractCode contractCode,
      YearMonth expiry,
      EtdVariant variant,
      SplitEtdOption option) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notNull(type, "type");
    JodaBeanUtils.notNull(exchangeId, "exchangeId");
    JodaBeanUtils.notNull(contractCode, "contractCode");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(variant, "variant");
    this.securityId = securityId;
    this.type = type;
    this.exchangeId = exchangeId;
    this.contractCode = contractCode;
    this.expiry = expiry;
    this.variant = variant;
    this.option = option;
  }

  @Override
  public TypedMetaBean<SplitEtdId> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the security ID that was split.
   * @return the value of the property, not null
   */
  public SecurityId getSecurityId() {
    return securityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of the contract - future or option.
   * @return the value of the property, not null
   */
  public EtdType getType() {
    return type;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the ID of the exchange where the instruments derived from the product are traded.
   * @return the value of the property, not null
   */
  public ExchangeId getExchangeId() {
    return exchangeId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the code supplied by the exchange for use in clearing and margining, such as in SPAN.
   * @return the value of the property, not null
   */
  public EtdContractCode getContractCode() {
    return contractCode;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year-month of the expiry.
   * <p>
   * Expiry will occur on a date implied by the variant of the ETD.
   * @return the value of the property, not null
   */
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
  public EtdVariant getVariant() {
    return variant;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional information if the ID is an option.
   * @return the optional value of the property, not null
   */
  public Optional<SplitEtdOption> getOption() {
    return Optional.ofNullable(option);
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
      SplitEtdId other = (SplitEtdId) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(type, other.type) &&
          JodaBeanUtils.equal(exchangeId, other.exchangeId) &&
          JodaBeanUtils.equal(contractCode, other.contractCode) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
          JodaBeanUtils.equal(variant, other.variant) &&
          JodaBeanUtils.equal(option, other.option);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(securityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(type);
    hash = hash * 31 + JodaBeanUtils.hashCode(exchangeId);
    hash = hash * 31 + JodaBeanUtils.hashCode(contractCode);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(variant);
    hash = hash * 31 + JodaBeanUtils.hashCode(option);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("SplitEtdId{");
    buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
    buf.append("type").append('=').append(JodaBeanUtils.toString(type)).append(',').append(' ');
    buf.append("exchangeId").append('=').append(JodaBeanUtils.toString(exchangeId)).append(',').append(' ');
    buf.append("contractCode").append('=').append(JodaBeanUtils.toString(contractCode)).append(',').append(' ');
    buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
    buf.append("variant").append('=').append(JodaBeanUtils.toString(variant)).append(',').append(' ');
    buf.append("option").append('=').append(JodaBeanUtils.toString(option));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code SplitEtdId}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<SplitEtdId> {

    private SecurityId securityId;
    private EtdType type;
    private ExchangeId exchangeId;
    private EtdContractCode contractCode;
    private YearMonth expiry;
    private EtdVariant variant;
    private SplitEtdOption option;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(SplitEtdId beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.type = beanToCopy.getType();
      this.exchangeId = beanToCopy.getExchangeId();
      this.contractCode = beanToCopy.getContractCode();
      this.expiry = beanToCopy.getExpiry();
      this.variant = beanToCopy.getVariant();
      this.option = beanToCopy.option;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case 3575610:  // type
          return type;
        case 913218206:  // exchangeId
          return exchangeId;
        case -1402840545:  // contractCode
          return contractCode;
        case -1289159373:  // expiry
          return expiry;
        case 236785797:  // variant
          return variant;
        case -1010136971:  // option
          return option;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          this.securityId = (SecurityId) newValue;
          break;
        case 3575610:  // type
          this.type = (EtdType) newValue;
          break;
        case 913218206:  // exchangeId
          this.exchangeId = (ExchangeId) newValue;
          break;
        case -1402840545:  // contractCode
          this.contractCode = (EtdContractCode) newValue;
          break;
        case -1289159373:  // expiry
          this.expiry = (YearMonth) newValue;
          break;
        case 236785797:  // variant
          this.variant = (EtdVariant) newValue;
          break;
        case -1010136971:  // option
          this.option = (SplitEtdOption) newValue;
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
    public SplitEtdId build() {
      preBuild(this);
      return new SplitEtdId(
          securityId,
          type,
          exchangeId,
          contractCode,
          expiry,
          variant,
          option);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the security ID that was split.
     * @param securityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder securityId(SecurityId securityId) {
      JodaBeanUtils.notNull(securityId, "securityId");
      this.securityId = securityId;
      return this;
    }

    /**
     * Sets the type of the contract - future or option.
     * @param type  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder type(EtdType type) {
      JodaBeanUtils.notNull(type, "type");
      this.type = type;
      return this;
    }

    /**
     * Sets the ID of the exchange where the instruments derived from the product are traded.
     * @param exchangeId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder exchangeId(ExchangeId exchangeId) {
      JodaBeanUtils.notNull(exchangeId, "exchangeId");
      this.exchangeId = exchangeId;
      return this;
    }

    /**
     * Sets the code supplied by the exchange for use in clearing and margining, such as in SPAN.
     * @param contractCode  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder contractCode(EtdContractCode contractCode) {
      JodaBeanUtils.notNull(contractCode, "contractCode");
      this.contractCode = contractCode;
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
     * Sets the additional information if the ID is an option.
     * @param option  the new value
     * @return this, for chaining, not null
     */
    public Builder option(SplitEtdOption option) {
      this.option = option;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("SplitEtdId.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("type").append('=').append(JodaBeanUtils.toString(type)).append(',').append(' ');
      buf.append("exchangeId").append('=').append(JodaBeanUtils.toString(exchangeId)).append(',').append(' ');
      buf.append("contractCode").append('=').append(JodaBeanUtils.toString(contractCode)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("variant").append('=').append(JodaBeanUtils.toString(variant)).append(',').append(' ');
      buf.append("option").append('=').append(JodaBeanUtils.toString(option));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

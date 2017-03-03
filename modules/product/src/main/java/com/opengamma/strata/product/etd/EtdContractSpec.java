/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.product.SecurityAttributeType;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.common.PutCall;

/**
 * The contract specification defining an Exchange Traded Derivative (ETD) product.
 * <p>
 * This can represent a future or an option. Instances of {@link EtdOptionSecurity} or {@link EtdFutureSecurity}
 * can be created using the {@code createFuture} and {@code createOption} methods and providing the information
 * required to fully define the contract such as the expiry, strike price and put / call.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class EtdContractSpec implements ImmutableBean {

  /**
   * The ID of this template.
   */
  @PropertyDefinition(validate = "notNull")
  private final EtdContractSpecId id;
  /**
   * The type of the contract - future or option.
   */
  @PropertyDefinition(validate = "notNull")
  private final EtdProductType productType;
  /**
   * The ID of the exchange where the instruments derived from the product are traded.
   */
  @PropertyDefinition(validate = "notNull")
  private final ExchangeId exchangeId;
  /**
   * The code supplied by the exchange for use in clearing and margining, such as in SPAN.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final String contractCode;
  /**
   * The human readable description of the product.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final String description;
  /**
   * The information about the security price.
   * This includes details of the currency, tick size, tick value, contract size.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityPriceInfo priceInfo;
  /**
   * The attributes.
   * <p>
   * Security attributes, provide the ability to associate arbitrary information
   * with a security template in a key-value map.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<SecurityAttributeType<?>, Object> attributes;

  //-------------------------------------------------------------------------
  /**
   * Returns a builder for building instances of {@code EtdContractSpec}.
   *
   * @return a builder for building instances of {@code EtdContractSpec}
   */
  public static EtdContractSpecBuilder builder() {
    return new EtdContractSpecBuilder();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the attribute associated with the specified type.
   * <p>
   * This method obtains the specified attribute.
   * This allows an attribute about a security to be obtained if available.
   * <p>
   * If the attribute is not found, an exception is thrown.
   *
   * @param <T>  the type of the result
   * @param type  the type to find
   * @return the attribute value
   * @throws IllegalArgumentException if the attribute is not found
   */
  public <T> T getAttribute(SecurityAttributeType<T> type) {
    return findAttribute(type).orElseThrow(() -> new IllegalArgumentException(
        Messages.format("Attribute not found for type '{}'", type)));
  }

  /**
   * Finds the attribute associated with the specified type.
   * <p>
   * This method obtains the specified attribute.
   * This allows an attribute about a security to be obtained if available.
   * <p>
   * If the attribute is not found, optional empty is returned.
   *
   * @param <T>  the type of the result
   * @param type  the type to find
   * @return the attribute value
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> findAttribute(SecurityAttributeType<T> type) {
    return Optional.ofNullable((T) attributes.get(type));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a future security based on this template.
   * <p>
   * The {@link #getProductType() product type} must be {@link EtdProductType#FUTURE}
   * otherwise an exception will be thrown.
   * <p>
   * The future is a standard monthly contract.
   *
   * @param securityId the ID of the future
   * @param expiry the expiry of the future
   * @return a future security based on this template
   * @throws IllegalStateException if the product type of the template is not {@code FUTURE}
   */
  public EtdFutureSecurity createFuture(SecurityId securityId, YearMonth expiry) {
    if (productType != EtdProductType.FUTURE) {
      throw new IllegalStateException("Cannot create a future from a template with product type " + productType);
    }
    return EtdFutureSecurity.builder()
        .info(SecurityInfo.of(securityId, priceInfo))
        .expiry(expiry)
        .contractSpecId(id)
        .build();
  }

  /**
   * Creates a future security based on this template.
   * <p>
   * The {@link #getProductType() product type} must be {@link EtdProductType#FUTURE}
   * otherwise an exception will be thrown.
   * <p>
   * The expiry day of the contract is specified by {@code expiryDateCode}.
   *
   * @param securityId the ID of the future
   * @param expiry the expiry of the future
   * @param expiryDateCode the code representing the actual day of expiry
   * @return a future security based on this template
   * @throws IllegalStateException if the product type of the template is not {@code FUTURE}
   */
  public EtdFutureSecurity createFuture(SecurityId securityId, YearMonth expiry, String expiryDateCode) {
    if (productType != EtdProductType.FUTURE) {
      throw new IllegalStateException("Cannot create a future from a template with product type " + productType);
    }
    return EtdFutureSecurity.builder()
        .info(SecurityInfo.of(securityId, priceInfo))
        .expiry(expiry)
        .expiryDateCode(expiryDateCode)
        .contractSpecId(id)
        .build();
  }

  /**
   * Creates an option security based on this template.
   * <p>
   * The {@link #getProductType() product type} must be {@link EtdProductType#OPTION}
   * otherwise an exception will be thrown.
   * <p>
   * The option is a standard monthly contract.
   *
   * @param securityId the ID of the option
   * @param putCall whether the option is a put or call
   * @param strikePrice the strike price of the option
   * @param expiry the expiry of the option
   * @return an option security based on this template
   * @throws IllegalStateException if the product type of the template is not {@code OPTION}
   */
  public EtdOptionSecurity createOption(SecurityId securityId, PutCall putCall, double strikePrice, YearMonth expiry) {
    if (productType != EtdProductType.OPTION) {
      throw new IllegalStateException("Cannot create an option from a template with product type " + productType);
    }
    return EtdOptionSecurity.builder()
        .info(SecurityInfo.of(securityId, priceInfo))
        .putCall(putCall)
        .strikePrice(strikePrice)
        .expiry(expiry)
        .contractSpecId(id)
        .build();
  }

  /**
   * Creates an option security based on this template.
   * <p>
   * The {@link #getProductType() product type} must be {@link EtdProductType#OPTION}
   * otherwise an exception will be thrown.
   * <p>
   * The expiry day of the contract is specified by {@code expiryDateCode}.
   *
   * @param securityId the ID of the option
   * @param putCall whether the option is a put or call
   * @param strikePrice the strike price of the option
   * @param expiry the expiry of the option
   * @param expiryDateCode the code representing the actual day of expiry
   * @return an option security based on this template
   * @throws IllegalStateException if the product type of the template is not {@code OPTION}
   */
  public EtdOptionSecurity createOption(
      SecurityId securityId,
      PutCall putCall,
      double strikePrice,
      YearMonth expiry,
      String expiryDateCode) {

    if (productType != EtdProductType.OPTION) {
      throw new IllegalStateException("Cannot create an option from a template with product type " + productType);
    }
    return EtdOptionSecurity.builder()
        .info(SecurityInfo.of(securityId, priceInfo))
        .putCall(putCall)
        .strikePrice(strikePrice)
        .expiry(expiry)
        .expiryDateCode(expiryDateCode)
        .contractSpecId(id)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code EtdContractSpec}.
   * @return the meta-bean, not null
   */
  public static EtdContractSpec.Meta meta() {
    return EtdContractSpec.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(EtdContractSpec.Meta.INSTANCE);
  }

  /**
   * Creates an instance.
   * @param id  the value of the property, not null
   * @param productType  the value of the property, not null
   * @param exchangeId  the value of the property, not null
   * @param contractCode  the value of the property, not empty
   * @param description  the value of the property, not empty
   * @param priceInfo  the value of the property, not null
   * @param attributes  the value of the property, not null
   */
  EtdContractSpec(
      EtdContractSpecId id,
      EtdProductType productType,
      ExchangeId exchangeId,
      String contractCode,
      String description,
      SecurityPriceInfo priceInfo,
      Map<SecurityAttributeType<?>, Object> attributes) {
    JodaBeanUtils.notNull(id, "id");
    JodaBeanUtils.notNull(productType, "productType");
    JodaBeanUtils.notNull(exchangeId, "exchangeId");
    JodaBeanUtils.notEmpty(contractCode, "contractCode");
    JodaBeanUtils.notEmpty(description, "description");
    JodaBeanUtils.notNull(priceInfo, "priceInfo");
    JodaBeanUtils.notNull(attributes, "attributes");
    this.id = id;
    this.productType = productType;
    this.exchangeId = exchangeId;
    this.contractCode = contractCode;
    this.description = description;
    this.priceInfo = priceInfo;
    this.attributes = ImmutableMap.copyOf(attributes);
  }

  @Override
  public EtdContractSpec.Meta metaBean() {
    return EtdContractSpec.Meta.INSTANCE;
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
   * Gets the ID of this template.
   * @return the value of the property, not null
   */
  public EtdContractSpecId getId() {
    return id;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of the contract - future or option.
   * @return the value of the property, not null
   */
  public EtdProductType getProductType() {
    return productType;
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
   * @return the value of the property, not empty
   */
  public String getContractCode() {
    return contractCode;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the human readable description of the product.
   * @return the value of the property, not empty
   */
  public String getDescription() {
    return description;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the information about the security price.
   * This includes details of the currency, tick size, tick value, contract size.
   * @return the value of the property, not null
   */
  public SecurityPriceInfo getPriceInfo() {
    return priceInfo;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the attributes.
   * <p>
   * Security attributes, provide the ability to associate arbitrary information
   * with a security template in a key-value map.
   * @return the value of the property, not null
   */
  public ImmutableMap<SecurityAttributeType<?>, Object> getAttributes() {
    return attributes;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      EtdContractSpec other = (EtdContractSpec) obj;
      return JodaBeanUtils.equal(id, other.id) &&
          JodaBeanUtils.equal(productType, other.productType) &&
          JodaBeanUtils.equal(exchangeId, other.exchangeId) &&
          JodaBeanUtils.equal(contractCode, other.contractCode) &&
          JodaBeanUtils.equal(description, other.description) &&
          JodaBeanUtils.equal(priceInfo, other.priceInfo) &&
          JodaBeanUtils.equal(attributes, other.attributes);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(id);
    hash = hash * 31 + JodaBeanUtils.hashCode(productType);
    hash = hash * 31 + JodaBeanUtils.hashCode(exchangeId);
    hash = hash * 31 + JodaBeanUtils.hashCode(contractCode);
    hash = hash * 31 + JodaBeanUtils.hashCode(description);
    hash = hash * 31 + JodaBeanUtils.hashCode(priceInfo);
    hash = hash * 31 + JodaBeanUtils.hashCode(attributes);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("EtdContractSpec{");
    buf.append("id").append('=').append(id).append(',').append(' ');
    buf.append("productType").append('=').append(productType).append(',').append(' ');
    buf.append("exchangeId").append('=').append(exchangeId).append(',').append(' ');
    buf.append("contractCode").append('=').append(contractCode).append(',').append(' ');
    buf.append("description").append('=').append(description).append(',').append(' ');
    buf.append("priceInfo").append('=').append(priceInfo).append(',').append(' ');
    buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code EtdContractSpec}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code id} property.
     */
    private final MetaProperty<EtdContractSpecId> id = DirectMetaProperty.ofImmutable(
        this, "id", EtdContractSpec.class, EtdContractSpecId.class);
    /**
     * The meta-property for the {@code productType} property.
     */
    private final MetaProperty<EtdProductType> productType = DirectMetaProperty.ofImmutable(
        this, "productType", EtdContractSpec.class, EtdProductType.class);
    /**
     * The meta-property for the {@code exchangeId} property.
     */
    private final MetaProperty<ExchangeId> exchangeId = DirectMetaProperty.ofImmutable(
        this, "exchangeId", EtdContractSpec.class, ExchangeId.class);
    /**
     * The meta-property for the {@code contractCode} property.
     */
    private final MetaProperty<String> contractCode = DirectMetaProperty.ofImmutable(
        this, "contractCode", EtdContractSpec.class, String.class);
    /**
     * The meta-property for the {@code description} property.
     */
    private final MetaProperty<String> description = DirectMetaProperty.ofImmutable(
        this, "description", EtdContractSpec.class, String.class);
    /**
     * The meta-property for the {@code priceInfo} property.
     */
    private final MetaProperty<SecurityPriceInfo> priceInfo = DirectMetaProperty.ofImmutable(
        this, "priceInfo", EtdContractSpec.class, SecurityPriceInfo.class);
    /**
     * The meta-property for the {@code attributes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<SecurityAttributeType<?>, Object>> attributes = DirectMetaProperty.ofImmutable(
        this, "attributes", EtdContractSpec.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "productType",
        "exchangeId",
        "contractCode",
        "description",
        "priceInfo",
        "attributes");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case -1491615543:  // productType
          return productType;
        case 913218206:  // exchangeId
          return exchangeId;
        case -1402840545:  // contractCode
          return contractCode;
        case -1724546052:  // description
          return description;
        case -2126070377:  // priceInfo
          return priceInfo;
        case 405645655:  // attributes
          return attributes;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends EtdContractSpec> builder() {
      return new EtdContractSpec.Builder();
    }

    @Override
    public Class<? extends EtdContractSpec> beanType() {
      return EtdContractSpec.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code id} property.
     * @return the meta-property, not null
     */
    public MetaProperty<EtdContractSpecId> id() {
      return id;
    }

    /**
     * The meta-property for the {@code productType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<EtdProductType> productType() {
      return productType;
    }

    /**
     * The meta-property for the {@code exchangeId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ExchangeId> exchangeId() {
      return exchangeId;
    }

    /**
     * The meta-property for the {@code contractCode} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> contractCode() {
      return contractCode;
    }

    /**
     * The meta-property for the {@code description} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> description() {
      return description;
    }

    /**
     * The meta-property for the {@code priceInfo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityPriceInfo> priceInfo() {
      return priceInfo;
    }

    /**
     * The meta-property for the {@code attributes} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<SecurityAttributeType<?>, Object>> attributes() {
      return attributes;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((EtdContractSpec) bean).getId();
        case -1491615543:  // productType
          return ((EtdContractSpec) bean).getProductType();
        case 913218206:  // exchangeId
          return ((EtdContractSpec) bean).getExchangeId();
        case -1402840545:  // contractCode
          return ((EtdContractSpec) bean).getContractCode();
        case -1724546052:  // description
          return ((EtdContractSpec) bean).getDescription();
        case -2126070377:  // priceInfo
          return ((EtdContractSpec) bean).getPriceInfo();
        case 405645655:  // attributes
          return ((EtdContractSpec) bean).getAttributes();
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
   * The bean-builder for {@code EtdContractSpec}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<EtdContractSpec> {

    private EtdContractSpecId id;
    private EtdProductType productType;
    private ExchangeId exchangeId;
    private String contractCode;
    private String description;
    private SecurityPriceInfo priceInfo;
    private Map<SecurityAttributeType<?>, Object> attributes = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case -1491615543:  // productType
          return productType;
        case 913218206:  // exchangeId
          return exchangeId;
        case -1402840545:  // contractCode
          return contractCode;
        case -1724546052:  // description
          return description;
        case -2126070377:  // priceInfo
          return priceInfo;
        case 405645655:  // attributes
          return attributes;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          this.id = (EtdContractSpecId) newValue;
          break;
        case -1491615543:  // productType
          this.productType = (EtdProductType) newValue;
          break;
        case 913218206:  // exchangeId
          this.exchangeId = (ExchangeId) newValue;
          break;
        case -1402840545:  // contractCode
          this.contractCode = (String) newValue;
          break;
        case -1724546052:  // description
          this.description = (String) newValue;
          break;
        case -2126070377:  // priceInfo
          this.priceInfo = (SecurityPriceInfo) newValue;
          break;
        case 405645655:  // attributes
          this.attributes = (Map<SecurityAttributeType<?>, Object>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public EtdContractSpec build() {
      return new EtdContractSpec(
          id,
          productType,
          exchangeId,
          contractCode,
          description,
          priceInfo,
          attributes);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("EtdContractSpec.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("productType").append('=').append(JodaBeanUtils.toString(productType)).append(',').append(' ');
      buf.append("exchangeId").append('=').append(JodaBeanUtils.toString(exchangeId)).append(',').append(' ');
      buf.append("contractCode").append('=').append(JodaBeanUtils.toString(contractCode)).append(',').append(' ');
      buf.append("description").append('=').append(JodaBeanUtils.toString(description)).append(',').append(' ');
      buf.append("priceInfo").append('=').append(JodaBeanUtils.toString(priceInfo)).append(',').append(' ');
      buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.util.HashMap;
import java.util.Map;

import org.joda.beans.JodaBeanUtils;

import com.opengamma.strata.product.SecurityAttributeType;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.common.ExchangeId;

/**
 * A builder for building instances of {@link EtdContractSpec}.
 */
public final class EtdContractSpecBuilder {

  /** The ID of the template. */
  private EtdContractSpecId id;

  /** The code of the product as given by the exchange in clearing and margining. */
  private EtdContractCode contractCode;

  /** The type of the product. */
  private EtdType type;

  /** The ID of the exchange where the instruments derived from the product are traded. */
  private ExchangeId exchangeId;

  /** The description of the product. */
  private String description;

  /** The information about the security price - currency, tick size, tick value, contract size. */
  private SecurityPriceInfo priceInfo;

  /**
   * The attributes.
   * <p>
   * Security attributes, provide the ability to associate arbitrary information
   * with a security template in a key-value map.
   */
  private final Map<SecurityAttributeType<?>, Object> attributes = new HashMap<>();

  /**
   * Creates an empty builder.
   */
  EtdContractSpecBuilder() {
  }

  /**
   * Builds a new specification from the data in this builder.
   *
   * @return a specification instance built from the data in this builder
   */
  public EtdContractSpec build() {
    if (id == null) {
      id = EtdIdUtils.contractSpecId(type, exchangeId, contractCode);
    }
    return new EtdContractSpec(id, type, exchangeId, contractCode, description, priceInfo, attributes);
  }

  /**
   * Sets the ID of the contract specification.
   *
   * @param id the ID
   * @return the ID of the template
   */
  public EtdContractSpecBuilder id(EtdContractSpecId id) {
    JodaBeanUtils.notNull(id, "id");
    this.id = id;
    return this;
  }

  /**
   * Sets the type of the contract specification.
   *
   * @param productType  the new value, not null
   * @return this, for chaining, not null
   */
  public EtdContractSpecBuilder type(EtdType productType) {
    JodaBeanUtils.notNull(productType, "productType");
    this.type = productType;
    return this;
  }

  /**
   * Sets the ID of the exchange where the instruments derived from the contract specification are traded.
   *
   * @param exchangeId  the new value, not null
   * @return this, for chaining, not null
   */
  public EtdContractSpecBuilder exchangeId(ExchangeId exchangeId) {
    JodaBeanUtils.notNull(exchangeId, "exchangeId");
    this.exchangeId = exchangeId;
    return this;
  }

  /**
   * Sets the code of the contract specification as given by the exchange in clearing and margining.
   *
   * @param contractCode  the new value, not empty
   * @return this, for chaining, not null
   */
  public EtdContractSpecBuilder contractCode(EtdContractCode contractCode) {
    JodaBeanUtils.notNull(contractCode, "contractCode");
    this.contractCode = contractCode;
    return this;
  }

  /**
   * Sets the description of the contract specification.
   *
   * @param description  the new value, not empty
   * @return this, for chaining, not null
   */
  public EtdContractSpecBuilder description(String description) {
    JodaBeanUtils.notEmpty(description, "description");
    this.description = description;
    return this;
  }

  /**
   * Sets the information about the security price - currency, tick size, tick value, contract size.
   *
   * @param priceInfo  the new value, not null
   * @return this, for chaining, not null
   */
  public EtdContractSpecBuilder priceInfo(SecurityPriceInfo priceInfo) {
    JodaBeanUtils.notNull(priceInfo, "priceInfo");
    this.priceInfo = priceInfo;
    return this;
  }

  /**
   * Adds an attribute to the builder.
   * <p>
   * Only one attribute is stored for each attribute type. If this method is called multiple times with the
   * same attribute type the previous attribute value will be replaced.
   *
   * @param attributeType the type of the attribute
   * @param attributeValue the value of the attribute
   * @param <T> the type of the attribute
   * @return this builder
   */
  public <T> EtdContractSpecBuilder addAttribute(SecurityAttributeType<T> attributeType, T attributeValue) {
    JodaBeanUtils.notNull(attributeType, "attributeType");
    JodaBeanUtils.notNull(attributeValue, "attributeValue");
    attributes.put(attributeType, attributeValue);
    return this;
  }

}

/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.PortfolioItemInfoBuilder;

import java.util.HashMap;
import java.util.Map;


public final class CorporateActionInfoBuilder implements PortfolioItemInfoBuilder<CorporateActionInfo> {

  private StandardId id;

  private StandardId corpRefProvider;

  private StandardId corpRefOfficial;

  private CorporateActionEventType eventType;

  private Map<AttributeType<?>, Object> attributes = new HashMap<>();

  // creates an empty instance
  CorporateActionInfoBuilder() {
  }

  // creates a populated instance
  CorporateActionInfoBuilder(
      StandardId id,
      StandardId corpRefProvider,
      StandardId corpRefOfficial,
      CorporateActionEventType eventType,
      Map<AttributeType<?>, Object> attributes) {

    this.id = id;
    this.corpRefProvider = corpRefProvider;
    this.corpRefOfficial = corpRefOfficial;
    this.eventType = eventType;
    this.attributes.putAll(attributes);
  }

  //-----------------------------------------------------------------------
  /**
   * Sets the primary identifier for the trade, optional.
   * <p>
   * The identifier is used to identify the trade.
   * 
   * @param id  the identifier
   * @return this, for chaining
   */
  @Override
  public CorporateActionInfoBuilder id(StandardId id) {
    this.id = id;
    return this;
  }

  public CorporateActionInfoBuilder corpRefProvider(StandardId corpRefProvider) {
    this.corpRefProvider = corpRefProvider;
    return this;
  }

  public CorporateActionInfoBuilder corpRefOfficial(StandardId corpRefOfficial) {
    this.corpRefOfficial = corpRefOfficial;
    return this;
  }

  public CorporateActionInfoBuilder eventType(CorporateActionEventType eventType) {
    this.eventType = eventType;
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> CorporateActionInfoBuilder addAttribute(AttributeType<T> attributeType, T attributeValue) {
    ArgChecker.notNull(attributeType, "attributeType");
    ArgChecker.notNull(attributeValue, "attributeValue");
    attributes.put(attributeType, attributeType.toStoredForm(attributeValue));
    return this;
  }


  @Override
  public CorporateActionInfo build() {
    return new CorporateActionInfo(
        id,
        corpRefProvider,
        corpRefOfficial,
        eventType,
        attributes);
  }

}

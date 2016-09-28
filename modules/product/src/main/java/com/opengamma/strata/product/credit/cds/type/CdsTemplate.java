/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.cds.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.cds.CdsTrade;

/**
 * A template for creating credit default swap trades.
 * <p>
 * This defines almost all the data necessary to create a credit default swap {@link CdsTrade}.
 * The legal entity ID, trade date, notional and fixed rate are required to complete the template and create the trade.
 * As such, it is often possible to get a market quote for a trade based on the template.
 * <p>
 * A CDS is quoted in points-upfront, par spread, or quoted spread. 
 * For the latter two cases, the market quotes are passed as the fixed rate.
 */
@BeanDefinition
public final class CdsTemplate
    implements TradeTemplate, ImmutableBean, Serializable {

  /**
  * The tenor of the credit default swap.
  * <p>
  * This is the period to the protection end.
  */
  @PropertyDefinition(validate = "notNull")
  private final Tenor tenor;
  /**
  * The market convention of the credit default swap.
  */
  @PropertyDefinition(validate = "notNull")
  private final CdsConvention convention;

  //-------------------------------------------------------------------------
  /**
   * Obtains a template based on the specified tenor and convention.
   * <p>
   * The protection end will be calculated based on standard semi-annual roll convention.
   * 
   * @param tenor  the tenor of the CDS
   * @param convention  the market convention
   * @return the template
   */
  public static CdsTemplate of(Tenor tenor, CdsConvention convention) {
    return of(tenor, convention);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified trade date.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the CDS, the protection is received from the counterparty on default, with the fixed coupon being paid.
   * If selling the CDS, the protection is paid to the counterparty on default, with the fixed coupon being received.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the date of the trade
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    return convention.toTrade(legalEntityId, tradeDate, tenor, buySell, notional, fixedRate, refData);
  }

  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified trade date.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the CDS, the protection is received from the counterparty on default, with the fixed coupon being paid.
   * If selling the CDS, the protection is paid to the counterparty on default, with the fixed coupon being received.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the date of the trade
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param upFrontFee  the reference data
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      AdjustablePayment upFrontFee,
      ReferenceData refData) {

    return convention.toTrade(legalEntityId, tradeDate, tenor, buySell, notional, fixedRate, upFrontFee, refData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CdsTemplate}.
   * @return the meta-bean, not null
   */
  public static CdsTemplate.Meta meta() {
    return CdsTemplate.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CdsTemplate.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CdsTemplate.Builder builder() {
    return new CdsTemplate.Builder();
  }

  private CdsTemplate(
      Tenor tenor,
      CdsConvention convention) {
    JodaBeanUtils.notNull(tenor, "tenor");
    JodaBeanUtils.notNull(convention, "convention");
    this.tenor = tenor;
    this.convention = convention;
  }

  @Override
  public CdsTemplate.Meta metaBean() {
    return CdsTemplate.Meta.INSTANCE;
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
   * Gets the tenor of the credit default swap.
   * <p>
   * This is the period to the protection end.
   * @return the value of the property, not null
   */
  public Tenor getTenor() {
    return tenor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the credit default swap.
   * @return the value of the property, not null
   */
  public CdsConvention getConvention() {
    return convention;
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
      CdsTemplate other = (CdsTemplate) obj;
      return JodaBeanUtils.equal(tenor, other.tenor) &&
          JodaBeanUtils.equal(convention, other.convention);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(tenor);
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("CdsTemplate{");
    buf.append("tenor").append('=').append(tenor).append(',').append(' ');
    buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CdsTemplate}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code tenor} property.
     */
    private final MetaProperty<Tenor> tenor = DirectMetaProperty.ofImmutable(
        this, "tenor", CdsTemplate.class, Tenor.class);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<CdsConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", CdsTemplate.class, CdsConvention.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "tenor",
        "convention");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 110246592:  // tenor
          return tenor;
        case 2039569265:  // convention
          return convention;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CdsTemplate.Builder builder() {
      return new CdsTemplate.Builder();
    }

    @Override
    public Class<? extends CdsTemplate> beanType() {
      return CdsTemplate.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code tenor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Tenor> tenor() {
      return tenor;
    }

    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CdsConvention> convention() {
      return convention;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 110246592:  // tenor
          return ((CdsTemplate) bean).getTenor();
        case 2039569265:  // convention
          return ((CdsTemplate) bean).getConvention();
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
   * The bean-builder for {@code CdsTemplate}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CdsTemplate> {

    private Tenor tenor;
    private CdsConvention convention;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CdsTemplate beanToCopy) {
      this.tenor = beanToCopy.getTenor();
      this.convention = beanToCopy.getConvention();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 110246592:  // tenor
          return tenor;
        case 2039569265:  // convention
          return convention;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 110246592:  // tenor
          this.tenor = (Tenor) newValue;
          break;
        case 2039569265:  // convention
          this.convention = (CdsConvention) newValue;
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
    public CdsTemplate build() {
      return new CdsTemplate(
          tenor,
          convention);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the tenor of the credit default swap.
     * <p>
     * This is the period to the protection end.
     * @param tenor  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder tenor(Tenor tenor) {
      JodaBeanUtils.notNull(tenor, "tenor");
      this.tenor = tenor;
      return this;
    }

    /**
     * Sets the market convention of the credit default swap.
     * @param convention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder convention(CdsConvention convention) {
      JodaBeanUtils.notNull(convention, "convention");
      this.convention = convention;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("CdsTemplate.Builder{");
      buf.append("tenor").append('=').append(JodaBeanUtils.toString(tenor)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * A market convention for three leg basis swap trades.
 * <p>
 * This defines the market convention for a single currency basis swap.
 * The convention is formed by combining three swap leg conventions in the same currency.
 * <p>
 * The market price is for the difference (spread) between the values of the two floating legs.
 * This convention has three legs, the "spread leg", the "spread floating leg" and the "flat floating leg". 
 * The "spread leg" represented by the fixed leg will be added to the "spread floating leg" 
 * which is typically the leg with the shorter underlying tenor.
 * Thus the "spread leg" and "spread floating leg" will have the same pay/receive direction.
 * <p>
 * The convention is defined by four key dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Spot date, the base for date calculations, typically 2 business days after the trade date
 * <li>Start date, the date on which the interest calculation starts, often the same as the spot date
 * <li>End date, the date on which the interest calculation ends, typically a number of years after the start date
 * </ul>
 */
@BeanDefinition
public final class ImmutableThreeLegBasisSwapConvention
    implements ThreeLegBasisSwapConvention, ImmutableBean, Serializable {

  /**
   * The convention name.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String name;
  /**
   * The market convention of the fixed leg for the spread.
   * <p>
   * This is to be applied to {@code floatingSpreadLeg}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FixedRateSwapLegConvention spreadLeg;
  /**
   * The market convention of the floating leg to which the spread leg is added.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborRateSwapLegConvention spreadFloatingLeg;
  /**
   * The market convention of the floating leg that does not have the spread applied.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborRateSwapLegConvention flatFloatingLeg;
  /**
   * The offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date to find the start date.
   * A typical value is "plus 2 business days".
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DaysAdjustment spotDateOffset;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified name and leg conventions.
   * <p>
   * The two leg conventions must be in the same currency.
   * The spot date offset is set to be the effective date offset of the index of the spread floating leg.
   * <p>
   * The spread is represented by {@code FixedRateSwapLegConvention} and to be applied to {@code floatingSpreadLeg}. 
   * 
   * @param name  the unique name of the convention
   * @param spreadLeg  the market convention for the spread leg added to one of the floating leg 
   * @param spreadFloatingLeg  the market convention for the spread floating leg
   * @param flatFloatingLeg  the market convention for the flat floating leg
   * @return the convention
   */
  public static ImmutableThreeLegBasisSwapConvention of(
      String name,
      FixedRateSwapLegConvention spreadLeg,
      IborRateSwapLegConvention spreadFloatingLeg,
      IborRateSwapLegConvention flatFloatingLeg) {

    return of(name, spreadLeg, spreadFloatingLeg, flatFloatingLeg, spreadFloatingLeg.getIndex().getEffectiveDateOffset());
  }

  /**
   * Obtains a convention based on the specified name and leg conventions.
   * <p>
   * The two leg conventions must be in the same currency.
   * <p>
   * The spread is represented by {@code FixedRateSwapLegConvention} and to be applied to {@code floatingSpreadLeg}. 
   * 
   * @param name  the unique name of the convention
   * @param spreadLeg  the market convention for the spread leg added to one of the floating leg 
   * @param spreadFloatingLeg  the market convention for the spread floating leg
   * @param flatFloatingLeg  the market convention for the flat floating leg
   * @param spotDateOffset  the offset of the spot value date from the trade date
   * @return the convention
   */
  public static ImmutableThreeLegBasisSwapConvention of(
      String name,
      FixedRateSwapLegConvention spreadLeg,
      IborRateSwapLegConvention spreadFloatingLeg,
      IborRateSwapLegConvention flatFloatingLeg,
      DaysAdjustment spotDateOffset) {

    return new ImmutableThreeLegBasisSwapConvention(name, spreadLeg, spreadFloatingLeg, flatFloatingLeg, spotDateOffset);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(spreadFloatingLeg.getCurrency().equals(spreadLeg.getCurrency()),
        "Conventions must have same currency");
    ArgChecker.isTrue(spreadFloatingLeg.getCurrency().equals(flatFloatingLeg.getCurrency()),
        "Conventions must have same currency");
  }

  //-------------------------------------------------------------------------
  @Override
  public SwapTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double spread) {

    Optional<LocalDate> tradeDate = tradeInfo.getTradeDate();
    if (tradeDate.isPresent()) {
      ArgChecker.inOrderOrEqual(tradeDate.get(), startDate, "tradeDate", "startDate");
    }
    SwapLeg leg1 = spreadLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isBuy()), notional, spread);
    SwapLeg leg2 = spreadFloatingLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isBuy()), notional);
    SwapLeg leg3 = flatFloatingLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isSell()), notional);
    return SwapTrade.builder()
        .info(tradeInfo)
        .product(Swap.of(leg1, leg2, leg3))
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableThreeLegBasisSwapConvention}.
   * @return the meta-bean, not null
   */
  public static ImmutableThreeLegBasisSwapConvention.Meta meta() {
    return ImmutableThreeLegBasisSwapConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableThreeLegBasisSwapConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableThreeLegBasisSwapConvention.Builder builder() {
    return new ImmutableThreeLegBasisSwapConvention.Builder();
  }

  private ImmutableThreeLegBasisSwapConvention(
      String name,
      FixedRateSwapLegConvention spreadLeg,
      IborRateSwapLegConvention spreadFloatingLeg,
      IborRateSwapLegConvention flatFloatingLeg,
      DaysAdjustment spotDateOffset) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(spreadLeg, "spreadLeg");
    JodaBeanUtils.notNull(spreadFloatingLeg, "spreadFloatingLeg");
    JodaBeanUtils.notNull(flatFloatingLeg, "flatFloatingLeg");
    JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
    this.name = name;
    this.spreadLeg = spreadLeg;
    this.spreadFloatingLeg = spreadFloatingLeg;
    this.flatFloatingLeg = flatFloatingLeg;
    this.spotDateOffset = spotDateOffset;
    validate();
  }

  @Override
  public ImmutableThreeLegBasisSwapConvention.Meta metaBean() {
    return ImmutableThreeLegBasisSwapConvention.Meta.INSTANCE;
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
   * Gets the convention name.
   * @return the value of the property, not null
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the fixed leg for the spread.
   * <p>
   * This is to be applied to {@code floatingSpreadLeg}.
   * @return the value of the property, not null
   */
  @Override
  public FixedRateSwapLegConvention getSpreadLeg() {
    return spreadLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the floating leg to which the spread leg is added.
   * @return the value of the property, not null
   */
  @Override
  public IborRateSwapLegConvention getSpreadFloatingLeg() {
    return spreadFloatingLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the floating leg that does not have the spread applied.
   * @return the value of the property, not null
   */
  @Override
  public IborRateSwapLegConvention getFlatFloatingLeg() {
    return flatFloatingLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date to find the start date.
   * A typical value is "plus 2 business days".
   * @return the value of the property, not null
   */
  @Override
  public DaysAdjustment getSpotDateOffset() {
    return spotDateOffset;
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
      ImmutableThreeLegBasisSwapConvention other = (ImmutableThreeLegBasisSwapConvention) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(spreadLeg, other.spreadLeg) &&
          JodaBeanUtils.equal(spreadFloatingLeg, other.spreadFloatingLeg) &&
          JodaBeanUtils.equal(flatFloatingLeg, other.flatFloatingLeg) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(spreadLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(spreadFloatingLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(flatFloatingLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableThreeLegBasisSwapConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", ImmutableThreeLegBasisSwapConvention.class, String.class);
    /**
     * The meta-property for the {@code spreadLeg} property.
     */
    private final MetaProperty<FixedRateSwapLegConvention> spreadLeg = DirectMetaProperty.ofImmutable(
        this, "spreadLeg", ImmutableThreeLegBasisSwapConvention.class, FixedRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code spreadFloatingLeg} property.
     */
    private final MetaProperty<IborRateSwapLegConvention> spreadFloatingLeg = DirectMetaProperty.ofImmutable(
        this, "spreadFloatingLeg", ImmutableThreeLegBasisSwapConvention.class, IborRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code flatFloatingLeg} property.
     */
    private final MetaProperty<IborRateSwapLegConvention> flatFloatingLeg = DirectMetaProperty.ofImmutable(
        this, "flatFloatingLeg", ImmutableThreeLegBasisSwapConvention.class, IborRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", ImmutableThreeLegBasisSwapConvention.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "spreadLeg",
        "spreadFloatingLeg",
        "flatFloatingLeg",
        "spotDateOffset");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 1302781851:  // spreadLeg
          return spreadLeg;
        case -1969210187:  // spreadFloatingLeg
          return spreadFloatingLeg;
        case 274878191:  // flatFloatingLeg
          return flatFloatingLeg;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableThreeLegBasisSwapConvention.Builder builder() {
      return new ImmutableThreeLegBasisSwapConvention.Builder();
    }

    @Override
    public Class<? extends ImmutableThreeLegBasisSwapConvention> beanType() {
      return ImmutableThreeLegBasisSwapConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code spreadLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixedRateSwapLegConvention> spreadLeg() {
      return spreadLeg;
    }

    /**
     * The meta-property for the {@code spreadFloatingLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateSwapLegConvention> spreadFloatingLeg() {
      return spreadFloatingLeg;
    }

    /**
     * The meta-property for the {@code flatFloatingLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateSwapLegConvention> flatFloatingLeg() {
      return flatFloatingLeg;
    }

    /**
     * The meta-property for the {@code spotDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> spotDateOffset() {
      return spotDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((ImmutableThreeLegBasisSwapConvention) bean).getName();
        case 1302781851:  // spreadLeg
          return ((ImmutableThreeLegBasisSwapConvention) bean).getSpreadLeg();
        case -1969210187:  // spreadFloatingLeg
          return ((ImmutableThreeLegBasisSwapConvention) bean).getSpreadFloatingLeg();
        case 274878191:  // flatFloatingLeg
          return ((ImmutableThreeLegBasisSwapConvention) bean).getFlatFloatingLeg();
        case 746995843:  // spotDateOffset
          return ((ImmutableThreeLegBasisSwapConvention) bean).getSpotDateOffset();
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
   * The bean-builder for {@code ImmutableThreeLegBasisSwapConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableThreeLegBasisSwapConvention> {

    private String name;
    private FixedRateSwapLegConvention spreadLeg;
    private IborRateSwapLegConvention spreadFloatingLeg;
    private IborRateSwapLegConvention flatFloatingLeg;
    private DaysAdjustment spotDateOffset;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableThreeLegBasisSwapConvention beanToCopy) {
      this.name = beanToCopy.getName();
      this.spreadLeg = beanToCopy.getSpreadLeg();
      this.spreadFloatingLeg = beanToCopy.getSpreadFloatingLeg();
      this.flatFloatingLeg = beanToCopy.getFlatFloatingLeg();
      this.spotDateOffset = beanToCopy.getSpotDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 1302781851:  // spreadLeg
          return spreadLeg;
        case -1969210187:  // spreadFloatingLeg
          return spreadFloatingLeg;
        case 274878191:  // flatFloatingLeg
          return flatFloatingLeg;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case 1302781851:  // spreadLeg
          this.spreadLeg = (FixedRateSwapLegConvention) newValue;
          break;
        case -1969210187:  // spreadFloatingLeg
          this.spreadFloatingLeg = (IborRateSwapLegConvention) newValue;
          break;
        case 274878191:  // flatFloatingLeg
          this.flatFloatingLeg = (IborRateSwapLegConvention) newValue;
          break;
        case 746995843:  // spotDateOffset
          this.spotDateOffset = (DaysAdjustment) newValue;
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
    public ImmutableThreeLegBasisSwapConvention build() {
      return new ImmutableThreeLegBasisSwapConvention(
          name,
          spreadLeg,
          spreadFloatingLeg,
          flatFloatingLeg,
          spotDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the convention name.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the market convention of the fixed leg for the spread.
     * <p>
     * This is to be applied to {@code floatingSpreadLeg}.
     * @param spreadLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spreadLeg(FixedRateSwapLegConvention spreadLeg) {
      JodaBeanUtils.notNull(spreadLeg, "spreadLeg");
      this.spreadLeg = spreadLeg;
      return this;
    }

    /**
     * Sets the market convention of the floating leg to which the spread leg is added.
     * @param spreadFloatingLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spreadFloatingLeg(IborRateSwapLegConvention spreadFloatingLeg) {
      JodaBeanUtils.notNull(spreadFloatingLeg, "spreadFloatingLeg");
      this.spreadFloatingLeg = spreadFloatingLeg;
      return this;
    }

    /**
     * Sets the market convention of the floating leg that does not have the spread applied.
     * @param flatFloatingLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder flatFloatingLeg(IborRateSwapLegConvention flatFloatingLeg) {
      JodaBeanUtils.notNull(flatFloatingLeg, "flatFloatingLeg");
      this.flatFloatingLeg = flatFloatingLeg;
      return this;
    }

    /**
     * Sets the offset of the spot value date from the trade date.
     * <p>
     * The offset is applied to the trade date to find the start date.
     * A typical value is "plus 2 business days".
     * @param spotDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ImmutableThreeLegBasisSwapConvention.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("spreadLeg").append('=').append(JodaBeanUtils.toString(spreadLeg)).append(',').append(' ');
      buf.append("spreadFloatingLeg").append('=').append(JodaBeanUtils.toString(spreadFloatingLeg)).append(',').append(' ');
      buf.append("flatFloatingLeg").append('=').append(JodaBeanUtils.toString(flatFloatingLeg)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

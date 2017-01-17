/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;

/**
 * The second order parameter sensitivity for parameterized market data.
 * <p>
 * Parameter sensitivity is the sensitivity of a value to the parameters of a
 * {@linkplain ParameterizedData parameterized market data} object that is used to determine the value.
 * The main application of this class is the parameter sensitivities for curves.
 * Thus {@code ParameterizedData} is typically {@link Curve}.
 * <p>
 * The sensitivity is expressed as a matrix.
 * The {@code (i,j)} component is the sensitivity of the {@code i}-th component of the {@code parameterMetadata} delta to
 * the {@code j}-th parameter in {@code order}.
 * <p>
 * The sensitivity represents a monetary value in the specified currency.
 */
@BeanDefinition(builderScope = "private")
public final class CrossGammaParameterSensitivity
    implements FxConvertible<CrossGammaParameterSensitivity>, ImmutableBean, Serializable {

  /**
   * The market data name.
   * <p>
   * This name is used in the market data system to identify the data that the sensitivities refer to.
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataName<?> marketDataName;
  /**
   * The list of parameter metadata.
   * <p>
   * There is one entry for each parameter.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends ParameterMetadata>")
  private final ImmutableList<ParameterMetadata> parameterMetadata;
  /**
   * The sensitivity order.
   * <p>
   * This defines the order of sensitivity values, which can be used as a key to interpret {@code sensitivity}.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>>")
  private final ImmutableList<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>> order;

  /**
  * The currency of the sensitivity.
  */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The parameter sensitivity values.
   * <p>
   * The curve delta sensitivities to parameterized market data.
   * This is a {@code n x m} matrix, where {@code n} must agree with the size of {@code parameterMetadata} and 
   * {@code m} must be the sum of parameter count in {@code order}.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleMatrix sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the market data name, metadata, currency and sensitivity.
   * <p>
   * This creates a sensitivity instance which stores the second order sensitivity values to a single market data, i.e., 
   * the block diagonal part of the full second order sensitivity matrix.
   * <p>
   * The market data name identifies the {@link ParameterizedData} instance that was queried.
   * The parameter metadata provides information on each parameter.
   * The size of the parameter metadata list must match the size of the sensitivity array.
   * 
   * @param marketDataName  the name of the market data that the sensitivity refers to
   * @param parameterMetadata  the parameter metadata
   * @param currency  the currency of the sensitivity
   * @param sensitivity  the sensitivity values, one for each parameter
   * @return the sensitivity object
   */
  public static CrossGammaParameterSensitivity of(
      MarketDataName<?> marketDataName,
      List<? extends ParameterMetadata> parameterMetadata,
      Currency currency,
      DoubleMatrix sensitivity) {

    return of(marketDataName, parameterMetadata, marketDataName, parameterMetadata, currency, sensitivity);
  }

  /**
   * Obtains an instance from the market data names, metadatas, currency and sensitivity.
   * <p>
   * This creates a sensitivity instance which stores the second order sensitivity values: the delta of a market data 
   * to another market data. The first market data and the second market data can be the same.
   * <p>
   * The market data name identifies the {@link ParameterizedData} instance that was queried.
   * The parameter metadata provides information on each parameter.
   * 
   * @param marketDataName  the name of the first market data that the sensitivity refers to
   * @param parameterMetadata  the first parameter metadata
   * @param marketDataNameOther  the name of the second market data that the sensitivity refers to
   * @param parameterMetadataOther  the second parameter metadata
   * @param currency  the currency of the sensitivity
   * @param sensitivity  the sensitivity values, one for each parameter
   * @return the sensitivity object
   */
  public static CrossGammaParameterSensitivity of(
      MarketDataName<?> marketDataName,
      List<? extends ParameterMetadata> parameterMetadata,
      MarketDataName<?> marketDataNameOther,
      List<? extends ParameterMetadata> parameterMetadataOther,
      Currency currency,
      DoubleMatrix sensitivity) {

    return new CrossGammaParameterSensitivity(marketDataName, parameterMetadata,
        ImmutableList.of(Pair.of(marketDataNameOther, parameterMetadataOther)), currency, sensitivity);
  }

  /**
   * Obtains an instance from the market data names, metadatas, currency and sensitivity.
   * <p>
   * This creates a sensitivity instance which stores the second order sensitivity values: the delta of a market data 
   * to a set of other market data. 
   * The market data set is represented in terms of {@code List<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>>}. 
   * which defines the order of the sensitivity values.
   * <p>
   * The market data name identifies the {@link ParameterizedData} instance that was queried.
   * The parameter metadata provides information on each parameter.
   * 
   * @param marketDataName  the name of the market data that the sensitivity refers to
   * @param parameterMetadata  the parameter metadata
   * @param order  the order
   * @param currency  the currency of the sensitivity
   * @param sensitivity  the sensitivity values, one for each parameter
   * @return the sensitivity object
   */
  public static CrossGammaParameterSensitivity of(
      MarketDataName<?> marketDataName,
      List<? extends ParameterMetadata> parameterMetadata,
      List<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>> order,
      Currency currency,
      DoubleMatrix sensitivity) {

    return new CrossGammaParameterSensitivity(marketDataName, parameterMetadata,
        order, currency, sensitivity);
  }

  @ImmutableValidator
  private void validate() {
    int col = sensitivity.columnCount();
    int row = sensitivity.rowCount();
    if (row != parameterMetadata.size()) {
      throw new IllegalArgumentException("row count of sensitivity and parameter metadata size must match");
    }
    int nParamsTotal = 0;
    for (Pair<MarketDataName<?>, List<? extends ParameterMetadata>> entry : order) {
      nParamsTotal += entry.getSecond().size();
    }
    if (col != nParamsTotal) {
      throw new IllegalArgumentException("column count of sensitivity and total parameter metadata size of order must match");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of parameters.
   * <p>
   * This returns the number of parameters in the {@link ParameterizedData} instance
   * which is the same size as the sensitivity array.
   * 
   * @return the number of parameters
   */
  public int getParameterCount() {
    return sensitivity.rowCount();
  }

  /**
   * Gets the parameter metadata at the specified index.
   * <p>
   * If there is no specific parameter metadata, an empty instance will be returned.
   * 
   * @param parameterIndex  the zero-based index of the parameter to get
   * @return the metadata of the parameter
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return parameterMetadata.get(parameterIndex);
  }

  /**
   * Compares the key of two sensitivity objects, excluding the parameter sensitivity values.
   * 
   * @param other  the other sensitivity object
   * @return positive if greater, zero if equal, negative if less
   */
  public int compareKey(CrossGammaParameterSensitivity other) {
    return ComparisonChain.start()
        .compare(marketDataName, other.marketDataName)
        .compare(order.toString(), other.order.toString())
        .compare(currency, other.currency)
        .result();
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this sensitivity to an equivalent in the specified currency.
   * <p>
   * Any FX conversion that is required will use rates from the provider.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the sensitivity object expressed in terms of the result currency
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public CrossGammaParameterSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    if (currency.equals(resultCurrency)) {
      return this;
    }
    double fxRate = rateProvider.fxRate(currency, resultCurrency);
    return mapSensitivity(s -> s * fxRate, resultCurrency);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the sensitivity values multiplied by the specified factor.
   * <p>
   * Each value in the sensitivity array will be multiplied by the factor.
   * 
   * @param factor  the multiplicative factor
   * @return an instance based on this one, with each sensitivity multiplied by the factor
   */
  public CrossGammaParameterSensitivity multipliedBy(double factor) {
    return mapSensitivity(s -> s * factor);
  }

  /**
   * Returns an instance with the specified operation applied to the sensitivity values.
   * <p>
   * Each value in the sensitivity array will be operated on.
   * For example, the operator could multiply the sensitivities by a constant, or take the inverse.
   * <pre>
   *   inverse = base.mapSensitivity(value -> 1 / value);
   * </pre>
   *
   * @param operator  the operator to be applied to the sensitivities
   * @return an instance based on this one, with the operator applied to the sensitivity values
   */
  public CrossGammaParameterSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return mapSensitivity(operator, currency);
  }

  // maps the sensitivities and potentially changes the currency
  private CrossGammaParameterSensitivity mapSensitivity(DoubleUnaryOperator operator, Currency currency) {
    return new CrossGammaParameterSensitivity(marketDataName, parameterMetadata, order, currency, sensitivity.map(operator));
  }

  /**
   * Returns an instance with new parameter sensitivity values.
   * 
   * @param sensitivity  the new sensitivity values
   * @return an instance based on this one, with the specified sensitivity values
   */
  public CrossGammaParameterSensitivity withSensitivity(DoubleMatrix sensitivity) {
    return new CrossGammaParameterSensitivity(marketDataName, parameterMetadata, order, currency, sensitivity);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the total of the sensitivity values.
   * 
   * @return the total sensitivity values
   */
  public CurrencyAmount total() {
    return CurrencyAmount.of(currency, sensitivity.total());
  }

  /**
   * Returns the diagonal part of the sensitivity as {@code CurrencyParameterSensitivity}.
   * 
   * @return the diagonal part
   */
  public CurrencyParameterSensitivity diagonal() {
    CrossGammaParameterSensitivity blockDiagonal = getSensitivity(getMarketDataName());
    int size = getParameterCount();
    return CurrencyParameterSensitivity.of(
        getMarketDataName(),
        getParameterMetadata(),
        getCurrency(),
        DoubleArray.of(size, i -> blockDiagonal.getSensitivity().get(i, i)));
  }

  /**
   * Returns the sensitivity to the market data specified by {@code name}.
   * <p>
   * This returns a sensitivity instance which stores the sensitivity of the {@code marketDataName} delta to another 
   * market data of {@code name}.
   * 
   * @param name  the name
   * @return the sensitivity
   * @throws IllegalArgumentException if the name does not match an entry
   */
  public CrossGammaParameterSensitivity getSensitivity(MarketDataName<?> name) {
    Pair<Integer, List<? extends ParameterMetadata>> indexAndMetadata = findStartIndexAndMetadata(name);
    int startIndex = indexAndMetadata.getFirst();
    int rowCt = getParameterCount();
    int colCt = indexAndMetadata.getSecond().size();
    double[][] sensi = new double[rowCt][colCt];
    for (int i = 0; i < rowCt; ++i) {
      System.arraycopy(getSensitivity().rowArray(i), startIndex, sensi[i], 0, colCt);
    }
    return CrossGammaParameterSensitivity.of(
        getMarketDataName(), getParameterMetadata(), name, indexAndMetadata.getSecond(), getCurrency(),
        DoubleMatrix.ofUnsafe(sensi));
  }

  private Pair<Integer, List<? extends ParameterMetadata>> findStartIndexAndMetadata(MarketDataName<?> name) {
    int startIndex = 0;
    for (Pair<MarketDataName<?>, List<? extends ParameterMetadata>> entry : order) {
      if (entry.getFirst().equals(name)) {
        return Pair.of(startIndex, entry.getSecond());
      }
      startIndex += entry.getSecond().size();
    }
    throw new IllegalArgumentException(Messages.format("Unable to find sensitivity: {} and {}", marketDataName, name));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CrossGammaParameterSensitivity}.
   * @return the meta-bean, not null
   */
  public static CrossGammaParameterSensitivity.Meta meta() {
    return CrossGammaParameterSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CrossGammaParameterSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CrossGammaParameterSensitivity(
      MarketDataName<?> marketDataName,
      List<? extends ParameterMetadata> parameterMetadata,
      List<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>> order,
      Currency currency,
      DoubleMatrix sensitivity) {
    JodaBeanUtils.notNull(marketDataName, "marketDataName");
    JodaBeanUtils.notNull(parameterMetadata, "parameterMetadata");
    JodaBeanUtils.notNull(order, "order");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(sensitivity, "sensitivity");
    this.marketDataName = marketDataName;
    this.parameterMetadata = ImmutableList.copyOf(parameterMetadata);
    this.order = ImmutableList.copyOf(order);
    this.currency = currency;
    this.sensitivity = sensitivity;
    validate();
  }

  @Override
  public CrossGammaParameterSensitivity.Meta metaBean() {
    return CrossGammaParameterSensitivity.Meta.INSTANCE;
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
   * Gets the market data name.
   * <p>
   * This name is used in the market data system to identify the data that the sensitivities refer to.
   * @return the value of the property, not null
   */
  public MarketDataName<?> getMarketDataName() {
    return marketDataName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the list of parameter metadata.
   * <p>
   * There is one entry for each parameter.
   * @return the value of the property, not null
   */
  public ImmutableList<ParameterMetadata> getParameterMetadata() {
    return parameterMetadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the sensitivity order.
   * <p>
   * This defines the order of sensitivity values, which can be used as a key to interpret {@code sensitivity}.
   * @return the value of the property, not null
   */
  public ImmutableList<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>> getOrder() {
    return order;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the sensitivity.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the parameter sensitivity values.
   * <p>
   * The curve delta sensitivities to parameterized market data.
   * This is a {@code n x m} matrix, where {@code n} must agree with the size of {@code parameterMetadata} and
   * {@code m} must be the sum of parameter count in {@code order}.
   * @return the value of the property, not null
   */
  public DoubleMatrix getSensitivity() {
    return sensitivity;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CrossGammaParameterSensitivity other = (CrossGammaParameterSensitivity) obj;
      return JodaBeanUtils.equal(marketDataName, other.marketDataName) &&
          JodaBeanUtils.equal(parameterMetadata, other.parameterMetadata) &&
          JodaBeanUtils.equal(order, other.order) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(marketDataName);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterMetadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(order);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("CrossGammaParameterSensitivity{");
    buf.append("marketDataName").append('=').append(marketDataName).append(',').append(' ');
    buf.append("parameterMetadata").append('=').append(parameterMetadata).append(',').append(' ');
    buf.append("order").append('=').append(order).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CrossGammaParameterSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code marketDataName} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<MarketDataName<?>> marketDataName = DirectMetaProperty.ofImmutable(
        this, "marketDataName", CrossGammaParameterSensitivity.class, (Class) MarketDataName.class);
    /**
     * The meta-property for the {@code parameterMetadata} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ParameterMetadata>> parameterMetadata = DirectMetaProperty.ofImmutable(
        this, "parameterMetadata", CrossGammaParameterSensitivity.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code order} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>>> order = DirectMetaProperty.ofImmutable(
        this, "order", CrossGammaParameterSensitivity.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", CrossGammaParameterSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<DoubleMatrix> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", CrossGammaParameterSensitivity.class, DoubleMatrix.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "marketDataName",
        "parameterMetadata",
        "order",
        "currency",
        "sensitivity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 842855857:  // marketDataName
          return marketDataName;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
        case 106006350:  // order
          return order;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CrossGammaParameterSensitivity> builder() {
      return new CrossGammaParameterSensitivity.Builder();
    }

    @Override
    public Class<? extends CrossGammaParameterSensitivity> beanType() {
      return CrossGammaParameterSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code marketDataName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MarketDataName<?>> marketDataName() {
      return marketDataName;
    }

    /**
     * The meta-property for the {@code parameterMetadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ParameterMetadata>> parameterMetadata() {
      return parameterMetadata;
    }

    /**
     * The meta-property for the {@code order} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>>> order() {
      return order;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code sensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleMatrix> sensitivity() {
      return sensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 842855857:  // marketDataName
          return ((CrossGammaParameterSensitivity) bean).getMarketDataName();
        case -1169106440:  // parameterMetadata
          return ((CrossGammaParameterSensitivity) bean).getParameterMetadata();
        case 106006350:  // order
          return ((CrossGammaParameterSensitivity) bean).getOrder();
        case 575402001:  // currency
          return ((CrossGammaParameterSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((CrossGammaParameterSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code CrossGammaParameterSensitivity}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<CrossGammaParameterSensitivity> {

    private MarketDataName<?> marketDataName;
    private List<? extends ParameterMetadata> parameterMetadata = ImmutableList.of();
    private List<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>> order = ImmutableList.of();
    private Currency currency;
    private DoubleMatrix sensitivity;

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
        case 842855857:  // marketDataName
          return marketDataName;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
        case 106006350:  // order
          return order;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 842855857:  // marketDataName
          this.marketDataName = (MarketDataName<?>) newValue;
          break;
        case -1169106440:  // parameterMetadata
          this.parameterMetadata = (List<? extends ParameterMetadata>) newValue;
          break;
        case 106006350:  // order
          this.order = (List<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>>) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 564403871:  // sensitivity
          this.sensitivity = (DoubleMatrix) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public CrossGammaParameterSensitivity build() {
      return new CrossGammaParameterSensitivity(
          marketDataName,
          parameterMetadata,
          order,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("CrossGammaParameterSensitivity.Builder{");
      buf.append("marketDataName").append('=').append(JodaBeanUtils.toString(marketDataName)).append(',').append(' ');
      buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata)).append(',').append(' ');
      buf.append("order").append('=').append(JodaBeanUtils.toString(order)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

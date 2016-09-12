/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * A matrix of foreign exchange rates.
 * <p>
 * This provides a matrix of foreign exchange rates, such that the rate can be queried for any available pair.
 * For example, if the matrix contains the currencies 'USD', 'EUR' and 'GBP', then six rates can be queried,
 * 'EUR/USD', 'GBP/USD', 'EUR/GBP' and the three inverse rates.
 * <p>
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class FxMatrix
    implements FxRateProvider, ImmutableBean {

  /**
   * An empty FX matrix containing neither currencies nor rates.
   */
  private static final FxMatrix EMPTY = builder().build();

  /**
   * The map between the currencies and their position within the
   * {@code rates} array. Generally the position reflects the order
   * in which the currencies were added, so the first currency added
   * will be assigned 0, the second 1 etc.
   * <p>
   * An ImmutableMap is used so that the currencies are correctly
   * ordered when the {@link #toString()} method is called.
   */
  @PropertyDefinition(validate = "notNull", get = "")
  private final ImmutableMap<Currency, Integer> currencies;
  /**
   * The matrix with all the exchange rates. Each row represents the
   * rates required to convert a unit of particular currency to all
   * other currencies in the matrix.
   * <p>
   * If currencies c1 and c2 are assigned indexes i and j respectively
   * in the {@code currencies} map, then the entry [i][j] is such that
   * 1 unit of currency c1 is worth {@code rates[i][j]} units of
   * currency c2.
   * <p>
   * If {@code currencies.get(EUR)} = 0 and {@code currencies.get(USD)} = 1,
   * then the element {@code rates[0][1]} is likely to be around
   * 1.40 and {@code rates[1][0]} around 0.7142. The rate {@code rates[1][0]}
   * will be computed from fxRate[0][1] when the object is constructed
   * by the builder. All the element of the matrix are meaningful and coherent.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleMatrix rates;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty FX matrix.
   * <p>
   * The result contains no currencies or rates.
   *
   * @return an empty matrix
   */
  public static FxMatrix empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance containing a single FX rate.
   * <p>
   * This is most likely to be used in testing.
   * <p>
   * An invocation of the method with {@code FxMatrix.of(CurrencyPair.of(GBP, USD), 1.6)}
   * indicates that 1 pound sterling is worth 1.6 US dollars.
   * The matrix can also be queried for the reverse rate, from USD to GBP.
   *
   * @param currencyPair  the currency pair to be added
   * @param rate  the FX rate between the base currency of the pair and the
   *   counter currency. The rate indicates the value of one unit of the base
   *   currency in terms of the counter currency.
   * @return a matrix containing the single FX rate
   */
  public static FxMatrix of(CurrencyPair currencyPair, double rate) {
    return FxMatrix.of(currencyPair.getBase(), currencyPair.getCounter(), rate);
  }

  /**
   * Obtains an instance containing a single FX rate.
   * <p>
   * This is most likely to be used in testing.
   * <p>
   * An invocation of the method with {@code FxMatrix.of(GBP, USD, 1.6)}
   * indicates that 1 pound sterling is worth 1.6 US dollars.
   * The matrix can also be queried for the reverse rate, from USD to GBP.
   *
   * @param ccy1  the first currency of the pair
   * @param ccy2  the second currency of the pair
   * @param rate  the FX rate between the first currency and the second currency.
   *   The rate indicates the value of one unit of the first currency in terms
   *   of the second currency.
   * @return a matrix containing the single FX rate
   */
  public static FxMatrix of(Currency ccy1, Currency ccy2, double rate) {
    return new FxMatrixBuilder().addRate(ccy1, ccy2, rate).build();
  }

  /**
   * Creates a builder that can be used to build instances of {@code FxMatrix}.
   *
   * @return a new builder
   */
  public static FxMatrixBuilder builder() {
    return new FxMatrixBuilder();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a {@code Collector} that allows a {@code Map.Entry} of currency pair to rate
   * to be streamed and collected into a new {@code FxMatrix}.
   *
   * @return a collector for creating an {@code FxMatrix} from a stream
   */
  public static Collector<? super Map.Entry<CurrencyPair, Double>, FxMatrixBuilder, FxMatrix> entriesToFxMatrix() {
    return collector((builder, entry) -> builder.addRate(entry.getKey(), entry.getValue()));
  }

  /**
   * Creates a {@code Collector} that allows a collection of pairs each containing
   * a currency pair and a rate to be streamed and collected into a new {@code FxMatrix}.
   *
   * @return a collector for creating an {@code FxMatrix} from a stream
   */
  public static Collector<? super Pair<CurrencyPair, Double>, FxMatrixBuilder, FxMatrix> pairsToFxMatrix() {
    return collector((builder, pair) -> builder.addRate(pair.getFirst(), pair.getSecond()));
  }

  private static <T> Collector<T, FxMatrixBuilder, FxMatrix> collector(BiConsumer<FxMatrixBuilder, T> accumulator) {
    return Collector.of(
        FxMatrix::builder,
        accumulator,
        FxMatrixBuilder::merge,
        FxMatrixBuilder::build);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the set of currencies held within this matrix.
   *
   * @return the currencies in this matrix
   */
  public ImmutableSet<Currency> getCurrencies() {
    return currencies.keySet();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the FX rate for the specified currency pair.
   * <p>
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   * 
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @return the FX rate for the currency pair
   * @throws IllegalArgumentException if no FX rate could be found
   */
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    if (baseCurrency.equals(counterCurrency)) {
      return 1d;
    }
    Integer index1 = currencies.get(baseCurrency);
    Integer index2 = currencies.get(counterCurrency);
    if (index1 != null && index2 != null) {
      return rates.get(index1, index2);
    } else {
      throw new IllegalArgumentException(Messages.format(
          "No FX rate found for {}/{}, matrix only contains rates for {}", baseCurrency, counterCurrency, currencies.keySet()));
    }
  }

  /**
   * Converts a {@code CurrencyAmount} into an amount in the specified
   * currency using the rates in this matrix.
   *
   * @param amount  the {@code CurrencyAmount} to be converted
   * @param targetCurrency  the currency to convert the {@code CurrencyAmount} to
   * @return the amount converted to the requested currency
   */
  public CurrencyAmount convert(CurrencyAmount amount, Currency targetCurrency) {
    ArgChecker.notNull(amount, "amount");
    ArgChecker.notNull(targetCurrency, "targetCurrency");
    // Only do conversion if we need to
    Currency originalCurrency = amount.getCurrency();
    if (originalCurrency.equals(targetCurrency)) {
      return amount;
    }
    return CurrencyAmount.of(targetCurrency, convert(amount.getAmount(), originalCurrency, targetCurrency));
  }

  /**
   * Converts a {@code MultipleCurrencyAmount} into an amount in the
   * specified currency using the rates in this matrix.
   *
   * @param amount  the {@code MultipleCurrencyAmount} to be converted
   * @param targetCurrency  the currency to convert all entries to
   * @return the total amount in the requested currency
   */
  public CurrencyAmount convert(MultiCurrencyAmount amount, Currency targetCurrency) {
    ArgChecker.notNull(amount, "amount");
    ArgChecker.notNull(targetCurrency, "targetCurrency");

    // We could do this using the currency amounts but to
    // avoid creating extra objects we'll use doubles
    double total = amount.getAmounts()
        .stream()
        .mapToDouble(ca -> convert(ca.getAmount(), ca.getCurrency(), targetCurrency))
        .sum();
    return CurrencyAmount.of(targetCurrency, total);
  }

  //-------------------------------------------------------------------------
  /**
   * Merges the entries from the other matrix into this one.
   * <p>
   * The other matrix should have at least one currency in common with this one.
   * The additional currencies from the other matrix are added one by one and
   * the exchange rate data created is coherent with some data in this matrix.
   * <p>
   * Note that if the other matrix has more than one currency in common with
   * this one, and the rates for pairs of those currencies are different to
   * the equivalents in this matrix, then the rates between the additional
   * currencies is this matrix will differ from those in the original.
   *
   * @param other  the matrix to be merged into this one
   * @return a new matrix containing the rates from this matrix
   *   plus any rates for additional currencies from the other matrix
   */
  public FxMatrix merge(FxMatrix other) {
    return toBuilder().merge(other.toBuilder()).build();
  }

  /**
   * Creates a new builder using the data from this matrix to
   * create a set of initial entries.
   *
   * @return a new builder containing the data from this matrix
   */
  public FxMatrixBuilder toBuilder() {
    return new FxMatrixBuilder(currencies, rates.toArray());
  }

  @Override
  public String toString() {
    return "FxMatrix[" + Joiner.on(", ").join(getCurrencies()) + " : " +
        Stream.of(rates.toArrayUnsafe()).map(Arrays::toString).collect(Collectors.joining(",")) + "]";
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxMatrix}.
   * @return the meta-bean, not null
   */
  public static FxMatrix.Meta meta() {
    return FxMatrix.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxMatrix.Meta.INSTANCE);
  }

  /**
   * Creates an instance.
   * @param currencies  the value of the property, not null
   * @param rates  the value of the property, not null
   */
  FxMatrix(
      Map<Currency, Integer> currencies,
      DoubleMatrix rates) {
    JodaBeanUtils.notNull(currencies, "currencies");
    JodaBeanUtils.notNull(rates, "rates");
    this.currencies = ImmutableMap.copyOf(currencies);
    this.rates = rates;
  }

  @Override
  public FxMatrix.Meta metaBean() {
    return FxMatrix.Meta.INSTANCE;
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
   * Gets the matrix with all the exchange rates. Each row represents the
   * rates required to convert a unit of particular currency to all
   * other currencies in the matrix.
   * <p>
   * If currencies c1 and c2 are assigned indexes i and j respectively
   * in the {@code currencies} map, then the entry [i][j] is such that
   * 1 unit of currency c1 is worth {@code rates[i][j]} units of
   * currency c2.
   * <p>
   * If {@code currencies.get(EUR)} = 0 and {@code currencies.get(USD)} = 1,
   * then the element {@code rates[0][1]} is likely to be around
   * 1.40 and {@code rates[1][0]} around 0.7142. The rate {@code rates[1][0]}
   * will be computed from fxRate[0][1] when the object is constructed
   * by the builder. All the element of the matrix are meaningful and coherent.
   * @return the value of the property, not null
   */
  public DoubleMatrix getRates() {
    return rates;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxMatrix other = (FxMatrix) obj;
      return JodaBeanUtils.equal(currencies, other.currencies) &&
          JodaBeanUtils.equal(rates, other.rates);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currencies);
    hash = hash * 31 + JodaBeanUtils.hashCode(rates);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxMatrix}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currencies} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Currency, Integer>> currencies = DirectMetaProperty.ofImmutable(
        this, "currencies", FxMatrix.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code rates} property.
     */
    private final MetaProperty<DoubleMatrix> rates = DirectMetaProperty.ofImmutable(
        this, "rates", FxMatrix.class, DoubleMatrix.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currencies",
        "rates");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1089470353:  // currencies
          return currencies;
        case 108285843:  // rates
          return rates;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxMatrix> builder() {
      return new FxMatrix.Builder();
    }

    @Override
    public Class<? extends FxMatrix> beanType() {
      return FxMatrix.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currencies} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Currency, Integer>> currencies() {
      return currencies;
    }

    /**
     * The meta-property for the {@code rates} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleMatrix> rates() {
      return rates;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1089470353:  // currencies
          return ((FxMatrix) bean).currencies;
        case 108285843:  // rates
          return ((FxMatrix) bean).getRates();
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
   * The bean-builder for {@code FxMatrix}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<FxMatrix> {

    private Map<Currency, Integer> currencies = ImmutableMap.of();
    private DoubleMatrix rates;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1089470353:  // currencies
          return currencies;
        case 108285843:  // rates
          return rates;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1089470353:  // currencies
          this.currencies = (Map<Currency, Integer>) newValue;
          break;
        case 108285843:  // rates
          this.rates = (DoubleMatrix) newValue;
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
    public FxMatrix build() {
      return new FxMatrix(
          currencies,
          rates);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("FxMatrix.Builder{");
      buf.append("currencies").append('=').append(JodaBeanUtils.toString(currencies)).append(',').append(' ');
      buf.append("rates").append('=').append(JodaBeanUtils.toString(rates));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

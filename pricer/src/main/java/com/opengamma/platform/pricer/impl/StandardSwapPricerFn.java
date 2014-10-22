/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

import org.threeten.bp.ZoneOffset;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.env.AnalyticsEnvironment;
import com.opengamma.analytics.financial.instrument.index.IndexIborMaster;
import com.opengamma.analytics.financial.interestrate.payments.derivative.CouponFixed;
import com.opengamma.analytics.financial.interestrate.payments.derivative.CouponIborSpread;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.basics.index.RateIndices;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.platform.finance.trade.swap.AccrualPeriod;
import com.opengamma.platform.finance.trade.swap.FixedRateAccrualPeriod;
import com.opengamma.platform.finance.trade.swap.FloatingRateAccrualPeriod;
import com.opengamma.platform.finance.trade.swap.SwapPaymentPeriod;
import com.opengamma.platform.finance.trade.swap.SwapTrade;
import com.opengamma.platform.pricer.SwapInstrumentsDataSet;
import com.opengamma.platform.pricer.SwapPricerFn;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Pricer for swaps.
 */
public class StandardSwapPricerFn implements SwapPricerFn {

  /**
   * Calc.
   */
  private static final PresentValueDiscountingCalculator CALCULATOR = PresentValueDiscountingCalculator.getInstance();

  /**
   * Calculates the present value of the swap.
   * 
   * @param environment  the pricing environment
   * @param valuationDate  the valuation date
   * @param trade  the trade to price
   * @return the present value of the swap
   */
  @Override
  public CurrencyAmount presentValue(MulticurveProviderInterface environment, LocalDate valuationDate, SwapTrade trade) {
    List<SwapPaymentPeriod> payments = new ArrayList<>();
    payments.addAll(trade.getLeg1().toExpanded().getPaymentPeriods());
    payments.addAll(trade.getLeg2().toExpanded().getPaymentPeriods());
//    MultipleCurrencyAmount pv1 = payments.stream()
//      .map(p -> presentValue(environment, valuationDate, p))
//      .filter(Objects::nonNull)
//      .reduce(MultipleCurrencyAmount.of(), MultipleCurrencyAmount::plus);
//    double amount = pv1.getAmount(com.opengamma.util.money.Currency.USD);
   
    List<SwapPaymentPeriod> aggregateFixed= new ArrayList<>();
    List<SwapPaymentPeriod> aggregateFloating = new ArrayList<>();
    for (int k = 0; k < payments.size(); k++)
    {
    	if(payments.get(k).getAccrualPeriods().get(0) instanceof FixedRateAccrualPeriod)
    	{
    		aggregateFixed.add(payments.get(k));
    	}
    	else if(payments.get(k).getAccrualPeriods().get(0) instanceof FloatingRateAccrualPeriod)
    	{
    		aggregateFloating.add(payments.get(k));
    	}
    	else
    	{
    		throw new OpenGammaRuntimeException("Unknown accrual period type in aggregation loop");
    	}
    }
    
    MultipleCurrencyAmount fixedPv = fixedLeg(environment, valuationDate, aggregateFixed.toArray(new SwapPaymentPeriod[0]));
    MultipleCurrencyAmount floatPv = floatLeg(environment, valuationDate, aggregateFloating.toArray(new SwapPaymentPeriod[0]));
    
      
    double sum = fixedPv.getAmount(com.opengamma.util.money.Currency.USD) + floatPv.getAmount(com.opengamma.util.money.Currency.USD);
       
    return CurrencyAmount.of(Currency.USD, sum);
  }
  
  
  MultipleCurrencyAmount fixedLeg(MulticurveProviderInterface environment, LocalDate valuationDate, SwapPaymentPeriod[] paymentPeriods)
  {
	  int nPeriods=paymentPeriods.length;
	  // reduction variable
	  MultipleCurrencyAmount total = MultipleCurrencyAmount.of(com.opengamma.util.money.Currency.USD,0); 
	  for(int k = 0; k < nPeriods; k++) {
		  // local ref to *this* payment period
		  SwapPaymentPeriod paymentPeriod = paymentPeriods[k];
		  
		  // historic payments have zero pv
		  if (paymentPeriod.getPaymentDate().isBefore(valuationDate)) {
		    continue;
		  }

		  AccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(0);
		  double paymentRelativeTime = relativeTime(valuationDate, paymentPeriod.getPaymentDate());
		  
		    // fixed leg stuff
		  FixedRateAccrualPeriod fixedAccrual = (FixedRateAccrualPeriod) accrualPeriod;
		  CouponFixed coupon = new CouponFixed(
		      currency(Currency.USD),
		      paymentRelativeTime,
		      fixedAccrual.getYearFraction(),
		      fixedAccrual.getNotional(),
		      fixedAccrual.getRate(),
		      date(accrualPeriod.getStartDate()).atStartOfDay(ZoneOffset.UTC),
		      date(accrualPeriod.getEndDate()).atStartOfDay(ZoneOffset.UTC));
		  MultipleCurrencyAmount loopValue = CALCULATOR.visitCouponFixed(coupon, environment);
		  total = total.plus(loopValue);
	  }
	  return total;
  };

/**
 * Computes the value of the floating leg.
 * 
 * Need to adjust this to take a CCY
 *  
 * @param environment void *
 * @param valuationDate the valuation date
 * @param paymentPeriods the swap payment periods on this leg
 * @return the amount
 */
  MultipleCurrencyAmount floatLeg(MulticurveProviderInterface environment, LocalDate valuationDate, SwapPaymentPeriod[] paymentPeriods)
  {
	  int nPeriods=paymentPeriods.length;
	  // reduction variable
	  MultipleCurrencyAmount total = MultipleCurrencyAmount.of(com.opengamma.util.money.Currency.USD,0); 
	  for(int k = 0; k < nPeriods; k++) {
		// local ref to *this* payment period
		SwapPaymentPeriod paymentPeriod = paymentPeriods[k];
		  
		// historic payments have zero pv
		if (paymentPeriod.getPaymentDate().isBefore(valuationDate)) {
			continue;
		}
		// common elements
		AccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(0);
		double paymentRelativeTime = relativeTime(valuationDate, paymentPeriod.getPaymentDate());
		  
		FloatingRateAccrualPeriod floatingAccrual = (FloatingRateAccrualPeriod) accrualPeriod;
		IborIndex index = (IborIndex) floatingAccrual.getIndex();
		LocalDate fixingDate = floatingAccrual.getFixingDate();
		LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
		LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
		
		// map index
		com.opengamma.analytics.financial.instrument.index.IborIndex idx =
		    IndexIborMaster.getInstance().getIndex(IndexIborMaster.USDLIBOR3M);
		LocalDateDoubleTimeSeries historicLibor = SwapInstrumentsDataSet.TS_USDLIBOR3M;
		if (index.equals(RateIndices.USD_LIBOR_6M)) {
		  idx = IndexIborMaster.getInstance().getIndex(IndexIborMaster.USDLIBOR6M);
		  historicLibor = SwapInstrumentsDataSet.TS_USDLIBOR6M;
		}
		// lookup known fixing data
		if (valuationDate.equals(fixingDate) || valuationDate.isAfter(fixingDate)) {
		  OptionalDouble fixedRate = historicLibor.get(fixingDate);
		  if (fixedRate.isPresent()) {
		    CouponFixed coupon = new CouponFixed(
		        currency(Currency.USD),
		        paymentRelativeTime,
		        floatingAccrual.getYearFraction(),
		        floatingAccrual.getNotional(),
		        fixedRate.getAsDouble(),
		        date(accrualPeriod.getStartDate()).atStartOfDay(ZoneOffset.UTC),
		        date(accrualPeriod.getEndDate()).atStartOfDay(ZoneOffset.UTC));
		    System.out.println(coupon);
		    MultipleCurrencyAmount loopValue = CALCULATOR.visitCouponFixed(coupon, environment);
		    total = total.plus(loopValue);
		    continue;
		  } else if (valuationDate.isAfter(fixingDate)) { // the fixing is required
			  throw new OpenGammaRuntimeException("Could not get fixing value for date " + fixingDate);
		  }
		}
		// floating Ibor
		CouponIborSpread coupon = new CouponIborSpread(
		    currency(Currency.USD),
		    paymentRelativeTime,
		    floatingAccrual.getYearFraction(),
		    floatingAccrual.getNotional(),
		    relativeTime(valuationDate, fixingDate),
		    idx,
		    relativeTime(valuationDate, fixingStartDate),
		    relativeTime(valuationDate, fixingEndDate),
		    index.getDayCount().getDayCountFraction(fixingStartDate, fixingEndDate),
		    floatingAccrual.getSpread());
		System.out.println(coupon);
		MultipleCurrencyAmount loopValue = CALCULATOR.visitCouponIborSpread(coupon, environment);
		total = total.plus(loopValue);
	  }
	  return total;
  };
  
  // present value
  MultipleCurrencyAmount presentValue(
      MulticurveProviderInterface environment, LocalDate valuationDate, SwapPaymentPeriod paymentPeriod) {
    // historic payments have zero pv
    if (paymentPeriod.getPaymentDate().isBefore(valuationDate)) {
      return null;
    }
    // common elements
    AccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(0);
    double paymentRelativeTime = relativeTime(valuationDate, paymentPeriod.getPaymentDate());
    // specific types
    if (accrualPeriod instanceof FixedRateAccrualPeriod) {
      // fixed
      FixedRateAccrualPeriod fixedAccrual = (FixedRateAccrualPeriod) accrualPeriod;
      CouponFixed coupon = new CouponFixed(
          currency(Currency.USD),
          paymentRelativeTime,
          fixedAccrual.getYearFraction(),
          fixedAccrual.getNotional(),
          fixedAccrual.getRate(),
          date(accrualPeriod.getStartDate()).atStartOfDay(ZoneOffset.UTC),
          date(accrualPeriod.getEndDate()).atStartOfDay(ZoneOffset.UTC));
      System.out.println(coupon);
      return CALCULATOR.visitCouponFixed(coupon, environment);
      
    } else {
      // floating
      FloatingRateAccrualPeriod floatingAccrual = (FloatingRateAccrualPeriod) accrualPeriod;
      IborIndex index = (IborIndex) floatingAccrual.getIndex();
      LocalDate fixingDate = floatingAccrual.getFixingDate();
      LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
      LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
//      System.out.println(fixingDate + " " + fixingStartDate + " " + fixingEndDate);
      // map index
      com.opengamma.analytics.financial.instrument.index.IborIndex idx =
          IndexIborMaster.getInstance().getIndex(IndexIborMaster.USDLIBOR3M);
      LocalDateDoubleTimeSeries historicLibor = SwapInstrumentsDataSet.TS_USDLIBOR3M;
      if (index.equals(RateIndices.USD_LIBOR_6M)) {
        idx = IndexIborMaster.getInstance().getIndex(IndexIborMaster.USDLIBOR6M);
        historicLibor = SwapInstrumentsDataSet.TS_USDLIBOR6M;
      }
      // lookup known fixing data
      if (valuationDate.equals(fixingDate) || valuationDate.isAfter(fixingDate)) {
        OptionalDouble fixedRate = historicLibor.get(fixingDate);
        if (fixedRate.isPresent()) {
          CouponFixed coupon = new CouponFixed(
              currency(Currency.USD),
              paymentRelativeTime,
              floatingAccrual.getYearFraction(),
              floatingAccrual.getNotional(),
              fixedRate.getAsDouble(),
              date(accrualPeriod.getStartDate()).atStartOfDay(ZoneOffset.UTC),
              date(accrualPeriod.getEndDate()).atStartOfDay(ZoneOffset.UTC));
          System.out.println(coupon);
          return CALCULATOR.visitCouponFixed(coupon, environment);
        } else if (valuationDate.isAfter(fixingDate)) { // the fixing is required
          throw new OpenGammaRuntimeException("Could not get fixing value for date " + fixingDate);
        }
      }
      // floating Ibor
      CouponIborSpread coupon = new CouponIborSpread(
          currency(Currency.USD),
          paymentRelativeTime,
          floatingAccrual.getYearFraction(),
          floatingAccrual.getNotional(),
          relativeTime(valuationDate, fixingDate),
          idx,
          relativeTime(valuationDate, fixingStartDate),
          relativeTime(valuationDate, fixingEndDate),
          index.getDayCount().getDayCountFraction(fixingStartDate, fixingEndDate),
          floatingAccrual.getSpread());
      System.out.println(coupon);
      return CALCULATOR.visitCouponIborSpread(coupon, environment);
    }
  }

  private double relativeTime(LocalDate valuationDate, LocalDate end) {
    if (end.isBefore(valuationDate)) {
      return -AnalyticsEnvironment.getInstance().getModelDayCount().getDayCountFraction(date(end), date(valuationDate));
    }
    return AnalyticsEnvironment.getInstance().getModelDayCount().getDayCountFraction(date(valuationDate), date(end));
  }

  private org.threeten.bp.LocalDate date(LocalDate start) {
    return org.threeten.bp.LocalDate.of(start.getYear(), start.getMonthValue(), start.getDayOfMonth());
  }

  private com.opengamma.util.money.Currency currency(Currency currency) {
    return com.opengamma.util.money.Currency.of(currency.getCode());
  }

//new com.opengamma.analytics.financial.instrument.index.IborIndex(
//com.opengamma.util.money.Currency.USD,
//Period.ofMonths(index.getTenor().getPeriod().getMonths()),
//index.getEffectiveDateOffset().getDays(),
//null,
//null, //index.getMaturityDateOffset().getAdjustment().getConvention(),
//index.getMaturityDateOffset().getAdditionConvention() == PeriodAdditionConventions.LAST_BUSINESS_DAY,
//index.getName());
}

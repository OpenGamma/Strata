package com.opengamma.strata.finance.credit;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.finance.credit.fee.SinglePayment;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

public class CdsExpandedTrade implements CdsModelTrade {

  private final CdsTrade _trade;

  public CdsExpandedTrade(CdsTrade cdsTrade) {
    _trade = cdsTrade;
  }

  @Override
  public LocalDate tradeDate() {
    return _trade.getTradeInfo().getTradeDate().get();
  }

  @Override
  public LocalDate stepInDate() {
    //TODO
    // TODO this is on the convention... need to copy into trade
    //modelTradeDate().plusDays(1);
    //modelTradeDate().plusDays(1);
    throw new UnsupportedOperationException();
  }

  @Override
  public LocalDate valueDate() {
    return _trade.getTradeInfo().getSettlementDate().get();
  }

  @Override
  public LocalDate accStartDate() {
    BusinessDayAdjustment businessDayAdjustment = BusinessDayAdjustment.of(
        businessdayAdjustmentConvention(),
        calendar()
    );
    return businessDayAdjustment.adjust(
        _trade.getProduct().getGeneralTerms().getEffectiveDate()
    );
  }

  @Override
  public LocalDate endDate() {
    return _trade.getProduct().getGeneralTerms().getScheduledTerminationDate();
  }

  @Override
  public boolean payAccOnDefault() {
    //TODO
    // TODO this is on the convention... need to copy into trade
    throw new UnsupportedOperationException();
  }

  @Override
  public Period paymentInterval() {
    return _trade.getProduct().getFeeLeg().getPeriodicPayments().getPaymentFrequency().getPeriod();
  }

  @Override
  public StubConvention stubConvention() {
    return _trade.getProduct().getFeeLeg().getPeriodicPayments().getStubConvention();
  }

  @Override
  public BusinessDayConvention businessdayAdjustmentConvention() {
    return _trade.getProduct().getGeneralTerms().getDateAdjustments().getConvention();
  }

  @Override
  public HolidayCalendar calendar() {
    return _trade.getProduct().getGeneralTerms().getDateAdjustments().getCalendar();
  }

  @Override
  public DayCount accrualDayCount() {
    return _trade.getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getDayCountFraction();
  }

  @Override
  public BuySell buySellProtection() {
    return _trade.getProduct().getGeneralTerms().getBuySellProtection();
  }

  @Override
  public double upfrontFeeAmount() {
    Optional<SinglePayment> fee = _trade.getProduct().getFeeLeg().getSinglePayment();
    if (fee.isPresent()) {
      return fee.get().getFixedAmount();
    } else {
      return Double.NaN;
    }
  }

  @Override
  public LocalDate upfrontFeePaymentDate() {
    Optional<SinglePayment> fee = _trade.getProduct().getFeeLeg().getSinglePayment();
    if (fee.isPresent()) {
      return fee.get().getPaymentDate();
    } else {
      throw new UnsupportedOperationException("There is no fee, cannot dereference the payment date");
    }
  }

  @Override
  public double coupon() {
    return _trade.getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getFixedRate();
  }

  @Override
  public double notional() {
    return _trade.getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getCalculationAmount().getAmount();
  }

  @Override
  public Currency currency() {
    return _trade.getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getCalculationAmount().getCurrency();
  }
}

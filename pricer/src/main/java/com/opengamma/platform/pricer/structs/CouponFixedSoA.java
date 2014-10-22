/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.platform.pricer.structs;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.util.money.Currency;

/**
 * Class describing a fixed coupon.
 */
public class CouponFixedSoA extends CouponSoA {

	/**
	 * The coupon fixed rate.
	 */
	private double[] _fixedRate;
	/**
	 * The paid amount.
	 */
	private double[] _amount;
	/**
	 * The start date of the coupon accrual period. Can be null if of no use.
	 */
	private ZonedDateTime[] _accrualStartDate;
	/**
	 * The end date of the coupon accrual period. Can be null if of no use.
	 */
	private ZonedDateTime[] _accrualEndDate;

	/**
	 * Number of coupons in the SoA
	 */
	private int _count;

	/**
	 * Constructor from all details.
	 * 
	 * @param currency
	 *            The payment currency.
	 * @param paymentTime
	 *            Time (in years) up to the payment.
	 * @param paymentYearFraction
	 *            The year fraction (or accrual factor) for the coupon payment.
	 * @param notional
	 *            Coupon notional.
	 * @param rate
	 *            The coupon fixed rate.
	 * @param accrualStartDate
	 *            The start date of the coupon accrual period.
	 * @param accrualEndDate
	 *            The end date of the coupon accrual period.
	 */
	public CouponFixedSoA(Currency currency, double[] paymentTime,
			double[] paymentYearFraction, double[] notional,
			double[] rate, ZonedDateTime[] accrualStartDate,
			ZonedDateTime[] accrualEndDate) {
		// need a check in here to make sure everything is the same length
		super(currency, paymentTime, paymentYearFraction, notional);
		_fixedRate = rate;
		_amount = new double[paymentYearFraction.length];
		for(int k = 0; k < paymentYearFraction.length; k++)
		{
			_amount[k] = paymentYearFraction[k] * notional[k] * rate[k];
		}
		_accrualStartDate = accrualStartDate.clone();
		_accrualEndDate = accrualEndDate.clone();
		_count = paymentYearFraction.length;
	}


	/**
	 * Gets the coupon fixed rate.
	 * 
	 * @return The fixed rate.
	 */
	public double[] getFixedRate() {
		return _fixedRate;
	}

	/**
	 * Gets the start date of the coupon accrual period.
	 * 
	 * @return The accrual start date.
	 */
	public ZonedDateTime[] getAccrualStartDate() {
		return _accrualStartDate;
	}

	/**
	 * Gets the end date of the coupon accrual period.
	 * 
	 * @return The accrual end date.
	 */
	public ZonedDateTime[] getAccrualEndDate() {
		return _accrualEndDate;
	}

	/**
	 * Gets the paid amount.
	 * 
	 * @return The amount.
	 */
	public double[] getAmount() {
		return _amount;
	}


	/**
	 * Gets the number of coupons.
	 * 
	 * @return The count.
	 */
	public int getCount() {
		return _count;
	}


	@Override
	public String toString() {
		return super.toString() + ", [Rate=" + _fixedRate + ", notional="
				+ getNotional() + ", year fraction=" + getPaymentYearFraction()
				+ "]";
	}


	@Override
	public <S, T> T accept(InstrumentDerivativeVisitor<S, T> visitor, S data) {
		return null;
	}


	@Override
	public <T> T accept(InstrumentDerivativeVisitor<?, T> visitor) {
		return null;
	}

}

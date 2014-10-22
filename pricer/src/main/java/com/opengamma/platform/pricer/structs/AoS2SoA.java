/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */

package com.opengamma.platform.pricer.structs;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.interestrate.payments.derivative.CouponFixed;
import com.opengamma.collect.ArgChecker;

/**
 * Contains coverters to go from arrays of structs to structs of arrays
 */
public class AoS2SoA {

	public static CouponFixedSoA ConvertCouponFixed(CouponFixed[] coup) {
		ArgChecker.notNull(coup, "coup");
		int len = coup.length;
		
		double[] paymentTime = new double[len];
		double[] paymentYearFraction = new double[len];
		double[] notional = new double[len];
		double[] rate = new double[len];
		ZonedDateTime[] accrualStartDate = new ZonedDateTime[len];
		ZonedDateTime[] accrualEndDate = new ZonedDateTime[len];
		for (int k = 0; k < len; k++) {
			paymentTime[k]=coup[k].getPaymentTime();
			paymentYearFraction[k]=coup[k].getPaymentYearFraction();
			notional[k]=coup[k].getNotional();
			rate[k]=coup[k].getFixedRate();
			accrualStartDate[k]=coup[k].getAccrualStartDate();
			accrualEndDate[k]=coup[k].getAccrualEndDate();
		}

		CouponFixedSoA ret = new CouponFixedSoA(
				coup[0].getCurrency(),
				paymentTime,
				paymentYearFraction,
				notional,
				rate,
				accrualStartDate,
				accrualEndDate);
		return ret;
	}

}

======================
Market data resolution
======================

In general, pricing a trade using credit analytics depends on the resolution of
two market data structures - a credit curve and a yield curve. The type of
trade being priced will determine how these structures are resolved and
calibrated. OpenGamma provides a simple framework for managing credit data and
the rules for resolving it.

Curve types
===========


Single name CDS curves
----------------------

Single name CDS curves are resolved using a combination of four CDS fields:

* Curve name (a.k.a. legal entity name)
* Currency 
* Seniority 
* Restructuring clause

The key object used to store these fields is ``CreditCurveDataKey``. The
``CreditCurveDataProviderFn`` is the interface used to resolve curves for these
keys.

Yield curves
------------

Yield curves are resolved by currency. Typically this currency will come from
the field on the ``CreditCurveDataKey`` used for resolving the credit curve.
The ``YieldCurveDataProviderFn`` interface is used to resolve yield curves by
currency.

Trade types
===========

Single name CDS
---------------

Four fields on a CDS are significant in resolving market data:

* Legal entity 
* Currency
* Seniority
* Restructuring clause

A resolution key is created by copying these fields from the target CDS and can
then be passed to the ``CreditCurveDataProviderFn`` to resolve the correct
curve.

For situations where quotes cannot be obtained for a specific CDS, a generic
curve (e.g. a AAA curve) can be used instead. This is achieved by storing a
list of ``CreditCurveDataKey`` to ``CreditCurveDataKey`` mappings.

For example, the following key for IBM might map to the triple A key below:

*IBM key*:

* Curve name (legal entity) = "IBM" 
* Currency = "USD"
* Seniority = "SNRFOR" 
* Restructuring clause = "XR"

*Triple A key*

* Curve name (legal entity) = "AAA"
* Currency = "USD"

(Note seniority and restructuring are optional so may be omitted).

These ``CreditCurveDataKey`` mappings are stored in ``CreditCurveDataKeyMap``.
If no mapping is defined for a key, the key will be passed directly into the
``CreditCurveDataProviderFn`` for direct resolution of a credit curve.

Bonds
-----

 Bonds can be configured to price on CDS data using an ID mapping
mechanism similar to the one described above for single name CDS. The class
which holds the map is the ``BondCreditCurveDataKeyMap``. Encapsulated within
is a map of ``ExternalIdBundle`` to ``CreditCurveDataKey``. The external id
bundle will be a reference to the bond (e.g. ISIN) and the curve key a
reference to a CDS curve.

Curve calibration and outputs
=============================

The top level interface for the building and resolution of credit curves is
``IsdaCompliantCreditCurveFn``. This function interface defines a single method
taking a pricing environment and credit curve key. When called, it will take
the following sequence of actions: 

1. Resolve a yield curve via``YieldCurveDataProviderFn`` using the currency on
the key. A ``YieldCurveData`` object is returned containing the market data and
term structure.

2. The ``IsdaCompliantYieldCurveFn`` will then bootstrap the curve using the
data returned in 1. An ``IsdaYieldCurve`` is returned which contains the
``YieldCurveData`` used as the calibration input and the calibrated
``IsdaCompliantCreditCurve``.

3. Resolve credit curve data using the ``CreditCurveDataProviderFn``. A
``CreditCurveData`` instance is returned containing the market data and term
structure.

4. The ``IsdaCompliantCreditCurveFn`` will then bootstrap the credit curve using
the credit curve data returned in 3 and the yield curve returned in 1. An
``IsdaCreditCurve`` is returned which contains the ``IsdaYieldCurve``
and``CreditCurveData`` instances used as inputs to the calibration and the
calibrated ``IsdaCompliantCreditCurve`` instance.

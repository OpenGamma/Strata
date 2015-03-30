================
Date Adjustments
================

Introduction
============

A key problem when working with dates is calculating one date relative to another
taking into account :doc:`holidays <holidays>`, weekends and other conventions.
To handle this, a set of date manipulation classes are provided.


Date adjustment
===============

The OG-Basics project includes a number of key classes for adjusting dates:

* `Business Day Adjustment`_
* `Days Adjustment`_
* `Period Adjustment`_
* `Tenor Adjustment`_
* `Adjustable Date`_


Business Day Adjustment
=======================

The simplest adjustment is provided by ``BusinessDayAdjustment``.
It takes an input date and returns the closest business day, as defined by the rules of the adjustment.

The rules consist of two items:

* the ``BusinessDayConvention``
* the ``HolidayCalendar``

A date is adjusted by passing the adjuster to ``LocalDate.with(TemporalAdjuster)``:

.. code-block:: java

    LocalDate adjusted = baseDate.with(myBusinessDayAdjustment);

The business day convention is a simple rule that specifies how to change the date from a holiday or weekend
to a valid business day. The most commonly used conventions are:

* 'Following', moving to the next valid business day
* 'Modified Following', moving to the next valid business day unless it is in the next month
* 'Preceding', moving to the previous valid business day

Other conventions are available, see the Javadoc of ``BusinessDayConventions``.

For example, if a ``BusinessDayAdjustment`` has a business day convention of 'Following' and a holiday
calendar of 'GBLO' (London holidays) then the following adjustments will occur:

+--------------+--------------+---------------------------------------------------------------------+
| Input date   | Output date  | Reason                                                              |
+==============+==============+=====================================================================+
| 1st Jan 2015 | 2nd Jan 2015 | 1st January is a holiday, select following business day             |
+--------------+--------------+---------------------------------------------------------------------+
| 2nd Jan 2015 | 2nd Jan 2015 | 2nd January is already a valid business day, make no change         |
+--------------+--------------+---------------------------------------------------------------------+
| 3rd Jan 2015 | 5th Jan 2015 | 3rd January is a Saturday (weekend), select following business day  |
+--------------+--------------+---------------------------------------------------------------------+
| 4th Jan 2015 | 5th Jan 2015 | 4th January is a Sunday (weekend), select following business day    |
+--------------+--------------+---------------------------------------------------------------------+
| 5th Jan 2015 | 5th Jan 2015 | 5th January is already a valid business day, make no change         |
+--------------+--------------+---------------------------------------------------------------------+


Days Adjustment
===============

The simplest relative adjustment is provided by ``DaysAdjustment``.
It takes an input date and adds a specific number of days.

The rules consist of three items:

* the number of days to add
* the ``HolidayCalendar`` for addition
* the ``BusinessDayAdjustment`` to produce the final result

A date is adjusted by passing the adjuster to ``LocalDate.with(TemporalAdjuster)``:

.. code-block:: java

    LocalDate adjusted = baseDate.with(myDaysAdjustment);

The ``DaysAdjustment`` class can be used to add calendar days or business days to a date.

When adding calendar days, the holiday calendar is set to 'NoHolidays'.
The number of days will be added to the base date using standard date arithmetic, ignoring holidays.
The business day adjustment is used to convert the result to a valid business day.

When adding business days, the holiday calendar is set to the calendar of the business days to be added.
The number of days will be added one by one to the base date using the holiday calendar.
The business day adjustment is used to convert the result to a valid business day.

Note that the holiday calendar used by the business day adjustment may differ from that used for addition,
which allows complex rules to be built.
For example, the USD LIBOR fixing rule is that the effective date is two business days after the fixing date
using the London holiday calendar, where that date is further adjusted to ensure it is a valid business day
in both London and New York.


Period Adjustment
=================

Longer relative adjustments are provided by ``PeriodAdjustment``.
It takes an input date and adds a specific period of years, months and days, as expressed by a ``Period``.

The rules consist of three items:

* the ``Period`` to add
* the ``PeriodAdditionConvention``
* the ``BusinessDayAdjustment`` to produce the final result

A date is adjusted by passing the adjuster to ``LocalDate.with(TemporalAdjuster)``:

.. code-block:: java

    LocalDate adjusted = baseDate.with(myPeriodAdjustment);

Addition is performed using standard calendar addition.
The date is then adjusted using the ``PeriodAdditionConvention``, which provides end-of-month rules,
including selecting the last day of the month, or the last *business* day of the month.

The result is then adjusted using a ``BusinessDayAdjustment`` to produce the final result.

For example, this class could be used to add 6 months to a date ensuring that if the input
date is the last business day of the month then the result will also be the last business day of the month.


Tenor Adjustment
================

This is identical to ``PeriodAdjustment`` except that the period is represented as a ``Tenor``.


Adjustable Date
===============

On some occasions, it can be useful to hold the base date and the business day adjustment together.
This can be achieved using ``AdjustableDate``.

The class consist of two items:

* the unadjusted ``LocalDate``
* the ``BusinessDayAdjustment`` that will be used to adjust it

The adjusted date can be obtained using ``AdjustableDate.adjust()``:

.. code-block:: java

    LocalDate adjusted = adjustableDate.adjust();



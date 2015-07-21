==========
Day Counts
==========

Introduction
============

A *day count* is used to convert dates to a numeric representation for financial maths.

A date consists of 12 months of varying lengths and occasional leap days.
As a result of this variation there are many possible ways to convert the amount of time between
two dates into a number. While counting the number of days between two dates is simple, there
are many different ways to convert the amount of time to a fraction of a year.
Each day count specifies a specific set of rules for the conversion.
Different rules are used in different markets and for different products.

The standard approach is based on converting the period between two dates to a *year fraction*.
A year fraction is a floating point number, where one year is represented by 1 and half a year by 0.5.

Most day counts are relatively simple, operating solely on the two dates, returning the year fraction.
Some day counts are more complicated, needing additional information about an associated schedule.

See `Wikipedia <http://en.wikipedia.org/wiki/Day_count_convention>`_ for more background information.


Day Count
=========

The Strata-Basics project includes a comprehensive implementation of day count conventions.
The key interface is ``DayCount`` which defines methods to obtain and query a specific day count.

Each day count implementation has a unique name.
This can be used to obtain the day count via the static method ``DayCount.of(String)``:

.. code-block:: java

    DayCount dayCount = DayCount.of("Act/360");

All available day counts can be listed using the static method  ``DayCount.extendedEnum()``.

Common day counts can also be obtained using static constants on ``DayCounts``:

.. code-block:: java

    DayCount dayCount = DayCounts.ACT_360;

Once a day count is obtained, two methods are available to obtain the year fraction:

.. code-block:: java

    double yf = dayCount.yearFraction(LocalDate, LocalDate);
    double yf = dayCount.yearFraction(LocalDate, LocalDate, ScheduleInfo);

Both methods convert the period of time between two dates to a floating point number.
The first handles all simple day counts, the second allows additional information about the schedule to be passed in.

.. note::

    Certain day count implementations cannot give an answer without the schedule information.
    As such, those implementations will throw an exception when called without ``ScheduleInfo``.


Standard Day Counts
===================

The following standard day counts have been implemented.
Each has a string name which acts as a unique key to obtain the day count.
They are also available as constants on the ``DayCounts`` class.
For more information on each, see the Javadoc of ``DayCounts``:

Actual/360
----------

:Name: ``Act/360``
:Constant: ``DayCounts.ACT_360``
:Description:
    Divides the actual number of days by 360.
    
    The result is a simple division.
    The numerator is the actual number of days in the requested period.
    The denominator is always 360.
:Also known: 'French'.
:Definition: 2006 ISDA definitions 4.16e and ICMA rule 251.1(i) part 1.

Actual/364
----------

:Name: ``Act/364``
:Constant: ``DayCounts.ACT_364``
:Description:
    Divides the actual number of days by 364.
    
    The result is a simple division.
    The numerator is the actual number of days in the requested period.
    The denominator is always 364.

Actual/365 Fixed
----------------

:Name: ``Act/365F``
:Constant: ``DayCounts.ACT_365F``
:Description:
    Divides the actual number of days by 365.
    
    The result is a simple division.
    The numerator is the actual number of days in the requested period.
    The denominator is always 365.
:Also known: 'English'.
:Definition: 2006 ISDA definitions 4.16d.

Actual/365 Actual
-----------------

:Name: ``Act/365 Actual``
:Constant: ``DayCounts.ACT_365_ACTUAL``
:Description:
    Divides the actual number of days by 366 if a leap day is contained, or by 365 if not.
    
    The result is a simple division.
    The numerator is the actual number of days in the requested period.
    The denominator is 366 if the period contains February 29th, if not it is 365.
    The first day in the period is excluded, the last day is included.
:Also known: 'Act/365A'.

Actual/365 Long
---------------

:Name: ``Act/365L``
:Constant: ``DayCounts.ACT_365L``
:Description:
    Divides the actual number of days by 365 or 366.
    
    The result is a simple division.
    The numerator is the actual number of days in the requested period.
    The denominator is determined by examining the frequency and the period end date (the date of the next coupon).
    If the frequency is annual then the denominator is 366 if the period contains February 29th,
    if not it is 365. The first day in the period is excluded, the last day is included.
    If the frequency is not annual, the the denominator is 366 if the period end date
    is in a leap year, if not it is 365.
:Schedules: This day count requires ``ScheduleInfo``.
:Also known: 'Act/365 Leap year'.
:Definition: 2006 ISDA definitions 4.16i and ICMA rule 251.1(i) part 2 as later clarified by ICMA and Swiss Exchange.

Actual/365.25
-------------

:Name: ``Act/365.25``
:Constant: ``DayCounts.ACT_365_25``
:Description:
    Divides the actual number of days by 365.25.
    
    The result is a simple division.
    The numerator is the actual number of days in the requested period.
    The denominator is always 365.25.    

NL/365
------

:Name: ``NL/365``
:Constant: ``DayCounts.NL_365``
:Description:
    Divides the actual number of days omitting leap days by 365.
    
    The result is a simple division.
    The numerator is the actual number of days in the requested period minus the number of occurrences of February 29.
    The denominator is always 365.
    The first day in the period is excluded, the last day is included.
:Also known: 'Act/365 No Leap'.

Actual/Actual ISDA
------------------

:Name: ``Act/Act ISDA``
:Constant: ``DayCounts.ACT_ACT_ISDA``
:Description:
    Divides the actual number of days in a leap year by 366 and the actual number of days in a standard year by 365.
    
    The result is calculated in two parts.
    The actual number of days in the requested period that fall in a leap year is divided by 366.
    The actual number of days in the requested period that fall in a standard year is divided by 365.
    The result is the sum of the two.
    The first day in the period is included, the last day is excluded.
:Definition: 2006 ISDA definitions 4.16b.

Actual/Actual ICMA
------------------

:Name: ``Act/Act ICMA``
:Constant: ``DayCounts.ACT_ACT_ICMA``
:Description:
    Divides the actual number of days by the actual number of days in the coupon period multiplied by the frequency.
    
    The result is calculated as follows.
    First, the underlying schedule period is obtained treating the first date as the start of the schedule period.
    Second, if the period is a stub, then nominal regular periods are created matching the
    schedule frequency, working forwards or backwards from the known regular schedule date.
    An end-of-month flag is used to handle month-ends.
    If the period is not a stub then the schedule period is treated as a nominal period below.
    Third, the result is calculated as the sum of a calculation for each nominal period.
    The actual days between the first and second date are allocated to the matching nominal period.
    Each calculation is a division. The numerator is the actual number of days in
    the nominal period, which could be zero in the case of a long stub.
    The denominator is the length of the nominal period  multiplied by the frequency.
    The first day in the period is included, the last day is excluded.
:Schedules: This day count requires ``ScheduleInfo``.
:Also known: 'ISMA-99'.
:Definition:
    2006 ISDA definitions 4.16c and ICMA rule 251.1(iii) and 251.3 as
    `later clarified <http://www.isda.org/c_and_a/pdf/mktc1198.pdf>`_ by ISDA.

Actual/Actual AFB
-----------------

:Name: ``Act/Act AFB``
:Constant: ``DayCounts.ACT_ACT_AFB``
:Description:
    Divides the actual number of days by 366 if a leap day is contained, or by 365 if not,
    with additional rules for periods over one year.
    
    The result is a simple division.
    The numerator is the actual number of days in the requested period.
    The denominator is determined by examining the period end date (the date of the next coupon).
    The denominator is 366 if the schedule period contains February 29th, if not it is 365.
    The first day in the schedule period is included, the last day is excluded.
    Read the Javadoc for a discussion of the algorithm, the
    `original French text <http://www.banque-france.fr/fileadmin/user_upload/banque_de_france/archipel/publications/bdf_bof/bdf_bof_1999/bdf_bof_01.pdf>`_
    and confusion with the `ISDA clarification <http://www.isda.org/c_and_a/pdf/ACT-ACT-ISDA-1999.pdf>`_.
:Definition:
    Association Francaise des Banques in September 1994 as 'Base Exact/Exact'
    in 'Definitions Communes plusieurs Additifs Techniques'.

30/360 ISDA
-----------

:Name: ``30/360 ISDA``
:Constant: ``DayCounts.THIRTY_360_ISDA``
:Description:
    A 30/360 style algorithm with special rules for the 31st day-of-month.
    
    The result is calculated as ``(360 * deltaYear + 30 * deltaMonth + deltaDay) / 360``.
    The deltaDay is then calculated once day-of-month adjustments have occurred.
    If the second day-of-month is 31 and the first day-of-month is 30 or 31, change the second day-of-month to 30.
    If the first day-of-month is 31, change the first day-of-month to 30.
:Also known: '30/360 U.S. Municipal' or '30/360 Bond Basis'.
:Definition: 2006 ISDA definitions 4.16f.

30U/360
-------

:Name: ``30U/360``
:Constant: ``DayCounts.THIRTY_U_360``
:Description:
    A 30/360 style algorithm with special rules for the 31st day-of-month and the end of February.
    
    The result is calculated as ``(360 * deltaYear + 30 * deltaMonth + deltaDay) / 360``.
    The deltaDay is then calculated once day-of-month adjustments have occurred.
    If the schedule uses EOM convention and both dates are the last day of February,
    change the second day-of-month to 30.
    If the schedule uses EOM convention and the first date is the last day of February,
    change the first day-of-month to 30.
    If the second day-of-month is 31 and the first day-of-month is 30 or 31, change the second day-of-month to 30.
    If the first day-of-month is 31, change the first day-of-month to 30.
	
    This day count has different rules depending on whether the EOM rule applies or not.
    The EOM rule is set in the ``ScheduleInfo``. The default value for EOM is true,
    
	There are two related day counts.
    The '30U/360 EOM' rule is identical to this rule when the EOM convention applies.
    The '30/360 ISDA' rule is identical to this rule when the EOM convention does not apply.
:Schedules: This day count assumes EOM convention is true if ``ScheduleInfo`` is not specified.
:Also known: '30/360 US', '30US/360' or '30/360 SIA'.

30U/360 EOM
-----------

:Name: ``30U/360 EOM``
:Constant: ``DayCounts.THIRTY_U_360_EOM``
:Description:
    A 30/360 style algorithm with special rules for the 31st day-of-month and the end of February.
    
    The result is calculated as ``(360 * deltaYear + 30 * deltaMonth + deltaDay) / 360``.
    The deltaDay is then calculated once day-of-month adjustments have occurred.
    If both dates are the last day of February, change the second day-of-month to 30.
    If the first date is the last day of February, change the first day-of-month to 30.
    If the second day-of-month is 31 and the first day-of-month is 30 or 31, change the second day-of-month to 30.
    If the first day-of-month is 31, change the first day-of-month to 30.

    This day count is not dependent on the EOM flag in ``ScheduleInfo``.
    
    This is the same as '30U/360' when the EOM convention applies.
    This day count would typically be used to be explicit about the EOM rule applying.
    In most cases, '30U/360' should be used in preference to this day count.
:Schedules: This day count assumes EOM convention is true if ``ScheduleInfo`` is not specified.
:Also known: '30/360 US', '30US/360' or '30/360 SIA'.

30/360 PSA
----------

:Name: ``30/360 PSA``
:Constant: ``DayCounts.THIRTY_360_PSA``
:Description:
    A 30/360 style algorithm with special rules for the 31st day-of-month and the end of February.
    
    The result is calculated as ``(360 * deltaYear + 30 * deltaMonth + deltaDay) / 360``.
    The deltaDay is then calculated once day-of-month adjustments have occurred.
    If the first date is the last day of February, change the first day-of-month to 30.
    If the second day-of-month is 31 and the first day-of-month is 30 or 31, change the second day-of-month to 30.
    If the first day-of-month is 31, change the first day-of-month to 30.
:Schedules: This day count assumes EOM convention is true if ``ScheduleInfo`` is not specified.
:Also known: '30/360 BMA' (PSA is the Public Securites Association, BMA is the Bond Market Association).

30E/360 ISDA
------------

:Name: ``30E/360 ISDA``
:Constant: ``DayCounts.THIRTY_E_360_ISDA``
:Description:
    A 30/360 style algorithm with special rules for the 31st day-of-month and the end of February.
    
    The result is calculated as ``(360 * deltaYear + 30 * deltaMonth + deltaDay) / 360``.
    The deltaDay is then calculated once day-of-month adjustments have occurred.
    If the first day-of-month is 31, change the first day-of-month to 30.
    If the second day-of-month is 31, change the second day-of-month to 30.
    If the first date is the last day of February, change the first day-of-month to 30.
    If the second date is the last day of February and it is not the maturity date,
    change the second day-of-month to 30.
:Schedules: This day count requires ``ScheduleInfo``.
:Also known: '30E/360 German' or 'German'.
:Definition: 2006 ISDA definitions 4.16h.

30E/360
-------

:Name: ``30E/360``
:Constant: ``DayCounts.THIRTY_E_360``
:Description:
    A 30/360 style algorithm with special rules for the 31st day-of-month.
    
    The result is calculated as ``(360 * deltaYear + 30 * deltaMonth + deltaDay) / 360``.
    The deltaDay is then calculated once day-of-month adjustments have occurred.
    If the first day-of-month is 31, it is changed to 30.
    If the second day-of-month is 31, it is changed to 30.
:Also known: '30/360 ISMA', '30/360 European', '30S/360 Special German' or 'Eurobond'.
:Definition: 2006 ISDA definitions 4.16g and ICMA rule 251.1(ii) and 252.2.

30E+/360
--------

:Name: ``30E+/360``
:Constant: ``DayCounts.THIRTY_EPLUS_360``
:Description:
    A 30/360 style algorithm with special rules for the 31st day-of-month.
    
    The result is calculated as ``(360 * deltaYear + 30 * deltaMonth + deltaDay) / 360``.
    The deltaDay and deltaMonth are calculated once adjustments have occurred.
    If the first day-of-month is 31, it is changed to 30.
    If the second day-of-month is 31, it is changed to 1 and the second month is incremented.

1/1
---

:Name: ``1/1``
:Constant: ``DayCounts.ONE_ONE``
:Description: An artifical day count that always returns one.
:Definition: Defined by the 2006 ISDA definitions 4.16a


package backtesting;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;

import com.opengamma.analytics.financial.instrument.index.GeneratorAttribute;
import com.opengamma.analytics.financial.instrument.index.GeneratorInstrument;


public class CurveData {
	protected ArrayList<GeneratorInstrument<? extends GeneratorAttribute>> GENERATORS;
	protected  ArrayList<Double> MARKET_QUOTES ;
	protected  ArrayList<Period> TENOR;
	public CurveData(
			ArrayList<GeneratorInstrument<? extends GeneratorAttribute>> gENERATORS,
			ArrayList<Double> mARKET_QUOTES, ArrayList<Period> tENOR) {
		super();
		GENERATORS = gENERATORS;
		MARKET_QUOTES = mARKET_QUOTES;
		TENOR = tENOR;
	}
	public CurveData() {
		GENERATORS=new ArrayList<GeneratorInstrument<? extends GeneratorAttribute>>();
		MARKET_QUOTES = new ArrayList<Double>();
		TENOR=new ArrayList<Period>();;
	}
	
	public boolean cleanData()
	{
		GENERATORS.clear();
		MARKET_QUOTES.clear();
		TENOR.clear();
		return true;
	}
	public GeneratorInstrument<? extends GeneratorAttribute>[] getGENERATORS() {
		return (GeneratorInstrument<? extends GeneratorAttribute>[]) GENERATORS.toArray();
	}
	
	public double[] getMARKET_QUOTES() {
		return ArrayUtils.toPrimitive((Double[])MARKET_QUOTES.toArray());
		
	}

	public Period[] getTENOR() {
		return (Period[]) TENOR.toArray();
	}

}

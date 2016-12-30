package backtesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

import com.opengamma.hts.loader.IcapIrsCsvLoader;
import com.opengamma.hts.utils.NamedTimeSeries;

public class ResourceLoader {
	private  File OISFile;
	private File IRSFile;
	private List<NamedTimeSeries> TimeSerises;
	
public ResourceLoader(String OSIPath,String IRSPath) throws IOException
{
	OISFile=new File(OSIPath);
	  IRSFile=new File(IRSPath);
	 TimeSerises= IcapIrsCsvLoader.loadTimeSeries(IRSPath,null , null);
	
}

public File getOISFile() {
	return OISFile;
}

public void setOISFile(File oISFile) {
	OISFile = oISFile;
}

public File getIRSFile() {
	return IRSFile;
}

public void setIRSFile(File iRSFile) {
	IRSFile = iRSFile;
}

public List<NamedTimeSeries> getTimeSerises() {
	return TimeSerises;
}

public void setTimeSerises(List<NamedTimeSeries> timeSerises) {
	TimeSerises = timeSerises;
}
public static String LocalDateTimeParser(LocalDate Referencedate) throws ParseException
{
	DateFormat fromFormat = new SimpleDateFormat("yyyy-MM-dd");
	fromFormat.setLenient(false);
	DateFormat toFormat = new SimpleDateFormat("dd/MM/yyyy");
	toFormat.setLenient(false);
	String dateStr =Referencedate.toString();
	Date date = fromFormat.parse(dateStr);
	return (toFormat.format(date));
	
}
	

public static Period periodExtractor(String desc) throws Exception
{
	String time=desc.substring(desc.lastIndexOf(' ')+1,desc.length()-1);
	int timeInt=Integer.parseInt(time);
	char x= desc.charAt(desc.length()-1);
	if(x=='y'||x=='Y')
		return Period.ofYears(timeInt);
	else if(x=='M'||x=='m')
		return Period.ofMonths(timeInt);
	else if(x=='D'||x=='d')
		return Period.ofDays(timeInt);

	else throw new Exception();

	
	
}
public void OISDataExtractor(LocalDate Referencedate) throws IOException, ParseException
{
    CSVReader csvReader = new CSVReader(new FileReader(OISFile));
    List<String[]> rows = null;
    try {
      rows = csvReader.readAll();
      
    } finally {
      if (csvReader != null) {
        csvReader.close();
      }
    }
    rows.remove(0); // Remove column names
    String DateString=LocalDateTimeParser(Referencedate);
    for (String[] row : rows) {
        if (row[0].equals(DateString)) {
          String record = row[3];
         
          }
   
    }
    }
public static void main(String[] args) throws Exception {
	//System.out.println(LocalDateTimeParser( LocalDate.of(2000,12,30))); 
	System.out.println(periodExtractor("IRS USD A A/360 v 3M LIBOR 24M").toString());
}
}

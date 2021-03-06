package com.yitianyike.calendar.appserver.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class DateUtil {
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private static final DateFormat timeFormat = new SimpleDateFormat("hhmmss");

	public static String getCurrentDate() {
		return dateFormat.format(Calendar.getInstance().getTime());
	}

	public static String getCurrentTime() {
		return timeFormat.format(Calendar.getInstance().getTime());
	}

	public static String getDecreaseDate(String befDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String decreaseDate = null;
		try {
			Date parse = sdf.parse(befDate);
			Calendar date = Calendar.getInstance();
			date.setTime(parse);
			date.set(Calendar.DATE, date.get(Calendar.DATE) - 1);
			decreaseDate = sdf.format(date.getTime());
		} catch (ParseException e) {
			return decreaseDate;
		}
		return decreaseDate;
	}

	public static String getGMTTime() {
		Calendar cd = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // 设置时区为GMT
		String str = sdf.format(cd.getTime());
		return str;
	}

	public static String getYearEndLongTime() {
		Calendar cd = Calendar.getInstance();
		int i = cd.get(Calendar.YEAR);
		String year = Integer.toString(i);
		String yearEnd = year + "-12-31";
		Date parse = null;
		try {
			parse = new SimpleDateFormat("yyyy-MM-dd").parse(yearEnd);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String yearLong = Long.toString(parse.getTime());
		return yearLong;
	}

	public static void main(String[] args) {
//		System.out.println(getCurrentDate());
//		System.out.println(getCurrentTime());
//		String yearEndLongTime = getYearEndLongTime();
//		System.out.println(yearEndLongTime);
		
		Set<String> validsubs = new HashSet<String>();
		validsubs.add("1");
		Set<String> default_aids_valid = new HashSet<String>();
		default_aids_valid.add("2");
		default_aids_valid.add("3");
		validsubs.addAll(default_aids_valid);
		System.out.println(validsubs);
		System.out.println(default_aids_valid);
	}
}

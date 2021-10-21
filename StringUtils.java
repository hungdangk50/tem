package com.seabank.hrsb.utils;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.seabank.hrsb.base.SJsonMapper;

public class StringUtils {

	private static final String JSON_PATTERN = "\"({0})\": \"([^\"]+)\"";
	private static final String JSON_REPLACEMENT_REGEX = "\"$1\": \"****\"";
	private static final String XML_PATTERN = "<({0})>([^\"]+)</({0})>";
	private static final String XML_REPLACEMENT_REGEX = "$1****$1";

	public static String maskJsonString(String json, String fields) {
		String retVal = "";
		if (!isEmpty(fields)) {
			String StrPattern = MessageFormat.format(JSON_PATTERN, fields);
			StringBuffer buffer = new StringBuffer();
			Pattern pattern = Pattern.compile(StrPattern);
			Matcher matcher = pattern.matcher(json);
			while (matcher.find()) {
				matcher.appendReplacement(buffer, JSON_REPLACEMENT_REGEX);
			}
			matcher.appendTail(buffer);
			retVal = buffer.toString();
		}
		return retVal;
	}

	public static String maskXMLString(String xml, String fields) {
		String retVal = "";
		if (!isEmpty(fields)) {
			String StrPattern = MessageFormat.format(XML_PATTERN, fields);
			StringBuffer buffer = new StringBuffer();
			Pattern pattern = Pattern.compile(StrPattern);
			Matcher matcher = pattern.matcher(xml);
			while (matcher.find()) {
				matcher.appendReplacement(buffer, XML_REPLACEMENT_REGEX);
			}
			matcher.appendTail(buffer);
			retVal = buffer.toString();
		}
		return retVal;
	}

	public static void main(String[] args) {
		String scn = "{\"ssn\": \"1234567890\", \"id\": \"ABC-123\", \"private\": \"someKey\"}";
		System.out.println(maskJsonString(scn, "ssn|id"));
		String xmlscn = "<a><ssn>12344</ssn><id>100</id></a>";
		System.out.println(maskXMLString(xmlscn, "ssn|id"));
	}

	public static boolean isEmpty(Object str) {
		return ((str == null) || (((String) str).trim().length() == 0));
	}

	public static boolean isNotEmpty(String str) {
		return (!(isEmpty(str)));
	}

	public static boolean isBlank(String str) {
		int strLen;
		if ((str == null) || ((strLen = str.length()) == 0)) {
			return true;
		}
		for (int i = 0; i < strLen; ++i) {
			if (!(Character.isWhitespace(str.charAt(i)))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isNotBlank(String str) {
		return (!(isBlank(str)));
	}

	/**
	 * @hung.dn2 Chuyen chuoi co dau thanh khong dau
	 * 
	 * @param s
	 * @return
	 */
	public static String unAccent(String s) {
		String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
		Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		return pattern.matcher(temp).replaceAll("").replaceAll("Ä�", "D").replaceAll("Ä‘", "d");
	}

	public static boolean checkLength(String str, int min, int max) {
		if ((str == null) || (str.length() == 0)) {
			return false;
		} else if (str.length() < min || str.length() > max) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean checkMinLength(String str, int min) {
		if ((str == null) || (str.length() == 0)) {
			return false;
		} else if (str.length() < min) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean checkMaxLength(String str, int max) {
		if ((str == null) || (str.length() == 0)) {
			return true;
		} else if (str.length() > max) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean equals(String str1, String str2) {
		return ((str1 == null) ? false : (str2 == null) ? true : str1.equals(str2));
	}

	public static boolean checkRegexStr(String str, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		return matcher.matches();
	}

	public static boolean isHasWhiteSpaceBeginEnd(String str) {
		if ((str == null) || (str.length() == 0))
			return false;
		return (str.endsWith(" ") || str.startsWith(" "));
	}

	public static boolean isHasWhiteSpace(String str) {
		if ((str == null) || (str.length() == 0))
			return false;
		return (str.contains(" "));
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		// only got here if we didn't return false
		return true;
	}

	public static boolean isDate(String timeValue, String pattem) {
		boolean isDate = false;
		SimpleDateFormat formatter = new SimpleDateFormat(pattem);
		try {
			formatter.parse(timeValue);
			isDate = true;
		} catch (ParseException e) {
			isDate = false;
		}
		return isDate;
	}

	public static Time getTimeBegin(String time) {
		Time tm = java.sql.Time.valueOf(time);
		return tm;

	}

	public static int compareTimestam(Timestamp tm1, Timestamp tm2) {
		if (tm1 != null || tm2 != null) {
			if (tm1.getTime() - tm2.getTime() == 0) {
				return 0;
			} else if (tm1.getTime() - tm2.getTime() > 0) {
				return 1;
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}

	public static Map<String, Integer> mapFromString(String varstring) {
		Map<String, Integer> mapresult = new HashMap<String, Integer>();
		String[] arrayconf = varstring.split("-");
		for (String str : arrayconf) {
			String[] tmp = str.split("=");
			mapresult.put(tmp[0].trim(), Integer.valueOf(tmp[1].trim()));
		}
		return mapresult;
	}

	public static String valueOfTimestamp(Date timestamp) {
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String result = "";
		if (timestamp != null) {
			result = dateFormat.format(new Date(timestamp.getTime()));
		}
		return result;
	}

	public static String valueOfTimestamp(Date timestamp, String format) {
		if (timestamp == null)
			return "";
		DateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(timestamp);
	}

	public static String MilisToHours(long milis) {
		String resule = "";
		resule = resule + (milis / 3600000) + " Giờ ";
		return resule;
	}

	public static Date toDate(String dateString, String format) {
		Date time = null;
		DateFormat dateFormat = new SimpleDateFormat(format);
		try {
			time = dateFormat.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return time;
	}

	public static String doubleFormat(double value) {
		String result = "";
		NumberFormat formatter = new DecimalFormat("#0.00");
		result = formatter.format(value);
		return result;
	}

	public static String doubleFormat(double value, String format) {
		String result = "";
		NumberFormat formatter = new DecimalFormat(format);
		result = formatter.format(value);
		return result;
	}

	public static String objectToJson(Object object) {
		SJsonMapper mapper = new SJsonMapper();
		String retStr = null;
		try {
			retStr = mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			retStr = e.getMessage();
		}
		return retStr;
	}

	public static String Trim(String stringToTrim, String stringToRemove) {
		String answer = "";
		if (!StringUtils.isEmpty(stringToTrim)) {
			answer = stringToTrim;
			while (answer.startsWith(stringToRemove)) {
				answer = answer.substring(stringToRemove.length());
			}
			while (answer.endsWith(stringToRemove)) {
				answer = answer.substring(0, answer.lastIndexOf(stringToRemove));
			}
		}
		return answer;
	}

	public static String standardString(String src) {
		String reVal = src.replaceAll(", Vietnam", "");
		reVal = reVal.replaceAll("Quận", "Q.");
		reVal = reVal.replaceAll("Phường", "P.");
		reVal = reVal.replaceAll("tp.", "TP.");

		return reVal;
	}

	public static String getDetailString(List<String> messege) {
		return "notification";
	}

	public static String getString(Object input) {
		String retVal = "";
		if (input != null && !isEmpty(input + "")) {
			retVal = input + "";
		}
		return retVal;
	}

	public static Integer getInteger(String input) {
		Integer retVal = -1;
		if (input != null) {
			try {
				retVal = Integer.valueOf(input);
			} catch (NumberFormatException e) {
				retVal = -1;
			}
		}
		return retVal;
	}

	public static Integer getIntegerObject(String input) {
		Integer retVal = null;
		if (input != null) {
			try {
				retVal = Integer.valueOf(input);
			} catch (NumberFormatException e) {
				retVal = null;
			}
		}
		return retVal;
	}

	public static String replaceFirst(String taget, String des, String str) {
		StringBuilder sb = new StringBuilder(str);
		if (str.contains(taget)) {
			sb.replace(str.indexOf(taget), str.indexOf(taget) + taget.length(), des);
		}
		return sb.toString();
	}

	public static String replaceLast(String source, String split) {
		if (isEmpty(split)) {
			return source;
		}
		if (source.endsWith(split)) {
			source = source.substring(0, source.length() - 1);
		}
		return source;
	}

	public static boolean isSameDay(Timestamp timeBefore, Timestamp timeAfter) {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
		String strBefor = fmt.format(timeBefore);
		String strAfter = fmt.format(timeAfter);
		return strBefor.equals(strAfter);
	}

	public static String getImgName() {
		StringBuffer name = new StringBuffer("");
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR);
		int month = now.get(Calendar.MONTH) + 1;
		int day = now.get(Calendar.DAY_OF_MONTH);
		int hour = now.get(Calendar.HOUR_OF_DAY);
		int minute = now.get(Calendar.MINUTE);
		int second = now.get(Calendar.SECOND);
		int milisecond = now.get(Calendar.MILLISECOND);
		name.append(year).append(month).append(day).append("_").append(hour).append(minute).append(second)
				.append(milisecond);
		return name.toString();
	}

	public static boolean isChange(String curentValue, String newValue) {
		boolean change = true;
		if ((StringUtils.isEmpty(curentValue) && StringUtils.isEmpty(newValue))
				|| (!StringUtils.isEmpty(curentValue) && curentValue.equals(newValue))
				|| (!StringUtils.isEmpty(newValue) && newValue.equals(curentValue))) {
			change = false;
		}
		return change;
	}

	public static String object2JsonString(Object object) {
		String retVal = "";
		if (object != null) {
			try {
				SJsonMapper mapper = new SJsonMapper();
				retVal = mapper.writeValueAsString(object);
			} catch (Exception e) {
				retVal = "";
			}
		}
		return retVal;

	}

	public static String getErrorDetail(String err) {
		String retVal = "";
		if (!isEmpty(err)) {
			int start = err.indexOf("#");
			int end = err.lastIndexOf("#");
			if (end > start) {
				retVal = err.substring(start + 1, end);
			} else {
				retVal = err;
			}
		}
		return retVal;
	}

	public static String object2json(Object obj) {
		String retVal = "";
		try {
			SJsonMapper mapper = new SJsonMapper();
			retVal = mapper.writeValueAsString(obj);
		} catch (Exception e) {
			retVal = "";
		}
		return retVal;
	};
}

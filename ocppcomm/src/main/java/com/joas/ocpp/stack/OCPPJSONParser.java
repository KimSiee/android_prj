/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:39
 */

package com.joas.ocpp.stack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.joas.utils.LogWrapper;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Description of OCPPJSONParser.
 * 
 * @author user
 */
public class OCPPJSONParser extends OCPPMessageParser {

	public static final String TAG = "OCPPJSONParser";
	public static final String OCPP_JSON_CLASS_PACKAGE = "com.joas.ocpp.msg";

	private boolean hasLongDateFormat = false;
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private static final String DATE_FORMAT_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
//	private static final String DATE_FORMAT_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ";		//edit by si.
	private static final int DATE_FORMAT_WITH_MS_LENGTH = 24;
//	private static final int DATE_FORMAT_WITH_MS_LENGTH = 29;				//edit by si.


	Gson gsonSerialize;
	Gson gsonDeSerialize;

	/**
	 * The constructor.
	 */
	public OCPPJSONParser() {
		super();

		GsonBuilder builderSerialize = new GsonBuilder();
		builderSerialize.registerTypeAdapter(GregorianCalendar.class, new CalendarSerializer());
		gsonSerialize = builderSerialize.create();

		GsonBuilder builderDeserialize = new GsonBuilder();
		builderDeserialize.registerTypeAdapter(Calendar.class, new CalendarDeserializer());
		gsonDeSerialize = builderDeserialize.create();
	}

	@Override
	public String Serialize(String action, Object msg) {
		String retMsg = null;

		try {
			retMsg = gsonSerialize.toJson(msg, Class.forName(OCPP_JSON_CLASS_PACKAGE + "." + action));
		}
		catch ( Exception e ) {
			LogWrapper.e(TAG, "Serialize Ex:"+e.toString());
		}

		return retMsg;
	}

	@Override
	public Object DeSerialize(String action, String jsonstr) {
		Object ret = null;

		try {
			ret = gsonDeSerialize.fromJson(jsonstr, Class.forName(OCPP_JSON_CLASS_PACKAGE + "." + action));
		}
		catch ( Exception e ) {
			LogWrapper.e(TAG, "Serialize Ex:"+e.toString());
		}

		return ret;
	}

	private class CalendarSerializer implements JsonSerializer<Calendar> {
		public JsonElement serialize(Calendar src, Type typeOfSrc, JsonSerializationContext context) {
			SimpleDateFormat formatter = new SimpleDateFormat(hasLongDateFormat ? DATE_FORMAT_WITH_MS : DATE_FORMAT);
			formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
			return new JsonPrimitive(formatter.format(src.getTime()));
		}
	}

	private class CalendarDeserializer implements JsonDeserializer<Calendar> {
		public Calendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				String dateString = json.getAsJsonPrimitive().getAsString();

				int dlen = dateString.length();
				hasLongDateFormat = dlen == DATE_FORMAT_WITH_MS_LENGTH;
				SimpleDateFormat formatter = new SimpleDateFormat(hasLongDateFormat ? DATE_FORMAT_WITH_MS : DATE_FORMAT);

				formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
				Calendar calendar = Calendar.getInstance();
				Date date = formatter.parse(dateString);
				calendar.setTime(date);
				return calendar;
			} catch (ParseException e) {
				throw new JsonParseException(e);
			}
		}
	}
}

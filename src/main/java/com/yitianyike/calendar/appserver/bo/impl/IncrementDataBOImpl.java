/**
 * 
 */
package com.yitianyike.calendar.appserver.bo.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yitianyike.calendar.appserver.bo.IncrementDataBO;
import com.yitianyike.calendar.appserver.common.EnumConstants;
import com.yitianyike.calendar.appserver.dao.DataDAO;
import com.yitianyike.calendar.appserver.dao.RedisDAO;
import com.yitianyike.calendar.appserver.dao.UserDAO;
import com.yitianyike.calendar.appserver.model.DataInfo;
import com.yitianyike.calendar.appserver.model.response.AppResponse;
import com.yitianyike.calendar.appserver.service.DataAccessFactory;
import com.yitianyike.calendar.appserver.util.CalendarUtil;
import com.yitianyike.calendar.appserver.util.DateUtil;
import com.yitianyike.calendar.appserver.util.ParameterValidation;

/**
 * @author xujinbo
 *
 */
@Component("incrementDataBO")
public class IncrementDataBOImpl implements IncrementDataBO {

	private static Logger logger = Logger.getLogger(IncrementDataBOImpl.class.getName());

	private DataDAO dataDAO = (DataDAO) DataAccessFactory.dataHolder().get("dataDAO");
	private RedisDAO redisDAO = (RedisDAO) DataAccessFactory.dataHolder().get("redisDAO");

	@Override
	public AppResponse process(Map<String, String> map, String content, long requestIndex) {
		String token = map.get("token");
		String aids = map.get("aids");
		String dateTime = map.get("datetime");

		AppResponse appResponse = new AppResponse();
		appResponse.setCode(EnumConstants.CALENDAR_SUCCESS_200);

		do {
			if (!ParameterValidation.validationUidAndToken(token)
					|| !ParameterValidation.validationAidsAndAidAndType(aids)
					|| !ParameterValidation.validationDateTime(dateTime)) {
				appResponse.setCode(EnumConstants.CALENDAR_ERROR_400);
				logger.error(requestIndex + " : param error, return 400");
				break;
			}
			// String check = aids.replaceAll(" ", "") + dateTime;
			// boolean result = check.matches("[0-9]+");
			// if (result == false) {
			// appResponse.setCode(EnumConstants.CALENDAR_ERROR_400);
			// logger.error(requestIndex + " : param error 2, return 400");
			// break;
			// }

			int redisIndex = CalendarUtil.getRedisIndex(token);
			String redisTemplate = "redisTemplate" + redisIndex;
			redisDAO.setRedisTemplate(redisTemplate);

			Map<String, String> tokenMap = redisDAO.hGetAll(token);

			String uid = tokenMap.get("uid");
			String valueString = "";
			if (uid != null) {

				Map<String, String> uidMap = redisDAO.hGetAll(uid);
//				if (uidMap == null || uidMap.isEmpty() || !uidMap.get("token").equalsIgnoreCase(token)) {
//					appResponse.setCode(EnumConstants.CALENDAR_ERROR_401);
//					if (uidMap.get("token") == null) {
//						logger.info(requestIndex + " : token exist, but current token : " + token
//								+ " is expired, return 401");
//					} else {
//						logger.info(requestIndex + " : token exist, but current token : " + token
//								+ " is invalid, new token : " + uidMap.get("token") + ", return 401");
//					}
//					redisDAO.delKey(token);
//					redisDAO.clearRedisTemplate();
//					break;
//				}

				String[] arr = aids.split(" ");
				List<String> list = java.util.Arrays.asList(arr);
				String currentDate = DateUtil.getCurrentDate();
				for (String aid : list) {
					String aidKey = uidMap.get("channel") + "-" + uidMap.get("version") + "-" + aid;
					String aidKeySet = uidMap.get("channel") + "-" + uidMap.get("version") + "-" + aid + "-zset";

					if (currentDate.equalsIgnoreCase(dateTime)) {
						String tempString = redisDAO.hGetValue(aidKey, dateTime);
						if (tempString != null) {
							if (tempString.charAt(0) == '[' && tempString.charAt(tempString.length() - 1) == ']') {
								tempString = tempString.substring(1, tempString.length() - 1);
							}
							valueString += tempString + ",";
						} else {
							if (redisDAO.keyExist(aidKey) == 0) {
								logger.info(requestIndex + " : hash key = " + aidKey + ", no exists!");
								List<DataInfo> dataInfos = dataDAO.getDataInfos(aidKey);
								if (dataInfos != null && dataInfos.size() > 0) {
									Map<String, String> dataMap = new HashMap<String, String>();
									for (DataInfo info : dataInfos) {
										dataMap.put(info.getField(), info.getCacheValue());
										if (dateTime.compareToIgnoreCase(info.getField()) <= 0
												&& currentDate.compareToIgnoreCase(info.getField()) >= 0) {
											tempString = info.getCacheValue();
											if (tempString.charAt(0) == '['
													&& tempString.charAt(tempString.length() - 1) == ']') {
												tempString = tempString.substring(1, tempString.length() - 1);
											}
											valueString += tempString + ",";
										}
									}
									redisDAO.hMsetAndZadd(aidKey, dataMap);
								} else {
									logger.info(requestIndex + " : aid key = " + aidKey
											+ " card data no exist in data cache");
								}
							} else {
								logger.info(requestIndex + " : aid key = " + aidKey + " redis key exist, but no data");
							}
						}
					} else {
						List<String> fieldList = redisDAO.zRangeByScoreWithScores(aidKeySet, Integer.parseInt(dateTime),
								Integer.parseInt(currentDate));

						if (fieldList != null && fieldList.size() > 0) {
							List<String> valueList = redisDAO.hMgetValue(aidKey, fieldList);
							if (valueList != null && valueList.size() > 0) {
								for (String tempString : valueList) {
									if (tempString.charAt(0) == '['
											&& tempString.charAt(tempString.length() - 1) == ']') {
										tempString = tempString.substring(1, tempString.length() - 1);
									}
									valueString += tempString + ",";
								}
							}
						} else {
							if (redisDAO.keyExist(aidKeySet) == 0) {
								logger.info(requestIndex + " zset key = " + aidKeySet + ", no exists!");
								List<DataInfo> dataInfos = dataDAO.getDataInfos(aidKey);
								if (dataInfos != null && dataInfos.size() > 0) {
									Map<String, String> dataMap = new HashMap<String, String>();
									for (DataInfo info : dataInfos) {
										dataMap.put(info.getField(), info.getCacheValue());
										if (dateTime.compareToIgnoreCase(info.getField()) <= 0
												&& currentDate.compareToIgnoreCase(info.getField()) >= 0) {
											String tempString = info.getCacheValue();
											if (tempString.charAt(0) == '['
													&& tempString.charAt(tempString.length() - 1) == ']') {
												tempString = tempString.substring(1, tempString.length() - 1);
											}
											valueString += tempString + ",";
										}
									}
									redisDAO.hMsetAndZadd(aidKey, dataMap);
								} else {
									logger.info(requestIndex + " : aid key = " + aidKey
											+ " card data no exist in data cache");
								}
							} else {
								logger.info(requestIndex + " : aid key set = " + aidKeySet
										+ " redis key exist, but no data");
							}
						}
					}
				}
				if (valueString.length() > 0) {
					valueString = "[" + valueString.substring(0, valueString.length() - 1) + "]";
				} else {
					valueString = "[]";
				}
				appResponse.setRespContent(valueString);

			} else {
				appResponse.setCode(EnumConstants.CALENDAR_ERROR_401);
				logger.error(requestIndex + " : token no exist, return 401");
			}
			redisDAO.clearRedisTemplate();

		} while (false);

		return appResponse;
	}

	public AppResponse getSportsData(Map<String, String> map, String content, long requestIndex) {
		String token = map.get("token");
		String aid = map.get("aid");
		AppResponse appResponse = new AppResponse();
		appResponse.setCode(EnumConstants.CALENDAR_SUCCESS_200);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // 获取本周一的日期
		String week = df.format(cal.getTime());
		String currentDate = DateUtil.getCurrentDate();
		Integer dateTime = Integer.parseInt(currentDate) - 2;
		do {
			if (token == null || aid == null || token.length() < 8 || aid.length() == 0) {
				appResponse.setCode(EnumConstants.CALENDAR_ERROR_400);
				logger.error(requestIndex + " : param error, return 400");
				break;
			}

			int redisIndex = CalendarUtil.getRedisIndex(token);
			String redisTemplate = "redisTemplate" + redisIndex;
			redisDAO.setRedisTemplate(redisTemplate);
			Map<String, String> tokenMap = redisDAO.hGetAll(token);
			String uid = tokenMap.get("uid");
			if (uid != null) {

				Map<String, String> uidMap = redisDAO.hGetAll(uid);
				if (uidMap == null || uidMap.isEmpty() || !uidMap.get("token").equalsIgnoreCase(token)) {
					appResponse.setCode(EnumConstants.CALENDAR_ERROR_401);
					if (uidMap.get("token") == null) {
						logger.info(requestIndex + " : token exist, but current token : " + token
								+ " is expired, return 401");
					} else {
						logger.info(requestIndex + " : token exist, but current token : " + token
								+ " is invalid, new token : " + uidMap.get("token") + ", return 401");
					}
					redisDAO.delKey(token);
					redisDAO.clearRedisTemplate();
					break;
				}
				String valueString = "";
				String aidKey = uidMap.get("channel") + "-" + uidMap.get("version") + "-" + aid;
				String detailsKey = uidMap.get("channel") + "-" + uidMap.get("version") + "-" + "details" + "-" + aid;
				String hGetValue = redisDAO.hGetValue(detailsKey, detailsKey);
				if (hGetValue == null) {
					hGetValue = redisDAO.hGetValue(aidKey, week);
					valueString = hGetValue;
					appResponse.setRespContent(valueString);
					break;
				}
				valueString = hGetValue;
				String one = valueString.substring(0, 1);

				if (one != "[" && !"[".equals(one)) {
					valueString = "[" + valueString + "]";
				}
				/*
				 * List<String> fieldList =
				 * redisDAO.zRangeByScoreWithScores(aidKeySet, dateTime,
				 * Integer.parseInt(currentDate)); if(fieldList.size()<=1){
				 * hGetValue = redisDAO.hGetValue(aidKey,week); valueString =
				 * hGetValue; appResponse.setRespContent(valueString); break; }
				 */
				/*
				 * if(fieldList != null && fieldList.size() > 0){ List<String>
				 * valueList = redisDAO.hMgetValue(aidKey, fieldList);
				 * if(valueList != null && valueList.size() > 0){ for(String
				 * tempString: valueList){ if(tempString.charAt(0) == '[' &&
				 * tempString.charAt(tempString.length()-1) == ']'){ tempString
				 * = tempString.substring(1, tempString.length()-1); }
				 * valueString += tempString + ","; } } }else{
				 * if(redisDAO.keyExist(aidKeySet) == 0){
				 * logger.info(requestIndex + " zset key = " + aidKeySet +
				 * ", no exists!"); List<DataInfo> dataInfos =
				 * dataDAO.getDataInfos(aidKey); if(dataInfos != null &&
				 * dataInfos.size() > 0){ Map<String, String> dataMap = new
				 * HashMap<String, String>(); for(DataInfo info: dataInfos){
				 * dataMap.put(info.getField(), info.getCacheValue());
				 * if(dateTime.toString().compareToIgnoreCase(info.getField())
				 * <= 0 && currentDate.compareToIgnoreCase(info.getField()) >=
				 * 0){ String tempString = info.getCacheValue();
				 * if(tempString.charAt(0) == '[' &&
				 * tempString.charAt(tempString.length()-1) == ']'){ tempString
				 * = tempString.substring(1, tempString.length()-1); }
				 * valueString += tempString + ","; } }
				 * redisDAO.hMsetAndZadd(aidKey, dataMap); }else{
				 * logger.info(requestIndex + " : aid key = " + aidKey +
				 * " card data no exist in data cache"); } }else{
				 * logger.info(requestIndex + " : aid key set = " + aidKeySet +
				 * " redis key exist, but no data"); } }
				 */
				appResponse.setRespContent(valueString);
			} else {
				appResponse.setCode(EnumConstants.CALENDAR_ERROR_401);
				logger.error(requestIndex + " : token no exist, return 401");
			}
		} while (false);
		return appResponse;
	}

}

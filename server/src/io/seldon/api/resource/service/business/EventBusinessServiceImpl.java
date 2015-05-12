/*
 * Seldon -- open source prediction engine
 * =======================================
 * Copyright 2011-2015 Seldon Technologies Ltd and Rummble Ltd (http://www.seldon.io/)
 *
 **********************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at       
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ********************************************************************************************** 
*/
package io.seldon.api.resource.service.business;

import io.seldon.api.APIException;
import io.seldon.api.Constants;
import io.seldon.api.logging.EventLogger;
import io.seldon.api.resource.ConsumerBean;
import io.seldon.api.resource.ErrorBean;
import io.seldon.api.resource.EventBean;
import io.seldon.api.resource.ResourceBean;
import io.seldon.api.service.ApiLoggerServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class EventBusinessServiceImpl implements EventBusinessService {

	public static final String JSON_KEY = "json";
	
	private boolean allowedKey(String key)
	{
		return (!(Constants.CONSUMER_KEY.equals(key) || 
					Constants.CONSUMER_SECRET.equals(key) ||
					Constants.OAUTH_TOKEN.equals(key) ||
					"jsonpCallback".equals(key)));
		
	}
	
	private ResourceBean getValidatedJsonResource(String jsonRaw)
	{
		ResourceBean responseBean;
		ObjectMapper mapper = new ObjectMapper();
	    JsonFactory factory = mapper.getJsonFactory();
	    try
	    {
	    	JsonParser parser = factory.createJsonParser(jsonRaw);
	    	JsonNode actualObj = mapper.readTree(parser);
	    	String json = actualObj.toString();
			EventLogger.log(json);
			responseBean = new EventBean(json);
	    } catch (IOException e) {
	    	ApiLoggerServer.log(this, e);
			APIException apiEx = new APIException(APIException.INCORRECT_FIELD);
			responseBean = new ErrorBean(apiEx);
		}
	    return responseBean;
	}
	
	@Override
	public ResourceBean addEvent(ConsumerBean consumerBean,Map<String, String[]> parameters) {
		ResourceBean responseBean;
		Map<String,String> keyVals = new HashMap<String,String>();
		if (parameters.containsKey(JSON_KEY))
		{
			String jsonRaw = parameters.get(JSON_KEY)[0];
			responseBean = getValidatedJsonResource(jsonRaw);
		}
		else
		{
			for(Map.Entry<String, String[]> reqMapEntry : parameters.entrySet())
			{
				if (reqMapEntry.getValue().length == 1 && allowedKey(reqMapEntry.getKey()))
				{
					keyVals.put(reqMapEntry.getKey(), reqMapEntry.getValue()[0]);
				}
			}
			ObjectMapper mapper = new ObjectMapper();
			try {
				String json = mapper.writeValueAsString(keyVals);
				EventLogger.log(json);
				responseBean = new EventBean(json);
			} catch (IOException e) {
				ApiLoggerServer.log(this, e);
				APIException apiEx = new APIException(APIException.INCORRECT_FIELD);
				responseBean = new ErrorBean(apiEx);
			}
		}
		return responseBean;
	}

	@Override
	public ResourceBean addEvent(ConsumerBean consumerBean, String event) {
		return getValidatedJsonResource(event);
	}

}

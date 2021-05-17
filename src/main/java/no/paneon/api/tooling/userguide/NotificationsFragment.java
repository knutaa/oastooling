package no.paneon.api.tooling.userguide;


import org.json.JSONObject;

import no.paneon.api.model.APIModel;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Utils;

import no.paneon.api.tooling.Args;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NotificationsFragment {

	static final Logger LOG = LogManager.getLogger(NotificationsFragment.class);
	
	private static final String JSON_REPRESENTATIONS = "jsonRepresentations";
	private static final String JSON_TABLE = "jsonTable";
		
	private static final String NOTIFICATION_TAG = "First Notification";

	private static final String EVENT_IMAGE = "userguide::eventsubscription";
		
	private static final String EVENT_TEMPLATE = "template";

	private static final String NEWLINE = "\n";
	
	Args.UserGuide args;
	
	UserGuideGenerator generator;
	UserGuideData userGuideData;

	public NotificationsFragment(UserGuideGenerator generator) {	
		this.generator = generator;
		
		this.args = generator.args;
		this.userGuideData = generator.userGuideData;
		
	}

	
	public void process() {

		JSONObject config = Config.getConfig("userguide::notificationFragments");
				
		List<String> resources = generator.getResources();
		List<String> notifications = APIModel.getAllNotifications();
				
		this.userGuideData.numberOfNotifications = notifications.size();
		
		for(String resource : resources) {
			notifications = APIModel.getNotificationsByResource(resource, null);
			
			if(notifications.isEmpty()) continue;
			
			List<UserGuideData.NotificationData> notifData = new LinkedList<>();

			for(String notification : notifications) {
				notifData.add( getNotificationDetailsForNotification(config, resource, notification) );
			}
			
			userGuideData.resources.get(resource).notifications = notifData;
	
		}				
		
	}

	private UserGuideData.NotificationData getNotificationDetailsForNotification(JSONObject config, String resource, String notification) {
		UserGuideData.NotificationData res = userGuideData.new NotificationData();

		String[] wordSplitEvent = notification.split("(?=[A-Z])");
		String formattedEvent = Arrays.asList(wordSplitEvent).stream().collect(Collectors.joining(" "));
						
		res.notification = notification;
		res.notificationLabel = formattedEvent;
				
		config = Config.getConfig(config, JSON_REPRESENTATIONS);
					
		String template = Config.getList(config, EVENT_TEMPLATE).stream().collect(Collectors.joining(NEWLINE));
				
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		
		Map<String,String> variables = new HashMap<>();
		variables.put("TIMESTAMP", strDate);
		variables.put("EVENT",notification);
		variables.put("PROPERTY", Utils.lowerCaseFirst(resource));
		variables.put("RESOURCENAME",resource);

		String json = Utils.replaceVariables(template, variables);
			
		res.sample = json;

		return res;
	}
	
	
}

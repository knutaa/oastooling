package no.paneon.api.tooling.userguide;

import org.json.JSONObject;

import jdk.internal.org.jline.utils.Log;
import no.paneon.api.model.APIModel;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Timestamp;
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

		List<String> resources = generator.getResources();
		List<String> notifications = APIModel.getAllNotifications();

		LOG.debug("notifications:: getAllNotifications={}", notifications);

		this.userGuideData.numberOfNotifications = notifications.size();

		for (String resource : resources) {
			notifications = APIModel.getNotificationsByResource(resource, null);

			LOG.debug("notifications:: getNotificationsByResource={}", notifications);

			if (notifications.isEmpty())
				continue;

			List<UserGuideData.NotificationData> notifData = new LinkedList<>();

			for (String notification : notifications) {
				
				JSONObject notificationConfig = APIModel.getNotificationFromRules(resource, notification);
				
				LOG.debug("notifications:: resource={} notification={} notificationConfig={}", resource, notification, notificationConfig.toString(2));

				if(!notificationConfig.isEmpty())
					notifData.add(getNotificationDetailsForNotification(notificationConfig, resource, notification));
				else {
					// Out.debug("... using default notification template for {}", notification);
				}
			}

			userGuideData.resources.get(resource).notifications = notifData;

			LOG.debug("notifications:: resource={} notifications.size={}", resource, notifData.size());
			
		}

		Timestamp.timeStamp("finished notification fragment");

	}

	private UserGuideData.NotificationData getNotificationDetailsForNotification(JSONObject config, String resource,
			String notification) {

		LOG.debug("getNotificationDetailsForNotification: resource={} notification={} config={}",  
				resource, notification, config);

		UserGuideData.NotificationData res = userGuideData.new NotificationData();

		String[] wordSplitEvent = notification.split("(?=[A-Z])");
		String formattedEvent = Arrays.asList(wordSplitEvent).stream().collect(Collectors.joining(" "));

		res.notification = notification;
		res.notificationLabel = formattedEvent;
		res.notificationLabelShort = notification;

		if(config.has("request")) {
			res.sample = getEventSample(notification, resource, config);
			res.message = config.optString("description");
		} else {
			Out.debug("... using default notificiation template for {}", notification);
			config = Config.getConfig("userguide::notificationFragments");
			res.sample = getDefaultEventSample(config, notification, resource);
		}
		
		return res;
	}

	private String getDefaultEventSample(JSONObject config, String notification, String resource) {

		config = Config.getConfig(config, JSON_REPRESENTATIONS);

		String template = Config.getList(config, EVENT_TEMPLATE).stream().collect(Collectors.joining(NEWLINE));

		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Date now = new Date();
		String strDate = sdfDate.format(now);

		Map<String, String> variables = new HashMap<>();
		variables.put("TIMESTAMP", strDate);
		variables.put("EVENT", notification);
		variables.put("PROPERTY", Utils.lowerCaseFirst(resource));
		variables.put("RESOURCENAME", resource);

		String json = Utils.replaceVariables(template, variables);

		return json;

	}

	private String getEventSample(String notification, String resource, JSONObject example) {
		StringBuilder res = new StringBuilder();

		String contentType = example.optString("content-type");
		contentType = !contentType.isEmpty() ? contentType : "application/json";

		res.append("Content-Type: " + contentType);

		LOG.debug("example: {}", example);

		String requestPayload = Samples.readPayload(this.args.workingDirectory, example, "request");
		if (!requestPayload.isEmpty()) {
			res.append(NEWLINE);
			res.append(requestPayload);
		}

		return res.toString();
	}


}

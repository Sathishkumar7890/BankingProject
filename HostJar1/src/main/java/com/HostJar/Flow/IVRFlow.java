package com.HostJar.Flow;

import org.json.JSONArray;
import org.json.JSONObject;

import com.HostJar.LoadValues.Load;

public class IVRFlow {
	
	public static JSONObject getNodeByDescription(String nodeName) {

	    JSONObject flowMap = Load.loadJson();
	    JSONArray flowArray = flowMap.getJSONArray("flow");

	    for (int i = 0; i < flowArray.length(); i++) {
	        JSONObject node = flowArray.getJSONObject(i);
	        String description = node.getString("Description");

	        if (nodeName.equalsIgnoreCase(description)) {

	            // Decide type
	            String nodeType;
	            if (description.toUpperCase().contains("_MENU")) {
	                nodeType = "MenuNode";
	            } else {
	                nodeType = "Announce";
	            }

	            // Add it into JSON
	            node.put("nodeType", nodeType);

	            return node; // return JSON + nodeType
	        }
	    }

	    return null;
	}
	
	public static void main(String[] args) {
		
		JSONObject NexNode = IVRFlow.getNodeByDescription("GENERAL_MESSAGE");
		
		System.out.println("-----------------------------------------------------------------------");
		
		System.out.println(NexNode);
		
		System.out.println(NexNode.get("nextNode"));
	}
}

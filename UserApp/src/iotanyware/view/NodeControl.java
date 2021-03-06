package iotanyware.view;

import java.io.IOException;
import java.io.StringWriter;

import org.json.simple.JSONObject;

import iotanyware.model.ModelSubscribe;

public class NodeControl implements State {

	View view;
	
	String validStr;
	String topicId;
	String saName;
	
	public NodeControl(View view) {
		// TODO Auto-generated constructor stub
		this.view = view;
	}

	@Override
	public void enterNumber(int number) {
		// TODO Auto-generated method stub
		if( number == 0) {
			view.setSaIndex(0);
			view.setStatus(view.getNodeList());
		}
		if( number == 1) {
			view.setSaIndex(0);
			view.setStatus(view.getNodeStatus());
		}
		else {
			if(saName.matches("autoalarmon") || saName.matches("autolightoff")) {
				if(topicId.matches("alarm on")) {
					System.out.println("alram is on!!!!!!!!!!!!!!!!!!!!!!!!");
					return;
				}
				
				String topic = topicId + "/control";
				
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("publisher", view.getUserEmail());
				jsonObj.put("name", saName);
				jsonObj.put("value", String.valueOf(number));
				
				StringWriter toStr = new StringWriter();
				try {
					jsonObj.writeJSONString(toStr);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				String payload = jsonObj.toString();
				view.publishMessage(topic, payload, 0);
			}
		}
	}

	@Override
	public void enterString(String string) {
		// TODO Auto-generated method stub
		
		//if you need to check the input string, please compare to validStr
		
		if(topicId.matches("alarm on")) {
			System.out.println("alram is on!!!!!!!!!!!!!!!!!!!!!!!!");
			return;
		}
		
		String topic = topicId + "/control";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("publisher", view.getUserEmail());
		jsonObj.put("name", saName);
		jsonObj.put("value", string);
		
		StringWriter toStr = new StringWriter();
		try {
			jsonObj.writeJSONString(toStr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String payload = jsonObj.toString();
		view.publishMessage(topic, payload, 0);
	}

	@Override
	public void updateState(ModelSubscribe model) {		
		// TODO Auto-generated method stub
		view.textPane.setText("");
		
		topicId = "";
		
        String initString[] =
            { "Node Control of ",
              " 0. Go SA node list.",
              " 1. Go SA node status.",
              ""};
        
        //Find control node.
        int controlNodeIdx = view.getSaIndex();
        int findControlTarget = -1;
        for(int i=0, k=-1; i<model.getSensorActuatorNum(view.getNodeIndex()); i++) {
    		findControlTarget++;
        	if(model.getSensorActuatorCanControl(view.getNodeIndex(), i)) {
        		k++;
        		if(k == controlNodeIdx) {
        	        System.out.println("real target sa index = " + findControlTarget);
        			break;
        		}
        	}
        }
        
        System.out.println("real target sa index = " + findControlTarget);
        
        //Error Node index is over!!!
        if(findControlTarget < 0 || findControlTarget >= model.getSensorActuatorNum(view.getNodeIndex())) {
            for (int i = 1; i < initString.length; i ++) {
            	view.textPane.append(initString[i] + view.newline);
            }
            view.textPane.append(""+ view.newline);
            view.textPane.append(""+ view.newline);
        	view.textPane.append("Oops, select number is not valid to control!!!"+ view.newline);
        	return;
        }
        
        //Basic String
        view.textPane.append(initString[0] + model.getNodeName(view.getNodeIndex()) + view.newline);
        for (int i = 1; i < initString.length; i ++) {
        	view.textPane.append(initString[i] + view.newline);
        }
        
        //Show the current status        
        view.textPane.append( "current status : " + 
        						(model.getSensorActuatorName(view.getNodeIndex(), findControlTarget)) + " = " +
        						(model.getSensorActuatorValue(view.getNodeIndex(), findControlTarget)) +
        						view.newline);  
        
        view.textPane.append(view.newline);  
        
        int alarmIdx = model.findSensorActuatorIndex("alarm", view.getNodeIndex());
        if((alarmIdx != findControlTarget) && (model.getSensorActuatorValue(view.getNodeIndex(), alarmIdx).matches("on")) ) {
        	view.textPane.append( "\n\nPlease off the alarm  first!!!" + view.newline);
        	topicId = "alarm on";
        }
        else {
	        //show the how to input.
	        topicId = "/sanode/" + model.getNodeId(view.getNodeIndex());
	        saName = model.getSensorActuatorName(view.getNodeIndex(), findControlTarget);
			validStr = model.getSensorActuatorProfile(view.getNodeIndex(), findControlTarget);
	        view.textPane.append( "Is new value  " + validStr + "?" + view.newline);
	        
	        view.textPane.append("\nIf OK, current status will be changed new one" + view.newline);
        }
	}
}

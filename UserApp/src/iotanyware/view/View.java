package iotanyware.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyListener;
import java.util.Observable;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import iotanyware.model.ModelSubscribe;
import iotanyware.view.View;

public class View extends JFrame implements java.util.Observer{
    	
	MqttClient pubClient;
    
    private WelcomeState welcome;
    private NodeStatus status;
    private NodeControl control;
    private NodeList nodeList;
    private Notification notification;
    private MakeAccount makeAccout;
    private NodeRegister register;
    private Configure configure;
    private LogView logview;
    private SharingUser sharingUser;
    
    private int nodeIndex;
    private int saIndex;
    private String username;
    private String userEmail;
    
    State state;
    
	JTextArea textPane;
    JTextPane changeLog;
    String newline = "\n";
         
    public View() {
        super("IoT Anyware");
 
        //Create the text pane and configure it.
        textPane = new JTextArea(5, 30);
        textPane.setCaretPosition(0);
        textPane.setMargin(new Insets(5,5,5,5));
        
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(1200, 900));
        textPane.setEditable(false);
        
        //Font font = new Font("Lucida Sans", Font.BOLD,  15);
        Font font = new Font("Consolas", Font.BOLD,  35);
        textPane.setFont(font);
 
        //Create the text area for the status log and configure it.
        changeLog = new JTextPane();
        changeLog.setEditable(true);
        changeLog.setFont(font);
 
        //Create a split pane for the change log and the text area.
        JScrollPane scrollPaneForLog = new JScrollPane(changeLog);
        JSplitPane splitPane = new JSplitPane(
                                       JSplitPane.VERTICAL_SPLIT,
                                       scrollPane, scrollPaneForLog);
        splitPane.setOneTouchExpandable(true);
  
        //Add the components.
        getContentPane().add(splitPane, BorderLayout.CENTER);

 
        //Put the initial text into the text pane.
        initDocument();
        textPane.setCaretPosition(0);
        
        welcome = new WelcomeState(this);
        status = new NodeStatus(this);
        control = new NodeControl(this);
        nodeList = new NodeList(this);
        notification = new Notification(this);
        makeAccout = new MakeAccount(this);
        register = new NodeRegister(this);
        configure = new Configure(this);
        logview = new LogView(this);
        sharingUser = new SharingUser(this);
        
        state = welcome;
    }
    
    public void setUserName(String name) {
    	username = name;
    }
    
    public String getUserName() {
    	return username;
    }
    
    public void clearInputText() {
    	changeLog.setText("");
    }
    
    public void setNodeIndex(int index) {
    	nodeIndex = index;
    }
    
    public int getNodeIndex() {
    	return nodeIndex;
    }
    
    public void setSaIndex(int index) {
    	saIndex = index;
    }
    
    public int getSaIndex() {
    	return saIndex;
    }    

    protected void initDocument() {
        String initString[] =
                { "Welcome to IoT Anyware.",
                  "Please select the number what you want to do.",
                  "",
                  "1. Login (Enter like \"id/password\")",
                  "2. Make Account\n"};
 
        for (int i = 0; i < initString.length; i ++) {
        	textPane.append(initString[i] + newline);
        }        
    }
    
	public void addController(KeyListener controller){
		System.out.println("View : adding controller");
		changeLog.addKeyListener(controller);
	}
	
	public String getInputString() {
		return changeLog.getText();
	}
	
	public void enterNumber(int number) {
		state.enterNumber(number);
	}
	
	public void enterString(String string) {
		state.enterString(string);
	}
	
	public void setStatus(State state) {
		this.state = state;
	}
	
	public State getStatus() {
		return this.state;
	}
	
	public State getConfigureState() {
		return configure;
	}
	
	public State getLogViewState() {
		return logview;
	}
	
    public State getWelcomeState() {
    	return welcome;
    }
    public State getNodeStatus() {
    	return status;
    }
    
    public State getNodeControl() {
    	return control;
    }

    public State getNodeList() {
    	return nodeList;
    }

    public State getNotification() {
    	return notification;
    }

    public State getMakeAccount() {
    	return makeAccout;
    }
    
    public State getNodeRegister() {
    	return register;
    }
    
    public void setMqttClientSocket(MqttClient client) {
    	pubClient = client;
    }
    
    public void addText(String str) {
    	textPane.append(str + newline);
    }
    
    public void addSubTopic(String topic){
    	synchronized(pubClient) {
			try{			
				System.out.println("view.addSubscribeTopic = " + topic);
					pubClient.subscribe(topic);
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    public void publishMessage(String topic, String payload, int qos) {
    	synchronized(pubClient) {
	    	System.out.println("Publishing topic  : " + topic);
	    	System.out.println("Publishing message: " + payload);
	    	
	    	int qosVale = 2;
	    	if( qos >= 0 && qos <= 2) {
	    		qosVale = qos;
	    	}
	    	
	    	try {
		        MqttMessage message = new MqttMessage(payload.getBytes());
		        message.setQos(qosVale);
		        pubClient.publish(topic, message);
		        System.out.println("Message published");
	    	} catch(MqttException me) {
	            System.out.println("reason " + me.getReasonCode());
	            System.out.println("msg    " + me.getMessage());
	            System.out.println("loc    " + me.getLocalizedMessage());
	            System.out.println("cause  " + me.getCause());
	            System.out.println("excep  " + me);
	            me.printStackTrace();
	        }
    	}
    }
        
	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		System.out.println("Node Status updated!!!");
		//synchronized(this) {
			state.updateState((ModelSubscribe)o);
		//}
	}

	public String getUserEmail() {
		return userEmail;
	}
	public void setUserEmail(String string) {
		// TODO Auto-generated method stub
		userEmail = string;
	}

	public State getSharingState() {
		return sharingUser;
	}
}
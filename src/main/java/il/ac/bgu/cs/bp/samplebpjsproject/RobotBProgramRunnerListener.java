package il.ac.bgu.cs.bp.samplebpjsproject;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.BProgramRunnerListener;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SafetyViolationTag;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class RobotBProgramRunnerListener implements BProgramRunnerListener {

    private RobotSensorsData robotData = new RobotSensorsData();
    private Channel channel;
    private final String QUEUE_NAME = "Cafe";

    RobotBProgramRunnerListener() throws IOException, TimeoutException {
//        openQueue();
    }

    @Override
    public void starting(BProgram bp) { }

    @Override
    public void started(BProgram bp) { }

    @Override
    public void ended(BProgram bp) { }

    @Override
    public void assertionFailed(BProgram bProgram, SafetyViolationTag safetyViolationTag) { }

    @Override
    public void halted(BProgram bp) { }

    @Override
    public void eventSelected(BProgram bp, BEvent theEvent) {
        String message, jsonString;
        switch (theEvent.name){
            case "Subscribe":
                System.out.println("Subscribing...");
                message = eventDataToJson(theEvent, "Subscribe");

                try {
                    send(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                jsonString = parseObjectToJsonString(theEvent.maybeData);
                robotData.addToBoardsMap(jsonString);
                break;

            case "Unsubscribe":
                System.out.println("Unsubscribing...");
                message = eventDataToJson(theEvent, "Unsubscribe");

                try {
                    send(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                jsonString = parseObjectToJsonString(theEvent.maybeData);
                robotData.removeFromBoardsMap(jsonString);
                break;

            case "Build":
                System.out.println("Building...");
                message = eventDataToJson(theEvent, "Build");

                try {
                    send(message); // Send new JSON string over to Robot side.
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "Update":
                // TODO: Collect sensor data that arrived from Robot and send it as a JSON string to injectEvent
                String jsonDataString = "{\"EV3\": {\"_0\": {\"_2\": 20}}}"; // Example

                injectEvent(bp, jsonDataString);
                break;

            case "Drive":
                System.out.println("Driving...");
                message = eventDataToJson(theEvent, "Drive");

                try {
                    send(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "Test":
//                try {
//                    Send("Red");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                System.out.println("!!!");
                break;
        }
    }
    private void injectEvent(BProgram bp, String message){
        bp.enqueueExternalEvent(new BEvent("GetSensorsData", message));
    }

    @Override
    public void superstepDone(BProgram bp) { }

    @Override
    public void bthreadAdded(BProgram bp, BThreadSyncSnapshot theBThread) { }

    @Override
    public void bthreadRemoved(BProgram bp, BThreadSyncSnapshot theBThread) { }

    @Override
    public void bthreadDone(BProgram bp, BThreadSyncSnapshot theBThread) { }

    private void openQueue() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.1.31");

        factory.setUsername("pi");
        factory.setPassword("pi");
        Connection connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    }

    private void send(String message) throws IOException {
//        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
//        System.out.println(" [x] Sent '" + message + "'");
    }

    private String eventDataToJson(BEvent theEvent, String command){
        String jsonString = parseObjectToJsonString(theEvent.maybeData);
        JsonElement jsonElement = new JsonParser().parse(jsonString);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Command", command);
        jsonObject.add("Data", jsonElement);
        return jsonObject.toString();
    }

    private String parseObjectToJsonString(Object data){
        return new Gson().toJson(data, Map.class);
    }
}

package il.ac.bgu.cs.bp.samplebpjsproject;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
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

    private HashMap<String, HashMap<Integer, Set<String>>> portsMap = new HashMap<>();
    private Channel channel;
    private final String QUEUE_NAME = "Cafe";

    RobotBProgramRunnerListener() {
//        openQueue();
    }

    @Override
    public void starting(BProgram bp) { }

    @Override
    public void started(BProgram bp) { }

    @Override
    public void ended(BProgram bp) { }

    @Override
    public void assertionFailed(BProgram bProgram, SafetyViolationTag safetyViolationTag) {

    }

    @Override
    public void halted(BProgram bp) { }

    @Override
    public void eventSelected(BProgram bp, BEvent theEvent) {
        String jsonString;
        JsonElement jsonElement;
        JsonObject jsonObject;
        switch (theEvent.name){
            case "Subscribe":
                System.out.println("Subscribing...");
                jsonString = ParseObjectToJsonString(theEvent.maybeData);
                jsonElement= new JsonParser().parse(jsonString);

                jsonObject = new JsonObject();
                jsonObject.addProperty("Command", "Subscribe");
                jsonObject.add("Data", jsonElement);

                try {
                    Send(jsonObject.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                AddToBoardsMap(jsonString);
                break;

            case "Unsubscribe":
                System.out.println("Unsubscribing...");
                jsonString = ParseObjectToJsonString(theEvent.maybeData);
                jsonElement = new JsonParser().parse(jsonString);

                jsonObject = new JsonObject();
                jsonObject.addProperty("Command", "Unsubscribe");
                jsonObject.add("Data", jsonElement);

                try {
                    Send(jsonObject.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                RemoveFromBoardsMap(jsonString);
                break;

            case "Build":
                System.out.println("Building...");
                jsonString = ParseObjectToJsonString(theEvent.maybeData); // Get JSON string
                jsonElement = new JsonParser().parse(jsonString); // Convert JSON string to element for wrapping the data

                jsonObject = new JsonObject(); // Create new JSON
                jsonObject.addProperty("Command", "Build"); // Add 'Command' key and value
                jsonObject.add("Data", jsonElement); // Add 'Data' as key and Data from 'Build' event as value.

                try {
                    Send(jsonObject.toString()); // Send new JSON string over to Robot side.
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "Update":
                // TODO: Collect sensor data that arrived from Robot and send it as a JSON string to injectEvent
                String jsonDataString = "{\"Ev3\": {\"_0\": {\"_2\": 20}}}"; // Example
                injectEvent(bp, jsonDataString);
                break;

            case "Test":
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
        factory.setHost("10.0.0.3");

        factory.setUsername("pi");
        factory.setPassword("pi");
        Connection connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    }

    private void Send(String message) throws IOException {
//        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
//        System.out.println(" [x] Sent '" + message + "'");
    }

    private String ParseObjectToJsonString(Object data){
        return new Gson().toJson(data, Map.class);
    }

    private void AddToBoardsMap(String json){
        HashMap<String, HashMap<Integer, Set<String>>> data = JsonToBoardsMap(json);

        for (Map.Entry<String, HashMap<Integer, Set<String>>> entry : data.entrySet()) {
            if (portsMap.keySet().contains(entry.getKey())){
                for (Map.Entry<Integer, Set<String>> entryInBoard : entry.getValue().entrySet()) {
                    HashMap<Integer, Set<String>> boardsMap = portsMap.get(entry.getKey());
                    if (boardsMap.keySet().contains(entryInBoard.getKey())){
                        boardsMap.get(entryInBoard.getKey()).addAll(entryInBoard.getValue());
                    } else {
                        boardsMap.put(entryInBoard.getKey(), entryInBoard.getValue());
                    }
                }
            } else {
                portsMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private void RemoveFromBoardsMap(String json){
        HashMap<String, HashMap<Integer, Set<String>>> data = JsonToBoardsMap(json);

        for (Map.Entry<String, HashMap<Integer, Set<String>>> entry : data.entrySet()) {
            if (portsMap.keySet().contains(entry.getKey())){
                for (Map.Entry<Integer, Set<String>> entryInBoard : entry.getValue().entrySet()) {
                    HashMap<Integer, Set<String>> boardsMap = portsMap.get(entry.getKey());
                    if (boardsMap.keySet().contains(entryInBoard.getKey())){
                        boardsMap.get(entryInBoard.getKey()).removeAll(entryInBoard.getValue());
                    }
                }
            }
        }
    }

    private HashMap<String, HashMap<Integer, Set<String>>> JsonToBoardsMap(String json) {
        HashMap<String, HashMap<Integer, Set<String>>> data = new HashMap<>();
        Gson gson = new Gson();
        Map element = gson.fromJson(json, Map.class);

        for (Object key: element.keySet()){
            data.put((String) key, new HashMap<>());
            Object value = element.get(key);
            if (value instanceof ArrayList){
                @SuppressWarnings("unchecked")
                Set<String> portList = new HashSet<>((ArrayList<String>) value);
                data.get(key).put(1, portList);

            } else if (value instanceof LinkedTreeMap){
                @SuppressWarnings("unchecked")
                Map<String, List<String>> valueMapped = (Map<String, List<String>>) value;
                for (Map.Entry<String, List<String>> intAndList : valueMapped.entrySet()) {

                    Set<String> portList = new HashSet<>(intAndList.getValue());
                    data.get(key).put(Integer.valueOf(intAndList.getKey()), portList);
                }
            }
        }
        return data;
    }
//    {"Ev3":{"1":["2"],"2":["3"]},"GrovePi":["D3"]}
}

package il.ac.bgu.cs.bp.samplebpjsproject;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.stream.Collectors;

public class RobotBProgramRunnerListener implements BProgramRunnerListener {

    private HashMap<String, HashMap<Integer, Set<Map.Entry<String, Double>>>> portsMap = new HashMap<>();
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
                String jsonDataString = "{\"EV3\": {\"_0\": {\"_2\": 20}}}"; // Example

                injectEvent(bp, jsonDataString);
                break;

            case "Drive":
                System.out.println("Driving...");
                jsonString = ParseObjectToJsonString(theEvent.maybeData);
                jsonElement = new JsonParser().parse(jsonString);

                jsonObject = new JsonObject();
                jsonObject.addProperty("Command", "Drive");
                jsonObject.add("Data", jsonElement);
                try {
                    Send(jsonObject.toString());
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

    private void Send(String message) throws IOException {
//        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
//        System.out.println(" [x] Sent '" + message + "'");
    }

    private String ParseObjectToJsonString(Object data){
        return new Gson().toJson(data, Map.class);
    }

    private void AddToBoardsMap(String json){
        HashMap<String, HashMap<Integer, Set<Map.Entry<String, Double>>>> boards = JsonToBoardsMap(json); // Build Map of Robot Ports in json

        for (Map.Entry<String, HashMap<Integer, Set<Map.Entry<String, Double>>>> board : boards.entrySet()) { // Iterate over board types
            if (portsMap.keySet().contains(board.getKey())){ // If board type already exist in portsMap
                for (Map.Entry<Integer, Set<Map.Entry<String, Double>>> entryInBoard : board.getValue().entrySet()) { // Iterate over board map
                    HashMap<Integer, Set<Map.Entry<String, Double>>> boardsMap = portsMap.get(board.getKey());
                    if (boardsMap.keySet().contains(entryInBoard.getKey())){ // If  existing boards map already contain this board
                        boardsMap.get(entryInBoard.getKey()).addAll(entryInBoard.getValue()); // Add boards value to pre existing port list
                    } else {
                        boardsMap.put(entryInBoard.getKey(), entryInBoard.getValue()); // Put new board into map
                    }
                }
            } else { // If board type doesn't exist in portMap.

                portsMap.put(board.getKey(), board.getValue()); // Add board type with all its data to map
            }
        }
    }

    private void RemoveFromBoardsMap(String json){
        HashMap<String, HashMap<Integer, Set<Map.Entry<String, Double>>>> data = JsonToBoardsMap(json);

        for (Map.Entry<String, HashMap<Integer, Set<Map.Entry<String, Double>>>> entry : data.entrySet()) {
            if (portsMap.keySet().contains(entry.getKey())){
                for (Map.Entry<Integer, Set<Map.Entry<String, Double>>> entryInBoard : entry.getValue().entrySet()) {
                    HashMap<Integer, Set<Map.Entry<String, Double>>> boardsMap = portsMap.get(entry.getKey());
                    if (boardsMap.keySet().contains(entryInBoard.getKey())){
                        boardsMap.get(entryInBoard.getKey()).removeAll(entryInBoard.getValue());
                    }
                }
            }
        }
    }

    private HashMap<String, HashMap<Integer, Set<Map.Entry<String, Double>>>> JsonToBoardsMap(String json) {
        HashMap<String, HashMap<Integer, Set<Map.Entry<String, Double>>>> data = new HashMap<>();
        Gson gson = new Gson();
        Map element = gson.fromJson(json, Map.class); // json String to Map

        for (Object key: element.keySet()){ // Iterate over board types
            data.put((String) key, new HashMap<>()); // Add board name to map
            Object value = element.get(key);

            // Check if board contains map of boards or list of ports
            // board in json might have mapping of a number of boards of its type
            // or list of ports that will be treated as if there's only one board of this type
            if (value instanceof ArrayList){ // If board has list of ports.

                @SuppressWarnings("unchecked")
                ArrayList<String> ports = (ArrayList<String>) value;
                Set<Map.Entry<String, Double>> portSet = ports.stream().map(port -> new AbstractMap.SimpleEntry<String, Double>(port, null)).collect(Collectors.toSet());
//                Set<Pair<String, Double>> portList = new HashSet<>();
//                for (String port :
//                        (ArrayList<String>)value) {
//
//                }
//                @SuppressWarnings("unchecked")
//                Set<String> portList = new HashSet<>((ArrayList<String>) value);
                data.get(key).put(1, portSet); // Index of the first board of this type is 1

            } else if (value instanceof LinkedTreeMap){ // If board has map boards of this type
                @SuppressWarnings("unchecked")
                Map<String, List<String>> valueMapped = (Map<String, List<String>>) value; // Map of boards to ports list
                for (Map.Entry<String, List<String>> intAndList : valueMapped.entrySet()) {

                    Set<String> portList = new HashSet<>(intAndList.getValue());
                    Set<Map.Entry<String, Double>> portSet = portList.stream().map(port -> new AbstractMap.SimpleEntry<String, Double>(port, null)).collect(Collectors.toSet());
                    data.get(key).put(Integer.valueOf(intAndList.getKey()), portSet);
                }
            }
        }
        return data;
    }
//    {"Ev3":{"1":["2"],"2":["3"]},"GrovePi":["D3"]}
}

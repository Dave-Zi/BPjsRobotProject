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
        switch (theEvent.name){
            case "Subscribe":
                System.out.println("Subscribing...");
                String jsonString = ParseObjectToJsonString(theEvent.maybeData);
                JsonElement jsonElement= new JsonParser().parse(jsonString);

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("Command", "Subscribe");
                jsonObject.add("Data", jsonElement);

//                try {
//                    Send(jsonString);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                UpdatePortsMap(jsonString);
                break;

            case "Unsubscribe":
                // TODO: Unsubscribe to sensors on robot
                break;

            case "Build":
                System.out.println("Building");
                break;
            // TODO: Build the robot object

            case "Update":
                System.out.println("Scan Data");
                break;
            // TODO: Check Queue for data from Robot
        }
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
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
    }

    private String ParseObjectToJsonString(Object data){
        return new Gson().toJson(data, Map.class);
    }

    private void UpdatePortsMap(String json){
        HashMap<String, HashMap<Integer, Set<String>>> data = new HashMap<>();
        Gson gson = new Gson();
        Map element = gson.fromJson(json, Map.class);

        for (Object key: element.keySet()){
            data.put((String) key, new HashMap<>());
            Object value = element.get(key);
            if (value instanceof  ArrayList){
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
//    {"Ev3":{"1":["2"],"2":["3"]},"GrovePi":["D3"]}
}

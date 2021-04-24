package il.ac.bgu.cs.bp.samplebpjsproject;

import Communication.ICommunication;
import RobotData.RobotSensorsData;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.BProgramRunnerListener;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SafetyViolationTag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RobotBProgramRunnerListener implements BProgramRunnerListener {

    private RobotSensorsData robotData = new RobotSensorsData();
    private ICommunication com;

    RobotBProgramRunnerListener(ICommunication communication) throws IOException, TimeoutException {
        com = communication;
        com.setCallback((consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Received: " + message);
            robotData.updateBoardMapValues(message);
        });
//        com.setCredentials("10.0.0.18", "pi", "pi");
        com.openSendQueue(true, true);
        com.openReceiveQueue(true, false);
    }

    @Override
    public void starting(BProgram bp) {
    }

    @Override
    public void started(BProgram bp) {
    }

    @Override
    public void ended(BProgram bp) {
    }

    @Override
    public void assertionFailed(BProgram bProgram, SafetyViolationTag safetyViolationTag) {
    }

    @Override
    public void halted(BProgram bp) {
    }

    @Override
    public void eventSelected(BProgram bp, BEvent theEvent) {
        if (commandToMethod.containsKey(theEvent.name)) {
            try {
                commandToMethod.get(theEvent.name).executeCommand(bp, theEvent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void injectEvent(BProgram bp, String message) {
        bp.enqueueExternalEvent(new BEvent("GetSensorsData", message));
    }

    @Override
    public void superstepDone(BProgram bp) {
    }

    @Override
    public void bthreadAdded(BProgram bp, BThreadSyncSnapshot theBThread) {
    }

    @Override
    public void bthreadRemoved(BProgram bp, BThreadSyncSnapshot theBThread) {
    }

    @Override
    public void bthreadDone(BProgram bp, BThreadSyncSnapshot theBThread) {
    }

    private String eventDataToJson(BEvent theEvent, String command) {
        String jsonString = parseObjectToJsonString(theEvent.maybeData);
        JsonElement jsonElement = new JsonParser().parse(jsonString);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Command", command);
        jsonObject.add("Data", jsonElement);
        return jsonObject.toString();
    }

    private String parseObjectToJsonString(Object data) {
        return new Gson().toJson(data, Map.class);
    }

    /**
     * Uniform Interface for BPjs Commands
     */
    @FunctionalInterface
    private interface ICommand {
        void executeCommand(BProgram bp, BEvent theEvent) throws IOException;
    }

    private ICommand subscribe = this::subscribe;
    private ICommand unsubscribe = this::unsubscribe;
    private ICommand build = this::build;
    private ICommand drive = this::drive;
    private ICommand rotate = this::rotate;
    private ICommand setSensor = this::setSensor;
    private ICommand update = this::update;

    private Map<String, ICommand> commandToMethod = Stream.of(new Object[][]{
            {"Subscribe", subscribe},
            {"Unsubscribe", unsubscribe},
            {"Build", build},
            {"Drive", drive},
            {"Rotate", rotate},
            {"SetSensor", setSensor},
            {"Update", update}
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (ICommand) data[1]));

    private void subscribe(BProgram bp, BEvent theEvent) {
        String message, jsonString;
//                System.out.println("Subscribing...");
        message = eventDataToJson(theEvent, "Subscribe");

        try {
            com.send(message, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        jsonString = parseObjectToJsonString(theEvent.maybeData);
        robotData.addToBoardsMap(jsonString);

    }

    private void unsubscribe(BProgram bp, BEvent theEvent) {
        String message, jsonString;
//                System.out.println("Unsubscribe...");
        message = eventDataToJson(theEvent, "Unsubscribe");

        try {
            com.send(message, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        jsonString = parseObjectToJsonString(theEvent.maybeData);
        robotData.removeFromBoardsMap(jsonString);
    }

    private void build(BProgram bp, BEvent theEvent) {
        String message;
//                System.out.println("Building...");
        message = eventDataToJson(theEvent, "Build");

        try {
            com.send(message, false); // Send new JSON string over to Robot side.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drive(BProgram bp, BEvent theEvent) {
        String message;
//                System.out.println("Driving...");
        message = eventDataToJson(theEvent, "Drive");
//                System.out.println(theEvent);

        try {
            com.send(message, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rotate(BProgram bp, BEvent theEvent) {
        String message;
        message = eventDataToJson(theEvent, "Rotate");
//                System.out.println(theEvent);

        try {
            com.send(message, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setSensor(BProgram bp, BEvent theEvent) {
        String message;
        message = eventDataToJson(theEvent, "SetSensor");
//                System.out.println(theEvent);

        try {
            com.send(message, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void update(BProgram bp, BEvent theEvent) {
//                String jsonDataString = "{\"EV3\": {\"_1\": {\"_2\": 20}, \"_2\": {\"_2\": 20, \"_3\": 20}, \"3\": {\"_2\": 20}}, GrovePi: {}}"; // Example
//                robotData.updateBoardMapValues(jsonDataString);

        if (robotData.isUpdated()) {
            String json = robotData.toJson();
            injectEvent(bp, json);
        }
    }
}

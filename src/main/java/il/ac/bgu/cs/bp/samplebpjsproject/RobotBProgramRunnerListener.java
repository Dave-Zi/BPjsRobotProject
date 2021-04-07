package il.ac.bgu.cs.bp.samplebpjsproject;

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
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RobotBProgramRunnerListener implements BProgramRunnerListener {

    private RobotSensorsData robotData = new RobotSensorsData();
    private CommunicationHandler com;

    RobotBProgramRunnerListener() throws IOException, TimeoutException {
        com = new CommunicationHandler(robotData);
        com.openQueues();
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
                    com.send(message);
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
                    com.send(message);
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
                    com.send(message); // Send new JSON string over to Robot side.
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "Update":
                String jsonDataString = "{\"EV3\": {\"_1\": {\"_2\": 20}, \"_2\": {\"_2\": 20, \"_3\": 20}, \"3\": {\"_2\": 20}}, GrovePi: {}}"; // Example
                robotData.updateBoardMapValues(jsonDataString);

                if (robotData.isUpdated()){
                    String json = robotData.toJson();
                    injectEvent(bp, json);
                }
                break;

            case "Drive":
                System.out.println("Driving...");
                message = eventDataToJson(theEvent, "Drive");

                try {
                    com.send(message);
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

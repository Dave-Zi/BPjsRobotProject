package il.ac.bgu.cs.bp.samplebpjsproject;

import Communication.ICommunication;
import Communication.QueueNameEnum;
import RobotData.RobotSensorsData;
import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.rabbitmq.client.AlreadyClosedException;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.BProgramRunnerListener;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SafetyViolationTag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RobotBProgramRunnerListener implements BProgramRunnerListener {

    private final RobotSensorsData robotData = new RobotSensorsData();
    private final ICommunication com;
    private final ICommand subscribe = this::subscribe;
    private final ICommand unsubscribe = this::unsubscribe;
    private final ICommand build = this::build;
    private final ICommand drive = this::drive;
    private final ICommand rotate = this::rotate;
    private final ICommand setSensorMode = this::setSensorMode;
    private final ICommand setActuatorData = this::setActuatorData;
    private final ICommand myAlgorithm = this::myAlgorithm;
    private final ICommand test = this::test;
    private final Map<String, ICommand> commandToMethod = Stream.of(new Object[][]{
            {"Subscribe", subscribe},
            {"Unsubscribe", unsubscribe},
            {"Build", build},
            {"Drive", drive},
            {"Rotate", rotate},
            {"SetSensorMode", setSensorMode},
            {"SetActuatorData", setActuatorData},
            {"MyAlgorithm", myAlgorithm},
            {"Test", test}
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (ICommand) data[1]));

    RobotBProgramRunnerListener(ICommunication communication, BProgram bp) throws IOException, TimeoutException {
        com = communication;
        // com.setCredentials("10.0.0.12", "pi", "pi");
        com.connect();
        com.purgeQueue(QueueNameEnum.Commands);
        com.purgeQueue(QueueNameEnum.SOS);
        com.purgeQueue(QueueNameEnum.Data);
        com.purgeQueue(QueueNameEnum.Free);
        com.consumeFromQueue(QueueNameEnum.Data, (consumerTag, delivery) ->
                robotData.updateBoardMapValues(new String(delivery.getBody(), StandardCharsets.UTF_8)));
        com.consumeFromQueue(QueueNameEnum.Free, (consumerTag, delivery) ->
                bp.enqueueExternalEvent(new BEvent("GetAlgorithmResult", new String(delivery.getBody(), StandardCharsets.UTF_8))));

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


    @Override
    public void superstepDone(BProgram bp) {
        String json = robotData.toJson();
//        System.out.println(json);
        bp.enqueueExternalEvent(new BEvent("GetSensorsData", json));
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
        if (command.equals("Build")){
            robotData.buildNicknameMaps(jsonString);
            jsonString = cleanNicknames(jsonString);
        }
        JsonElement jsonElement = new JsonParser().parse(jsonString);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Command", command);
        jsonObject.add("Data", jsonElement);
        return jsonObject.toString();
    }

    private String parseObjectToJsonString(Object data) {
        return new Gson().toJson(data, Map.class);
    }

    private void test(BProgram bp, BEvent theEvent) {
        System.out.println("Test Completed!");
    }

    private void subscribe(BProgram bp, BEvent theEvent) {
        String message = eventDataToJson(theEvent, "Subscribe");
        String jsonString = parseObjectToJsonString(theEvent.maybeData);

        send(message, QueueNameEnum.SOS);
        robotData.addToBoardsMap(jsonString);
    }

    private void unsubscribe(BProgram bp, BEvent theEvent) {
        String message = eventDataToJson(theEvent, "Unsubscribe");
        String jsonString = parseObjectToJsonString(theEvent.maybeData);

        send(message, QueueNameEnum.SOS);
        robotData.removeFromBoardsMap(jsonString);
    }

    private void build(BProgram bp, BEvent theEvent) {
        String message = eventDataToJson(theEvent, "Build");
        send(message, QueueNameEnum.SOS);
    }

    private void drive(BProgram bp, BEvent theEvent) {
        String message = eventDataToJson(theEvent, "Drive");
        send(message, QueueNameEnum.Commands);
    }

    private void rotate(BProgram bp, BEvent theEvent) {
        String message = eventDataToJson(theEvent, "Rotate");
        send(message, QueueNameEnum.Commands);
    }

    private void setSensorMode(BProgram bp, BEvent theEvent) {
        String message = eventDataToJson(theEvent, "SetSensorMode");
        send(message, QueueNameEnum.SOS);
    }

    private void setActuatorData(BProgram bp, BEvent theEvent) {
        String message = eventDataToJson(theEvent, "SetActuatorData");
        send(message, QueueNameEnum.SOS);
    }

    private void myAlgorithm(BProgram bp, BEvent theEvent) {
        String message = eventDataToJson(theEvent, "MyAlgorithm");
        send(message, QueueNameEnum.SOS);
    }

    private void send(String message, QueueNameEnum queue){
        try {
            com.send(message, queue);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AlreadyClosedException ignore) { }
    }

    private String cleanNicknames(String jsonString){
        Gson gson = new Gson();
        Map<?, ?> element = gson.fromJson(jsonString, Map.class); // json String to Map
        for (Object boardNameKey : element.keySet()) { // Iterate over board types
            @SuppressWarnings("unchecked")
            ArrayList<Map<String, ?>> boardsDataList =
                    (ArrayList<Map<String, ?>>) element.get(boardNameKey);

            for (int i = 0; i < boardsDataList.size(); i++) {
                Map<String, ?> portDataMap = boardsDataList.get(i);
                portDataMap.remove("Name");
                Map<String, String> newPortsValues = new HashMap<>();
                for (Map.Entry<String, ?> ports : portDataMap.entrySet()) {
                    if (ports.getValue() instanceof LinkedTreeMap) { // Check if port value is actually a map with nickname
                        @SuppressWarnings("unchecked")
                        Map<String, String> valueMap = (Map<String, String>) ports.getValue();
                        if (valueMap.containsKey("Device")) {
                            String nickname = valueMap.get("Device");
                            newPortsValues.put(ports.getKey(), nickname);
                        }
                    } else {
                        newPortsValues.put(ports.getKey(), (String) ports.getValue());
                    }
                }
                boardsDataList.set(i, newPortsValues);
            }
        }
        return new GsonBuilder().create().toJson(element);
    }

    /**
     * Uniform Interface for BPjs Commands
     */
    @FunctionalInterface
    private interface ICommand {
        void executeCommand(BProgram bp, BEvent theEvent) throws IOException;
    }
}

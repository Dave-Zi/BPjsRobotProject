package il.ac.bgu.cs.bp.samplebpjsproject;

import Communication.ICommunication;
import Communication.QueueNameEnum;
import RobotData.RobotSensorsData;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.AlreadyClosedException;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.BProgramRunnerListener;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SafetyViolationTag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RobotBProgramRunnerListener implements BProgramRunnerListener {

    private RobotSensorsData robotData = new RobotSensorsData();
    private ICommunication com;
    private ICommand subscribe = this::subscribe;
    private ICommand unsubscribe = this::unsubscribe;
    private ICommand build = this::build;
    private ICommand drive = this::drive;
    private ICommand rotate = this::rotate;
    private ICommand setSensor = this::setSensor;
    private ICommand myAlgorithm = this::myAlgorithm;
    private ICommand test = this::test;
    private Map<String, ICommand> commandToMethod = Stream.of(new Object[][]{
            {"Subscribe", subscribe},
            {"Unsubscribe", unsubscribe},
            {"Build", build},
            {"Drive", drive},
            {"Rotate", rotate},
            {"SetSensor", setSensor},
            {"MyAlgorithm", myAlgorithm},
            {"Test", test}
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (ICommand) data[1]));

    RobotBProgramRunnerListener(ICommunication communication, BProgram bp) throws IOException {
        com = communication;
        com.purgeQueue(QueueNameEnum.Commands);
        com.purgeQueue(QueueNameEnum.SOS);
        com.purgeQueue(QueueNameEnum.Data);
        com.purgeQueue(QueueNameEnum.Free);
        com.consumeFromQueue(QueueNameEnum.Data, (consumerTag, delivery) ->
                robotData.updateBoardMapValues(new String(delivery.getBody(), StandardCharsets.UTF_8)));
        com.consumeFromQueue(QueueNameEnum.Free, (consumerTag, delivery) ->
                bp.enqueueExternalEvent(new BEvent("GetAlgorithmResult", new String(delivery.getBody(), StandardCharsets.UTF_8))));

//        com.setCredentials("10.0.0.12", "pi", "pi");
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

    private void setSensor(BProgram bp, BEvent theEvent) {
        String message = eventDataToJson(theEvent, "SetSensor");
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

    /**
     * Uniform Interface for BPjs Commands
     */
    @FunctionalInterface
    private interface ICommand {
        void executeCommand(BProgram bp, BEvent theEvent) throws IOException;
    }
}

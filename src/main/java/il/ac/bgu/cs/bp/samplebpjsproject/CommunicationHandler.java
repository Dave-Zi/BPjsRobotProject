package il.ac.bgu.cs.bp.samplebpjsproject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

class CommunicationHandler {

    private RobotSensorsData robotData;
    private Channel sendChannel;
    private final String SEND_QUEUE_NAME = "Commands";
    private final String RECEIVE_QUEUE_NAME = "Data";

    CommunicationHandler(RobotSensorsData robotData){
        this.robotData = robotData;
    }

    void openQueues() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();

//        factory.setUsername("pi");
//        factory.setPassword("pi");
        Connection connection = factory.newConnection();
        sendChannel = connection.createChannel();
        sendChannel.queueDeclare(SEND_QUEUE_NAME, false, false, false, null);


        Channel receiveChannel = connection.createChannel();
        receiveChannel.queueDeclare(RECEIVE_QUEUE_NAME, false, false, false, null);

        receiveChannel.basicConsume(RECEIVE_QUEUE_NAME, true, this::onReceiveCallback, consumerTag -> { });
    }

    void send(String message) throws IOException {
        sendChannel.basicPublish("", SEND_QUEUE_NAME, null, message.getBytes());
    }

    private void onReceiveCallback(String consumerTag, Delivery delivery){
        String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
        robotData.updateBoardMapValues(json);
    }
}

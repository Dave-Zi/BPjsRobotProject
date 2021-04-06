package il.ac.bgu.cs.bp.samplebpjsproject;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.*;

class RobotSensorsData {
    private Map<String, Map<Integer, Map<String, Double>>> portsMap = new HashMap<>();

    Map<Integer, Map<String, Double>> getBoardsByName(String name){
        return portsMap.get(name);
    }

    Map<String, Double> getPorts(String boardName, int index){
        return getBoardsByName(boardName).get(index);
    }

    Map<String, Double> getPorts(String boardName){
        return getBoardsByName(boardName).get(1);
    }

    Double getPortValue(String boardName, int boardIndex, String portName) throws Exception {
        Map<String, Double> portsSet = getPorts(boardName, boardIndex);
        for (Map.Entry<String, Double> port : portsSet.entrySet()) {
            if (port.getKey().equals(portName)){
                return port.getValue();
            }
        }
        throw new Exception("Port Not Found!");
    }
    Double getPortValue(String boardName, String portName) throws Exception {
        return getPortValue(boardName, 1, portName);
    }

    void setPortValue(String boardName, int boardIndex, String portName, Double newValue) {
        Map<String, Double> ports = getPorts(boardName, boardIndex);
        ports.replace(portName, newValue);
    }

    void setPortValue(String boardName, String portName, Double value) {
        setPortValue(boardName, 1, portName, value);
    }

        // Add new sensors from json to mapping

    void addToBoardsMap(String json){
        Map<String, Map<Integer, Map<String, Double>>> boards = jsonToBoardsMap(json); // Build Map of Robot Ports in json

        for (Map.Entry<String, Map<Integer, Map<String, Double>>> board : boards.entrySet()) { // Iterate over board types
            if (portsMap.keySet().contains(board.getKey())){ // If board type already exist in portsMap
                for (Map.Entry<Integer, Map<String, Double>> entryInBoard : board.getValue().entrySet()) { // Iterate over board map
                    Map<Integer, Map<String, Double>> boardsMap = portsMap.get(board.getKey());
                    if (boardsMap.keySet().contains(entryInBoard.getKey())){ // If  existing boards map already contain this board
                        boardsMap.get(entryInBoard.getKey()).putAll(entryInBoard.getValue()); // Add boards value to pre existing port list
                    } else {
                        boardsMap.put(entryInBoard.getKey(), entryInBoard.getValue()); // Put new board into map
                    }
                }
            } else { // If board type doesn't exist in portMap.

                portsMap.put(board.getKey(), board.getValue()); // Add board type with all its data to map
            }
        }
    }

    // Remove from mapping any sensors that exist on given json
    void removeFromBoardsMap(String json){
        Map<String, Map<Integer, Map<String, Double>>> data = jsonToBoardsMap(json);

        for (Map.Entry<String, Map<Integer, Map<String, Double>>> entry : data.entrySet()) { // Iterate over boards
            if (portsMap.keySet().contains(entry.getKey())){ // If our board map contains this board
                for (Map.Entry<Integer, Map<String, Double>> entryInBoard : entry.getValue().entrySet()) { // Iterate over board indexes
                    Map<Integer, Map<String, Double>> boardsMap = portsMap.get(entry.getKey());
                    if (boardsMap.keySet().contains(entryInBoard.getKey())){ // If our board map contains board with this index
                        entryInBoard.getValue().forEach((port, value) -> boardsMap.get(entryInBoard.getKey()).remove(port));
                    }
                }
            }
        }
    }

    // Create new mapping of board name -> index -> ports and values
    // from given json
    private Map<String, Map<Integer, Map<String, Double>>> jsonToBoardsMap(String json) {
        Map<String, Map<Integer, Map<String, Double>>> data = new HashMap<>();
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
                Map<String, Double> portMap = new HashMap<>();
                ports.forEach(port -> portMap.put(port, null));
                data.get(key).put(1, portMap); // Index of the first board of this type is 1

            } else if (value instanceof LinkedTreeMap){ // If board has map boards of this type
                @SuppressWarnings("unchecked")
                Map<String, List<String>> valueMapped = (Map<String, List<String>>) value; // Map of boards to ports list
                for (Map.Entry<String, List<String>> intAndList : valueMapped.entrySet()) {

                    Set<String> portList = new HashSet<>(intAndList.getValue());

                    Map<String, Double> portMap = new HashMap<>();
                    portList.forEach(port -> portMap.put(port, null));
                    data.get(key).put(Integer.valueOf(intAndList.getKey()), portMap);
                }
            }
        }
        return data;
    }
//    {"Ev3":{"1":["2"],"2":["3"]},"GrovePi":["D3"]}

}

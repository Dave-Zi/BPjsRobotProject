var allEventsButBuildEventSet = bp.EventSet("Block all for build", function (e) {
    return !e.name.equals("Build");
});

var dataEventSet = bp.EventSet("", function (e) {
    return e.name.equals("GetSensorsData");
});

bp.registerBThread("Rotate Forward But Do Not Hit The Wall!", function (){
    bp.sync({request: bp.Event("Subscribe",{"EV3": ["UV_Sensor"]})})

    while (true){
        var e = bp.sync({waitFor: dataEventSet})
        var data = JSON.parse(e.data)
        var distance1 = data.EV3.My_Ev3.UV_Sensor

        if (distance1 != null && distance1 > 35){
            bp.sync({request: bp.Event("Rotate", {"EV3": {"B": 100, "C": 100, "speed": 15}})})
            bp.log.info("Drive " + distance1)
        }
    }
})
// bp.registerBThread("Also Do Not Hit The Wall On The Left!", function (){
//     bp.sync({request: bp.Event("Subscribe",{"EV3": ["UV_Sensor_Left"]})})
//
//     while (true){
//         var e = bp.sync({waitFor: dataEventSet})
//         var data = JSON.parse(e.data)
//         var distance = data.EV3.My_Ev3.UV_Sensor_Left
//
//         if ((distance != null) && (distance < 10)){
//             bp.sync({request: bp.Event("Rotate", {"EV3": {"B": 70, "C": 50, "speed": 15}})})
//             bp.log.info("Rotate " + distance)
//         }
//     }
// })

bp.registerBThread("Initiation", function () {
    bp.sync({
        block: allEventsButBuildEventSet, request: bp.Event("Build",
            {
                "EV3":
                    [{
                        "Name": "My_Ev3",
                        "Port": "rfcomm2",
                        "1": {"Name": "UV_Sensor_Left"},
                        "2": {"Name": "UV_Sensor"}
                    }]
                ,
                "GrovePi":
                    [{
                        "A0": "",
                        "A1": "",
                        "A2": "",
                        "D2": "",
                        "D3": "",
                        "D4": "Ultrasonic",
                        "D5": "",
                        "D6": "",
                        "D7": "",
                        "D8": "Led"
                    }]
            }
        )
    })
});
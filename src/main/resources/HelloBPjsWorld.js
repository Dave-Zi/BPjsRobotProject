var updateEventSet = bp.EventSet("Get Update", function (e) {
    return (e.data == null) ? false : e.data.boards.Robot;
});

var allEventsButBuildEventSet = bp.EventSet("Block all for build", function (e) {
    return !e.name.equals("Build");
});

var dataEventSet = bp.EventSet("", function (e) {
    return e.name.equals("GetSensorsData");
});

var AlgorithmEventSet = bp.EventSet("", function (e) {
    return e.name.equals("GetAlgorithmResult");
});

var direction = 1;

bp.registerBThread("Do Something with Data", function () {
    bp.sync({request: bp.Event("Subscribe", {"GrovePi": ["D4"]})});

    while (true) {
        var e = bp.sync({waitFor: dataEventSet});
        var data = JSON.parse(e.data);
        var distance  = data.GrovePi._1.D4;
        if (distance < 15){
            direction = -1 * direction ;
        }
    }
});


bp.registerBThread("Too Far Right", function () {
    bp.sync({request: bp.Event("Subscribe", {"EV3": ["2"]})});

    while (true) {
        var e = bp.sync({waitFor: dataEventSet});
        var data = JSON.parse(e.data);
        var distance  = data.EV3._1._2;
        if (distance > 20){
            bp.sync({request: bp.Event("Drive", {"EV3": {"B": 15, "C": 18 }})});
        }
    }
});

bp.registerBThread("Too Far Left", function () {
    bp.sync({request: bp.Event("Subscribe", {"EV3": ["2"]})});

    while (true) {
        var e = bp.sync({waitFor: dataEventSet});
        var data = JSON.parse(e.data);
        var distance  = data.EV3._1._2;
        if (distance < 15){
            bp.sync({request: bp.Event("Drive", {"EV3": {"B": 18, "C": 15 }})});
        }
    }
});

bp.registerBThread("Do My Algorithm", function () {
    bp.sync({request: bp.Event("MyAlgorithm", {"EV3": {1: {"Param1": 10, "Param2": {"Param2_1" : "Hello"}}}, "GrovePi": {2: {"Param3": 100}}})});
    var e = bp.sync({waitFor: AlgorithmEventSet});
    var data = JSON.parse(e.data);
    bp.sync({request: bp.Event("Test", data)});
});

//
// bp.registerBThread("Align to Left Wall", function () {
//     bp.sync({request: bp.Event("Subscribe", {"EV3": ["2"]})});
//     var speedOffset = 0;
//
//     while (true) {
//         var e = bp.sync({waitFor: dataEventSet});
//         var data = JSON.parse(e.data);
//         var myDistance = data.EV3._1._2; // get data from port 2 on Ev3
//
//         if (myDistance > 20) { // Offset is incrementally changed to slowly fix direction until wall is found and aligned with.
//             speedOffset = Math.max(speedOffset, 0); // speedOffset is reset to 0 if it was negative
//             speedOffset = Math.min(3, speedOffset + 1); // Incrementally add 1 to the speed offset to max difference of 10
//         } else if (myDistance < 10) {
//             speedOffset = Math.min(speedOffset, 0); // speedOffset is reset to 0 if it was positive
//             speedOffset = Math.max(-3, speedOffset - 1); // Incrementally subtract 1 to the speed offset to max difference of 10
//         } else {
//             speedOffset = 0
//         }
//         bp.sync({request: bp.Event("Drive", {"EV3": {"B": direction * (15 - speedOffset), "C": direction * (15 + speedOffset)}})});
//     }
// });


bp.registerBThread("Initiation", function () {
    bp.sync({
        block: allEventsButBuildEventSet, request: bp.Event("Build",
            {
                "EV3":
                    [{
                        "Port": "rfcomm0"
                    }]
                ,
                "GrovePi":
                    [{
                        "A0": "",
                        "A1": "",
                        "A2": "",
                        "D2": "Led",
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
var allEventsButBuildEventSet = bp.EventSet("Block all for build", function (e) {
    return !e.name.equals("Build");
});

var dataEventSet = bp.EventSet("", function (e) {
    return e.name.equals("GetSensorsData");
});


bp.registerBThread("Forward", function () {
    bp.sync({request: bp.Event("Subscribe", {"EV3": ["UV_Sensor"]})});

    while (true) {

        var e = bp.sync({waitFor: dataEventSet});
        var data = JSON.parse(e.data);
        var distance = data.EV3.My_Ev3.UV_Sensor;
        if (distance > 30){
            bp.sync({request: bp.Event("Drive", {"EV3": {"B": 10, "C": 10}})});
        }
    }
});

bp.registerBThread("Stop", function () {
    bp.sync({request: bp.Event("Subscribe", {"EV3": ["UV_Sensor"]})});

    while (true) {
        var e = bp.sync({waitFor: dataEventSet});
        var data = JSON.parse(e.data);
        var distance = data.EV3.My_Ev3.UV_Sensor;
        if (distance < 30){
            bp.sync({request: bp.Event("Drive", {"EV3": {"B": 6, "C": -6}})});
        }
    }
});

bp.registerBThread("Manually turn around", function () {
    bp.sync({request: bp.Event("Subscribe", {"GrovePi": ["D4"]})});

    while (true) {
        var e = bp.sync({waitFor: dataEventSet});
        var data = JSON.parse(e.data);
        var distance = data.GrovePi._1.D4;
        if (distance < 10){
            bp.sync({request: bp.Event("Rotate", {"EV3": {"B": 720, "C": -720, "speed": 60}})});
        }
    }
});

bp.registerBThread("Initiation", function () {
    bp.sync({
        block: allEventsButBuildEventSet, request: bp.Event("Build",
            {
                "EV3":
                    [{
                        "Name": "My_Ev3",
                        "Port": "rfcomm0",
                        "2": {"Name": "UV_Sensor"}
                    }]
                ,
                "GrovePi":
                    [{
                        "Name": "My_GrovePi",
                        "A0": {"Name": "", "Device": ""},
                        "A1": "",
                        "A2": "",
                        "D2": {"Name": "MyLed", "Device": "Led"},
                        "D3": "",
                        "D4": {"Name": "UV", "Device": "Ultrasonic"},
                        "D5": "",
                        "D6": "",
                        "D7": "",
                        "D8": "Led"
                    }]
            }
        )
    })
});
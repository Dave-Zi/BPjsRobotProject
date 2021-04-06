// bp.registerBThread("orders", function(){
//     var drinks=["black tea", "green tea", "espresso coffee", "flatwhite coffee",
//         "latte coffee", "sparkling water"];
//     // order 100 drinks
//     for (var i = 0; i < 100; i++) {
//         var idx = Math.floor(Math.random()*drinks.length);
//         bp.sync({request:bp.Event(drinks[idx])});
//     }
// });
//
// bp.registerBThread("coffee supply", function() {
//     var coffeeOrderES = bp.EventSet( "coffee orders", function(evt){
//         return evt.name.endsWith("coffee");
//     });
//     while ( true ) {
//         for ( var i=0; i<10; i++ ) {
//             bp.sync({waitFor:coffeeOrderES});
//         }
//         bp.sync({request:bp.Event("Grind more coffee!"),
//             block:coffeeOrderES});
//     }
// });

var updateEventSet = bp.EventSet("Get Update", function (e) {
    return (e.data == null) ? false : e.data.boards.Robot;
});

var allEventsButBuildEventSet = bp.EventSet("Block all for build", function (e) {
    return !e.name.equals("Build");
});

var dataEventSet = bp.EventSet("", function (e) {
    return e.name.equals("GetSensorsData");
});

//
// bp.registerBThread("move forward", function () {
//     while(true) {
//         bp.sync({request: bp.Event("UpdateVelocity", {"Ev3": {"A": 50}})});
//         bp.log.info("move forward");
//     }
// });


bp.registerBThread("Avoid walls ahead", function () {
    // bp.sync({request: bp.Event("Subscribe", {"EV3": {1: ["2"], 2: ["3"]}, "GrovePi": ["D3"]})});
    // bp.sync({request: bp.Event("Update")});
    // while(true) {
    //     var e = bp.sync({ waitFor: scanDataEventSet });
    //     var ranges = e.data.ranges;
    //     var range_max = e.data.range_max;
    //     if(getDistanceToObstacle(ranges[CENTER], range_max) < minimum_forward_dist) {
    //         bp.sync({block: moveEventSet, waitFor: updateVelocityEventSet})
    //     }
    // }
});

bp.registerBThread("Get Sensors Data", function () {
    // while (true) {
    //     bp.sync({request: bp.Event("Update")});
    // }
    // bp.sync({request: bp.Event("Update")});
});

bp.registerBThread("Do Something with Data", function () {
    bp.sync({request: bp.Event("Subscribe", {"EV3": {1: ["2"], 2: ["3"]}, "GrovePi": ["D3"]})});
    bp.sync({request: bp.Event("Update")});
    var e = bp.sync({waitFor: dataEventSet});
    var data = JSON.parse(e.data);
    var s  = data.EV3._1._2;
    bp.sync({request: bp.Event("Test", {"t": s})})
});
//
// bp.registerBThread("Align to Left Wall", function(){
//     bp.sync({request: bp.Event("Subscribe", {"Ev3": ["2"]})});
//     var speedOffset = 0;
//
//     while (true) {
//         var e = bp.sync({waitFor: dataEventSet});
//         var data = JSON.parse(e.data);
//         var myDistance = data.EV3._0._2; // get data from port 2 on Ev3
//
//         if (myDistance > 20) { // Offset is incrementally changed to slowly fix direction until wall is found and aligned with.
//             speedOffset = Math.max(speedOffset, 0); // speedOffset is reset to 0 if it was negative
//             speedOffset = Math.min(5, speedOffset + 1); // Incrementally add 1 to the speed offset to max difference of 10
//         } else if (myDistance < 20) {
//             speedOffset = Math.min(speedOffset, 0); // speedOffset is reset to 0 if it was positive
//             speedOffset = Math.max(-5, speedOffset - 1); // Incrementally subtract 1 to the speed offset to max difference of 10
//         } else {
//             speedOffset = 0
//         }
//         bp.sync({request: bp.Event("Update")});
//         bp.sync({request: bp.Event("Drive", {"EV3": {"B": 30 - speedOffset, "C": 30 + speedOffset}})});
//     }
// });


bp.registerBThread("Initiation", function () {
    bp.sync({block: allEventsButBuildEventSet, request: bp.Event("Build",
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
            )})
});
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

var allEventsButScanEventSet = bp.EventSet("", function (e) {
    return !e.name.equals("scan_data");
});

var allEventsButBuildEventSet = bp.EventSet("", function (e) {
    return !e.name.equals("Build");
});


//
// bp.registerBThread("move forward", function () {
//     while(true) {
//         bp.sync({request: bp.Event("UpdateVelocity", {"Ev3": {"A": 50}})});
//         bp.log.info("move forward");
//     }
// });


bp.registerBThread("Avoid walls ahead", function () {
    bp.sync({request: bp.Event("Subscribe", {"Ev3": {1: ["2"], 2: ["3"]}, "GrovePi": ["D3"]})});
    bp.sync({request: bp.Event("Subscribe", {"Ev3": {1: ["2", "3"], 2: ["4"]}, "GrovePi": ["D3", "D4"]})})
    // while(true) {
    //     var e = bp.sync({ waitFor: scanDataEventSet });
    //     var ranges = e.data.ranges;
    //     var range_max = e.data.range_max;
    //     if(getDistanceToObstacle(ranges[CENTER], range_max) < minimum_forward_dist) {
    //         bp.sync({block: moveEventSet, waitFor: updateVelocityEventSet})
    //     }
    // }
});

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
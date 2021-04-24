package il.ac.bgu.cs.bp.samplebpjsproject;

import Communication.CommunicationHandler;
import il.ac.bgu.cs.bp.bpjs.execution.BProgramRunner;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Simple class running a BPjs program that selects "hello world" events.
 *
 * @author michael
 */
public class HelloWorld {

    public static void main(String[] args) throws IOException, TimeoutException {
        // This will load the program file  <Project>/src/main/resources/HelloBPjsWorld.js
        final BProgram bprog = new ResourceBProgram("HelloBPjsWorld.js");
        bprog.setWaitForExternalEvents(true);

        BProgramRunner rnr = new BProgramRunner(bprog);
        bprog.setEventSelectionStrategy(new UpdateMakeAllHotSelectionStrategy());

        // Print program events to the console
//        rnr.addListener( new PrintBProgramRunnerListener() );
        rnr.addListener(new RobotBProgramRunnerListener(new CommunicationHandler("Commands", "Data")));


        // go!
        rnr.run();
    }

}

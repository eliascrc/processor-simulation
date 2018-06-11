package cr.ac.ucr.ecci.ci1323.controller;

import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.controller.parser.FileParser;
import cr.ac.ucr.ecci.ci1323.core.CoreOne;
import cr.ac.ucr.ecci.ci1323.core.CoreZero;
import cr.ac.ucr.ecci.ci1323.memory.DataBlock;
import cr.ac.ucr.ecci.ci1323.memory.DataBus;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;

import java.util.ArrayList;

public class SimulationController {

    private volatile ContextQueue contextQueue;
    private volatile ArrayList<Context> finishedThreads;
    private volatile InstructionBus instructionBus;
    private volatile DataBus dataBus;
    private CoreZero coreZero;
    private CoreOne coreOne;

    public SimulationController() {
        this.contextQueue = new ContextQueue();
        this.finishedThreads = new ArrayList<Context>();
        this.instructionBus = new InstructionBus(new InstructionBlock[SimulationConstants.TOTAL_INSTRUCTION_BLOCKS]);
        this.dataBus = new DataBus(new DataBlock[SimulationConstants.TOTAL_DATA_BLOCKS]);
    }

    private void parseContextFile () {
        FileParser fileParser = new FileParser(this.contextQueue, this.instructionBus);
        fileParser.prepareSimulation();
    }

    public void runSimulation() {
        this.parseContextFile();

        this.contextQueue.tryLock();
        this.coreZero = new CoreZero(5, this.contextQueue.getNextContext(), this);
        this.coreOne = new CoreOne(5, this.contextQueue.getNextContext(), this);
        this.contextQueue.unlock();

        this.dataBus.setCoreZeroCache(this.coreZero.getDataCache());
        this.dataBus.setCoreOneCache(this.coreOne.getDataCache());

        this.coreZero.run();
        this.coreOne.run();
    }

    public ContextQueue getContextQueue() {
        return contextQueue;
    }

    public ArrayList<Context> getFinishedThreads() {
        return finishedThreads;
    }

}

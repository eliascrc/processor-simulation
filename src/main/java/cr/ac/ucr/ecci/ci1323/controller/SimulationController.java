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
import java.util.Scanner;
import java.util.concurrent.Phaser;

/**
 * Main thread which controls the simulation and initializes everything needed to start the execution of threads in
 * both cores.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
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

        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el Quantum maximo: ");

        int maxQuantum = scanner.nextInt();
        while (maxQuantum < 1) {
            maxQuantum = scanner.nextInt();
        }

        Phaser simulationBarrier = new Phaser();

        this.contextQueue.tryLock();
        this.coreZero = new CoreZero(simulationBarrier, maxQuantum, this.contextQueue.getNextContext(), this,
                this.instructionBus, this.dataBus,0);
        this.coreOne = new CoreOne(simulationBarrier, maxQuantum, this.contextQueue.getNextContext(), this,
                this.instructionBus, this.dataBus, 1);
        this.contextQueue.unlock();

        this.dataBus.setCoreZeroCache(this.coreZero.getDataCache());
        this.dataBus.setCoreOneCache(this.coreOne.getDataCache());

        this.coreZero.start();
        this.coreOne.start();

        while (true) {
            simulationBarrier.arriveAndAwaitAdvance();
            simulationBarrier.arriveAndAwaitAdvance();
        }
    }

    /**
     * Adds a finished thread to the finished threads list for statistical purposes.
     *
     * @param context
     */
    public synchronized void addFinishedThread(Context context) {
        this.finishedThreads.add(context);
    }

    public ContextQueue getContextQueue() {
        return contextQueue;
    }

    public ArrayList<Context> getFinishedThreads() {
        return finishedThreads;
    }

}

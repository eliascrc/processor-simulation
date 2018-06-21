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
    private volatile ArrayList<Context> finishedContexts;
    private volatile InstructionBus instructionBus;
    private volatile DataBus dataBus;
    private CoreZero coreZero;
    private CoreOne coreOne;

    public SimulationController() {
        this.contextQueue = new ContextQueue();
        this.finishedContexts = new ArrayList<>();
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
        simulationBarrier.register();

        this.contextQueue.tryLock();
        contextQueue.print();

        Context nextContext = this.contextQueue.getNextContext();
        nextContext.setOldContext(true);
        this.coreZero = new CoreZero(simulationBarrier, maxQuantum, nextContext, this,
                this.instructionBus, this.dataBus,0);

        nextContext = this.contextQueue.getNextContext();
        nextContext.setOldContext(false);
        this.coreOne = new CoreOne(simulationBarrier, maxQuantum, nextContext, this,
                this.instructionBus, this.dataBus, 1);

        this.contextQueue.unlock();

        this.dataBus.setCoreZeroCache(this.coreZero.getDataCache());
        this.dataBus.setCoreOneCache(this.coreOne.getDataCache());

        this.coreZero.start();
        this.coreOne.start();

        while (simulationBarrier.getRegisteredParties() > 1) {
            simulationBarrier.arriveAndAwaitAdvance();
            simulationBarrier.arriveAndAwaitAdvance();
        }

        simulationBarrier.arriveAndDeregister();

        System.out.println("Finished Contexts:");
        for (Context context: this.finishedContexts) {
            context.print();
            System.out.println();
        }

        System.out.println("Preciosisimo!");
    }

    /**
     * Adds a finished thread to the finished threads list for statistical purposes.
     *
     * @param context
     */
    public synchronized void addFinishedThread(Context context) {
        this.finishedContexts.add(context);
    }

    public ContextQueue getContextQueue() {
        return contextQueue;
    }

    public ArrayList<Context> getFinishedContexts() {
        return finishedContexts;
    }

}

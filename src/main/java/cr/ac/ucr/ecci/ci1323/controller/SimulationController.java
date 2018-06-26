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
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Phaser;

/**
 * Main thread which controls the simulation and initializes everything needed to start the execution of threads in
 * both cores.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class SimulationController {

    private volatile int maxQuantum;
    private volatile ContextQueue contextQueue;
    private volatile ArrayList<Context> finishedContexts;
    private volatile InstructionBus instructionBus;
    private volatile DataBus dataBus;
    private volatile CoreZero coreZero;
    private volatile CoreOne coreOne;
    private volatile int simulationTicks;

    public SimulationController(int maxQuantum) {
        this.contextQueue = new ContextQueue();
        this.finishedContexts = new ArrayList<>();
        this.instructionBus = new InstructionBus(new InstructionBlock[SimulationConstants.TOTAL_INSTRUCTION_BLOCKS]);

        // Fill the shared data memory with 1
        DataBlock[] dataBlocks = new DataBlock[SimulationConstants.TOTAL_DATA_BLOCKS];
        for(int i = 0; i < dataBlocks.length; i++) {
            int[] words = new int[SimulationConstants.WORDS_PER_DATA_BLOCK];
            Arrays.fill(words, 1);
            dataBlocks[i] = new DataBlock(words);
        }
        this.dataBus = new DataBus(dataBlocks);

        this.simulationTicks = 0;
        this.maxQuantum = maxQuantum;
    }

    private void parseContextFile () {
        FileParser fileParser = new FileParser(this.contextQueue, this.instructionBus);
        fileParser.prepareSimulation();
    }

    public void runSimulation() {
        this.parseContextFile();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el Quantum maximo: ");

        while (this.maxQuantum < 1) {
            this.maxQuantum = scanner.nextInt();
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
            System.out.println("Ciclo de reloj #" + this.simulationTicks);
            this.coreZero.printContext();
            this.coreOne.printContext();
            System.out.println();

            simulationBarrier.arriveAndAwaitAdvance();
            this.simulationTicks++;
            simulationBarrier.arriveAndAwaitAdvance();
        }

        simulationBarrier.arriveAndDeregister();

        System.out.println("La simulacion ha terminado!");
        System.out.println();

        System.out.println("Memoria compartida de datos:");
        this.dataBus.printMemory();
        System.out.println();

        System.out.println("Contenido de la cache de datos:");
        this.coreZero.printCaches();
        System.out.println();
        this.coreOne.printCaches();
        System.out.println();

        System.out.println("Contextos que finalizaron:");
        for (Context context: this.finishedContexts) {
            context.print();
            System.out.println();
        }

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

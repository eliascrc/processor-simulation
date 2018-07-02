package cr.ac.ucr.ecci.ci1323.controller;

import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.controller.parser.FileParser;
import cr.ac.ucr.ecci.ci1323.core.CoreOne;
import cr.ac.ucr.ecci.ci1323.core.CoreZero;
import cr.ac.ucr.ecci.ci1323.exceptions.NoContextFilesException;
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

    /**
     * Constructor which initializes the main elements, including shared data memory with 1's.
     *
     * @param maxQuantum
     */
    public SimulationController(int maxQuantum) {
        this.contextQueue = new ContextQueue();
        this.finishedContexts = new ArrayList<>();
        this.instructionBus = new InstructionBus(new InstructionBlock[SimulationConstants.TOTAL_INSTRUCTION_BLOCKS]);

        // Fill the shared data memory with 1
        DataBlock[] dataBlocks = new DataBlock[SimulationConstants.TOTAL_DATA_BLOCKS];
        for (int i = 0; i < dataBlocks.length; i++) {
            int[] words = new int[SimulationConstants.WORDS_PER_DATA_BLOCK];
            Arrays.fill(words, 1);
            dataBlocks[i] = new DataBlock(words);
        }
        this.dataBus = new DataBus(dataBlocks);

        this.simulationTicks = 0;
        this.maxQuantum = maxQuantum;
    }

    public SimulationController() {
        this.contextQueue = new ContextQueue();
        this.finishedContexts = new ArrayList<>();
        this.instructionBus = new InstructionBus(new InstructionBlock[SimulationConstants.TOTAL_INSTRUCTION_BLOCKS]);

        // Fill the shared data memory with 1
        DataBlock[] dataBlocks = new DataBlock[SimulationConstants.TOTAL_DATA_BLOCKS];
        for (int i = 0; i < dataBlocks.length; i++) {
            int[] words = new int[SimulationConstants.WORDS_PER_DATA_BLOCK];
            Arrays.fill(words, 1);
            dataBlocks[i] = new DataBlock(words);
        }
        this.dataBus = new DataBus(dataBlocks);

        this.simulationTicks = 0;
    }

    /**
     * Calls the FileParser to parse the contexts to run the simulation and initialize the instruction memory with all
     * the instructions.
     */
    private void parseContextFile() {
        FileParser fileParser = new FileParser(this.contextQueue, this.instructionBus);
        fileParser.prepareSimulation();
    }

    /**
     * It starts the simulation, by requesting the user to input the quantum, and the execution mode (slow or fast). It
     * then creates the simulation barrier, prints the initial state of the context queue and creates both threads
     * of each core and begins their execution. While the execution is not finished, it prints the clock's cycle and
     * the contexts running on each core for each cycle. When finished, it prints the final state of the shared data
     * memory, the state of the data caches and the registers of each finished context.
     */
    public void runSimulation() {
        this.parseContextFile();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el Quantum maximo: ");

        int maxQuantum = -1;
        if (scanner.hasNextInt()) {
            maxQuantum = scanner.nextInt();
        }
        while (maxQuantum < 1) {
            System.out.println("Ingrese el Quantum maximo (Debe ser un numero entero mayor o igual a 1: ");
            while (!scanner.hasNextInt()) {
                System.out.println("Ingrese el Quantum maximo (Debe ser un numero entero mayor o igual a 1: ");
                scanner.next();
            }
            maxQuantum = scanner.nextInt();
        }

        scanner = new Scanner(System.in);
        System.out.print("Desea correr la simulacion en modo lento? 1. Sí 2. No ");

        int slowMode = -1;
        if (scanner.hasNextInt()) {
            slowMode = scanner.nextInt();
        }
        while (slowMode != 1 && slowMode != 2) {
            System.out.print("Desea correr la simulacion en modo lento? 1. Sí 2. No ");
            while (!scanner.hasNextInt()) {
                System.out.print("Desea correr la simulacion en modo lento? 1. Sí 2. No ");
                scanner.next();
            }
            slowMode = scanner.nextInt();
        }

        Phaser simulationBarrier = new Phaser();
        simulationBarrier.register();

        this.contextQueue.tryLock();
        if (this.contextQueue.size() < 2) {
            throw new NoContextFilesException("The simulation requires at least 2 context files to execute.");
        } else {
            contextQueue.print();

            Context nextContext = this.contextQueue.getNextContext();
            nextContext.setOldContext(true);
            this.coreZero = new CoreZero(simulationBarrier, maxQuantum, nextContext, this,
                    this.instructionBus, this.dataBus, 0);

            nextContext = this.contextQueue.getNextContext();
            nextContext.setOldContext(false);
            this.coreOne = new CoreOne(simulationBarrier, maxQuantum, nextContext, this,
                    this.instructionBus, this.dataBus, 1);

            this.contextQueue.unlock();

            this.dataBus.setCoreZeroCache(this.coreZero.getDataCache());
            this.dataBus.setCoreOneCache(this.coreOne.getDataCache());

            this.coreZero.start();
            this.coreOne.start();

            if (slowMode == 1) {
                String continueSimulation = "";
                char continueSim = 'x';
                while (simulationBarrier.getRegisteredParties() > 1) {
                    scanner = new Scanner(System.in);
                    System.out.print("Oprima la tecla 'c' para avanzar 20 ciclos de reloj ");

                    continueSimulation = scanner.next();
                    if (continueSimulation.length() == 1) {
                        continueSim = continueSimulation.charAt(0);
                    }

                    while (continueSim != 'c') {
                        System.out.print("Oprima la tecla 'c' para avanzar 20 ciclos de reloj ");
                        continueSimulation = scanner.next();
                        continueSim = continueSimulation.charAt(0);
                    }
                    for (int i = 0; i < 20; i++) {
                        System.out.println("Ciclo de reloj #" + this.simulationTicks);
                        this.coreZero.printContext();
                        this.coreOne.printContext();
                        System.out.println();

                        simulationBarrier.arriveAndAwaitAdvance();
                        this.simulationTicks++;
                        simulationBarrier.arriveAndAwaitAdvance();
                    }
                }
            } else {
                while (simulationBarrier.getRegisteredParties() > 1) {
                    System.out.println("Ciclo de reloj #" + this.simulationTicks);
                    this.coreZero.printContext();
                    this.coreOne.printContext();
                    System.out.println();

                    simulationBarrier.arriveAndAwaitAdvance();
                    this.simulationTicks += 1;
                    simulationBarrier.arriveAndAwaitAdvance();
                }
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
            for (Context context : this.finishedContexts) {

                context.print();
                System.out.println();
            }
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

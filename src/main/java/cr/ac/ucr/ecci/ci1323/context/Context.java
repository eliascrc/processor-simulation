package cr.ac.ucr.ecci.ci1323.context;

import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;

/**
 * Model for the context of a Thread within the processor.
 * It contains the program counter, register and execution tics.
 *
 * @author Elias Calderon
 */
public class Context {

    /**
     * The current program counter of the context.
     */
    private volatile int programCounter;

    /**
     * The register values of the context.
     */
    private volatile int[] registers;

    /**
     * The clock tics that the context has consumed.
     * It is used for statistics.
     */
    private volatile int executionTics;

    /**
     * The number that identifies the context
     */
    private volatile int contextNumber;

    private volatile int currentQuantum;

    /**
     * Defines the priority of the context.
     */
    private volatile boolean isOldContext;

    private volatile int finishingCore;


    /**
     * Constructor that sets the PC, initializes the registers and sets the tics to 0.
     * @param programCounter the context's PC.
     */
    public Context(int programCounter, int contextNumber) {
        this.programCounter = programCounter;
        this.registers = new int[SimulationConstants.TOTAL_REGISTERS];
        this.executionTics = SimulationConstants.INITIAL_TICKS;
        this.contextNumber = contextNumber;
        this.currentQuantum = SimulationConstants.INITIAL_QUANTUM;
    }

    /**
     * Prints the context's values. Used for testing purposes.
     */
    public void print() {
        System.out.print("Contexto #" + this.contextNumber + ": PC = " + this.programCounter
                + ", Registros = { ");
        for (int register : this.registers) {
            System.out.print(register + ", ");
        }
        System.out.println("} , Ciclos consumidos = " + this.executionTics + ", Nucleo en que termino: " + this.finishingCore);
    }

    public synchronized void incrementQuantum() {
        this.currentQuantum++;
    }

    public synchronized void incrementClockCycle() {
        this.executionTics++;
    }

    public synchronized void incrementPC() {
        this.programCounter += SimulationConstants.WORD_SIZE;
    }

    public synchronized void decrementPC() {
        this.programCounter -= SimulationConstants.WORD_SIZE;
    }

    //----------------------------------------------------------------------------------------
    // Setters and Getters
    //----------------------------------------------------------------------------------------
    public int getCurrentQuantum() {
        return currentQuantum;
    }

    public void setCurrentQuantum(int currentQuantum) {
        this.currentQuantum = currentQuantum;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    public int[] getRegisters() {
        return registers;
    }

    public void setRegisters(int[] registers) {
        this.registers = registers;
    }

    public int getExecutionTics() {
        return executionTics;
    }

    public void setExecutionTics(int executionTics) {
        this.executionTics = executionTics;
    }

    public boolean isOldContext() {
        return isOldContext;
    }

    public void setOldContext(boolean oldContext) {
        isOldContext = oldContext;
    }

    public int getFinishingCore() {
        return finishingCore;
    }

    public void setFinishingCore(int finishingCore) {
        this.finishingCore = finishingCore;
    }

    public int getContextNumber() {
        return contextNumber;
    }

    public void setContextNumber(int contextNumber) {
        this.contextNumber = contextNumber;
    }

}

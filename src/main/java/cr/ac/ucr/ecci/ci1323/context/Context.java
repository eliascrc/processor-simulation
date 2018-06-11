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

    /**
     * Constructor that sets the PC, initializes the registers and sets the tics to 0.
     * @param programCounter the context's PC.
     */
    public Context(int programCounter, int contextNumber) {
        this.programCounter = programCounter;
        this.registers = new int[SimulationConstants.TOTAL_REGISTERS];
        this.executionTics = SimulationConstants.INITIAL_TICKS;
        this.contextNumber = contextNumber;
    }

    /**
     * Prints the context's values. Used for testing purposes.
     */
    public void print() {
        System.out.print("Context: PC = " + this.programCounter
                + ", Registers = { ");
        for (int register : this.registers) {
            System.out.print(register + ", ");
        }
        System.out.println("} , Tics = " + this.executionTics + ", #Context = " + this.contextNumber);
    }

    //----------------------------------------------------------------------------------------
    // Setters and Getters
    //----------------------------------------------------------------------------------------

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
}

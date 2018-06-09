package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.DataCache;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCache;
import cr.ac.ucr.ecci.ci1323.control.context.Context;
import cr.ac.ucr.ecci.ci1323.control.SimulationController;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;

/**
 * Abstract core which contains the shared properties of both cores and inherits them the methods for
 * memory direction conversion.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
abstract class AbstractCore extends Thread {

    protected DataCache dataCache;
    protected InstructionCache instructionCache;
    protected Context currentContext;
    protected SimulationController simulationController;
    protected int maxQuantum;
    protected int currentQuantum;

    protected AbstractCore (int maxQuantum, Context startingContext, SimulationController simulationController) {

        this.maxQuantum = maxQuantum;
        this.simulationController = simulationController;
        this.currentContext = startingContext;
        this.currentQuantum = 0;
    }

    protected void executeDADDI (Instruction instruction) {
        this.getRegisters()[instruction.getField(2)] = this.getRegisters()[instruction.getField(1)]
                + instruction.getField(3);
    }

    protected void executeDADD (Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                + this.getRegisters()[instruction.getField(2)];
    }

    protected void executeDSUB (Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                - this.getRegisters()[instruction.getField(2)];
    }

    protected void executeDMUL (Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                * this.getRegisters()[instruction.getField(2)];
    }

    protected void executeDDIV (Instruction instruction) {
        try {
            this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                    / this.getRegisters()[instruction.getField(2)];
        } catch (ArithmeticException e) {
            System.err.println("Executing DDIV error: Division by zero.");
            System.exit(1);
        }
    }

    protected void executeBEQZ (Instruction instruction) {
        if (this.getRegisters()[instruction.getField(1)] == 0) {
            int newPC = this.getPC() + 4 * instruction.getField(3);
            this.setPC(newPC);
        }
    }

    protected void executeBNEZ (Instruction instruction) {
        if (this.getRegisters()[instruction.getField(1)] != 0) {
            int newPC = this.getPC() + 4 * instruction.getField(3);
            this.setPC(newPC);
        }
    }

    protected void executeJAL (Instruction instruction) {
        this.getRegisters()[31] = this.getPC();
        this.setPC(instruction.getField(3));
    }

    protected void executeJR (Instruction instruction) {
        this.setPC(this.getRegisters()[instruction.getField(1)]);
    }

    private int[] getRegisters() {
        return this.currentContext.getRegisters();
    }

    private int getPC() {
        return this.currentContext.getProgramCounter();
    }

    private void setPC(int newPC) {
        this.currentContext.setProgramCounter(newPC);
    }

    /**
     * Calculates the data block number of a load or store instruction.
     * @param instruction
     * @return
     */
    public int calculateDataBlockNumber(Instruction instruction) {
        int sourceRegister = this.currentContext.getRegisters()[instruction.getInstructionFields()[1]];
        int immediate = instruction.getInstructionFields()[3];
        int blockNumber = (sourceRegister + immediate) / 16;
        return blockNumber;
    }

    /**
     * Calculates the offset of a cache position block for a load or store instruction.
     * @param instruction
     * @return
     */
    public int calculateDataOffset(Instruction instruction) {
        int sourceRegister = this.currentContext.getRegisters()[instruction.getInstructionFields()[1]];
        int immediate = instruction.getInstructionFields()[3];
        int offset = ((sourceRegister + immediate) % 16) / 4;
        return offset;
    }

    /**
     * Calculates the data cache position for a data block.
     * @param blockNumber
     * @param coreNumber
     * @return
     */
    public int calculateCachePosition(int blockNumber, int coreNumber) {
        if(coreNumber == 0) {
            return blockNumber % 8;
        }
        return blockNumber % 4;
    }

    /**
     * Calculates the data cache position for a data block in the other core cache.
     * @param blockNumber
     * @param coreNumber
     * @return
     */
    public int calculateDataOtherCachePosition(int blockNumber, int coreNumber) {
        if(coreNumber == 0) {
            return blockNumber % 4;
        }
        return blockNumber % 8;
    }

    /**
     * Calculates the block number of an instruction.
     * @return
     */
    public int calculateInstructionBlockNumber() {
        return (int)Math.floor(this.currentContext.getProgramCounter() / 16);
    }

    /**
     * Calculates the offset for an instruction block.
     * @return
     */
    public int calculateInstructionOffset() {
        return (this.currentContext.getProgramCounter() % 16) / 4;
    }

}

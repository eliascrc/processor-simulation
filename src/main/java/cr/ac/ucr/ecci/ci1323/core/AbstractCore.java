package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.*;
import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.controller.SimulationController;
import cr.ac.ucr.ecci.ci1323.memory.DataBus;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;

import java.util.concurrent.Phaser;

/**
 * Abstract core which contains the shared properties of both cores and inherits them the methods for
 * memory direction conversion.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public abstract class AbstractCore extends Thread {

    protected Phaser simulationBarrier;
    protected DataCache dataCache;

    protected InstructionCache instructionCache;
    protected Context currentContext;
    protected SimulationController simulationController;
    protected int maxQuantum;
    protected boolean executionFinished;
    protected int coreNumber;

    protected AbstractCore(Phaser simulationBarrier, int maxQuantum, Context startingContext,
                           SimulationController simulationController, int totalCachePositions,
                           InstructionBus instructionBus, DataBus dataBus, int coreNumber) {

        this.simulationBarrier = simulationBarrier;
        this.simulationBarrier.register();
        this.maxQuantum = maxQuantum;
        this.simulationController = simulationController;
        this.currentContext = startingContext;

        this.instructionCache = new InstructionCache(instructionBus, totalCachePositions);
        this.dataCache = new DataCache(dataBus, totalCachePositions);

        this.executionFinished = false;
        this.coreNumber = coreNumber;
    }

    protected void executeCore() {

        while (!this.executionFinished) {
            int nextInstructionBlockNumber = this.calculateInstructionBlockNumber();
            int nextInstructionCachePosition = this.calculateCachePosition(nextInstructionBlockNumber, this.coreNumber);
            int nextInstructionCachePositionOffset = this.calculateInstructionOffset();

            Instruction instructionToExecute = this.getInstructionFromCache(nextInstructionBlockNumber,
                    nextInstructionCachePosition, nextInstructionCachePositionOffset);
            this.currentContext.incrementPC(SimulationConstants.WORD_SIZE);

            this.executeInstruction(instructionToExecute);
            this.currentContext.incrementQuantum();
            this.advanceClockCycle();

            if(this.currentContext.getCurrentQuantum() >= this.maxQuantum) {
                this.quantumExpired();
            }
        }

    }

    protected void executeInstruction(Instruction instruction) {
        int operationCode = instruction.getOperationCode();
        switch (operationCode) {
            case 2:
                this.executeJR(instruction);
                break;
            case 3:
                this.executeJAL(instruction);
                break;
            case 4:
                this.executeBEQZ(instruction);
                break;
            case 5:
                this.executeBNEZ(instruction);
                break;
            case 8:
                this.executeDADDI(instruction);
                break;
            case 12:
                this.executeDMUL(instruction);
                break;
            case 14:
                this.executeDDIV(instruction);
                break;
            case 32:
                this.executeDADD(instruction);
                break;
            case 34:
                this.executeDSUB(instruction);
                break;
            case 35:
                this.executeLW(instruction);
                break;
            case 43:
                this.executeSW(instruction);
                break;
            case 63:
                this.executeFIN(instruction);
                break;
            default:
                throw new IllegalArgumentException("Invalid instruction operation code");
        }
    }

    protected void executeFIN(Instruction instruction) {
        this.currentContext.incrementQuantum();
        this.advanceClockCycle();
        this.simulationController.addFinishedThread(this.currentContext);
        this.finishFINExecution();
    }

    public void advanceClockCycle() {
        this.simulationBarrier.arriveAndAwaitAdvance();
        this.currentContext.incrementClockCycle();
        // Por ahora nada mas
        this.simulationBarrier.arriveAndAwaitAdvance();
    }

    protected void finishFINExecution() {
        ContextQueue contextQueue = this.simulationController.getContextQueue();

        // Tries to lock the context queue
        while (!contextQueue.tryLock()) {
            this.advanceClockCycle();
        }

        Context nextContext = contextQueue.getNextContext();
        if (nextContext == null) { // checks if there is no other context in the queue
            this.executionFinished = true;
        } else { // there is a context waiting in the queue
            this.currentContext = nextContext;
        }

        contextQueue.unlock();
    }

    protected void quantumExpired() {
        ContextQueue contextQueue = this.simulationController.getContextQueue();

        // Tries to lock the context queue
        while (!contextQueue.tryLock()) {
            this.advanceClockCycle();
        }

        Context nextContext = contextQueue.getNextContext();
        if (nextContext == null) { // checks if there is no other context in the queue
            this.currentContext.setCurrentQuantum(SimulationConstants.INITIAL_QUANTUM);
        }
        else {
            contextQueue.pushContext(this.currentContext);
            this.currentContext = nextContext;
        }

        contextQueue.unlock();
    }

    protected abstract void executeSW(Instruction instruction);

    protected abstract void executeLW(Instruction instruction);

    protected abstract Instruction getInstructionFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition, int nextInstructionCachePositionOffset);

    protected void executeDADDI(Instruction instruction) {
        this.getRegisters()[instruction.getField(2)] = this.getRegisters()[instruction.getField(1)]
                + instruction.getField(3);
    }

    protected void executeDADD(Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                + this.getRegisters()[instruction.getField(2)];
    }

    protected void executeDSUB(Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                - this.getRegisters()[instruction.getField(2)];
    }

    protected void executeDMUL(Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                * this.getRegisters()[instruction.getField(2)];
    }

    protected void executeDDIV(Instruction instruction) {
        try {
            this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                    / this.getRegisters()[instruction.getField(2)];
        } catch (ArithmeticException e) {
            System.err.println("Executing DDIV error: Division by zero.");
            System.exit(1);
        }
    }

    protected void executeBEQZ(Instruction instruction) {
        if (this.getRegisters()[instruction.getField(1)] == 0) {
            int newPC = this.getPC() + 4 * instruction.getField(3);
            this.setPC(newPC);
        }
    }

    protected void executeBNEZ(Instruction instruction) {
        if (this.getRegisters()[instruction.getField(1)] != 0) {
            int newPC = this.getPC() + 4 * instruction.getField(3);
            this.setPC(newPC);
        }
    }

    protected void executeJAL(Instruction instruction) {
        this.getRegisters()[31] = this.getPC();
        this.setPC(instruction.getField(3));
    }

    protected void executeJR(Instruction instruction) {
        this.setPC(this.getRegisters()[instruction.getField(1)]);
    }

    /**
     * Calculates the data block number of a load or store instruction.
     *
     * @param instruction
     * @return
     */
    protected int calculateDataBlockNumber(Instruction instruction) {
        int sourceRegister = this.currentContext.getRegisters()[instruction.getInstructionFields()[1]];
        int immediate = instruction.getInstructionFields()[3];
        int blockNumber = (sourceRegister + immediate) / SimulationConstants.BLOCK_SIZE;
        return blockNumber;
    }

    /**
     * Calculates the offset of a cache position block for a load or store instruction.
     *
     * @param instruction
     * @return
     */
    protected int calculateDataOffset(Instruction instruction) {
        int sourceRegister = this.currentContext.getRegisters()[instruction.getInstructionFields()[1]];
        int immediate = instruction.getInstructionFields()[3];
        int offset = ((sourceRegister + immediate) % SimulationConstants.BLOCK_SIZE) / SimulationConstants.TOTAL_DATA_BLOCK_WORDS;
        return offset;
    }

    /**
     * Calculates the data cache position for a data block.
     *
     * @param blockNumber
     * @param coreNumber
     * @return
     */
    protected int calculateCachePosition(int blockNumber, int coreNumber) {
        if (coreNumber == 0) {
            return blockNumber % SimulationConstants.TOTAL_CORE_CERO_CACHE_POSITIONS;
        }
        return blockNumber % SimulationConstants.TOTAL_FIRST_CORE_CACHE_POSITIONS;
    }

    /**
     * Calculates the data cache position for a data block in the other core cache.
     *
     * @param blockNumber
     * @param coreNumber
     * @return
     */
    protected int calculateDataOtherCachePosition(int blockNumber, int coreNumber) {
        if (coreNumber == 0) {
            return blockNumber % SimulationConstants.TOTAL_FIRST_CORE_CACHE_POSITIONS;
        }
        return blockNumber % SimulationConstants.TOTAL_CORE_CERO_CACHE_POSITIONS;
    }

    /**
     * Calculates the block number of an instruction.
     *
     * @return
     */
    protected int calculateInstructionBlockNumber() {
        return (int) Math.floor(this.currentContext.getProgramCounter() / SimulationConstants.BLOCK_SIZE);
    }

    /**
     * Calculates the offset for an instruction block.
     *
     * @return
     */
    protected int calculateInstructionOffset() {
        return (this.currentContext.getProgramCounter() % SimulationConstants.BLOCK_SIZE) / SimulationConstants.INSTRUCTIONS_PER_BLOCK;
    }

    //----------------------------------------------------------------------------------------
    // Setters and Getters
    //----------------------------------------------------------------------------------------


    public int getCoreNumber() {
        return coreNumber;
    }

    public void setCoreNumber(int coreNumber) {
        this.coreNumber = coreNumber;
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

    public DataCache getDataCache() {
        return dataCache;
    }

    public InstructionCache getInstructionCache() {
        return instructionCache;
    }

    public void setInstructionCache(InstructionCache instructionCache) {
        this.instructionCache = instructionCache;
    }

}

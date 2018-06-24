package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.*;
import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.controller.SimulationController;
import cr.ac.ucr.ecci.ci1323.memory.DataBus;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;

import java.util.concurrent.Phaser;

/**
 * Abstract core which contains the shared properties of both cores and inherits them the methods for
 * memory direction conversion.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public abstract class AbstractCore extends AbstractThread {

    protected DataCache dataCache;
    protected InstructionCache instructionCache;
    protected SimulationController simulationController;
    protected int maxQuantum;
    protected boolean executionFinished;
    protected int coreNumber;

    protected AbstractCore(Phaser simulationBarrier, int maxQuantum, Context startingContext,
                           SimulationController simulationController, int totalCachePositions,
                           InstructionBus instructionBus, DataBus dataBus, int coreNumber) {

        super(simulationBarrier, startingContext);

        this.simulationBarrier.register();
        this.maxQuantum = maxQuantum;
        this.simulationController = simulationController;

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

            InstructionBlock instructionBlock = this.getInstructionBlockFromCache(nextInstructionBlockNumber,
                    nextInstructionCachePosition);

            if (instructionBlock != null) {

                Instruction instructionToExecute = instructionBlock.getInstruction(nextInstructionCachePositionOffset);
                this.currentContext.incrementPC(SimulationConstants.WORD_SIZE);

                this.executeInstruction(instructionToExecute);
                this.currentContext.incrementQuantum();
                this.advanceClockCycle();

                if (instructionToExecute.getOperationCode() != 63 &&
                        this.currentContext.getCurrentQuantum() >= this.maxQuantum) {
                    this.quantumExpired();
                }

            } else
                this.advanceClockCycle();

        }

        this.simulationBarrier.arriveAndDeregister();

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
        this.currentContext.setFinishingCore(this.coreNumber);
        this.simulationController.addFinishedThread(this.currentContext);
        this.finishFINExecution();
    }

    @Override
    public void advanceClockCycle() {
        this.simulationBarrier.arriveAndAwaitAdvance();
        this.currentContext.incrementClockCycle();
        this.changeContext();
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
        } else {
            this.currentContext.setOldContext(false);
            contextQueue.pushContext(this.currentContext);
            nextContext.setOldContext(true);
            this.currentContext = nextContext;
        }

        contextQueue.unlock();
    }

    protected void executeSW(Instruction instruction) {
        int blockNumber = this.calculateDataBlockNumber(instruction);
        int dataCachePositionOffset = this.calculateDataOffset(instruction);
        int dataCachePositionNumber = this.calculateCachePosition(blockNumber, this.coreNumber);
        DataCachePosition dataCachePosition = this.dataCache.getDataCachePosition(dataCachePositionNumber);
        int value = this.currentContext.getRegisters()[instruction.getField(2)];

        boolean solvedMiss = false;
        while (!solvedMiss) {
            this.blockDataCachePosition(dataCachePositionNumber);

            if (dataCachePosition.getTag() != blockNumber || dataCachePosition.getState() == CachePositionState.INVALID) {
                solvedMiss = this.handleStoreMiss(blockNumber, dataCachePosition, dataCachePositionOffset, value);
            } else { // Hit
                solvedMiss = this.handleStoreHit(blockNumber, dataCachePosition, dataCachePositionOffset, value);
            }
            dataCachePosition.unlock();
        }
    }

    protected void executeLW(Instruction instruction) {
        int blockNumber = this.calculateDataBlockNumber(instruction);
        int dataCachePositionOffset = this.calculateDataOffset(instruction);
        int dataCachePositionNumber = this.calculateCachePosition(blockNumber, this.coreNumber);
        DataCachePosition dataCachePosition = this.dataCache.getDataCachePosition(dataCachePositionNumber);

        boolean solvedMiss = false;
        while (!solvedMiss) {
            this.blockDataCachePosition(dataCachePositionNumber);

            if (dataCachePosition.getTag() != blockNumber || dataCachePosition.getState() == CachePositionState.INVALID) {
                solvedMiss = this.handleLoadMiss(blockNumber, dataCachePosition, dataCachePositionOffset, dataCachePositionNumber, instruction.getField(2));
            } else { // Hit
                this.currentContext.getRegisters()[instruction.getField(2)] = dataCachePosition.getDataBlock().getWord(dataCachePositionOffset);
                solvedMiss = true;
            }
            dataCachePosition.unlock();
        }
    }

    protected abstract boolean handleLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int finalRegister);

    protected abstract boolean handleStoreMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value);

    protected abstract boolean handleStoreHit(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value);

    protected abstract void blockDataCachePosition(int dataCachePosition);

    protected abstract void changeContext();

    protected abstract InstructionBlock getInstructionBlockFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition);

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
        int newPC = this.getPC() + instruction.getField(3);
        this.setPC(newPC);
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
        int offset = ((sourceRegister + immediate) % SimulationConstants.BLOCK_SIZE) / SimulationConstants.WORDS_PER_DATA_BLOCK;
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
            //System.exit(500);
            return blockNumber % SimulationConstants.TOTAL_CORE_ZERO_CACHE_POSITIONS;
        }
        return blockNumber % SimulationConstants.TOTAL_FIRST_CORE_CACHE_POSITIONS;
    }

    /**
     * Calculates the data cache position for a data block in the other core cache.
     *
     * @param blockNumber
     * @return
     */
    protected int calculateOtherDataCachePosition(int blockNumber) {
        if (this.coreNumber == 0) {
            //System.exit(500);
            return blockNumber % SimulationConstants.TOTAL_FIRST_CORE_CACHE_POSITIONS;
        }
        return blockNumber % SimulationConstants.TOTAL_CORE_ZERO_CACHE_POSITIONS;
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

    public void printContext() {
        System.out.println("El Contexto #" + this.currentContext.getContextNumber() + " esta corriendo en el Nucleo #" +
                this.coreNumber);
    }

    public void printCaches() {
        System.out.println("Caches para el Nucleo #" + this.coreNumber);

        System.out.println("Cache de instrucciones:");
        InstructionCachePosition[] instructionCachePositions = this.instructionCache.getInstructionCachePositions();
        for (int i = 0; i < instructionCachePositions.length; i++) {
            System.out.print("Posicion #" + i + ": ");
            instructionCachePositions[i].print();
        }

        System.out.println("Cache de datos:");
        DataCachePosition[] dataCachePositions = this.dataCache.getDataCachePositions();
        for (int i = 0; i < dataCachePositions.length; i++) {
            System.out.print("Posicion #" + i + ": ");
            dataCachePositions[i].print();
        }
    }
}

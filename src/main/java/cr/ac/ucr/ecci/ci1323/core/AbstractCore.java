package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.*;
import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.controller.SimulationController;
import cr.ac.ucr.ecci.ci1323.exceptions.ContextChangeException;
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

    protected volatile DataCache dataCache;

    protected volatile InstructionCache instructionCache;

    protected volatile SimulationController simulationController;

    protected volatile int maxQuantum;

    protected volatile boolean executionFinished;

    protected volatile int coreNumber;

    protected volatile ContextChange changeContext;

    protected volatile Context nextContext;

    protected volatile boolean instructionFinished;

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

        this.changeContext = ContextChange.NONE;
        this.nextContext = null;

        this.instructionFinished = true;
    }

    protected abstract boolean handleLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int finalRegister);

    protected abstract boolean handleStoreMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value);

    protected abstract void lockDataCachePosition(int dataCachePositionNumber);

    protected abstract void changeContext();

    protected abstract InstructionBlock getInstructionBlockFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition);

    protected Instruction lastInstruction;

    protected void executeCore() {

        while (!this.executionFinished) {

            int nextInstructionBlockNumber = this.calculateInstructionBlockNumber();
            int nextInstructionCachePosition = this.calculateCachePosition(nextInstructionBlockNumber, this.coreNumber);
            int nextInstructionCachePositionOffset = this.calculateInstructionOffset();

            try {
                InstructionBlock instructionBlock = this.getInstructionBlockFromCache(nextInstructionBlockNumber,
                        nextInstructionCachePosition);

                Instruction instructionToExecute = instructionBlock.getInstruction(nextInstructionCachePositionOffset);
                this.lastInstruction = instructionToExecute;
                this.currentContext.incrementPC();
                this.setInstructionFinished(false);
                this.executeInstruction(instructionToExecute);
                this.setInstructionFinished(true);

                this.currentContext.incrementQuantum();

                if (instructionToExecute.getOperationCode() != 63 &&
                        this.currentContext.getCurrentQuantum() >= this.maxQuantum) {
                    this.quantumExpired();
                }

                this.advanceClockCycle();

            } catch (ContextChangeException e) {
                // If the context changed then check if the quantum passed for the new context
                if (this.currentContext.getCurrentQuantum() >= this.maxQuantum)
                    this.quantumExpired();
            }

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
                this.executeFIN();
                break;
            default:
                throw new IllegalArgumentException("Invalid instruction operation code");
        }
    }

    @Override
    public void advanceClockCycle() {
        this.simulationBarrier.arriveAndAwaitAdvance();
        this.currentContext.incrementClockCycle();
        this.changeContext();
    }

    private void executeFIN() {
        this.modifiableFINExecution();
        this.currentContext.setFinishingCore(this.coreNumber);
        this.simulationController.addFinishedThread(this.currentContext);
    }

    protected void modifiableFINExecution() {
        ContextQueue contextQueue = this.simulationController.getContextQueue();

        // Tries to lock the context queue
        while (!contextQueue.tryLock()) {
            this.advanceBarriers();
        }

        this.setNextContext(contextQueue.getNextContext());
        if (this.nextContext == null) { // checks if there is no other context in the queue
            this.executionFinished = true;

        } else { // there is a context waiting in the queue
            this.setChangeContext(ContextChange.NEXT_CONTEXT);
        }

        contextQueue.unlock();
    }

    protected void quantumExpired() {
        ContextQueue contextQueue = this.simulationController.getContextQueue();

        // Tries to lock the context queue
        while (!contextQueue.tryLock()) {
            this.advanceBarriers();
        }

        this.setNextContext(contextQueue.getNextContext());
        this.currentContext.setCurrentQuantum(SimulationConstants.INITIAL_QUANTUM);

        if (this.nextContext != null) { // If there is a context in the queue, bring it to execution

            this.currentContext.setOldContext(false);
            this.nextContext.setOldContext(true);
            contextQueue.pushContext(this.currentContext);
            this.setChangeContext(ContextChange.NEXT_CONTEXT);
        }

        contextQueue.unlock();
    }

    protected void executeLW(Instruction instruction) {

        int blockNumber = this.calculateDataBlockNumber(instruction);
        int dataCachePositionOffset = this.calculateDataOffset(instruction);
        int dataCachePositionNumber = this.calculateCachePosition(blockNumber, this.coreNumber);
        DataCachePosition dataCachePosition = this.dataCache.getDataCachePosition(dataCachePositionNumber);

        boolean solvedMiss = false;
        while (!solvedMiss) {
            this.lockDataCachePosition(dataCachePositionNumber);

            if (dataCachePosition.getTag() != blockNumber || dataCachePosition.getState() == CachePositionState.INVALID) {
                solvedMiss = this.handleLoadMiss(blockNumber, dataCachePosition, dataCachePositionOffset, dataCachePositionNumber, instruction.getField(2));

            } else { // Hit
                this.currentContext.getRegisters()[instruction.getField(2)] = dataCachePosition.getDataBlock().getWord(dataCachePositionOffset);
                dataCachePosition.unlock();
                solvedMiss = true;
            }
        }
    }

    protected void executeSW(Instruction instruction) {
        int blockNumber = this.calculateDataBlockNumber(instruction);
        int dataCachePositionOffset = this.calculateDataOffset(instruction);
        int dataCachePositionNumber = this.calculateCachePosition(blockNumber, this.coreNumber);
        DataCachePosition dataCachePosition = this.dataCache.getDataCachePosition(dataCachePositionNumber);
        int value = this.currentContext.getRegisters()[instruction.getField(2)];

        boolean solvedMiss = false;
        while (!solvedMiss) {
            this.lockDataCachePosition(dataCachePositionNumber);

            if (dataCachePosition.getTag() != blockNumber || dataCachePosition.getState() == CachePositionState.INVALID) {
                solvedMiss = this.handleStoreMiss(blockNumber, dataCachePosition, dataCachePositionOffset, value);
            } else { // Hit
                solvedMiss = this.handleStoreHit(blockNumber, dataCachePosition, dataCachePositionNumber, dataCachePositionOffset, value);
            }
        }
    }

    protected boolean handleStoreHit(int blockNumber, DataCachePosition dataCachePosition, int dataCachePositionNumber, int positionOffset, int value) {
        if (dataCachePosition.getState() == CachePositionState.MODIFIED) {
            dataCachePosition.getDataBlock().getWords()[positionOffset] = value;
            dataCachePosition.unlock();
            this.advanceClockCycle();
            return true;
        }

        // Data Cache Position is shared
        if (!this.coreZeroCanMakeReservation(dataCachePositionNumber)) {
            dataCachePosition.unlock();
            this.advanceClockCycle();
            return false;
        }

        DataBus dataBus = this.dataCache.getDataBus();
        if (!dataBus.tryLock()) {
            dataCachePosition.unlock();
            this.advanceClockCycle();
            return false;
        }
        this.advanceClockCycle();

        // TODO Candados pueden dar problemas

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(dataCachePosition.getTag());
        DataCachePosition otherCachePosition = dataBus.getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);

        while (!otherCachePosition.tryLock()) {
            this.advanceClockCycle();
        }
        this.advanceClockCycle();

        if (otherCachePosition.getTag() == blockNumber && otherCachePosition.getState() != CachePositionState.SHARED)
            otherCachePosition.setState(CachePositionState.INVALID);

        dataCachePosition.setState(CachePositionState.MODIFIED);
        dataCachePosition.getDataBlock().getWords()[positionOffset] = value;

        otherCachePosition.unlock();

        this.coreZeroRemoveReservation();

        dataBus.unlock();
        dataCachePosition.unlock();

        return true;
    }

    protected boolean coreZeroCanMakeReservation(int dataCachePositionNumber) {
        // Core Zero must Override this method
        return true;
    }

    protected void coreZeroRemoveReservation() { }

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
        System.out.println("@ " + sourceRegister + " " + immediate + " " + blockNumber);
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
        System.out.println();
    }

    public void printContext() {
        System.out.println("El Contexto #" + this.currentContext.getContextNumber() + " esta corriendo en el Nucleo #" +
                this.coreNumber);
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

    public ContextChange getChangeContext() {
        return changeContext;
    }

    public synchronized void setChangeContext(ContextChange changeContext) {
        this.changeContext = changeContext;
    }

    public Context getNextContext() {
        return nextContext;
    }

    public synchronized void setNextContext(Context nextContext) {
        this.nextContext = nextContext;
    }

    public boolean isInstructionFinished() {
        return instructionFinished;
    }

    public synchronized void setInstructionFinished(boolean instructionFinished) {
        this.instructionFinished = instructionFinished;
    }

}

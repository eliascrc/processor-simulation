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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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

    /**
     * Registers the core to the Phaser barrier, and sets the provided references. It also initializes the
     * instruction and data caches for the core.
     * @param simulationBarrier
     * @param maxQuantum
     * @param startingContext
     * @param simulationController
     * @param totalCachePositions
     * @param instructionBus
     * @param dataBus
     * @param coreNumber
     */
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

    protected abstract boolean handleStoreHit(int blockNumber, DataCachePosition dataCachePosition, int dataCachePositionNumber, int positionOffset, int value);

    protected abstract boolean handleStoreMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value);

    protected abstract void lockDataCachePosition(int dataCachePositionNumber);

    protected abstract void changeContext();

    protected abstract InstructionBlock getInstructionBlockFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition);

    protected Instruction lastInstruction;

    /**
     * Starts the execution of the core, which runs while its execution is not finished, which means that there are
     * still contexts to execute. Its flow consists on calculating the block number, cache position and offset of the
     * instruction to execute, and then tries to obtain the instruction block from the instruction cache. Once it has
     * the instruction block, it gets the one to execute according to the offset and then increments the PC and
     * executes the instruction. When the instruction is executed, it increments the context's quantum and finally it
     * checks if its quantum expired to call the respective method if needed. After executed, it advances the
     * clock cycle.
     */
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

    /**
     * Executes the instruction received, depending on the operation code. If no operation code is matched, it
     * throws an exception.
     * @param instruction
     */
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

    /**
     * Advances the clock cycle of the current context, by first waiting on the barrier. It then calls the
     * change context method to check if a context switch is needed. In the change context method the second barrier
     * is present, in order to make the cycle incrementation and context switch in "zero time"
     */
    @Override
    public void advanceClockCycle() {
        this.simulationBarrier.arriveAndAwaitAdvance();
        this.currentContext.incrementClockCycle();
        this.changeContext();
    }

    /**
     * Executes a FIN instruction, by first calling the modifiable FIN, which is different for core zero. It then
     * sets the finishing core of the context, for statistical purposes, and adds it to the finished contexts list,
     * for the same purpose.
     */
    private void executeFIN() {
        this.modifiableFINExecution();
        this.currentContext.setFinishingCore(this.coreNumber);
        this.simulationController.addFinishedThread(this.currentContext);
    }

    /**
     * Executes the FIN instruction of Core One by locking the context queue to see if there is another context to load.
     * If no context is present, it sets the execution finished flag to true since the core finished its execution. If
     * there is another one left, it sets the context change flag to NEXT_CONTEXT so that it is loaded on the next cycle.
     * It finally unlocks the context queue.
     */
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

    /**
     * Processes the quantum expiration of a context in the core, by trying to lock the context queue and when done,
     * it gets the next context and restarts the quantum of the current context. If a context is present in the queue,
     * it brings it to execution by pushing the current one and setting the change context flag to NEXT_CONTEXT so it
     * is changed in the next cycle.
     */
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

    /**
     * Executes a LOAD instruction by first calculating the block number, offset and position and then mapping them
     * to the respective DataCachePosition object. It then loops while the miss is not solved, locking the data
     * cache position and then checking its tag and state. If its a different tag or the state is invalid, it handles
     * the miss, if not, it is a hit and it loads the data from the respective position and offset in the cache.
     * @param instruction
     */
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

    /**
     * Executes a STORE instruction by first calculating the block number, offset and position and then mapping them
     * to the respective DataCachePosition object. It then loops while the miss is not solved, locking the data
     * cache position and then checking its tag and state. If its a different tag or the state is invalid, it handles
     * the miss, if not, it is a hit and it handles a store hit.
     * @param instruction
     */
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

    /**
     * Executes a DADDI instruction.
     * @param instruction
     */
    protected void executeDADDI(Instruction instruction) {
        this.getRegisters()[instruction.getField(2)] = this.getRegisters()[instruction.getField(1)]
                + instruction.getField(3);
    }

    /**
     * Executes a DADD instruction.
     * @param instruction
     */
    protected void executeDADD(Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                + this.getRegisters()[instruction.getField(2)];
    }

    /**
     * Executes a DSUB instruction.
     * @param instruction
     */
    protected void executeDSUB(Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                - this.getRegisters()[instruction.getField(2)];
    }

    /**
     * Executes a DMUL instruction.
     * @param instruction
     */
    protected void executeDMUL(Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                * this.getRegisters()[instruction.getField(2)];
    }

    /**
     * Executes a DDIV instruction.
     * @param instruction
     */
    protected void executeDDIV(Instruction instruction) {
        try {
            this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                    / this.getRegisters()[instruction.getField(2)];
        } catch (ArithmeticException e) {
            System.err.println("Executing DDIV error: Division by zero.");
            System.exit(1);
        }
    }

    /**
     * Executes a BEQZ instruction.
     * @param instruction
     */
    protected void executeBEQZ(Instruction instruction) {
        if (this.getRegisters()[instruction.getField(1)] == 0) {
            int newPC = this.getPC() + 4 * instruction.getField(3);
            this.setPC(newPC);
        }
    }

    /**
     * Executes a BNEZ instruction.
     * @param instruction
     */
    protected void executeBNEZ(Instruction instruction) {
        if (this.getRegisters()[instruction.getField(1)] != 0) {
            int newPC = this.getPC() + 4 * instruction.getField(3);
            this.setPC(newPC);
        }
    }

    /**
     * Executes a JAL instruction.
     * @param instruction
     */
    protected void executeJAL(Instruction instruction) {
        this.getRegisters()[31] = this.getPC();
        int newPC = this.getPC() + instruction.getField(3);
        this.setPC(newPC);
    }

    /**
     * Executes a JR instruction.
     * @param instruction
     */
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

    /**
     * Prints the instruction cache and data cache of the core.
     */
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

    /**
     * Prints the context being executed.
     */
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

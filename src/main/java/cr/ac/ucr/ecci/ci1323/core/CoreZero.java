package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.InstructionCachePosition;
import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.controller.SimulationController;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.memory.DataBus;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;

import java.util.concurrent.Phaser;

/**
 * Core zero of the simulated processor.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class CoreZero extends AbstractCore {

    private MissHandler missHandler;
    private Context waitingContext;

    private volatile int reservedDataCachePosition;

    private volatile int reservedInstructionCachePosition;

    private volatile boolean changeContext;
    private boolean waitingForReservation;

    public CoreZero(Phaser simulationBarrier, int maxQuantum, Context startingContext,
                    SimulationController simulationController, InstructionBus instructionBus,
                    DataBus dataBus, int coreNumber) {
        super(simulationBarrier, maxQuantum, startingContext, simulationController,
                SimulationConstants.TOTAL_CORE_CERO_CACHE_POSITIONS, instructionBus, dataBus, coreNumber);

        this.waitingContext = null;
        this.reservedDataCachePosition = this.reservedInstructionCachePosition = -1;
        this.changeContext = false;
    }

    @Override
    public void run() {
        super.executeCore();
    }

    @Override
    protected void finishFINExecution() {
        if (this.waitingContext == null) { // checks if there is no other context waiting in execution

            super.finishFINExecution();

        } else { // there is a waiting context in execution
            this.waitingContext.setOldContext(true);
            this.currentContext = this.waitingContext;
            this.waitingContext = null;
        }
    }

    @Override
    protected void executeSW(Instruction instruction) {

    }

    @Override
    protected void executeLW(Instruction instruction) {

    }

    @Override
    protected void quantumExpired() {
        if (this.waitingContext == null) { // checks if there is no other context waiting in execution

            super.quantumExpired();

        } else { // there is a waiting context in execution
            ContextQueue contextQueue = this.simulationController.getContextQueue();

            // Tries to lock the context queue
            while (!contextQueue.tryLock()) {
                this.advanceClockCycle();
            }
            this.currentContext.setOldContext(false);
            contextQueue.pushContext(this.currentContext);
            contextQueue.unlock();

            this.currentContext = this.waitingContext;
            this.waitingContext = null;
        }
    }

    @Override
    protected InstructionBlock getInstructionBlockFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition) {
        boolean solvedMiss = false;
        while (!solvedMiss) {
            while (this.reservedInstructionCachePosition != -1) {
                this.advanceClockCycle();
            }

            InstructionCachePosition instructionCachePosition = this.instructionCache
                    .getInstructionCachePosition(nextInstructionCachePosition);

            if (instructionCachePosition.getTag() != nextInstructionBlockNumber) { // miss
                solvedMiss = this.enterCacheMiss(MissType.INSTRUCTION, nextInstructionBlockNumber,
                        nextInstructionCachePosition);
            } else {
                solvedMiss = true;
            }
        }

        return this.instructionCache.getInstructionCachePosition(nextInstructionCachePosition).getInstructionBlock();
    }

    private boolean enterCacheMiss(MissType missType, int nextBlockNumber, int nextCachePosition) {
        boolean solvedMiss = true;
        ContextQueue contextQueue = this.simulationController.getContextQueue();
        if (this.waitingContext != null) { // there is a waiting context,
            this.missHandler = new MissHandler(this, this.currentContext, missType, this.simulationBarrier ,
                    nextBlockNumber, nextCachePosition);
            this.currentContext = this.waitingContext;
            this.waitingContext = null;
            this.missHandler.start();

        } else if (this.missHandler != null) { // miss handler is running, must try to solve by itself
            solvedMiss = this.solveMissLocally(missType, nextBlockNumber, nextCachePosition);
        } else { // miss handler is not running and there is no waiting context
            this.missHandler = new MissHandler(this, this.currentContext, missType, this.simulationBarrier,
                    nextBlockNumber, nextCachePosition);

            while (!contextQueue.tryLock())
                this.advanceClockCycle();

            Context nextContext = contextQueue.getNextContext();
            if (nextContext != null) {
                nextContext.setOldContext(false);
                this.currentContext = nextContext;
                this.missHandler.start();
            }
            contextQueue.unlock();
        }

        return solvedMiss;
    }

    public boolean solveMissLocally(MissType missType, int nextBlockNumber, int nextCachePosition) {

        switch (missType) {
            case INSTRUCTION:
                return this.solveInstructionMissLocally(nextBlockNumber, nextCachePosition);
            default:
                throw new IllegalArgumentException("Invalid Miss Type in core zero.");
        }
    }

    public boolean solveInstructionMissLocally(int nextBlockNumber, int nextCachePosition) {
        if (this.reservedInstructionCachePosition == -1) { // there is no other cache position reserved
            this.waitingForReservation = false;
            this.reservedInstructionCachePosition = nextCachePosition;
            this.instructionCache.getInstructionBlockFromMemory(nextBlockNumber, nextCachePosition, this);
            this.reservedInstructionCachePosition = -1;
            return true;
        }

        this.waitingForReservation = true;
        return false;
    }

    private void solvedMiss(Context solvedMissContext) {
        this.changeContext = false;

        if(this.currentContext == null) { // there is no other thread in execution
            this.currentContext = this.waitingContext;
        } else {
            if(this.waitingContext.isOldContext()) { // miss was resolved in old thread
                this.changeContext = true;
                /**/
            } else { // miss was resolved in newer thread
                if(this.waitingForReservation) { // current thread in execution is in miss
                    this.changeContext = true;
                } // current thread in execution is not in miss
            }
        }
    }

    public void finishMissHandlerExecution() {
        this.solvedMiss(this.missHandler.getCurrentContext());
        this.missHandler = null;
    }

    public void changeContext() {
        if (this.changeContext) {
            Context tempContext = currentContext;
            this.currentContext = this.waitingContext;
            this.waitingContext = tempContext;
        }
    }

    public boolean isWaitingForReservation() {
        return waitingForReservation;
    }

    public void setWaitingForReservation(boolean waitingForReservation) {
        this.waitingForReservation = waitingForReservation;
    }

    public boolean isChangeContext() {
        return changeContext;
    }

    public void setChangeContext(boolean changeContext) {
        this.changeContext = changeContext;
    }

    public MissHandler getMissHandler() {
        return missHandler;
    }

    public void setMissHandler(MissHandler missHandler) {
        this.missHandler = missHandler;
    }

    public Context getWaitingContext() {
        return waitingContext;
    }

    public void setWaitingContext(Context waitingContext) {
        this.waitingContext = waitingContext;
    }

    public int getReservedDataCachePosition() {
        return reservedDataCachePosition;
    }

    public void setReservedDataCachePosition(int reservedDataCachePosition) {
        this.reservedDataCachePosition = reservedDataCachePosition;
    }

    public int getReservedInstructionCachePosition() {
        return reservedInstructionCachePosition;
    }

    public void setReservedInstructionCachePosition(int reservedInstructionCachePosition) {
        this.reservedInstructionCachePosition = reservedInstructionCachePosition;
    }

}

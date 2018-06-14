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
    private int reservedDataCachePosition;
    private int reservedInstructionCachePosition;

    public CoreZero(Phaser simulationBarrier, int maxQuantum, Context startingContext,
                    SimulationController simulationController, InstructionBus instructionBus,
                    DataBus dataBus, int coreNumber) {
        super(simulationBarrier, maxQuantum, startingContext, simulationController, SimulationConstants.TOTAL_CORE_CERO_CACHE_POSITIONS,
                instructionBus, dataBus, coreNumber);

        this.waitingContext = null;
        this.reservedDataCachePosition = this.reservedInstructionCachePosition = -1;
    }

    @Override
    public void run() {
        System.out.println("Core Zero! Ready. The context is: ");
        super.currentContext.print();

    }

    @Override
    protected void finishFINExecution() {
        if (this.waitingContext == null) { // checks if there is no other context waiting in execution

            super.finishFINExecution();

        } else { // there is a waiting context in execution
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

            contextQueue.pushContext(this.currentContext);
            contextQueue.unlock();

            this.currentContext = this.waitingContext;
            this.waitingContext = null;
        }
    }

    @Override
    protected Instruction getInstructionFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition,
                                                  int nextInstructionCachePositionOffset) {
        boolean solvedMiss = false;
        while (!solvedMiss) {
            while (this.reservedInstructionCachePosition != nextInstructionCachePosition) {
                this.advanceClockCycle();
            }

            InstructionCachePosition instructionCachePosition = this.instructionCache
                    .getInstructionCachePosition(nextInstructionCachePosition);

            if (!(instructionCachePosition.getTag() == nextInstructionBlockNumber)) { // miss
                enterCacheMiss(MissType.INSTRUCTION);

            }
        }
        return this.instructionCache.getInstructionCachePosition(nextInstructionCachePosition)
                .getInstructionBlock().getInstruction(nextInstructionCachePositionOffset);
    }

    // TODO Manejar el hilillo principal
    private boolean enterCacheMiss(MissType missType) {
        boolean solvedMiss = true;
        ContextQueue contextQueue = this.simulationController.getContextQueue();

        if (this.waitingContext != null) {

            this.missHandler = new MissHandler(this, this.currentContext, missType, this.simulationBarrier);
            this.currentContext = this.waitingContext;
            this.waitingContext = null;
            this.missHandler.run();

        } else if (missHandler != null) {
            MissHandler.setContext(this.currentContext);
            MissHandler.setCoreZero(this);
            MissHandler.setMissType(missType);
            MissHandler.setSimulationBarrier(this.simulationBarrier);

            solvedMiss = MissHandler.solveMiss();

        } else { // miss handler is not running and there is not a waiting context
            this.missHandler = new MissHandler(this, this.currentContext, missType, this.simulationBarrier);

            while (contextQueue.tryLock())
                this.advanceClockCycle();

            Context nextContext = contextQueue.getNextContext();
            if (nextContext != null) {
                this.currentContext = nextContext;
                this.missHandler.run();
            }
            contextQueue.unlock();
        }

        return solvedMiss;
    }

    public void finishMissHandlerExecution() {
        this.missHandler = null;
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



}

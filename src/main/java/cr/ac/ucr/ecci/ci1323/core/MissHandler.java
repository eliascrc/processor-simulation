package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.DataCache;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCache;
import cr.ac.ucr.ecci.ci1323.context.Context;

import java.util.concurrent.Phaser;

public class MissHandler extends Thread {

    private static CoreZero coreZero;
    private static Context context;
    private static MissType missType;
    private static Phaser simulationBarrier;

    MissHandler(CoreZero coreZero, Context context, MissType missType, Phaser simulationBarrier) {
        MissHandler.coreZero = coreZero;
        MissHandler.context = context;
        MissHandler.missType = missType;
        MissHandler.simulationBarrier = simulationBarrier;
    }

    @Override
    public void run() {
        MissHandler.simulationBarrier.register();
        MissHandler.solveMiss();
        // TODO (DANIEL) resolvio fallo
    }

    public static boolean solveMiss() {

        switch (MissHandler.missType) {
            case INSTRUCTION:
                return MissHandler.solveInstructionMiss();
            default:
                throw new IllegalArgumentException("Invalid Miss Type in miss handler.");
        }
    }

    private static boolean solveInstructionMiss() {
        if (reservedInstructionCachePosition == -1) { // there is no other cache position reserved
            reservedInstructionCachePosition = nextInstructionCachePosition;
            instructionCache.getInstructionBlockFromMemory(nextInstructionBlockNumber,
                    nextInstructionCachePosition, MissHandler.coreZero);
            reservedInstructionCachePosition = -1;
            return true;
        }

        // there is another cache position reserved
        MissHandler.advanceClockCycle(); // TODO El miss handler deberia tener su propio metodo para esto
        return false;

    }

    public static void setCoreZero(CoreZero coreZero) {
        MissHandler.coreZero = coreZero;
    }

    public static void setContext(Context context) {
        MissHandler.context = context;
    }

    public static void setMissType(MissType missType) {
        MissHandler.missType = missType;
    }

    public static void setSimulationBarrier(Phaser simulationBarrier) {
        MissHandler.simulationBarrier = simulationBarrier;
    }

}

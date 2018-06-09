package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.control.Context;
import cr.ac.ucr.ecci.ci1323.control.ContextQueue;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;

/**
 * Abstract core which contains the shared properties of both cores and inherits them the methods for
 * memory direction conversion.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
abstract class AbstractCore extends Thread {

    /*
    private DataCache dataCache;
    private InstructionCache instructionCache;*/
    Context currentContext;
    ContextQueue contextQueue;
    int maxQuantum;
    int currentQuantum;
    int[] registers;

    AbstractCore (int maxQuantum, Context startingContext, ContextQueue contextQueue) {
        this.maxQuantum = maxQuantum;
        this.contextQueue = contextQueue;
        this.currentContext = startingContext;
        this.currentQuantum = 0;
        this.registers = new int[32];
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

package cr.ac.ucr.ecci.ci1323.cache;

import cr.ac.ucr.ecci.ci1323.exceptions.TryLockException;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the position of a instruction cache, with its respective block and tag.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class InstructionCachePosition {

    private volatile int tag;
    private volatile InstructionBlock instructionBlock;

    /**
     * Class constructor
     * @param tag the tag of the instruction cache position
     * @param instructionBlock the instruction block that the instruction cache position holds
     */
    public InstructionCachePosition(int tag, InstructionBlock instructionBlock) {
        this.tag = tag;
        this.instructionBlock = instructionBlock;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public InstructionBlock getInstructionBlock() {
        return instructionBlock;
    }

    public void setInstructionBlock(InstructionBlock instructionBlock) {
        this.instructionBlock = instructionBlock;
    }

    /**
     * Prints the information of the instruction cache position
     */
    public void print() {
        System.out.print("Etiqueta " + this.tag + ", Bloque de Instrucciones: { ");
        if (this.instructionBlock != null)
            this.instructionBlock.printBlock();
        else
            System.out.print("Vacio");
        System.out.println(" }");
    }
}

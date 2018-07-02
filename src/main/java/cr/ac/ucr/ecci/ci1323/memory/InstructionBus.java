package cr.ac.ucr.ecci.ci1323.memory;

import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;

/**
 * References the instruction memory and manages its access, through the Bus class.
 *
 * @author Josue Leon Sarkis, Elias Calderon, Daniel Montes de Oca
 */
public class InstructionBus extends Bus {

    private volatile InstructionBlock[] instructionMemory;

    /**
     * Constructor which sets the instruction memory to the one provided, created by the parser.
     * @param instructionMemory
     */
    public InstructionBus(InstructionBlock[] instructionMemory) {
        super();
        this.instructionMemory = instructionMemory;
    }

    public InstructionBlock getInstructionBlock(int index) {
        // The total data blocks are subtracted for mapping purposes
        return instructionMemory[index - SimulationConstants.TOTAL_DATA_BLOCKS];
    }

    public void setInstructionBlock(int index, InstructionBlock instructionBlock) {
        this.instructionMemory[index] = instructionBlock;
    }

    public void setInstructionMemory(InstructionBlock[] instructionMemory) {
        this.instructionMemory = instructionMemory;
    }

    public Instruction getMemoryInstructionBlockInstruction(int blockNumber, int offset) {
        return this.getInstructionBlock(blockNumber).getInstruction(offset);
    }

    public InstructionBlock[] getInstructionMemory() {
        return this.instructionMemory;
    }

    public void printMemory() {
        System.out.println(instructionMemory.length);
        for (int i = 0; i < instructionMemory.length; i++) {
            this.instructionMemory[i].printBlock();
        }
    }
}

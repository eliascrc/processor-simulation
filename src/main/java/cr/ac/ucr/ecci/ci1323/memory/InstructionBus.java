package cr.ac.ucr.ecci.ci1323.memory;

public class InstructionBus extends Bus {

    InstructionBlock[] instructionMemory;

    InstructionBus(InstructionBlock[] instructionMemory) {
        super();
        this.instructionMemory = instructionMemory;
    }

    public InstructionBlock getInstructionBlock(int index) {
        return instructionMemory[index];
    }

}

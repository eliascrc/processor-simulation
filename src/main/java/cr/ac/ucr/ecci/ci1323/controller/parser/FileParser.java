package cr.ac.ucr.ecci.ci1323.controller.parser;

import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;
import cr.ac.ucr.ecci.ci1323.exceptions.InvalidInstructionException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages all the parsing of the contexts, which includes filling the instruction memory with the respective
 * instructions and also initializing each context with its according PC.
 */
public class FileParser {

    private List<File> files;
    private ContextQueue contextQueue;
    private InstructionBus instructionBus;

    /**
     * Constructor which checks how many contexts there are to add them to the files list and then parse each
     * one of them later.
     * @param contextQueue
     * @param instructionBus
     */
    public FileParser(ContextQueue contextQueue, InstructionBus instructionBus) {
        this.contextQueue = contextQueue;
        this.instructionBus = instructionBus;
        boolean fileExists = true;
        this.files = new ArrayList<>();
        for (int i = 0; fileExists; i++) {
            ClassLoader classLoader = getClass().getClassLoader();
            fileExists = (classLoader.getResource(i + ".txt") != null);

            if (fileExists) {
                File file = new File(classLoader.getResource(i + ".txt").getFile());
                this.files.add(file);
            }
        }
    }

    /**
     * Obtains all the instructions from the provided context files. For each instruction line it creates an
     * Instruction object and adds it to the respective instruction block which is added to the instruction memory
     * once the instruction block contains the 4 Instruction objects. If it is the last block and it didnt' fill the
     * 4 Instruction slots, it still adds it to the memory. Finally, it sets the instruction memory to the instruction
     * bus.
     */
    public void prepareSimulation() {
        List<String> instructions = this.readFiles();

        Instruction[] instructionBlockArray = new Instruction[SimulationConstants.TOTAL_INSTRUCTION_FIELDS];
        InstructionBlock[] instructionMemory = this.instructionBus.getInstructionMemory();
        int instructionMemoryIndex = 0;
        int instructionBlockIndex = 0;
        int neededInstructionBlocks = (int) Math.ceil((double) instructions.size() / SimulationConstants.INSTRUCTIONS_PER_BLOCK);
        for (String instructionString : instructions) {
            instructionBlockArray[instructionBlockIndex % 4] = this.parseInstruction(instructionString);
            instructionBlockIndex++;
            if(instructionBlockIndex % SimulationConstants.INSTRUCTIONS_PER_BLOCK == 0) {
                InstructionBlock instructionBlock = new InstructionBlock(instructionBlockArray);
                instructionMemory[instructionMemoryIndex] = instructionBlock;
                instructionBlockArray = new Instruction[SimulationConstants.TOTAL_INSTRUCTION_FIELDS];
                instructionMemoryIndex++;
            }
        }

        if(instructionMemoryIndex == neededInstructionBlocks - 1) {
            instructionMemory[instructionMemoryIndex] = new InstructionBlock(instructionBlockArray);
        }

        this.instructionBus.setInstructionMemory(instructionMemory);
    }

    /**
     * For each context file, it reads its lines of instructions and creates a new context with its respective
     * initial program counter based on where the last instruction of the previous context finished, or from the
     * memory direction in which instructions start (384) for the first one. It then pushes each context to the
     * context queue.
     * @return all the lines of instructions from all contexts
     */
    private List<String> readFiles() {
        List<String> lines = new LinkedList<>();

        int programCounterIndex = SimulationConstants.INSTRUCTIONS_START;
        int contextNumber = 0;
        for (File file: files) {
            Context context = new Context(programCounterIndex, contextNumber);
            List<String> newLines = getLinesFromFile(file);
            lines.addAll(newLines);
                programCounterIndex += SimulationConstants.WORD_SIZE * newLines.size();
            this.contextQueue.pushContext(context);
            contextNumber++;
        }

        return lines;
    }

    /**
     * Reads all the lines of instructions from each context file.
     * @param file
     * @return the instructions of a context file
     */
    private List<String> getLinesFromFile(File file) {
        List<String> fileLines = new LinkedList<>();

        try {
            fileLines = FileUtils.readLines(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileLines;
    }

    /**
     * Converts an instruction of 4 string numbers into an array of 4 ints.
     * @param strings
     * @return the array of the 4 ints that compose an instruction
     */
    private int[] stringsToInts(String[] strings) {
        int [] ints = new  int[strings.length];
        for (int i = 0; i < strings.length; i++)
            ints[i] = Integer.parseInt(strings[i]);

        return ints;
    }

    /**
     * Parses each instruction line of a context file to an Instruction object.
     * @param fileLine
     * @return the Instruction object with the instruction fields
     */
    private Instruction parseInstruction(String fileLine) {
        int[] instructionFields = stringsToInts(fileLine.split(" "));
        if (instructionFields.length != 4)
            throw new InvalidInstructionException("Instruction from file has " + instructionFields.length + " fields");

        return new Instruction(instructionFields);
    }
}

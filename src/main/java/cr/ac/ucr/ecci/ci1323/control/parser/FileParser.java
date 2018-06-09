package cr.ac.ucr.ecci.ci1323.control.parser;

import cr.ac.ucr.ecci.ci1323.control.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;
import cr.ac.ucr.ecci.ci1323.support.InvalidInstructionException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileParser {

    List<File> files;

    public static void main(String[] args) {
        new FileParser(new ContextQueue(), new InstructionBus()).parseFiles();
    }

    public FileParser(ContextQueue contextQueue, InstructionBus instructionBus) {
        boolean fileExists = true;

        for (int i = 0; fileExists; i++) {
            File file = new File("0.txt");
            fileExists = file.exists();
            System.out.println(file.exists());
            if (fileExists)
                this.files.add(file);
        }
    }

    public InstructionBlock[] parseFiles() {
        for (String fileLine : this.getLinesFromFiles()) {
            System.out.println(fileLine);
        }

        return null;
    }

    private List<String> getLinesFromFiles () {
        List fileLines = new ArrayList<String>();

        for (File file : this.files) {
            try {
                fileLines.addAll(FileUtils.readLines(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fileLines;
    }

    private int[] stringsToInts(String[] strings) {
        int [] ints = new  int[strings.length];
        for (int i = 0; i < strings.length; i++)
            ints[i] = Integer.parseInt(strings[i]);

        return ints;
    }

    private Instruction parseInstruction(String fileLine) {
        int[] instructionFields = stringsToInts(fileLine.split(" "));
        if (instructionFields.length != 4)
            throw new InvalidInstructionException("Instruction from file has " + instructionFields.length + " fields");

        Instruction instruction = new Instruction(instructionFields);
        return instruction;
    }
}

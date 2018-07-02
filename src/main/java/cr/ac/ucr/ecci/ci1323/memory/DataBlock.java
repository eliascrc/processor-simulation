package cr.ac.ucr.ecci.ci1323.memory;

import java.util.Arrays;

/**
 * Represents a data block which is composed by an array of integers which is the data.
 *
 * @author Josue Leon Sarkis, Elias Calderon, Daniel Montes de Oca
 */
public class DataBlock implements Cloneable {

    private volatile int[] words;

    public DataBlock(int[] words) {
        this.words = words;
    }

    /**
     * Returns a copy of the data block which is needed, because if not it would return a reference to it and it will
     * modify the shared data memory, which is not desired.
     * @return
     */
    @Override
    public DataBlock clone() {
        try {
            DataBlock dataBlockClone = (DataBlock) super.clone();
            dataBlockClone.setWords(Arrays.copyOf(this.words, this.words.length));
            return dataBlockClone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError("Unable to clone object of type [" + getClass().getName() + "]");
        }
    }

    public void printBlock() {
        for (int i = 0; i < this.words.length; i++) {
            System.out.print(this.words[i] + "\t");
        }
    }

    public void printBlockChangeLine() {
        for (int i = 0; i < this.words.length; i++) {
            System.out.print(this.words[i] + "\t");
        }
        System.out.println();
    }

    public int[] getWords() {
        return this.words;
    }

    public int getWord(int index) {
        return this.words[index];
    }

    public void setWords(int[] words) {
        this.words = words;
    }
}

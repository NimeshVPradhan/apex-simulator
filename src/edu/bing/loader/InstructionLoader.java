package edu.bing.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.bing.beans.Instruction;

public class InstructionLoader 
{
	private String filename;

	public InstructionLoader(String filename)
	{
		super();
		this.filename = filename;
	}

	public List<Instruction> loadInstructions() throws IOException
	{
		List<Instruction> instructionList = new ArrayList<Instruction>();
		BufferedReader br = null;
		try {
			File file = new File(filename);
			br = new BufferedReader(new FileReader(file));
			String line = null;
			String tempSplitIns[] = null;

			while((line = br.readLine()) != null)
			{
				tempSplitIns = line.split("[ ,#]+");
				Instruction instruction = new Instruction();
				switch (tempSplitIns.length) {
				case 1:
					instruction.setInstr_type(tempSplitIns[0]);
					break;

				case 2:
					instruction.setInstr_type(tempSplitIns[0]);
					instruction.setLiteral(Integer.parseInt(tempSplitIns[1]));
					break;

				case 3:
					instruction.setInstr_type(tempSplitIns[0]);
					instruction.setDest(tempSplitIns[1]);
					instruction.setLiteral(Integer.parseInt(tempSplitIns[2]));
					break;

				case 4:
					if(tempSplitIns[0].equalsIgnoreCase("STORE"))
					{
						instruction.setInstr_type(tempSplitIns[0]);
						instruction.setSrc3(tempSplitIns[1]);
						instruction.setSrc1(tempSplitIns[2]);
						if(tempSplitIns[3].startsWith("R"))
							instruction.setSrc2(tempSplitIns[3]);
						else
							instruction.setLiteral(Integer.parseInt(tempSplitIns[3]));
						break;
					}
					else
					{
						instruction.setInstr_type(tempSplitIns[0]);
						instruction.setDest(tempSplitIns[1]);
						instruction.setSrc1(tempSplitIns[2]);
						if(tempSplitIns[3].startsWith("R"))
							instruction.setSrc2(tempSplitIns[3]);
						else
							instruction.setLiteral(Integer.parseInt(tempSplitIns[3]));
						break;
					}
				}
				instructionList.add(instruction);
				//System.out.println(line);

			}
		}
		catch (FileNotFoundException e) {
			System.err.println("Please enter a valid filename/filepath");
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			System.err.println("Encountered error while reading an input file");
			e.printStackTrace();
			System.exit(0);
		}finally {
			br.close();
		}

		return instructionList;
	}

	/*public static void main (String args[])
	{
		InstructionLoader isl = new InstructionLoader("./Instructions.txt");
		List<Instruction> temp = isl.loadInstructions();
	}*/
}
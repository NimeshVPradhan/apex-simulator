package edu.bing.simulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import edu.bing.beans.Instruction;
import edu.bing.beans.Register;
import edu.bing.loader.InstructionLoader;


public class Apex {

	boolean initialized = false; 
	private static int MEM_LENGTH = 10000;
	int PC_value = 4000;
	int result_inEx1ALU2, result_inEx1ALU1, result_inEx2ALU2, result_inEx2ALU1, result_inWb;
	Integer result_inMem;
	int literal_zero = 0000; 
	int offset = 0;
	HashMap<Integer, Object> memory = new HashMap <Integer, Object> ();

	Scanner sc = new Scanner(System.in);
	static ArrayList<Instruction> instructionsToProcess = new ArrayList<Instruction>();
	boolean wb, mem, ex1a1, ex1a2, ex2a1, ex2a2, decode, fetch, branch, halt, set_exStage2, set_exStage1, jump_flag, branch_instr, src1IsDependent, src2IsDependent, src3IsDependent;
	boolean stall_fetch, stall_decode, stall_exstage, stall_mem, stall_wb;
	Instruction inFetch, inDecode, inEx1ALU1, inEx1ALU2, inEx2ALU1, inEx2ALU2, inMem, inWb;

	HashMap<String, Register> registerFile = new HashMap<String, Register>();
	HashMap<String, Register> shadowRegisterFile = new HashMap<String, Register>();
	int count=0;

	void operations() throws IOException
	{
		while(true)
		{
			System.out.println("Select an Option for Simulator: ");
			System.out.println("1. Initialise \n2. Simulate\n3. Display\n4. Help\n5. Exit");
			int choice=sc.nextInt();
			switch(choice)
			{
			case 1:
				initialize();
				break;
			case 2:
				simulate();
				break;
			case 3:
				display();
				break;
			case 4:
			case 5: System.exit(0);
			}
		}
	}

	void initialize()
	{
		initialized = true;
		System.out.println("Initialized...");
		for (int i = 0; i < MEM_LENGTH; i++){
			memory.put(i, new Integer(0));
		}

		for (int n=0; n<16; n++)
		{
			Register r = new Register();
			r.setReg_name("R"+n);
			r.setStatus(0);
			registerFile.put("R"+n, r);
			shadowRegisterFile.put("R"+n, r);
		}
		Register r = new Register();
		r.setReg_name("X");
		registerFile.put("X", r);
		shadowRegisterFile.put("X", r);
	}

	void simulate() throws IOException
	{
		if(!initialized){
			System.out.println("Please initialize the simulator");
		}
		else
		{
			InstructionLoader isl = new InstructionLoader("./Instructions.txt");
			ArrayList<Instruction> instructionsToProcess = new ArrayList<Instruction>();
			instructionsToProcess = (ArrayList<Instruction>) isl.loadInstructions();
			int i= PC_value,j=0;
			for (j=0; j < instructionsToProcess.size(); j++){
				memory.put(i, instructionsToProcess.get(j));
				i+=4;				
			}

			System.out.println("Enter the Number of Cycles for Simulation"); // commenting as confusing 
			int numberOfCycles = sc.nextInt();			//Actual Code Line
			//int numberOfCycles = instructionsToProcess.size()+10;				//Temp Code Line +10 is just a random number
			for(int n=0; n<numberOfCycles; n++)
			{
				if(!(fetch&&decode&&ex1a1&&ex1a2&&ex2a1&&ex2a2&&mem&&wb)) //iff all stages are empty
					//if(!halt)
				{
					wbStage(n);
					count++;
				}

			}
			System.out.println(count);
		}
	}


	void wbStage(int n)
	{
		wb = true;
		if(mem)
		{
			mem=false;
			inWb = inMem;
			result_inWb = result_inMem;
			if(!(inWb.getInstr_type().equals("STORE") || inWb.getInstr_type().equals("HALT") || inWb.getInstr_type().equals("BNZ") || inWb.getInstr_type().equals("BZ")||inWb.getInstr_type().equals("JUMP")||inWb.getInstr_type().equals("BAL")));
			{
				registerFile.get(inWb.getDest()).setReg_value(result_inWb);
				registerFile.get(inWb.getDest()).setStatus(0);
			}				
			System.out.println(registerFile.get(inWb.getDest()));
			wb=false;
			if(inWb.getInstr_type().equalsIgnoreCase("HALT"))
			{
				halt = true;
			}
			memStage(n);

		}
		else
		{
			memStage(n);
		}
	}


	void memStage(int n)
	{
		if(ex1a2 || ex2a2)
		{
			if(ex1a2)
			{
				mem = true;
				ex1a2 = false;
				inMem = inEx1ALU2;
				if(!(inMem.getInstr_type().equalsIgnoreCase("LOAD") || inMem.getInstr_type().equalsIgnoreCase("STORE")))
				{
					result_inMem = result_inEx1ALU2;
				}
				else
				{
					result_inMem = (Integer) memory.get(result_inEx1ALU2);
					if(inMem.getInstr_type().equalsIgnoreCase("LOAD"))
					{
						shadowRegisterFile.get(inMem.getDest()).setReg_value(result_inMem);
					}

					if(inMem.getInstr_type().equalsIgnoreCase("STORE"))
					{
						memory.put(result_inMem, inMem.getSrc3_value());
					}
				}

			}
			if(ex2a2)
			{
				mem = true;
				ex2a2 = false;
				inMem = inEx2ALU2;
			}
			exStage(n);
		}

		else
		{
			mem = false;
			exStage(n);
		}

	}

	//this will ensure that both EX stages are called in one cycle
	void exStage(int n){

		exStage1ALU2(n);
		exStage1ALU1(n);

		ex_branchDelayStage(n);
		ex_branchStage(n);

		decodeStage(n);
	}

	void exStage1ALU2(int n)
	{
		if(ex1a1)
		{
			ex1a2 = true;
			ex1a1=false;
			set_exStage1 = false;
			inEx1ALU2 = inEx1ALU1;
			switch (inEx1ALU2.getInstr_type())
			{
			case "ADD":
				result_inEx1ALU2 = inEx1ALU2.getSrc1_value() + inEx1ALU2.getSrc2_value();
				shadowRegisterFile.get(inEx1ALU2.getDest()).setReg_value(result_inEx1ALU2);
//				registerFile.get(inEx1ALU2.getDest()).setStatus(0);
				if((src1IsDependent||src2IsDependent||src3IsDependent)&&stall_decode)
					{stall_decode = false;
				stall_fetch = false;}
				break;

			case "SUB":
				result_inEx1ALU2 = inEx1ALU2.getSrc1_value() - inEx1ALU2.getSrc2_value();
				shadowRegisterFile.get(inEx1ALU2.getDest()).setReg_value(result_inEx1ALU2);
//				registerFile.get(inEx1ALU2.getDest()).setStatus(0);
				if((src1IsDependent||src2IsDependent||src3IsDependent)&&stall_decode)
					{stall_decode = false;
				stall_fetch = false;}
				break;

			case "MUL":
				result_inEx1ALU2 = inEx1ALU2.getSrc1_value() * inEx1ALU2.getSrc2_value();
				shadowRegisterFile.get(inEx1ALU2.getDest()).setReg_value(result_inEx1ALU2);
//				registerFile.get(inEx1ALU2.getDest()).setStatus(0);
				if((src1IsDependent||src2IsDependent||src3IsDependent)&&stall_decode)
					{stall_decode = false;
				stall_fetch = false;}
				break;

			case "AND":
				result_inEx1ALU2 = inEx1ALU2.getSrc1_value() & inEx1ALU2.getSrc2_value();
				shadowRegisterFile.get(inEx1ALU2.getDest()).setReg_value(result_inEx1ALU2);
//				registerFile.get(inEx1ALU2.getDest()).setStatus(0);
				if((src1IsDependent||src2IsDependent||src3IsDependent)&&stall_decode)
					{stall_decode = false;
					stall_fetch = false;}
				break;

			case "OR":
				result_inEx1ALU2 = inEx1ALU2.getSrc1_value() | inEx1ALU2.getSrc2_value();
				shadowRegisterFile.get(inEx1ALU2.getDest()).setReg_value(result_inEx1ALU2);
//				registerFile.get(inEx1ALU2.getDest()).setStatus(0);
				if((src1IsDependent||src2IsDependent||src3IsDependent)&&stall_decode)
					{stall_decode = false;
					stall_fetch = false;}
				break;

			case "EX-OR":
				result_inEx1ALU2 = inEx1ALU2.getSrc1_value() ^ inEx1ALU2.getSrc2_value();
				shadowRegisterFile.get(inEx1ALU2.getDest()).setReg_value(result_inEx1ALU2);
//				registerFile.get(inEx1ALU2.getDest()).setStatus(0);
				if((src1IsDependent||src2IsDependent||src3IsDependent)&&stall_decode)
					{stall_decode = false;
				stall_fetch = false;
			}
				break;

			case "MOVC":
				result_inEx1ALU2 = inEx1ALU2.getSrc1_value() + literal_zero;
				shadowRegisterFile.get(inEx1ALU2.getDest()).setReg_value(result_inEx1ALU2);
//				registerFile.get(inEx1ALU2.getDest()).setStatus(0);
				if((src1IsDependent||src2IsDependent||src3IsDependent)&&stall_decode)
					{stall_decode = false;
					stall_fetch = false;
					}
				break;

			case "LOAD":
				result_inEx1ALU2 = inEx1ALU2.getSrc1_value() + inEx1ALU2.getLiteral();
				break;
			case "STORE":
				result_inEx1ALU2 = inEx1ALU2.getSrc1_value() + inEx1ALU2.getLiteral();
				break;
			}

		}
		else {
			ex1a2 = false;
		}

	}

	private void updateSrcValues(Instruction current) 
	{
		if(src1IsDependent)
		{
			inEx1ALU1.setSrc1_value(shadowRegisterFile.get(current.getSrc1()).getReg_value());
			src1IsDependent=false;
		}
		else if(src2IsDependent)
		{
			inEx1ALU1.setSrc2_value(shadowRegisterFile.get(current.getSrc2()).getReg_value());
			src2IsDependent = false;
		}
		else if(src3IsDependent)
		{
			inEx1ALU1.setSrc3_value(shadowRegisterFile.get(current.getSrc3()).getReg_value());
			src3IsDependent = false;
		}	
	}

	void checkDependency(Instruction current, Instruction previous){
		if(current != null && previous != null)
		{
			if(previous.getDest().equals(current.getSrc1()))
				src1IsDependent = true;
			else if(previous.getDest().equals(current.getSrc2()))
				src2IsDependent = true;
			else if (previous.getDest().equals(current.getSrc3()))
				src3IsDependent = true;
		}
	}

	void exStage1ALU1(int n)
	{
		if(decode && !branch_instr)
		{
			ex1a1 = true;
			decode = false;
			inEx1ALU1 = inDecode;
			updateSrcValues(inEx1ALU1);
		}
		else
		{
			ex1a1 = false;
		}
	}

	void ex_branchDelayStage(int n)
	{
		if(ex2a1)
		{
			ex2a2=true;
			ex2a1=false;
			set_exStage2 = false;
			inEx2ALU2 = inEx2ALU1;
			if(branch){
				branch=false;
				jump_flag = true;
				//ex2a2=ex2a1=ex1a1=ex1a2=false;//flush instructions in EX stages
				//				halt = false;
			}
		}
		else
			ex2a1 = false;


	}

	void ex_branchStage(int n)
	{

		if(decode && branch_instr)
		{
			ex2a1 = true;
			decode = false;
			inEx2ALU1 = inDecode;
			branch_instr = false;

			switch(inEx2ALU1.getInstr_type())
			{
			case "HALT":
				//halt=true;
				break;

			case "BZ":
				if(result_inEx1ALU2==0)
				{ // assuming forwarding 
					PC_value = PC_value - 8 + inEx2ALU1.getLiteral();//updated PC value
					System.out.println("PC value updated: "+PC_value);
					branch = true;	
				}
				break;

			case "JUMP":
				System.out.println("PC value before: "+PC_value);
				PC_value = PC_value - 8 + inEx2ALU1.getLiteral();
				System.out.println("PC value updated: "+PC_value);
				branch=true;
				break;

			case "BNZ":
				if(result_inEx1ALU2!=0){ // assuming forwarding 
					PC_value = PC_value - 8 + inEx2ALU1.getLiteral();//updated PC value, please cross check destination address calculation
					System.out.println("PC value updated: "+PC_value);
					branch = true;		
				}
				break;

			case "BAL":
				registerFile.get("X").setReg_value(PC_value-4);
				PC_value = registerFile.get("X").getReg_value() + inEx2ALU1.getLiteral();
				branch = true;
				break;
			}
		}
		else
			ex2a1 = false;
	}



	void decodeStage(int n)
	{
		if(stall_decode || jump_flag){
			if (stall_decode)
				stall_fetch = true;
			decode = false;
			fetchStage(n);
		}
		else
			if(fetch)
			{
				decode = true;
				int source1,source2,source3,literal;
				Instruction previousInstruction = null;
				previousInstruction = inDecode;
				inDecode = inFetch;
				
				if((previousInstruction==null&&inDecode==null)&&(previousInstruction.getInstr_type().equals("LOAD")&&inDecode.getInstr_type().equals("STORE")))
				{
					stall_decode = true;
					stall_fetch = true;
//					decode = false;
				}
				checkDependency(inDecode, previousInstruction);

				switch(inDecode.getInstr_type()){
				case "ADD":
				case "SUB":
				case "MUL":
				case "AND":
				case "OR":
				case "EX-OR":
				case "LOAD":
					if(registerFile.get(inDecode.getSrc1()).getStatus()==1){//dependency scenario yet to be finalized
						System.out.println("Destination register busy.");
						stall_fetch = true;
						stall_decode = true;
//						decode = false;
						break;
					}
					else if(inDecode.getSrc2()!=null && registerFile.get(inDecode.getSrc2()).getStatus()==1){
						System.out.println("Destination register busy.");
						stall_fetch = true;
						stall_decode = true;
//						decode = false;
						break;
					}
					else{
						source1 = registerFile.get(inDecode.getSrc1()).getReg_value();
						inDecode.setSrc1_value(source1);
						if (inDecode.getSrc2()!=null){
							source2 = registerFile.get(inDecode.getSrc2()).getReg_value();
							inDecode.setSrc2_value(source2);
						}
						else {
							literal = inDecode.getLiteral();
							inDecode.setSrc2_value(literal);
						}
						branch_instr = false;
						set_exStage1 = true;
						registerFile.get(inDecode.getDest()).setStatus(1);
					}
					break;


				case "BZ":
				case "BNZ":
					stall_decode=true;
				case "HALT":	
				case "JUMP":
				case "BAL":
					literal = inDecode.getLiteral();
					inDecode.setSrc1_value(literal);
					inDecode.setDest("");
					set_exStage2 = true;
					branch_instr = true;				
					break;

				case "MOVC":
					if(registerFile.get(inDecode.getDest()).getStatus()==1){
						System.out.println("Destination register busy in MOVC.");
						stall_fetch = true;
						stall_decode = true;
						//					decode=false;
						break;
					}
					else{
						source1 = inDecode.getLiteral();
						inDecode.setSrc1_value(source1);
						set_exStage1 = true;
						branch_instr = false;
						registerFile.get(inDecode.getDest()).setStatus(1);
						break;
					}

				case "STORE":
					if(registerFile.get(inDecode.getSrc1()).getStatus()==1){//dependency scenario yet to be finalized
						System.out.println("Destination register busy.");
						stall_fetch = true;
						stall_decode = true;
//						decode = false;
//						decode = false;
						break;
					}
					else if(inDecode.getSrc2()!=null && registerFile.get(inDecode.getSrc2()).getStatus()==1){
						System.out.println("Destination register busy.");
						stall_fetch = true;
						stall_decode = true;
//						decode = false;
						break;
					}
					else if(inDecode.getSrc3()!=null && registerFile.get(inDecode.getSrc3()).getStatus()==1){
						System.out.println("Destination register busy.");
						stall_fetch = true;
						stall_decode = true;
//						decode = false;
						break;
					}
					else{
						stall_decode = false;
						stall_fetch = false;
					source1 = registerFile.get(inDecode.getSrc1()).getReg_value();
					inDecode.setSrc1_value(source1);
					source3 = registerFile.get(inDecode.getSrc3()).getReg_value();
					inDecode.setSrc3_value(source3);
					if (inDecode.getSrc2()!=null){
						source2 = registerFile.get(inDecode.getSrc2()).getReg_value();
						inDecode.setSrc2_value(source2);
					}
					else {
						literal = inDecode.getLiteral();
						inDecode.setSrc2_value(literal);
					}
					set_exStage1 = true;
					branch_instr = false;
					inDecode.setDest("");
					}
					break;


				}
				fetchStage(n);
			}
			else{
				decode = false;
				fetchStage(n);
			}

	}

	void fetchStage(int n)
	{	
		if(memory.get(PC_value).equals(new Integer(0))  || halt){// add stall_decode fr loger op
			if (stall_fetch)
				stall_fetch = false;
			if (jump_flag)
				jump_flag = false;
			if(stall_decode)
				stall_decode = false;
			if(halt)
				fetch = decode = ex1a1 = ex1a2 = ex2a1 = ex2a2 = mem = wb = true;
		}
		else{
			jump_flag = false;
			fetch = true;
			inFetch = (Instruction) memory.get(PC_value);
			PC_value = PC_value + 4;
			offset+=4;
		}
	}


	void display()
	{

	}

	public static void main(String[] args) throws IOException
	{
		System.out.println("Starting simulator...");
		Apex ap = new Apex();
		ap.operations();
	}

}
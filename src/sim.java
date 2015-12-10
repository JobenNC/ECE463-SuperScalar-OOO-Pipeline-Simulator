import java.util.List;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class sim {
	
	//RMT
	public static final int valid = 0;
	public static final int rt = 1;
	
	//Issue Queue
	public static final int dstTag = 1;
	public static final int rs1Rdy = 2;
	public static final int rs1TagVal = 3;
	public static final int rs2Rdy = 4;
	public static final int rs2TagVal = 5;
	
	//Reorder Buffer
	public static final int value = 0;
	public static final int dstROB = 1;
	public static final int rdy = 2;
	public static final int exc = 3;
	public static final int mis = 4;
	public static final int pc = 5;
	
	//InstrList & regs
	public static final int seqNo = 0;
	public static final int opTypeIL = 1;
	public static final int src1 = 2;
	public static final int src2 = 3;
	public static final int dstIL = 4;
	public static final int seqNoIL = 5;
	public static final int src1Rob = 6;
	public static final int src2Rob = 7;
	public static final int dstRob = 8;
	public static final int instRobTag = 9;
	
	//for exec list
	public static final int timer = 10;
	
	//instrList reg trackers
	public static final int FE = 5;
	public static final int DE = 6;
	public static final int RN = 7;
	public static final int RR = 8;
	public static final int DI = 9;
	public static final int IS = 10;
	public static final int EX = 11;
	public static final int WB = 12;
	public static final int RT = 13;
	
	public static final int retiredRT = 9;
	

	public sim() {
		// TODO Auto-generated constructor stub
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		
		
		// TODO Auto-generated method stub
		boolean end = false;
		int instrCount = 0;
		int cycleCount = 0;
		int bundleLen = 0;
		int robTag = -1;
		int iqSpace = 0;
		
		
		int robSize = Integer.parseInt(args[0]);
		int iqSize = Integer.parseInt(args[1]);
		int width = Integer.parseInt(args[2]);
		String traceFile = args[3];
		BufferedReader br = new BufferedReader(new FileReader(traceFile));
		String line = null;
		//HashMap<Integer, int[][]> cache;
		//c1.cache.get(j)
		//this.cache.put(index, set);
		
		int count;
		int lastInstr=-1;
		
		int[][] DEReg = new int[width][10];
		int[][] RNReg = new int[width][10];
		
		int[][] RRReg = new int[width][10];
		
		int[][] DIReg = new int[width][10];
		//int[][] IQ = new int[iqSize][10];
		List<int[]> IQ = new ArrayList<int[]>();
		int[] iqEntry = new int[10];
		
		//int[][] exec_list = new int[width*5][11];
		List<int[]> exec_list = new ArrayList<int[]>();
		int[] elEntry = new int[11];
		
		//int[][] WBReg = new int[width*5][10];
		List<int[]> WBReg = new ArrayList<int[]>();
		int[] wbEntry = new int[10];
		
		int[][] RMT = new int[67][2];
		int[] ARF = new int[67];
		
		int[][] entry = new int[14][2];
		
		List<int[][]> instrList = new ArrayList<int[][]>();
		
		rob ROB = new rob(robSize);
		
		//while ((line = br.readLine()) != null) {
		while (end != true) {
		//while (cycleCount < 8) {
			count = 0;
			//System.out.println("CYCLE NO:" + cycleCount + " ---------------------------------------------------");
			
			
			//Retire
			//Retuire up to width ready instrs from ROB head
				
			int retCount = 0;
			while ((ROB.canRetire()) && (retCount < width))
			{
				//ROB.printOut();
				int[] retired = ROB.retire();
				if (retired[seqNoIL] == lastInstr) 
				{
					//System.out.println("handles last instr: " + retired[seqNoIL]);
					end = true;
				}
				retCount++;
				
				//TODO: how do we know if we're retiring last instr?
				//TODO: how to map instr to retired ROB
				//if (retired[1] == lastInstr) end = true; 
				
				//TODO: update RMT
				// how does this work?
				int retiredDst = 1;
				//System.out.println(retired[retiredDst]);
				//if (retired[retiredDst] != -1) System.out.println(RMT[retired[retiredDst]][rt]);
				if ((retired[retiredDst] != -1) && (RMT[retired[retiredDst]][rt] == retired[retiredRT])) 
				{
					//System.out.println("Clearing RMT entry:" + retired[retiredDst]);
					RMT[retired[retiredDst]][valid] = 0;
				}
				//update timing data!
				entry = instrList.get(retired[seqNoIL]);
				entry[RT][1] = ((cycleCount + 1) - entry[RT][0]);
				//entry[RT][0] = cycleCount+1;
				//entry[RT][1] = (cycleCount - entry[RT][0]);
				instrList.set(retired[seqNoIL], entry);
				
				/*System.out.println(
					"Retiring in cycle:" + cycleCount + " Instr num:" + 
					retired[seqNoIL]
				);*/
				//ROB.printOut();
			}
			
			//Writeback ----------------------------------------------------------------------------
			//for each instr in WB, mark the instr as ready in its ROB entry
			//then remove from WBReg
			if (WBReg.size() > width*5) throw new NullPointerException();
			for (Iterator<int[]> iter = WBReg.iterator(); iter.hasNext();)
			{	//TODO: no upper limit here?
				wbEntry = iter.next();
				ROB.setReady(wbEntry[instRobTag]);
				iter.remove();
				//update timing data!
				entry = instrList.get(wbEntry[seqNoIL]);
				entry[WB][1] = ((cycleCount + 1) - entry[WB][0]);
				entry[RT][0] = cycleCount+1;
				//entry[EX][1] = (cycleCount - entry[EX][0]);
				//entry[WB][0] = cycleCount;
				//entry[RT][0] = cycleCount+1;
				instrList.set(wbEntry[seqNoIL], entry);
				
				/*System.out.println(
					"Write Back in cycle:" + cycleCount + " Instr num:" + 
					wbEntry[seqNoIL] + " opType:" + wbEntry[opTypeIL] + " dstIL:" + 
					wbEntry[dstIL] + " src1:" + wbEntry[src1] + " src2:" + 
					wbEntry[src2] + " dstRob:" + wbEntry[dstRob] + " src1Rob:" +
					wbEntry[src1Rob] + " src2Rob:" + wbEntry[src2Rob] + 
					" instRobTag:" + wbEntry[instRobTag]
				);*/
			}
			
			//Execute -----------------------------------------------------------------------------
			//from exclist, check for inss finishing exec THIS cycle.
				//1 - remove the instr from the exc list
				//2 - add the instr to WB
				//3 - wakeup dep insts (set their src op ready flags) in the IQ,
				//		DI (dispatch bundle), and the RR (rr bundle)
			//for (int i = 0; i < exec_list.size(); i++)
			if (exec_list.size() > width*5) throw new NullPointerException();
			for (Iterator<int[]> iter = exec_list.iterator(); iter.hasNext();)
			{
				
				elEntry = iter.next();
				elEntry[timer]--;
				if (elEntry[timer] == 0)
				{
					//remove
					iter.remove();
					//add to WB
					wbEntry = new int[10];
					wbEntry[valid] = elEntry[valid];
					wbEntry[opTypeIL] = elEntry[opTypeIL];
					wbEntry[src1] = elEntry[src1];
					wbEntry[src2] = elEntry[src2];
					wbEntry[dstIL] = elEntry[dstIL];
					wbEntry[seqNoIL] = elEntry[seqNoIL];
					wbEntry[src1Rob] = elEntry[src1Rob];
					wbEntry[src2Rob] = elEntry[src2Rob];
					wbEntry[dstRob] = elEntry[dstRob];
					wbEntry[instRobTag] = elEntry[instRobTag];
					
					WBReg.add(wbEntry);
					//TODO: may not be necessary, but check
					if (WBReg.size() > width*5) throw new NullPointerException();
					
					//wake up
					//TODO: debug only
					/*if (wbEntry[instRobTag] == 3)
					{
						System.out.println("----" + cycleCount);
						System.out.println("stop");
					}*/
					//set src op rdy flags in IQ, DI, RR bundles
					//if rob true for src & src is our robTag, check ROB for readiness, 
					//if true set rob to false.  Done?
					//IQ
					for (int i = 0; i < IQ.size(); i++)
					{
						//TODO: can't wake up from rob tags?
						iqEntry = IQ.get(i);
						//System.out.println("checking instr:" + iqEntry[seqNoIL]);
						if ((iqEntry[src1Rob] == 1) && (iqEntry[src1] == wbEntry[instRobTag]))
						{
							//System.out.println("Waking up from IQ: " + iqEntry[seqNoIL] + " Reg 1");
							//System.out.println(ROB.get(wbEntry[instRobTag])[seqNoIL]);
							iqEntry[src1Rob] = 0;
							
						}
						if ((iqEntry[src2Rob] == 1) && (iqEntry[src2] == wbEntry[instRobTag]))
						{
							//System.out.println("Waking up from IQ: " + iqEntry[seqNoIL] + " Reg 2");
							iqEntry[src2Rob] = 0;
							
						}
						IQ.set(i, iqEntry);
					}
					//DI
					for (int i = 0; i < width; i++)
					{
						if (DIReg[i][valid] == 0) break;
						if ((DIReg[i][src1Rob] == 1) && (DIReg[i][src1] == wbEntry[instRobTag]))
						{
							//System.out.println("Waking up from DI: " + DIReg[i][seqNoIL] + " Reg 1");
							DIReg[i][src1Rob] = 0;
						}
						if ((DIReg[i][src2Rob] == 1) && (DIReg[i][src2] == wbEntry[instRobTag]))
						{
							//System.out.println("Waking up from DI: " + DIReg[i][seqNoIL] + " Reg 2");
							DIReg[i][src2Rob] = 0;
						}
						
					}
					//RR
					for (int i = 0; i < width; i++)
					{
						if (RRReg[i][valid] == 0) break;
						if ((RRReg[i][src1Rob] == 1) && (RRReg[i][src1] == wbEntry[instRobTag]))
						{
							//System.out.println("Waking up from RR: " + RRReg[i][seqNoIL] + " Reg 1");
							RRReg[i][src1Rob] = 0;
						}
						if ((RRReg[i][src2Rob] == 1) && (RRReg[i][src2] == wbEntry[instRobTag]))
						{
							//System.out.println("Waking up from RR: " + RRReg[i][seqNoIL] + " Reg 2");
							RRReg[i][src2Rob] = 0;
						}	
					}
					
					//update timing data
					entry = instrList.get(wbEntry[seqNoIL]);
					entry[EX][1] = ((cycleCount + 1) - entry[EX][0]);
					entry[WB][0] = cycleCount+1;
					//entry[IS][1] = (cycleCount - entry[IS][0]);
					//entry[EX][0] = cycleCount;
					instrList.set(wbEntry[seqNoIL], entry);
					
					//log
					/*System.out.println(
						"Finishing execution in cycle:" + cycleCount + " Instr num:" + 
						wbEntry[seqNoIL] + " opType:" + wbEntry[opTypeIL] + " dstIL:" + 
						wbEntry[dstIL] + " src1:" + wbEntry[src1] + " src2:" + 
						wbEntry[src2] + " dstRob:" + wbEntry[dstRob] + " src1Rob:" +
						wbEntry[src1Rob] + " src2Rob:" + wbEntry[src2Rob] + 
						" instRobTag:" + wbEntry[instRobTag]
					);*/
				}
			}
			
			//Issue --------------------------------------------------------------------------------------
				//issue up to W oldest instructions from the IQ.
					//1 approach, implement oldest-first issuing is to make mult passes
						//through  the iq, each time find next oldest rdy instr and issue
					//1 way, annotate age of instr by assigning incrm seq num to each 
					//	instr as it is fetched from tr file
				// to issue
					//1- remove instr from iq
					//2 - add instr to execu list.  set a timer for the instr in exclist
						//that will allow model of its exec latency
			
			int seqNo = 0;
			int oldestSeqNo = instrCount;
			int oldestIndex = -1;
			for (int i = 0; i < width; i++)
			{
				//TODO: Verify these are going into exec_list in order!
				//get UP TO width oldest insts TODO: (means check for empty IQ each time)
				oldestIndex = -1;
				seqNo = 0;
				oldestSeqNo = instrCount;
				if (IQ.size() == 0) break;
				for (int j = 0; j < IQ.size(); j ++)
				{
					iqEntry = IQ.get(j);
					seqNo =  iqEntry[seqNoIL];
					if ((seqNo < oldestSeqNo) && (iqEntry[src1Rob] == 0) && (iqEntry[src2Rob] == 0)) 
					{
						oldestSeqNo = seqNo;
						oldestIndex = j;
					}
				}
				if (oldestIndex == -1) break;
				iqEntry = IQ.get(oldestIndex);
				//TODO: This removal is safe b/c iteration is done, right?
				//don't issue unless all ops are ready!
				//if ((iqEntry[src1Rob] == 0) && (iqEntry[src2Rob] == 0))
				//{
				IQ.remove(oldestIndex);
				
					//add to exec list! reinitialize new array before adding?
				
					if (exec_list.size() < width*5)
					{
						elEntry = new int[11];
						elEntry[valid] = iqEntry[valid];
						elEntry[opTypeIL] = iqEntry[opTypeIL];
						elEntry[src1] = iqEntry[src1];
						elEntry[src2] = iqEntry[src2];
						elEntry[dstIL] = iqEntry[dstIL];
						elEntry[seqNoIL] = iqEntry[seqNoIL];
						elEntry[src1Rob] = iqEntry[src1Rob];
						elEntry[src2Rob] = iqEntry[src2Rob];
						elEntry[dstRob] = iqEntry[dstRob];
						elEntry[instRobTag] = iqEntry[instRobTag];
						
						if (iqEntry[opTypeIL] == 2) elEntry[timer] = 5;
						else elEntry[timer] = iqEntry[opTypeIL] + 1;
						exec_list.add(elEntry);
					}
					else
					{
						//TODO: Remove!
						System.out.println("exceeded size of the exec list");
						throw new NullPointerException();
					}
					
					//update timing data!
					//Ending stay in issue, gong to execute
					entry = instrList.get(iqEntry[seqNoIL]);
					entry[IS][1] = ((cycleCount + 1) - entry[IS][0]);
					entry[EX][0] = cycleCount+1;
					
					//entry[IS][0] = cycleCount;
					//entry[DI][1] = (cycleCount - entry[DI][0]);
					
					instrList.set(iqEntry[seqNoIL], entry);
					
					/*System.out.println(
						"Issuing in cycle:" + cycleCount + " Instr num:" + 
						elEntry[seqNoIL] + " opType:" + elEntry[opTypeIL] + " dstIL:" + 
						elEntry[dstIL] + " src1:" + elEntry[src1] + " src2:" + 
						elEntry[src2] + " dstRob:" + elEntry[dstRob] + " src1Rob:" +
						elEntry[src1Rob] + " src2Rob:" + elEntry[src2Rob] + 
						" instRobTag:" + elEntry[instRobTag] + " timer:" + elEntry[timer]
					);*/
				//}
				
			}
			
			
			
			//Dispatch-----------------------------------------------------------------------------------
				//if DI has bundle, if num of free IQ entries less than size of the 
				//disp bndle in DI, do nothing.  if num free IQ entries greater than or
				// equal to size of dispatch bundle in DI, dispatchall instrs from DI
				//to the IQ
			
			//check IQ space left
			for (int i = 0; i <= iqSize; i ++)
			{
				//TODO: Will this ever be a problem?
				//if the IQ is longer than i
				iqSpace = iqSize - i;
				//iqSpace = iqSize;
				if (IQ.size() > i)
				{
					iqEntry = IQ.get(i);
					if (iqEntry[valid] == 0) 
					{
						break;
					}
				}
				else break;
			}
			//Check bundle length
			for (int i = 0; i < width; i++)
			{
				if (DIReg[i][valid] == 0) break;
				bundleLen = i+1;
				
			}
			
			
			//System.out.println("IQ space:" + iqSpace + " DI bundle len:" + bundleLen);
			if ((DIReg[0][valid] == 1) && iqSpace >= bundleLen)
			{
				for (int i = 0; i < width; i++)
				{
					if (DIReg[i][valid] == 0) break;
					//put in IQ!
					
					//TODO: !!!!!IMPORTANT must reinitialize arrays before adding!!!!
					iqEntry = new int[10];
					iqEntry[valid] = 1;
					iqEntry[opTypeIL] = DIReg[i][opTypeIL];
					iqEntry[src1] = DIReg[i][src1];
					iqEntry[src2] = DIReg[i][src2];
					iqEntry[dstIL] = DIReg[i][dstIL];
					iqEntry[seqNoIL] = DIReg[i][seqNoIL];
					iqEntry[src1Rob] = DIReg[i][src1Rob];
					iqEntry[src2Rob] = DIReg[i][src2Rob];
					iqEntry[dstRob] = DIReg[i][dstRob];
					iqEntry[instRobTag] = DIReg[i][instRobTag];
					//add to IQ at end
					IQ.add(iqEntry);
					
					if (IQ.size() > iqSize) 
					{
						System.out.println(iqSpace);
						throw new NullPointerException();
					}
					
					//remove from RR
					DIReg[i][valid] = 0;
					
					//update timing data!
					//Ending stay in dispatch, going to issue
					entry = instrList.get(DIReg[i][seqNoIL]);
					//entry[IS][0] = cycleCount+1;
					//entry = instrList.get(RRReg[i][seqNoIL]);
					entry[DI][1] = ((cycleCount + 1) - entry[DI][0]);
					entry[IS][0] = cycleCount+1;
					
					
					//entry[RR][1] = (cycleCount - entry[RR][0]);
					//entry[DI][0] = cycleCount;
					instrList.set(DIReg[i][seqNoIL], entry);
					
					if (DIReg[i][seqNoIL] == 9618) 
					{
						System.out.println(iqSpace);
						System.out.println("IQ data:");
						for (int j = 0; j < IQ.size(); j++)
						{
							iqEntry = IQ.get(j);
							System.out.println(
								j +": " + "\t cycle No:" + cycleCount + " seq No:" + 
								iqEntry[seqNoIL] + " opType:" + iqEntry[opTypeIL] + " dstIL:" + 
								iqEntry[dstIL] + " src1:" + iqEntry[src1] + " src2:" + 
								iqEntry[src2] + " dstRob:" + iqEntry[dstRob] + " src1Rob:" +
								iqEntry[src1Rob] + " src2Rob:" + iqEntry[src2Rob] + 
								" instRobTag:" + iqEntry[instRobTag]
							);
						}
					}
					
					/*System.out.println(
							"Dispatched on cycle " + cycleCount + ": " + 
							iqEntry[seqNoIL] + " opType:" + iqEntry[opTypeIL] + " dstIL:" + 
							iqEntry[dstIL] + " src1:" + iqEntry[src1] + " src2:" + 
							iqEntry[src2] + " dstRob:" + iqEntry[dstRob] + " src1Rob:" +
							iqEntry[src1Rob] + " src2Rob:" + iqEntry[src2Rob] + 
							" instRobTag:" + iqEntry[instRobTag]
					);*/
					
				}
				
			}
			
			//Reg Read ----------------------------------------------------------------------------------
				//if RR has bundle, if DI is not empty do nothing.  If DI is empty 
				// process RR bundle and advance it to DI
					// not modeling vals,just ascertain readiness of renamed src ops
					// take care that producers in last cycle of exe wakeup dep operands
					// not just in IQ, but in 2 other stages incl RR. (to avoid deadlock)
			
			//check if RR has bundle and DI is empty
			if ((RRReg[0][valid] == 1) && (DIReg[0][valid] == 0))
			{
				for (int i = 0; i < width; i++)
				{
					if (RRReg[i][valid] == 0) break;

					//ascertain readiness of renamed src ops
					//if src is a rob tag, check ready bit!
					/*if (RRReg[i][seqNoIL] == 8)
					{
						System.out.println(RRReg[i][src1Rob]);
						System.out.println("stop");
					}*/
					if (RRReg[i][src1Rob] == 1)
					{
						/*if (RRReg[i][seqNoIL] == 8)
						{
							System.out.println("stop");
						}*/
						if (ROB.isReady(RRReg[i][src1]))
						{
							//we don't need to track that original reg val anymore, do we?
							DIReg[i][src1Rob] = 0;
						}
						else
						{
							DIReg[i][src1Rob] = 1;
						}
					}
					else DIReg[i][src1Rob] = 0;
					DIReg[i][src1] = RRReg[i][src1];
					
					
					if (RRReg[i][src2Rob] == 1)
					{
						//System.out.println(RRReg[i][src2]);
						if (ROB.isReady(RRReg[i][src2]))
						{
							
							DIReg[i][src2Rob] = 0;
						}
						else
						{
							DIReg[i][src2Rob] = 1;
						}
					}
					else DIReg[i][src2Rob] = 0;
					DIReg[i][src2] = RRReg[i][src2];
					
					//move all to DI
					DIReg[i][valid] = 1;
					DIReg[i][opTypeIL] = RRReg[i][opTypeIL];
					DIReg[i][dstIL] = RRReg[i][dstIL];
					DIReg[i][seqNoIL] = RRReg[i][seqNoIL];
					DIReg[i][dstRob] = RRReg[i][dstRob];
					DIReg[i][instRobTag] = RRReg[i][instRobTag];
					
					//remove from RR
					RRReg[i][valid] = 0;
					
					//update timing data!
					//Ending stay in RR, going to Dispatch
					entry = instrList.get(RRReg[i][seqNoIL]);
					entry[RR][1] = ((cycleCount + 1) - entry[RR][0]);
					entry[DI][0] = cycleCount+1;
					//instrList.set(DEReg[i][seqNoIL], entry);
					
					//entry = instrList.get(RRReg[i][seqNoIL]);
					//entry[RN][1] = (cycleCount - entry[RN][0]);
					//entry[RR][0] = cycleCount;
					instrList.set(RRReg[i][seqNoIL], entry);
					
					/*System.out.println(
							"Register Read on cycle " + cycleCount + ": " + 
							DIReg[i][seqNoIL] + " opType:" + DIReg[i][opTypeIL] + " dstIL:" + 
							DIReg[i][dstIL] + " src1:" + DIReg[i][src1] + " src2:" + 
							DIReg[i][src2] + " dstRob:" + DIReg[i][dstRob] + " src1Rob:" +
							DIReg[i][src1Rob] + " src2Rob:" + DIReg[i][src2Rob] + 
							" instRobTag:" + DIReg[i][instRobTag]
					);*/
				}
			}
			
			//Rename --------------------------------------------------------------------------------------------------
				//if bundle, if RR not empty or ROB doesn't have enough free entries
				// for the bundle, do nothing.
			
				// if RR is empty and ROB has room, prcoess to RR
					//allocate entry in ROB for instr
					//rename its src regs
					//rename its destin reg (if has)
					//must rename in program order (should be in order here)
			
			//Check bundle length
			for (int i = 0; i < width; i++)
			{
				if (RNReg[i][valid] == 0) break;
				bundleLen = i+1;
				
			}
			//also chekc for open ROB entries
			//System.out.println("bundle read into RN: " + bundleLen);
			
			if ((RNReg[0][valid] == 1) && (RRReg[0][valid]== 0) && (ROB.getSpace() >= bundleLen))
			{ 
				//put in ROB, already in program order
				for (int i = 0; i < bundleLen; i++)
				{
					//instr is palced in rob, dstIL replaed w/ rob tag in RR stage
					//this is just to mark the instr
					robTag = ROB.put(RNReg[i]);
					//ROB.printOut();
					RRReg[i][valid] = 1;
					RRReg[i][instRobTag] = robTag;
					
					
					//rename src reg1
					if ((RNReg[i][src1] != -1) && (RMT[RNReg[i][src1]][valid] == 1))
					{	
						/*if (RNReg[i][seqNoIL] == 8)
						{
							System.out.println("stop");
						}*/
						RRReg[i][src1Rob] = 1;
						RRReg[i][src1] = RMT[RNReg[i][src1]][rt];
					}
					else 
					{
						RRReg[i][src1Rob] = RNReg[i][src1Rob];
						RRReg[i][src1] = RNReg[i][src1];
					}
					
					//rename src reg2
					if ((RNReg[i][src2] != -1) && (RMT[RNReg[i][src2]][valid] == 1))
					{
						RRReg[i][src2Rob] = 1;
						RRReg[i][src2] = RMT[RNReg[i][src2]][rt];
					}
					else 
					{
						RRReg[i][src2Rob] = RNReg[i][src2Rob];
						RRReg[i][src2] = RNReg[i][src2];
					}
					
					//rename the dst reg if applicable
					if (RNReg[i][dstIL] != -1)
					{
						RRReg[i][dstRob] = 1;
						RRReg[i][dstIL] = robTag;
						//update the RMT
						//System.out.println("Updating RMT in rename, entry:" + RNReg[i][dstIL] + " val:" + robTag);
						RMT[RNReg[i][dstIL]][valid] = 1;
						RMT[RNReg[i][dstIL]][rt] = robTag;
						
					}
					else 
					{
						RRReg[i][dstIL] = RNReg[i][dstIL];
					}
					
					//advance from RN to RR
					RRReg[i][opTypeIL] = RNReg[i][opTypeIL];
					RRReg[i][seqNoIL] = RNReg[i][seqNoIL];
					
					//remove from RNReg
					RNReg[i][valid] = 0;
					
					//update timing data!
					//Ending stay in rename, goind to Register Read on the next cycle.
					entry = instrList.get(RNReg[i][seqNoIL]);
					entry[RN][1] = (cycleCount+1) - entry[RN][0];
					entry[RR][0] = cycleCount + 1;
					//entry[DE][1] = (cycleCount - entry[DE][0]);
					//entry[RN][0] = cycleCount;
					instrList.set(RNReg[i][seqNoIL], entry);
					
					/*System.out.println(
						"Ranamed on cycle " + cycleCount + ": " + 
						RRReg[i][seqNoIL] + " opType:" + RRReg[i][opTypeIL] + " dstIL:" + 
						RRReg[i][dstIL] + " src1:" + RRReg[i][src1] + " src2:" + 
						RRReg[i][src2] + " dstRob:" + RRReg[i][dstRob] + " src1Rob:" +
						RRReg[i][src1Rob] + " src2Rob:" + RRReg[i][src2Rob] + 
						" instRobTag:" + RRReg[i][instRobTag]
					);*/
				}
			}
			
			//Decode ---------------------------------------------------------------------------------------
				//if DE has bundl
					//if RN not empty, do nothihgn
					//if empty, advance dec from DE to RN
			
			if ((RNReg[0][valid] == 0) && DEReg[0][valid] == 1)
			{
				for (int i = 0; i < width; i++)
				{
					if (DEReg[i][valid] == 0) break;
					
					
					
					RNReg[i][valid] = DEReg[i][valid];
					RNReg[i][opTypeIL] = DEReg[i][opTypeIL];
					RNReg[i][src1] = DEReg[i][src1];
					RNReg[i][src2] = DEReg[i][src2];
					RNReg[i][dstIL] = DEReg[i][dstIL];
					RNReg[i][seqNoIL] = DEReg[i][seqNoIL];
					RNReg[i][src1Rob] = DEReg[i][src1Rob];
					RNReg[i][src2Rob] = DEReg[i][src2Rob];
					
					
					//Clear as it leaves
					DEReg[i][valid] = 0;
					
					//Update FE Timing and DE entry
					//Retrieved from DE bundle, FE i over.
					//TODO: Fetch already set DE in.
					//  This lets you kow how long you stayed.  will start Rename on next cycle.
					entry = instrList.get(DEReg[i][seqNoIL]);
					//entry[FE][1] = (cycleCount - entry[FE][0]);
					//entry[DE][0] = cycleCount;
					entry[DE][1] = ((cycleCount + 1) - entry[DE][0]);
					entry[RN][0] = cycleCount+1;
					instrList.set(DEReg[i][seqNoIL], entry);
		
					/*System.out.println(
						"Decoding on cycle " + cycleCount + " seqNo:" +  
						DEReg[i][seqNoIL] + " opType:" + DEReg[i][opTypeIL] + " dst:" + 
						DEReg[i][dstIL] + " src1:" + DEReg[i][src1] + " src2:" + DEReg[i][src2]
					);*/
					
				}
				
			}
			
			//Fetch ---------------------------------------------------------------------------------------
				//do nothing if no more instrs or DE is not empty
				//if there are more instrs in trace file & de empty, fetch up to
				// wide instrs from the trace file into DE.  only fetch fewer if that's
				//all that remains in trace file.
			
			if ((end==false) && (DEReg[0][0] == 0))
			{
				while ((count < width) && ((line = br.readLine()) != null))
				{
					instrCount++;
				
					String pc = line.split(" ")[0];
					int opType = Integer.parseInt(line.split(" ")[1]);
					int dstReg = Integer.parseInt(line.split(" ")[2]);
					int srcReg1 = Integer.parseInt(line.split(" ")[3]);
					int srcReg2 = Integer.parseInt(line.split(" ")[4]);
					
				
					//TODO: This seq no may be redundant
					entry = new int[14][2]; //if you don't initialize, you'll have problems
					//entry[seqNo][0] = instrCount-1;
					entry[opTypeIL][0] = opType;
					entry[src1][0] = srcReg1;
					entry[src2][0]= srcReg2;
					entry[dstIL][0] = dstReg;
					//TODO:  You can't stay in FE for more than 1 cycle.
					// So Decode will always start on the next cycle.
					entry[FE][0] = cycleCount;
					entry[FE][1] = 1;
					entry[DE][0] = cycleCount+1;
					
					instrList.add(instrCount-1, entry);
					
					/*System.out.println(
						"Fetching on cycle " + cycleCount + " instrCount:" + 
						(instrCount-1) + " opType:" + opType + " dstReg:" + dstReg + 
						" srcReg1:" + srcReg1 + " srcReg2:" + srcReg2
					);*/
					
					//put into DE, they go into the reg in program order
					//TODO: need to unset valid!
					DEReg[count][valid] = 1;
					DEReg[count][opTypeIL] = opType;
					DEReg[count][src1] = srcReg1;
					DEReg[count][src2] = srcReg2;
					DEReg[count][dstIL] = dstReg;	
					DEReg[count][seqNoIL] = instrCount-1;
					
					
					count++;
				}
				
				if (line==null) 
				{
					lastInstr = instrCount-1;
					for (int i = count; i < width; i++)
					{
						DEReg[i][valid] = 0;
					}
				}
				//advance the cycle	
			}
		
		/*ROB.printOut();
		System.out.println("RMT:");
		for (int i = 0; i < 67; i++)
		{
			System.out.println(
				"\t" + i + ": " + RMT[i][valid] + " " + RMT[i][rt]
			);
		}*/
			
		cycleCount++;
		
		//if (cycleCount >= 23000)
		/*if (cycleCount >= 3000)
		{
			ROB.printOut();
			System.out.println(ROB.canRetire());
			System.out.println("IQ data:");
			for (int i = 0; i < IQ.size(); i++)
			{
				iqEntry = IQ.get(i);
				System.out.println(
					i +": " + "\t cycle No:" + cycleCount + " seq No:" + 
					iqEntry[seqNoIL] + " opType:" + iqEntry[opTypeIL] + " dstIL:" + 
					iqEntry[dstIL] + " src1:" + iqEntry[src1] + " src2:" + 
					iqEntry[src2] + " dstRob:" + iqEntry[dstRob] + " src1Rob:" +
					iqEntry[src1Rob] + " src2Rob:" + iqEntry[src2Rob] + 
					" instRobTag:" + iqEntry[instRobTag]
				);
			}
			
			System.out.println("RMT:");
			for (int i = 0; i < 67; i++)
			{
				System.out.println(
					"\t" + i + ": " + RMT[i][valid] + " " + RMT[i][rt]
				);
			}
			
			System.out.println(instrCount);
			
			System.out.println("Exec List data:");
			for (int i = 0; i < exec_list.size(); i++)
			{
				elEntry = exec_list.get(i);
				System.out.println(
					i +": " + "\t cycle No:" + cycleCount + " seq No:" + 
					elEntry[seqNoIL] + " opType:" + elEntry[opTypeIL] + " dstIL:" + 
					elEntry[dstIL] + " src1:" + elEntry[src1] + " src2:" + 
					elEntry[src2] + " dstRob:" + elEntry[dstRob] + " src1Rob:" +
					elEntry[src1Rob] + " src2Rob:" + elEntry[src2Rob] + 
					" instRobTag:" + elEntry[instRobTag] + " timer:" + elEntry[timer]
				);
			}
		}*/
		}
		//OB.printOut();
		System.out.println("instruction count:" + instrCount);

		PrintWriter writer = new PrintWriter("runOutput.txt", "UTF-8");
		//writer.println("The first line");
		//writer.println("The second line");
		
		for (int i = 0; i < instrList.size(); i++)
		//for (int i = 0; i < 4; i++)
		{
			entry = instrList.get(i);
			writer.println(
				i + " " + "fu{" + entry[opTypeIL][0] + "} " + "src{" + entry[src1][0] + "," + entry[src2][0] + "} " + 
				"dst{" + entry[dstIL][0] + "} " + "FE{" + entry[FE][0] + "," + entry[FE][1] + "} " + 
				"DE{" + entry[DE][0] + "," + entry[DE][1] + "} " +
				"RN{" + entry[RN][0] + "," + entry[RN][1] + "} " + 
				"RR{" + entry[RR][0] + "," + entry[RR][1] + "} " +
				"DI{" + entry[DI][0] + "," + entry[DI][1] + "} " + 
				"IS{" + entry[IS][0] + "," + entry[IS][1] + "} " + 
				"EX{" + entry[EX][0] + "," + entry[EX][1] + "} " +
				"WB{" + entry[WB][0] + "," + entry[WB][1] + "} " + 
				"RT{" + entry[RT][0] + "," + entry[RT][1] + "}"
			);
		}
		
		writer.println("# === Simulator Command =========");
		writer.println("# " + "./sim" + " " + args[0] + " " + args[1] + " " + args[2] + " " + args[3]);
		writer.println("# === Processor Configuration ===");
		writer.println("# ROB_SIZE = " + args[0]);
		writer.println("# IQ_SIZE = " + args[1]);
		writer.println("# WIDTH = " + args[2]);
		writer.println("# === Simulation Results ========");
		writer.println("# Dynamic Instruction Count    = " + instrCount);
		writer.println("# Cycles                       = " + cycleCount);
		writer.println("# Instructions Per Cycle (IPC) = " + String.format("%1$, .2f", ((double) instrCount/ (double) cycleCount)));
		writer.close();
				
		System.out.println("cycle count: " + cycleCount);
		
	}
	

}
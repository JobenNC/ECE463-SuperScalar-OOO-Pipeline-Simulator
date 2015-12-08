
public class rob {
	int [][] buffer;
	int size;
	int head;
	int tail;
	boolean full;
	boolean empty;
	
	public static final int value = 0;
	public static final int dst = 1;
	public static final int ready = 2;
	
	public static final int seqNo = 0;
	public static final int opTypeIL = 1;
	public static final int src1 = 2;
	public static final int src2 = 3;
	public static final int dstIL = 4;
	public static final int seqNoIL = 5;
	public static final int src1Rob = 6;
	public static final int src2Rob = 7;
	public static final int dstRob = 8;
	
	public static final int retiredRT = 9;
	
	
	public rob(int robSize) {
		// TODO Auto-generated constructor stub
		int[][] rBuffer = new int[robSize][10];
		this.empty = true;
		this.full = false;
		this.size = robSize;
		this.buffer = rBuffer;
		this.head = 0;
		this.tail = 0;
	}
	
	public int getSpace()
	{
		if (this.empty == true) return this.size;
		
		if (this.full) return 0;
		
		if ((tail - head) > 0) return this.size - (tail - head);
		else
		{
			//TODO: this also works for full :-/
			return this.size - ((this.size + tail) - head);
		}
	
	}
	
	
	public int put(int[] instr)
	{
		//handle rollover
		if (this.full == true) 
		{
			this.printOut();
			throw new NullPointerException();
		}
		
		this.empty = false;
		
		if ((this.tail) == (this.size-1))
		{
			this.tail = 0;
		}
		else this.tail++;
		
		int[] add = new int[10];
		add[dst] = instr[dstIL];
		add[ready] = 0;
		add[seqNoIL] = instr[seqNoIL];
		//System.out.println(this.tail);
		this.buffer[this.tail] = add;
		
		if (this.tail == this.head) this.full = true;
		
		return this.tail;
	}
	
	public boolean isReady(int index)
	{
		if (this.buffer[index][ready] == 1) return true;
		else return false;
	}
	
	public void setReady(int index)
	{
		//TODO: any checks needed?
		this.buffer[index][ready] = 1;
	}
	
	public int[] retire()
	{
		//retire instrs in progr order
		//this.printOut();
		this.full = false;
		int[] toReturn = new int[10];
		int nextHead = 0;
		if ((this.head+1) == (this.size)) nextHead = 0;
		else nextHead = head+1;	
		this.head = nextHead;
		
		toReturn = this.buffer[this.head];
		toReturn[retiredRT] = this.head;
		
		//can't check greater than, bc circular buffer
		if (this.head == this.tail)
		{
			this.empty = true;
			System.out.println("empty!");
			//this.printOut();
			//throw new NullPointerException();
		}
		return toReturn;
	}
	
	public int[] get(int index)
	{
		return this.buffer[index];
	}
	
	public boolean canRetire()
	{
		//if (this.head == this.tail) return false;
		//System.out.println(nextHead);
		if (this.empty == true) return false;
		
		int nextHead = 0;
		if ((this.head+1) == this.size) nextHead = 0;
		else nextHead = head+1;
		//if ((this.buffer[nextHead][ready] == 1) || (this.buffer[nextHead][dst] == -1)) return true;
		if ((this.buffer[nextHead][ready] == 1)) return true;
		else return false;
		
	}
	
	public void printOut()
	{
		System.out.println("ROB:");
		for (int i = 0; i < this.size; i++)
		{
			System.out.print("\t ");
			if (i == this.head) System.out.print("h -> ");
			if (i == this.tail) System.out.print("t -> ");
			System.out.print(
				"dst:" + this.buffer[i][dst] + " rdy:" + this.buffer[i][ready] + " inst:" + this.buffer[i][seqNoIL] + "\n"
			);
		}
	}

}

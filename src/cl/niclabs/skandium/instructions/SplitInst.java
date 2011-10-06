/*   Skandium: A Java(TM) based parallel skeleton library.
 *   
 *   Copyright (C) 2011 NIC Labs, Universidad de Chile.
 * 
 *   Skandium is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Skandium is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.

 *   You should have received a copy of the GNU General Public License
 *   along with Skandium.  If not, see <http://www.gnu.org/licenses/>.
 */
package cl.niclabs.skandium.instructions;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import cl.niclabs.skandium.events.When;
import cl.niclabs.skandium.events.Where;
import cl.niclabs.skandium.muscles.Merge;
import cl.niclabs.skandium.system.events.SkeletonTraceElement;


/**
 * This is a utility instruction and does not represent a {@link cl.niclabs.skandium.skeletons.Skeleton} in particular.
 * Given a list of substacks and taking the array produced by the {@link cl.niclabs.skandium.muscles.Split} execution, it creates
 * a list of child substacks to execute each element of the <code>param</code> array.
 */

public class SplitInst extends AbstractInstruction {
	
	boolean cond;
	List<Stack<Instruction>> substacks;
	@SuppressWarnings("rawtypes")
	Merge merge;
	Stack<Integer> rbranch;

	/**
	 * The main constructor.
	 * @param substacks list of substacks, if the list has just 1 substack, it is copied to complete the <code>param</code> size. 
	 * @param merge The code to merge the results of the execution of each subparam.
	 * @param strace nested skeleton tree branch of the current execution.
	 */
	@SuppressWarnings("rawtypes")
	public SplitInst(List<Stack<Instruction>> substacks, Merge merge, SkeletonTraceElement[] strace){
		super(strace);
		this.substacks = substacks;
		this.merge = merge;
	}
	
	/**
	 * The constructor when reducing DaCInst.
	 */
	@SuppressWarnings("rawtypes")
	public SplitInst(List<Stack<Instruction>> substacks, Merge merge, SkeletonTraceElement[] strace, Stack<Integer> rbranch){
		this(substacks,merge,strace);
		this.rbranch = rbranch;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <P> Object interpret(P param, Stack<Instruction> stack, 
			List<Stack<Instruction>> children) throws Exception {
		Object[] params = (Object[])param;
		int subsize = substacks.size();
		if((subsize != 1) && params.length != subsize){
			throw new Exception("Invalid number of divisions. Expected "+ substacks.size() +" but was "+params.length+".");
		}
		
		// For each stack copy all of its elements
		for(int i=0; i < params.length; i++){
			Stack<Instruction> subStack;
			// ID and RBranch (in DaC case) calculation
			if (rbranch != null) {
				Stack<Integer> subrbranch = new Stack<Integer>();
				subrbranch.addAll(rbranch);
				subrbranch.push(i);
				subStack = copyStack(this.substacks.get(0));
				DaCInst subDaC = ((DaCInst)subStack.peek());
				subDaC.rbranch = subrbranch;
				int id = Arrays.deepHashCode(subrbranch.toArray(new Integer[subrbranch.size()]));
				SkeletonTraceElement[] subtrace = subDaC.getSkeletonTrace();
				subtrace[subtrace.length-1] = new SkeletonTraceElement(subtrace[subtrace.length-1].getSkel(),id);
				subStack.push(new EventInst(When.BEFORE, Where.CONDITION, subDaC.getSkeletonTrace(), subrbranch, cond));				
			} else {
				subStack = copyStack(subsize == 1? this.substacks.get(0) : this.substacks.get(i));
				setChildIds(subStack, i);
				subStack.add(0,new EventInst(When.AFTER, Where.NESTED_SKELETON, strace, i));
				subStack.push(new EventInst(When.BEFORE, Where.NESTED_SKELETON, strace, i));
			}
			children.add(subStack);
		}
		stack.push(new EventInst(When.AFTER, Where.MERGE, strace, rbranch));
		stack.push(new MergeInst(merge, strace));
		stack.push(new EventInst(When.BEFORE, Where.MERGE, strace, rbranch));
	
		return params;
	}

	@Override
	public Instruction copy() {
		return new SplitInst(substacks, merge, copySkeletonTrace());
	}
}

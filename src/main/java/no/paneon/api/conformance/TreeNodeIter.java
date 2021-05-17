package no.paneon.api.conformance;

import java.util.Iterator;
import java.util.NoSuchElementException;

//
//inspired by https://github.com/gt4dev/yet-another-tree-structure 
//

public class TreeNodeIter<T> implements Iterator<TreeNode<T>> {

	enum ProcessStages {
		PROCESS_PARENT, 
		PROCESS_CHILD_CURRENT_NODE, 
		PROCESS_CHILD_SUB_NODE
	}

	private TreeNode<T> treeNode;

	public TreeNodeIter(TreeNode<T> treeNode) {
		this.treeNode = treeNode;
		this.doNext = ProcessStages.PROCESS_PARENT;
		this.childrenCurNodeIter = treeNode.getChildren().iterator();
	}

	private ProcessStages doNext;
	private TreeNode<T> next;
	private Iterator<TreeNode<T>> childrenCurNodeIter;
	private Iterator<TreeNode<T>> childrenSubNodeIter;

	@Override
	public boolean hasNext() {

		if (this.doNext == ProcessStages.PROCESS_PARENT) {
			this.next = this.treeNode;
			this.doNext = ProcessStages.PROCESS_CHILD_CURRENT_NODE;
			return true;
		}

		if (this.doNext == ProcessStages.PROCESS_CHILD_CURRENT_NODE) {
			if (childrenCurNodeIter.hasNext()) {
				TreeNode<T> childDirect = childrenCurNodeIter.next();
				childrenSubNodeIter = childDirect.iterator();
				this.doNext = ProcessStages.PROCESS_CHILD_SUB_NODE;
				return hasNext();
			}

			else {
				this.doNext = null;
				return false;
			}
		}
		
		if (this.doNext == ProcessStages.PROCESS_CHILD_SUB_NODE) {
			if (childrenSubNodeIter.hasNext()) {
				this.next = childrenSubNodeIter.next();
				return true;
			}
			else {
				this.next = null;
				this.doNext = ProcessStages.PROCESS_CHILD_CURRENT_NODE;
				return hasNext();
			}
		}

		return false;
	}

	@Override
	public TreeNode<T> next() {
	    if(!hasNext()){
	      throw new NoSuchElementException();
	    }
	    return this.next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}

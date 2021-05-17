package no.paneon.api.conformance;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//
// inspired by https://github.com/gt4dev/yet-another-tree-structure 
//
public class TreeNode<T> implements Iterable<TreeNode<T>> {

	private T data;
	private TreeNode<T> parent;
	private List<TreeNode<T>> children;

	public T getData() {
		return data;
	}

	public TreeNode<T> getParent() {
		return parent;
	}

	public List<TreeNode<T>> getChildren() {
		return children;
	}

	public boolean isRoot() {
		return parent == null;
	}

	public boolean isLeaf() {
		return children.isEmpty();
	}

	private List<TreeNode<T>> elementsIndex;

	public TreeNode(T data) {
		this.data = data;
		this.children = new LinkedList<>();
		this.elementsIndex = new LinkedList<>();
		this.elementsIndex.add(this);
	}

	public TreeNode<T> addChild(TreeNode<T> childNode) {
		childNode.parent = this;
		this.children.add(childNode);
		this.registerChildForSearch(childNode);
		return childNode;
	}

	public TreeNode<T> addChild(T child) {
		TreeNode<T> childNode = new TreeNode<>(child);
		childNode.parent = this;
		this.children.add(childNode);
		this.registerChildForSearch(childNode);
		return childNode;
	}

	public int getLevel() {
		if (this.isRoot())
			return 0;
		else
			return parent.getLevel() + 1;
	}

	private void registerChildForSearch(TreeNode<T> node) {
		elementsIndex.add(node);
		if (parent != null)
			parent.registerChildForSearch(node);
	}

	public TreeNode<T> findTreeNode(Comparable<T> cmp) {
		for (TreeNode<T> element : this.elementsIndex) {
			T elData = element.data;
			if (cmp.compareTo(elData) == 0)
				return element;
		}

		return null;
	}

	@Override
	public String toString() {
		return toString(0);
	}

	private static final String INDENTS = "                                                                   ";
	
	public String toString(int indent) {
		StringBuilder bld = new StringBuilder();
		
		bld.append( (data != null ? data.toString() : "[data null]") + "\n" );
	
		for(TreeNode<T> child : children) {
		    bld.append(INDENTS.substring(0,indent) + child.toString(indent+2));
		}
		return bld.toString();
	}
	
	@Override
	public Iterator<TreeNode<T>> iterator() {
		return new TreeNodeIter<>(this);
	}

}

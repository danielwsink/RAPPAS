/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tree;

import java.io.Serializable;
import java.text.NumberFormat;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author ben
 */
public class PhyloNode extends DefaultMutableTreeNode implements Serializable {

    private static final long serialVersionUID = 2010L;

    //node  metadata
    private int id=-1; //nodeId internal to the program
    private int externalId=-1;//to memorize the id that the node had eventually
    //in the external program used to build the posterior probabilites.
    private String label=null;
    private float branchLengthToAncestor=0.0f;    
    
    /** The number of leaves under this internal node (or 1 for leaves). */
    public int numberLeaves;
    /** Leftmost (minimum) leaf node under this internal node (or this node for leaves). */
    public PhyloNode leftmostLeaf;
    /** Rightmost (maximum) leaf node under this internal node (or this node for leaves). */
    public PhyloNode rightmostLeaf;
    /** The next preorder node. */
    public PhyloNode preorderNext = null;
    /** The next postorder node. */
    public PhyloNode posorderNext = null;

    NumberFormat nf=NumberFormat.getNumberInstance();

    
    
    /**
     * empty constructor for situation where node can be filled with metadata only later
     */
    public PhyloNode() {
        nf.setMinimumFractionDigits(3);
        nf.setMaximumFractionDigits(12);
    }
    
    
    public PhyloNode(int id, String label, float branchLengthToAncestor) {
        nf.setMinimumFractionDigits(3);
        nf.setMaximumFractionDigits(12);
        this.branchLengthToAncestor=branchLengthToAncestor;
        this.id=id;
        this.label=label;
    }

    public int getId() {
        return id;
    }
    
    public void setExternalId(int externalId) {
        this.externalId=externalId;
    }
    
    public int getExternalId() {
        return this.externalId;
    }
    
    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getBranchLengthToAncestor() {
        return branchLengthToAncestor;
    }

    public void setBranchLengthToAncestor(float branchLengthToAncestor) {
        this.branchLengthToAncestor = branchLengthToAncestor;
    }

    public int getNumberLeaves() {
        return numberLeaves;
    }

    public void setNumberLeaves(int numberLeaves) {
        this.numberLeaves = numberLeaves;
    }
    
    

    @Override
    public String toString() {
        //return this.hashCode()+" id:"+id+" label:"+label+" bl:"+nf.format(branchLengthToAncestor);
        //System.out.println("id:"+id+" extId:"+externalId+" label:"+label+" bl:"+nf.format(branchLengthToAncestor));
        return "id:"+id+" extId:"+externalId+" label:"+label+" bl:"+nf.format(branchLengthToAncestor);
    }

    
    /**
     * Set the extreme leaves for this node.  This is done in leaf->root direction, so all linking can be done in O(n) time.
     *
     */
    public void setExtremeLeaves() {
            if (isLeaf()) {
                    leftmostLeaf = this;
                    rightmostLeaf = this;
                    return;
            }
            leftmostLeaf = firstChild().leftmostLeaf;
            rightmostLeaf = lastChild().rightmostLeaf;
    }

    /** root->leaf traversal, depth first in direction of leftmost leaf. */
    public void linkNodesInPreorder() {
            if (isLeaf())
                    return;
            preorderNext = firstChild();
            for (int i = 0; i < getChildCount() - 1; i++)
                    getChildAt(i).rightmostLeaf.preorderNext = getChildAt(i + 1);
            // rightmostLeaf.preorderNext = null; // redundant
    }

    /** Leaf->root traversal, starting at leftmost leaf of tree. */
    public void linkNodesInPostorder() {
            if (isLeaf())
                    return;
            // n.posorderNext = null; // redundant
            for (int i = 0; i < getChildCount()- 1; i++)
                    getChildAt(i).posorderNext = getChildAt(i + 1).leftmostLeaf;
            lastChild().posorderNext = this;
    }

    /**
     * Sets the number of leaves, must be run on leaves first (pre-order)
     * 
     * @return The number of leaves ({@link #numberLeaves}) including the
     *         current node (leaves = 1)
     */
    public int setNumberLeaves() {
            numberLeaves = 0;
            if (isLeaf())
                    numberLeaves = 1;
            else
                    for (int i = 0; i < children.size(); i++)
                            numberLeaves += getChildAt(i).numberLeaves;
            return numberLeaves;
    }

    /** Get the first child of this node. Doesn't work with leaf nodes.
     * @return First child of this internal node.
     */
    protected PhyloNode firstChild() {
            return (PhyloNode) children.get(0);
    }

    /** Get the last child of this node. Doesn't work with leaf nodes.
     * @return Last child of this internal node.
     */
    public PhyloNode lastChild() {
            return (PhyloNode) children.get(children.size() - 1);
    }

    @Override
    public PhyloNode getChildAt(int index) {
        return (PhyloNode)super.getChildAt(index); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
}

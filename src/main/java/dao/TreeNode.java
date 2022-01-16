package dao;

import javax.swing.tree.MutableTreeNode;
import java.util.*;

public class TreeNode implements javax.swing.tree.TreeNode, MutableTreeNode {
    protected ArrayList<TreeNode> children = new ArrayList<>();
    protected TreeNode parentNullable = null;
    protected String nameNotNull;

    protected String overrideString = null;

    public TreeNode(String nameNotNull) {
        if (!nameNotNull.matches("[\\u4e00-\\u9fa5_a-zA-Z0-9.\\-:]+")) {
            throw new IllegalArgumentException("nameNotNull illegal");
        }
        this.nameNotNull = Objects.requireNonNull(nameNotNull);
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        final int size = children.size();
        if (childIndex < 0 || childIndex > size) {
            throw new IndexOutOfBoundsException();
        }
        return children.get(childIndex);
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public TreeNode getParent() {
        return parentNullable;
    }

    @Override
    public int getIndex(javax.swing.tree.TreeNode node) {
        if (!(node instanceof TreeNode)) {
            throw new ClassCastException("only accept ZkNode");
        }
        TreeNode t = (TreeNode) node;
        int size = children.size();
        for (int i = 0; i < size; i++) {
            if (children.get(i) == t) {
                return i;
            }
        }
        return -1;
    }

    public void setOverrideString(String overrideString) {
        this.overrideString = overrideString;
    }

    public String getNameNotNull() {
        return nameNotNull;
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public Enumeration<TreeNode> children() {
        return new ZkNodeEnumeration(this);
    }

    public List<TreeNode> fetchAllChildren() {
        return Collections.unmodifiableList(children);
    }

    protected void modifyParent(TreeNode parentNullable) {
        this.parentNullable = parentNullable;
    }

    @Override
    public String toString() {
        final String overrideString = this.overrideString;
        if (overrideString != null) {
            return overrideString;
        }
        return this.nameNotNull;
    }

    @Override
    public void insert(MutableTreeNode child, int index) {
        final boolean typeOk = child instanceof TreeNode;
        if (!typeOk) {
            throw new ClassCastException("child can not be cast to ZkNode");
        }
        TreeNode c = (TreeNode) child;
        c.setParent(this);
        children.add(index, c);

    }

    public void insert(MutableTreeNode child) {
        final int index = children.size();
        insert(child, index);
    }

    @Override
    public void remove(int index) {
        children.remove(index);
    }

    @Override
    public void remove(MutableTreeNode node) {
        if (!(node instanceof TreeNode)) {
            return;
        }
        if (!children.contains(node)) {
            return;
        }
        TreeNode node1 = (TreeNode) node;
        node1.setParent(null);
        children.remove(node1);
    }

    @Override
    public void setUserObject(Object object) {
        if (!(object instanceof String)) {
            return;
        }
        this.nameNotNull = (String) object;
    }

    @Override
    public void removeFromParent() {
        final TreeNode parent = this.parentNullable;
        if (parent == null) {
            return;
        }
        this.parentNullable = null;
        parent.remove(this);
    }

    @Override
    public void setParent(MutableTreeNode newParent) {
        if (null == newParent) {
            this.parentNullable = null;
            return;
        }

        if (!(newParent instanceof TreeNode)) {
            throw new ClassCastException("newParent is not instance of ZkNode");
        }
        this.parentNullable = (TreeNode) newParent;
    }

    public String getPathNotNull() {
        Stack<TreeNode> queue = new Stack<>();
        TreeNode index = this;
        StringBuilder stringBuilder = new StringBuilder();
        while (index != null) {
            queue.push(index);
            index = index.getParent();
        }
        while (true) {
            index = queue.pop();
            if (index == null) {
                break;
            }
            // 根节点的特殊处理
            if (index.getParent() == null && index.overrideString.compareToIgnoreCase("/") == 0) {
                if (this == index) {
                    stringBuilder.append('/');
                    break;
                }
                // ignore
            } else {
                stringBuilder.append(index.getNameNotNull());
            }
            if (queue.size() == 0) {
                break;
            } else {
                stringBuilder.append('/');
            }
        }
        return stringBuilder.toString();
    }
}

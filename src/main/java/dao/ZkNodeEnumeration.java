package dao;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Objects;

public class ZkNodeEnumeration implements Enumeration<TreeNode> {
    @Override
    public boolean hasMoreElements() {
        final int zkNodeNum = treeNode.getChildCount();
        return zkNodeNum > 0 && this.index < zkNodeNum;
    }

    @Override
    public TreeNode nextElement() {
        final int zkNodeNum = treeNode.getChildCount();
        final int nextIndex = this.index + 1;
        if(nextIndex >= zkNodeNum) {
            throw new IndexOutOfBoundsException(MessageFormat.format("index {0,number,#} must be lower than {1,number,1}",nextIndex, zkNodeNum));
        }
        return treeNode.getChildAt(nextIndex);
    }

    protected final TreeNode treeNode;
    protected int index = -1;

    public ZkNodeEnumeration(TreeNode treeNode) {
        this.treeNode = Objects.requireNonNull(treeNode);
    }
}

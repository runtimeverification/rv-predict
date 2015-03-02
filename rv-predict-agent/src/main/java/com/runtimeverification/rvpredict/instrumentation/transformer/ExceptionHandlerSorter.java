package com.runtimeverification.rvpredict.instrumentation.transformer;

import java.util.Collections;
import java.util.Comparator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * Sorts the exception handlers in a method innermost-to-outermost. This allows
 * the programmer to add handlers without worrying about ordering them correctly
 * with respect to existing, in-code handlers.
 *
 * Behavior is only defined for properly-nested handlers. If any "try" blocks
 * overlap (something that isn't possible in Java code) then this may not do
 * what you want.
 *
 * @author <a href= "http://homes.cs.washington.edu/~asampson/">Adrian Sampson</a>
 * @author YilongL
 */
public class ExceptionHandlerSorter extends MethodNode {

    private final MethodVisitor mv;

    public ExceptionHandlerSorter(MethodVisitor mv, int access, String name, String desc,
            String signature, String[] exceptions) {
        super(Opcodes.ASM5, access, name, desc, signature, exceptions);
        this.mv = mv;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visitEnd() {
        // unfortunately, the type parameter of `tryCatchBlocks' is erased
        Collections.sort(tryCatchBlocks, new TryCatchBlockLengthComparator());

        if (mv != null) {
            accept(mv);
        }
    }

    /**
     * Compares {@link TryCatchBlockNode}s by the length of their "try" block.
     */
    private class TryCatchBlockLengthComparator implements Comparator<TryCatchBlockNode> {
        @Override
        public int compare(TryCatchBlockNode node1, TryCatchBlockNode node2) {
            return blockLength(node1) - blockLength(node2);
        }

        private int blockLength(TryCatchBlockNode node) {
            InsnList insnList = ExceptionHandlerSorter.this.instructions;
            return insnList.indexOf(node.end) - insnList.indexOf(node.start);
        }
    }

}

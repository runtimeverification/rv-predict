package com.runtimeverification.rvpredict.instrument.transformer;

import java.util.Collections;
import java.util.Comparator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * {@link TryCatchBlockSorter} patched with Eric Bruneton's fix in <a href=
 * "http://forge.ow2.org/tracker/?func=detail&aid=317537&group_id=23&atid=100023"
 * >ASM bug [ #317537 ] Wrong tryCatch block sorting</a>.
 *
 * @see TryCatchBlockSorter
 *
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
        Comparator<TryCatchBlockNode> comp = new Comparator<TryCatchBlockNode>() {
            @Override
            public int compare(TryCatchBlockNode t1, TryCatchBlockNode t2) {
                int start1 = instructions.indexOf(t1.start);
                int start2 = instructions.indexOf(t2.start);
                if (start1 == start2) {
                    // Do not change the relative order of exception handlers
                    // for the same try catch block (this relies on the fact
                    // that Collections.sort is a stable sort).
                    return 0;
                }
                int len1 = instructions.indexOf(t1.end) - start1;
                int len2 = instructions.indexOf(t2.end) - start2;
                return len1 - len2;
            }
        };

        Collections.sort(tryCatchBlocks, comp);

        if (mv != null) {
            accept(mv);
        }
    }

}

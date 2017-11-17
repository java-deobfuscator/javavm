package com.javadeobfuscator.javavm;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExecutionOptions {

    private Set<AbstractInsnNode> _watchlist = new HashSet<>();
    private List<StackTraceHolder> _stacktrace = new ArrayList<>();

    public ExecutionOptions watch(AbstractInsnNode insn) {
        _watchlist.add(insn);
        return this;
    }

    public boolean shouldRecord(AbstractInsnNode target) {
        return !_watchlist.isEmpty() && _watchlist.contains(target);
    }

}

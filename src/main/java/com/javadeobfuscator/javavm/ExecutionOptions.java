package com.javadeobfuscator.javavm;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.*;
import java.util.function.Consumer;

public class ExecutionOptions {

    private Map<AbstractInsnNode, Consumer<BreakpointInfo>> _watchlist = new HashMap<>();
    private List<StackTraceHolder> _stacktrace = new ArrayList<>();

    public ExecutionOptions watch(AbstractInsnNode insn, Consumer<BreakpointInfo> consumer) {
        _watchlist.put(insn, consumer);
        return this;
    }

    public boolean shouldRecord(AbstractInsnNode target) {
        return !_watchlist.isEmpty() && _watchlist.containsKey(target);
    }

    public void notify(AbstractInsnNode node, BreakpointInfo info) {
        _watchlist.get(node).accept(info);
    }

    public static class BreakpointInfo {
        private Stack stack;
        private Locals locals;

        public BreakpointInfo(Stack stack, Locals locals) {
            this.stack = stack;
            this.locals = locals;
        }

        public Stack getStack() {
            return stack;
        }

        public Locals getLocals() {
            return locals;
        }
    }
}

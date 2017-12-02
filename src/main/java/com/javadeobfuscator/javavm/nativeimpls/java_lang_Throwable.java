/*
 * Copyright 2017 Sam Sun <github-contact@samczsun.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.javadeobfuscator.javavm.nativeimpls;

import com.javadeobfuscator.javavm.Cause;
import com.javadeobfuscator.javavm.Effect;
import com.javadeobfuscator.javavm.StackTraceHolder;
import com.javadeobfuscator.javavm.VirtualMachine;
import com.javadeobfuscator.javavm.hooks.HookGenerator;
import com.javadeobfuscator.javavm.mirrors.JavaClass;
import com.javadeobfuscator.javavm.values.JavaWrapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class java_lang_Throwable {
    private static final String THIS = "java/lang/Throwable";

    public static void registerNatives(VirtualMachine vm) {
        vm.hook(HookGenerator.generateUnknownHandlingHook(vm, THIS, "fillInStackTrace", "(I)Ljava/lang/Throwable;", false, Cause.NONE, Effect.NONE, (ctx, inst, args) -> {
            List<StackTraceHolder> stacktrace = vm.getStacktrace();

            while (!stacktrace.isEmpty()) {
                StackTraceHolder holder = stacktrace.get(0);
                if (holder.getMethod().name.equalsIgnoreCase("fillInStackTrace0") ||
                        holder.getMethod().name.equalsIgnoreCase("fillInStackTrace")) {
                    stacktrace.remove(0);
                    continue;
                }
                if (holder.getMethod().name.equals("<init>") && vm.getSystemDictionary().getJavaLangThrowable().isAssignableFrom(JavaClass.forName(vm, holder.getClassNode().name))) {
                    stacktrace.remove(0);
                    continue;
                }
                break;
            }
            inst.get().setMetadata("backtrace", stacktrace);

            return inst;
        }));
        vm.hook(HookGenerator.generateUnknownHandlingHook(vm, THIS, "getStackTraceDepth", "()I", false, Cause.NONE, Effect.NONE, (ctx, inst, args) -> {
            return JavaWrapper.createInteger(vm, ((List) inst.get().getMetadata("backtrace")).size());
        }));
        vm.hook(HookGenerator.generateUnknownHandlingHook(vm, THIS, "getStackTraceElement", "(I)Ljava/lang/StackTraceElement;", false, Cause.NONE, Effect.NONE, (ctx, inst, args) -> {
            JavaClass stackTraceElementClazz = JavaClass.forName(vm, "java/lang/StackTraceElement");

            List<StackTraceHolder> queue = inst.get().getMetadata("backtrace");

            StackTraceHolder elem = queue.get(args[0].asInt());

            int lineNumber = -1;
            if (Modifier.isNative(elem.getMethod().access)) {
                lineNumber = -2;
            } else {
                if (elem.getInstruction() != null) {
                    AbstractInsnNode target = elem.getInstruction();
                    List<LineNumberNode> lines = new ArrayList<>();
                    for (MethodNode m : elem.getClassNode().methods) {
                        if (m.instructions != null && m.instructions.getFirst() != null) {
                            for (AbstractInsnNode i = m.instructions.getFirst(); i.getNext() != null; i = i.getNext()) {
                                if (i instanceof LineNumberNode) {
                                    lines.add((LineNumberNode) i);
                                }
                            }
                        }
                    }
                    outer:
                    for (AbstractInsnNode i = target.getPrevious(); i != null; i = i.getPrevious()) {
                        if (i instanceof LabelNode) {
                            for (LineNumberNode ln : lines) {
                                if (ln.start.getLabel().equals(((LabelNode) i).getLabel())) {
                                    lineNumber = ln.line;
                                    break outer;
                                }
                            }
                        }
                    }
                }
            }

            return vm.newInstance(stackTraceElementClazz, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V",
                    vm.getString(elem.getClassNode().name.replace('/', '.')),
                    vm.getString(elem.getMethod().name),
                    vm.getString(elem.getClassNode().sourceFile),
                    JavaWrapper.createInteger(vm, lineNumber)
            );
        }));
    }
}

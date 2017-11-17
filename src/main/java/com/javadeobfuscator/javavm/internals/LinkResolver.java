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

package com.javadeobfuscator.javavm.internals;

import com.javadeobfuscator.javavm.VirtualMachine;
import com.javadeobfuscator.javavm.exceptions.ExecutionException;
import com.javadeobfuscator.javavm.mirrors.JavaMethod;
import com.javadeobfuscator.javavm.mirrors.JavaMethodHandle;
import com.javadeobfuscator.javavm.nativeimpls.java_lang_invoke_MethodHandleNatives;
import com.javadeobfuscator.javavm.values.JavaValueType;
import com.javadeobfuscator.javavm.values.JavaWrapper;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public class LinkResolver {
    private final VirtualMachine vm;

    public LinkResolver(VirtualMachine vm) {
        this.vm = vm;
    }

    public void resolve_virtual_call(CallInfo result, JavaWrapper recv, KlassHandle receiver_klass, KlassHandle resolved_klass,
                                     String method_name, String method_signature, KlassHandle current_klass,
                                     boolean check_access, boolean check_null_and_abstract) {
        MethodHandle resolved_method = new MethodHandle();
        linktime_resolve_virtual_method(resolved_method, resolved_klass, method_name, method_signature, current_klass, check_access);
        runtime_resolve_virtual_method(result, resolved_method, resolved_klass, recv, receiver_klass, check_null_and_abstract);
    }


    // throws linktime exceptions
    void linktime_resolve_virtual_method(MethodHandle resolved_method, KlassHandle resolved_klass,
                                         String method_name, String method_signature,
                                         KlassHandle current_klass, boolean check_access) {
        // normal method resolution
        resolveMethod(resolved_method, resolved_klass, method_name, method_signature, current_klass, check_access, true);

        // check if private interface method
        if (resolved_klass.get().isInterface() && resolved_method.get().isPrivate()) {
            String fmt = String.format("private interface method requires invokespecial, not invokevirtual: method %s, caller-class:%s", "tmp", "tmp");
            throw vm.newThrowable(VMSymbols.java_lang_IncompatibleClassChangeError, fmt);
        }

        // check if not static
        if (resolved_method.get().isStatic()) {
            String fmt = String.format("Expecting non-static method %s", "tmp");
            throw vm.newThrowable(VMSymbols.java_lang_IncompatibleClassChangeError, fmt);
        }
    }

    // throws runtime exceptions
    void runtime_resolve_virtual_method(CallInfo result,
                                        MethodHandle resolved_method,
                                        KlassHandle resolved_klass,
                                        JavaWrapper recv,
                                        KlassHandle recv_klass,
                                        boolean check_null_and_abstract
    ) {
        // setup default return values
        int vtable_index = JavaMethod.invalid_vtable_index;
        MethodHandle selected_method = new MethodHandle();

        // runtime method resolution
        if (check_null_and_abstract && recv.is(JavaValueType.NULL)) { // check if receiver exists
            throw vm.newThrowable(VMSymbols.java_lang_NullPointerException);
        }

        // do lookup based on receiver klass using the vtable index
        if (resolved_method.get().getDeclaringClass().isInterface()) { // default or miranda method
//            vtable_index = vtable_index_of_interface_method(resolved_klass,
//                    resolved_method);
//            assert(vtable_index >= 0 , "we should have valid vtable index at this point");
//
//            InstanceKlass* inst = InstanceKlass::cast(recv_klass());
//            selected_method = methodHandle(THREAD, inst->method_at_vtable(vtable_index));
            throw new ExecutionException("Unsupported");
        } else {
//            // at this point we are sure that resolved_method is virtual and not
//            // a default or miranda method; therefore, it must have a valid vtable index.
//            assert(!resolved_method->has_itable_index(), "");
//            vtable_index = resolved_method->vtable_index();
//            // We could get a negative vtable_index for final methods,
//            // because as an optimization they are they are never put in the vtable,
//            // unless they override an existing method.
//            // If we do get a negative, it means the resolved method is the the selected
//            // method, and it can never be changed by an override.
//            if (vtable_index == Method::nonvirtual_vtable_index) {
//                assert(resolved_method->can_be_statically_bound(), "cannot override this method");
//                selected_method = resolved_method;
//            } else {
//                // recv_klass might be an arrayKlassOop but all vtables start at
//                // the same place. The cast is to avoid virtual call and assertion.
//                InstanceKlass* inst = (InstanceKlass*)recv_klass();
//                selected_method = methodHandle(THREAD, inst->method_at_vtable(vtable_index));
//            }

//            vtable_index = -2; //resolved_method.get().getClassNode().methods.indexOf(resolved_method.get().getMethodNode());
            selected_method = resolved_method;
        }

        // check if method exists
        if (selected_method.isNull()) {
            throw new ExecutionException("asdf");
//            ResourceMark rm(THREAD);
//            THROW_MSG(vmSymbols::java_lang_AbstractMethodError(),
//                    Method::name_and_sig_as_C_string(resolved_klass(),
//                    resolved_method->name(),
//                    resolved_method->signature()));
        }

        // check if abstract
        if (check_null_and_abstract && selected_method.get().isAbstract()) {
            throw new ExecutionException("asdf");
//            ResourceMark rm(THREAD);
//            THROW_MSG(vmSymbols::java_lang_AbstractMethodError(),
//                    Method::name_and_sig_as_C_string(resolved_klass(),
//                    selected_method->name(),
//                    selected_method->signature()));
        }
        // setup result
        result.setVirtual(resolved_klass, recv_klass, resolved_method, selected_method, vtable_index);
    }

    public void resolveStaticCall(CallInfo result, KlassHandle resolvedClass, String methodName, String methodDesc, KlassHandle currentClass, boolean checkAccess, boolean initialize) {
        MethodHandle resolvedMethod = new MethodHandle();
        linktimeResolveStaticMethod(resolvedMethod, resolvedClass, methodName, methodDesc, currentClass, checkAccess);

        if (initialize && resolvedClass.get().shouldBeInitialized()) {
            vm.initialize(resolvedClass.get());
            linktimeResolveStaticMethod(resolvedMethod, resolvedClass, methodName, methodDesc, currentClass, checkAccess);
        }

        result.setStatic(resolvedClass, resolvedMethod);
    }

    public void linktimeResolveStaticMethod(MethodHandle resolvedMethod, KlassHandle resolvedClass, String methodName, String methodDesc, KlassHandle currentClass, boolean checkAccess) {
        if (!resolvedClass.get().isInterface()) {
            resolveMethod(resolvedMethod, resolvedClass, methodName, methodDesc, currentClass, checkAccess, false);
        } else {

        }

        if (!Modifier.isStatic(resolvedMethod.get().getMethodNode().access)) {
            String fmt = String.format("Expected static method %s", resolvedClass.get().getClassNode().name + "." + resolvedMethod.get().getMethodNode().name + resolvedMethod.get().getMethodNode().desc);
            throw vm.newThrowable(VMSymbols.java_lang_IncompatibleClassChangeError, fmt);
        }
    }

    public void resolveMethod(MethodHandle resolvedMethod, KlassHandle resolvedClass, String methodName, String methodSignature, KlassHandle currentClass, boolean checkAccess, boolean requireMethodRef) {
        if (requireMethodRef && resolvedClass.get().isInterface()) {
            String fmt = String.format("Found interface %s, but class was expected",
                    resolvedClass.get().getClassNode().name);
            throw vm.newThrowable(VMSymbols.java_lang_IncompatibleClassChangeError, fmt);
        }

        lookupMethodInClasses(resolvedMethod, resolvedClass, methodName, methodSignature, true, false);

        if (resolvedMethod.isNull() && !resolvedClass.get().isArray()) {
            lookupMethodsInInterfaces(resolvedMethod, resolvedClass, methodName, methodSignature);

            if (resolvedMethod.isNull()) {
                lookupPolymorphicMethod(resolvedMethod, resolvedClass, methodName, methodSignature, currentClass, null, null);
            }
        }

        if (resolvedMethod.isNull()) {
            String fmt = String.format("%s.%s%s",
                    resolvedClass.get().getClassNode().name, methodName, methodSignature);
            throw vm.newThrowable(VMSymbols.java_lang_NoSuchMethodError, fmt);
        }

        if (checkAccess) {
            // todo security stuff
        }
    }

    public void lookupMethodsInInterfaces(MethodHandle result, KlassHandle klass, String name, String signature) {
        // todo maybe should implement
        return;
    }

    public void lookupPolymorphicMethod(MethodHandle result, KlassHandle klass, String name, String fullSignature, KlassHandle currentClass, Object appendixOrNull, Object methodTypeResult) {
        VMSymbols.VMIntrinsics id = JavaMethodHandle.signaturePolymorphicNameId(name);

        if (/* EnableInvokeDynamic && */ klass.get() == vm.getSystemDictionary().getJavaLangInvokeMethodHandle()) {
            if (JavaMethodHandle.isSignaturePolymorphicIntrinsic(id)) {
                boolean keepLastArg = JavaMethodHandle.isSignaturePolymorphicStatic(id);
                String basicSignature = java_lang_invoke_MethodHandleNatives.lookupBasicTypeSignature(vm, fullSignature, keepLastArg);
                MethodHandle result1 = vm.getSystemDictionary().findMethodHandleIntrinsic(id, basicSignature);
                if (result1 != null) {
                    result.set(result1.get());
                }
            } else {
                throw new ExecutionException("Unsupported");
            }
        }
    }

    public void lookupMethodInClasses(MethodHandle result, KlassHandle klass, String name, String signature, boolean checkPolymorphism, boolean inImethodResolve) {
        JavaMethod resultOop = klass.get().uncachedLookupMethod(name, signature);
        if (klass.get().isArray()) {
            result.set(resultOop);
            return;
        }

        if (inImethodResolve &&
                resultOop != null &&
                klass.get().isInterface() &&
                (resultOop.isStatic() || !resultOop.isPublic()) &&
                resultOop.getDeclaringClass() == vm.getSystemDictionary().getJavaLangObject()) {
            resultOop = null;
        }

        if (resultOop == null) {
            resultOop = klass.get().findMethod(name, signature);
        }

        if (resultOop == null) {
            // Check default methods

        }

        if (checkPolymorphism && /* EnableInvokeDynamic */ resultOop != null) {
            // if is polymorphic, ignore
        }

        result.set(resultOop);
    }

    public void resolveField(FieldDescriptor fd, KlassHandle resolvedKlass, String field, String sig, KlassHandle currentKlass, int bytecode, boolean checkAccess, boolean initializeClass) {
        boolean is_static = bytecode == Opcodes.GETSTATIC || bytecode == Opcodes.PUTSTATIC;
        boolean is_put = bytecode == Opcodes.PUTFIELD || bytecode == Opcodes.PUTSTATIC;

        if (resolvedKlass.isNull()) {
            throw vm.newThrowable(VMSymbols.java_lang_NoSuchFieldError, field);
        }

        KlassHandle selKlass = new KlassHandle(resolvedKlass.get().findField(field, sig, fd));
        if (selKlass.isNull()) {
            throw vm.newThrowable(VMSymbols.java_lang_NoSuchFieldError, field);
        }

        if (!checkAccess) {
            return;
        }

        throw new ExecutionException("Checking access is unsupported");
    }
}

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

package com.javadeobfuscator.javavm;

import com.javadeobfuscator.javavm.ext.net.InterfaceAddress;
import com.javadeobfuscator.javavm.ext.net.NetworkInterface;
import com.javadeobfuscator.javavm.ext.net.*;
import com.javadeobfuscator.javavm.utils.*;
import com.javadeobfuscator.javavm.values.*;
import org.junit.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.ClassReader;

import javax.tools.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;

import static com.javadeobfuscator.javavm.TestHelper.*;

public class CompilationTest {
    private VirtualMachine _vm;

    @Before
    public void setup() throws ReflectiveOperationException {
        List<byte[]> jvmFiles = new ArrayList<>();
        jvmFiles.addAll(loadBytes(new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar")));
        jvmFiles.addAll(loadBytes(new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar")));
        jvmFiles.addAll(loadBytes(new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jsse.jar")));
        jvmFiles.addAll(loadBytes(new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jsse.jar")));
        jvmFiles.addAll(loadBytes(new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "ext" + File.separator + "sunjce_provider.jar")));
        jvmFiles.addAll(loadBytes(new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "ext" + File.separator + "sunec.jar")));

        System.out.println("Starting VM");
        _vm = new VirtualMachine(jvmFiles);
        _vm.fullInitialization();
        _vm.getFilesystem().map(new File("\\C:\\java_home_dir\\lib\\security\\java.security"), new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator + "java.security"));
        _vm.getFilesystem().map(new File("\\C:\\java_home_dir\\lib\\jce.jar"), new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar"));
        _vm.getFilesystem().map(new File("\\C:\\\\user_dir\\java_home_dir\\lib"), new File(System.getProperty("java.home") + File.separator + "lib"));
        _vm.getFilesystem().map(new File("\\C:\\\\user_dir\\java_home_dir\\lib\\jce.jar"), new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar"));
        _vm.getFilesystem().map(new File("\\C:\\\\user_dir\\user_dir\\java_home_dir\\lib\\jce.jar"), new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar"));
        _vm.getFilesystem().map(new File("\\C:\\java_home_dir\\lib\\meta-index"), new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "meta-index"));
        _vm.getFilesystem().map(new File("\\C:\\java_home_dir\\lib\\security\\US_export_policy.jar"), new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator + "US_export_policy.jar"));
        _vm.getFilesystem().map(new File("\\C:\\java_home_dir\\lib\\security\\local_policy.jar"), new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator + "local_policy.jar"));
        _vm.getFilesystem().map(new File("\\C:\\java_home_dir\\lib\\security\\cacerts"), new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator + "cacerts"));
        _vm.getFilesystem().map(new File("java_home_dir\\lib\\currency.data"), new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "currency.data"));
        try {
            _vm.getNetwork().addInterface(new NetworkInterface(
                    "lo",
                    "Software Loopback Interface 1",
                    Arrays.asList(
                            new InterfaceAddress(
                                    InetAddress.getByAddress(new byte[]{127, 0, 0, 1}),
                                    (Inet4Address) InetAddress.getByAddress(new byte[]{127, (byte) 255, (byte) 255, (byte) 255}),
                                    (short) 128
                            )
                    )
            ));
            _vm.getNetwork().addInterface(new NetworkInterface(
                    "eth0",
                    "Inten (R) Ethernet Connection",
                    Arrays.asList(
                            new InterfaceAddress(
                                    InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 1, 2}),
                                    (Inet4Address) InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, (byte) 1, (byte) 255}),
                                    (short) 24
                            )
                    )
            ));
//            _vm.getNetwork().registerDNSResolver((host, result) -> {
//                if (host.equals("www.google.com")) {
//                    try {
//                        result.add(InetAddress.getByAddress(new byte[]{1, 2, 3, 4}));
//                    } catch (UnknownHostException e) {
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            });
            _vm.getNetwork().registerDNSResolver(new SystemDNSResolver());
            _vm.getNetwork().registerConnectionEstablisher((socket, target) -> {
                try {
                    Socket actualSocket = new Socket(new java.net.Proxy(Proxy.Type.SOCKS, new InetSocketAddress(InetAddress.getLocalHost(), 8889)));
                    actualSocket.connect(target);
                    socket.setInput(actualSocket.getInputStream());
                    socket.setOutput(actualSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExecution() throws Throwable {
        String source = "import javax.crypto.Cipher;\n" +
                "import javax.crypto.spec.SecretKeySpec;\n" +
                "\n" +
                "public class Test {\n" +
                "    public static void main(String[] args) throws Throwable {\n" +
                "        run();\n" +
                "    }\n" +
                "\n" +
                "    public static void run() throws Throwable {\n" +
                "        Class.forName(\"java.lang.invoke.TypeConvertingMethodAdapter\").getName();" +
                "    }\n" +
                "}";

        File root = Files.createTempDirectory("test").toFile();
        File sourceFile = new File(root, "Test.java");
        File targetFile = new File(root, "Test.class");
        sourceFile.getParentFile().mkdirs();
        Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourceFile.getPath());

        Map<String, ClassNode> loaded;
        ClassNode target;

        loaded = load(targetFile);
        target = loaded.get("Test");

//        VirtualMachine.TRACE = true;
//        VirtualMachine.DEBUG_PRINT_EXCEPTIONS = true;
        {
            System.out.println("===EXECUTING WITHIN VM===");
            MethodNode targetMethod = ASMHelper.findMethod(target, "run", "()V");
            List<JavaWrapper> params = Arrays.asList();
            ExecutionOptions options = new ExecutionOptions();
            _vm.classpath(Arrays.asList(target));
            long start = System.nanoTime();
            MethodExecution execution = _vm.execute(target, targetMethod, null, params, options);
            long end = System.nanoTime();
            System.out.println("===EXECUTION COMPLETED (" + TimeUnit.NANOSECONDS.toMillis(end - start) + "ms) ===\r\nResult: " + execution.getReturnValue());
        }
    }
}
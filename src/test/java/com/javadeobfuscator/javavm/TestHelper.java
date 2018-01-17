package com.javadeobfuscator.javavm;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class TestHelper {

	private static byte[] extractStream(InputStream in) throws IOException {
    	byte[] buf = new byte[128];
    	int len;
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	while ((len = in.read(buf)) >= 0) {
    		out.write(buf, 0, len);
    	}
    	return out.toByteArray();
    }

	public static List<byte[]> loadBytes(File input) {
        List<byte[]> result = new ArrayList<>();

        if (input.getName().endsWith(".jar")) {
            try (ZipFile zipIn = new ZipFile(input)) {
                Enumeration<? extends ZipEntry> e = zipIn.entries();
                while (e.hasMoreElements()) {
                    ZipEntry next = e.nextElement();
                    if (next.getName().endsWith(".class")) {
                        try (InputStream in = zipIn.getInputStream(next)) {
                            result.add(extractStream(in));
                        } catch (IllegalArgumentException x) {
                            System.out.println("Could not parse " + next.getName() + " (is it a class?)");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
        } else if (input.getName().endsWith(".class")) {
            try (InputStream in = new FileInputStream(input)) {
                result.add(extractStream(in));
            } catch (Throwable x) {
                System.out.println("Could not parse " + input.getName() + " (is it a class?)");
            }
        }

        return result;
    }

	public static Map<String, ClassNode> load(File file) throws IOException {
        Map<String, ClassNode> map = new HashMap<>();

    	ClassReader reader = new ClassReader(new FileInputStream(file));
        ClassNode node = new ClassNode();
		reader.accept(node, 0 | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        map.put(node.name, node);

        return map;
    }
}
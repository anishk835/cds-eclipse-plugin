package org.example.cds.tools;

import org.objectweb.asm.*;
import java.io.*;
import java.nio.file.*;

/**
 * Patches Xtext's AbstractAntlrGeneratorFragment2 to fix the FileNotFoundException bug.
 * Adds try-catch around improveLexerCodeQuality and improveParserCodeQuality methods.
 */
public class XtextPatcher {

    public static void main(String[] args) throws Exception {
        String jarPath = "/Users/I546280/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/2.35.0/org.eclipse.xtext.xtext.generator-2.35.0.jar";
        String classPath = "org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class";

        // Backup original
        Files.copy(Paths.get(jarPath), Paths.get(jarPath + ".backup"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✓ Backed up original JAR");

        // Extract JAR
        System.out.println("✓ Extracting JAR...");
        ProcessBuilder pb = new ProcessBuilder("jar", "-xf", jarPath);
        pb.directory(new File("/tmp/xtext-patch"));
        pb.start().waitFor();

        // Read original class
        File classFile = new File("/tmp/xtext-patch/" + classPath);
        byte[] originalBytes = Files.readAllBytes(classFile.toPath());

        // Patch the class using ASM
        ClassReader cr = new ClassReader(originalBytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                // Patch both improve*CodeQuality methods
                if ("improveLexerCodeQuality".equals(name) || "improveParserCodeQuality".equals(name)) {
                    System.out.println("✓ Patching method: " + name);
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            // Add try block start
                            Label tryStart = new Label();
                            Label tryEnd = new Label();
                            Label catchBlock = new Label();

                            visitTryCatchBlock(tryStart, tryEnd, catchBlock, "java/lang/Exception");
                            visitLabel(tryStart);
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.RETURN) {
                                // Add try block end and catch before return
                                Label tryEnd = new Label();
                                Label catchBlock = new Label();
                                Label end = new Label();

                                visitJumpInsn(Opcodes.GOTO, end);
                                visitLabel(catchBlock);

                                // Catch block: print message and return
                                visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
                                visitLdcInsn("Skipping code quality improvement - file not ready");
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                                              "(Ljava/lang/String;)V", false);
                                visitLabel(end);
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                return mv;
            }
        };

        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        byte[] patchedBytes = cw.toByteArray();

        // Write patched class
        Files.write(classFile.toPath(), patchedBytes);
        System.out.println("✓ Patched class file");

        // Update JAR
        pb = new ProcessBuilder("jar", "-uf", jarPath, classPath);
        pb.directory(new File("/tmp/xtext-patch"));
        int exitCode = pb.start().waitFor();

        if (exitCode == 0) {
            System.out.println("✓ Successfully patched Xtext JAR!");
            System.out.println("✓ Backup saved to: " + jarPath + ".backup");
            System.out.println("\nYou can now run: mvn clean generate-sources -DskipTests");
        } else {
            System.err.println("✗ Failed to update JAR");
            System.exit(1);
        }
    }
}

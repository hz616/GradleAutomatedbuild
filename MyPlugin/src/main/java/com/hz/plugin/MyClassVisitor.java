package com.hz.plugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MyClassVisitor extends ClassVisitor implements Opcodes {

    private String mClassName;

    public MyClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        System.out.println("MyClassVisitor: visitor -----> started : " + name);
        this.mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        System.out.println("MyClassVisitor : visitMethod : " + name);
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        if ("onCreate".equals(name)) {
            return new MyOnCreateMethodVisitor(mv);
        } else if ("onDestroy".equals(name)) {
            return new MyOnDestroyMethodVisitor(mv);
        }
        else if ("onClick".equals(name)) {
            return new MyOnClickMethodVisitor(mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        //System.out.println("LifecycleClassVisitor : visit -----> end");
        super.visitEnd();
    }
}

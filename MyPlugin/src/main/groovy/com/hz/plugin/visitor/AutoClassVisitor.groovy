package com.hz.plugin.visitor


import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import com.hz.plugin.util.Logger

class AutoClassVisitor extends ClassVisitor {

    private String mClassName
    private String mSuperName
    private String[] mInterfaces
    private ClassVisitor classVisitor


    AutoClassVisitor(final ClassVisitor classVisitor) {
        super(Opcodes.ASM6, classVisitor)
        this.classVisitor = classVisitor
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        mClassName = name
        mInterfaces = interfaces
        mSuperName = superName
//        Logger.info("\n||---开始扫描类：${mClassName}")
//        Logger.info("||---类详情：version=${version};\taccess=${Logger.accCode2String(access)};\tname=${name};\tsignature=${signature};\tsuperName=${superName};\tinterfaces=${interfaces.toArrayString()}")
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = classVisitor.visitMethod(access, name, descriptor, signature, exceptions);
        MethodVisitor autoMethodVisitor = new AutoMethodVisitor(mv, access, name, descriptor, mSuperName, mClassName, mInterfaces)
        return autoMethodVisitor
    }


    @Override
    void visitEnd() {
//        Logger.info("||---结束扫描类：${mClassName}\n")
        super.visitEnd();
    }

}
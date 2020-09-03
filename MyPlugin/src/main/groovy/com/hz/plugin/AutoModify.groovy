package com.hz.plugin

import com.hz.plugin.visitor.AutoClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

class AutoModify {


    static byte[] modifyClasses(String className, byte[] srcByteCode) {
        byte[] classBytesCode = null
        try {
            classBytesCode = modifyClass(srcByteCode)
            //调试模式下再遍历一遍看修改的方法情况
//            if (Logger.isDebug()) {
//                seeModifyMethod(classBytesCode)
//            }
            return classBytesCode
        } catch (Exception e) {
            e.printStackTrace()
        }
        if (classBytesCode == null) {
            classBytesCode = srcByteCode
        }
        return classBytesCode
    }

    /**
     * 真正修改类中方法字节码
     */
    private static byte[] modifyClass(byte[] srcClass) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor adapter = new AutoClassVisitor(classWriter)
        ClassReader cr = new ClassReader(srcClass)
        cr.accept(adapter, ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }

}
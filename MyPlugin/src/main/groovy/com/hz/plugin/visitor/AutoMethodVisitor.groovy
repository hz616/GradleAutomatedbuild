package com.hz.plugin.visitor

import com.hz.plugin.LogHookConfig
import com.hz.plugin.bean.LogMethodCell
import com.hz.plugin.util.LogAnalyticsUtil
import com.hz.plugin.util.Logger
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class AutoMethodVisitor extends AdviceAdapter {


    String methodName
    int access
    MethodVisitor methodVisitor
    String methodDesc
    String superName
    String className
    String[] interfaces


    boolean isHasTracked = false

    protected AutoMethodVisitor(MethodVisitor methodVisitor, int access, String name, String desc, String superName, String className, String[] interfaces) {
        super(Opcodes.ASM6, methodVisitor, access, name, desc)
        this.methodName = name
        this.access = access
        this.methodVisitor = methodVisitor
        this.methodDesc = desc
        this.superName = superName
        this.className = className
        this.interfaces = interfaces
//        Logger.info("||开始扫描方法: ${Logger.accCode2String(access)} ${methodName} ${desc}")
    }

    @Override
    void visitEnd() {
        super.visitEnd()
//        Logger.info("||结束扫描方法: ${methodName}")
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter()

        //方法不是public 和 static直接返回
        if (!(LogAnalyticsUtil.isPublic(access)) && !LogAnalyticsUtil.isStatic(access)) return

        String methodNameDesc = methodName + methodDesc


        if (className.startsWith("android") || className.startsWith("androidx")) return

        Logger.info("||method name  is : $methodName and method desc is $methodDesc and access is ${Logger.accCode2String(access)}")

        if (methodName.trim().startsWith('lambda$') && LogAnalyticsUtil.isPrivate(access) && LogAnalyticsUtil.isSynthetic(access)) {
            LogMethodCell logMethodCell = LogHookConfig.sLambdaMethods.get(methodDesc)
            if (logMethodCell != null) {
                int paramStart = logMethodCell.paramsStart
                if (LogAnalyticsUtil.isStatic(access)) {
                    paramStart = paramStart - 1
                }
                Logger.info("paramStart is $paramStart and paramCount is ${logMethodCell.paramsCount}")
                LogAnalyticsUtil.visitMethodWithLoadedParams(methodVisitor, Opcodes.INVOKESTATIC, LogHookConfig.LOG_ANALYTICS_BASE,
                        logMethodCell.agentName, logMethodCell.agentDesc, paramStart, logMethodCell.paramsCount, logMethodCell.opcodes)
                isHasTracked = true
                return
            }
        }


        if (!isHasTracked) {
            if (methodNameDesc == 'onClick(Landroid/view/View;)V') {
                Logger.info("method enter and name: ${methodName}")
                methodVisitor.visitVarInsn(ALOAD, 1)
                methodVisitor.visitMethodInsn(INVOKESTATIC, LogHookConfig.LOG_ANALYTICS_BASE, "trackViewOnClick", "(Landroid/view/View;)V", false)
                isHasTracked = true
            }
        }


    }
}
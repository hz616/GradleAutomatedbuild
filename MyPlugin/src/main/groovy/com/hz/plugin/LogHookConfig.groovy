package com.hz.plugin

import com.hz.plugin.bean.LogMethodCell
import org.objectweb.asm.Opcodes

class LogHookConfig {


    public final static HashMap<String, LogMethodCell> sLambdaMethods = new HashMap<>()

    static {

        sLambdaMethods.put('(Landroid/view/View;)V', new LogMethodCell(
                'onClick',
                '(Landroid/view/View;)V',
                'android/view/View$OnClickListener',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                1, 1,
                [Opcodes.ALOAD]
        ))
    }

}
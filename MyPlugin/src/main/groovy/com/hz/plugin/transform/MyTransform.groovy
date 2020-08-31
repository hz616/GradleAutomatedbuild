package com.hz.plugin.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.hz.plugin.MyClassVisitor
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class MyTransform extends Transform {

    @Override
    String getName() {
        return "MyTransform"
    }


    /**
     * 需要处理的数据类型，目前 ContentType 有六种枚举类型，通常我们使用比较频繁的有前两种：
     * 1、CONTENT_CLASS：表示需要处理 java 的 class 文件。
     * 2、CONTENT_JARS：表示需要处理 java 的 class 与 资源文件。
     * 3、CONTENT_RESOURCES：表示需要处理 java 的资源文件。
     * 4、CONTENT_NATIVE_LIBS：表示需要处理 native 库的代码。
     * 5、CONTENT_DEX：表示需要处理 DEX 文件。
     * 6、CONTENT_DEX_WITH_RESOURCES：表示需要处理 DEX 与 java 的资源文件。
     * SCOPE_FULL_PROJECT 是一个 Scope 集合，包含 Scope.PROJECT, Scope.SUB_PROJECTS, Scope.EXTERNAL_LIBRARIES 这三项，即当前 Transform 的作用域包括当前项目、子项目以及外部的依赖库
     * @return
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }


    /**
     * 表示 Transform 要操作的内容范围，目前 Scope 有五种基本类型：
     * 1、PROJECT                   只有项目内容
     * 2、SUB_PROJECTS              只有子项目
     * 3、EXTERNAL_LIBRARIES        只有外部库
     * 4、TESTED_CODE               由当前变体（包括依赖项）所测试的代码
     * 5、PROVIDED_ONLY             只提供本地或远程依赖项
     * @return
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 是否需要支持增量更新
     * @return
     */
    @Override
    boolean isIncremental() {
        return false
    }


    /**
     * 进行具体的装换过程
     * @param transformInvocation
     * @throws TransformException* @throws InterruptedException* @throws IOException
     */
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        println("----------MyTransform visit start----------")
        def startTime = System.currentTimeMillis()
        def inputs = transformInvocation.inputs
        TransformOutputProvider outPutProvider = transformInvocation.outputProvider
        if (outPutProvider != null) {
            outPutProvider.deleteAll()
        }

        inputs.each { TransformInput input ->

                input.directoryInputs.each {
                    DirectoryInput directoryInput ->
                        handleDirectInput(directoryInput, outPutProvider)
                }

                //遍历jarInputs
                input.jarInputs.each { JarInput jarInput ->
                    //处理jarInputs
                    handleJarInputs(jarInput, outPutProvider)
                }
        }

        def cost = (System.currentTimeMillis() - startTime) / 1000
        println("----------MyTransform visit end----------")
        println("myTransform cost: $cost s")
    }

    static void handleDirectInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {
        //directoryInput.changedFiles  changeFile可以获取增量模式下的修改过的文件
        if (directoryInput.file.isDirectory()) {
            directoryInput.file.eachFileRecurse {
                File file ->
                    def name = file.name
                    println("deal class name is $name")
                    if (name.endsWith(".class") && !name.startsWith("R\$") && "R.class" != name
                            && "BuildConfig.class" != name && "MainActivity.class" == name) {
                        println("----------deal with class file <$name>")
                        ClassReader classReader = new ClassReader(file.bytes)
                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                        ClassVisitor cv = new MyClassVisitor(classWriter)
                        classReader.accept(cv, ClassReader.EXPAND_FRAMES)
                        byte[] code = classWriter.toByteArray()
                        FileOutputStream fos = new FileOutputStream(file.parentFile.absolutePath + File.separator + name)
                        fos.write(code)
                        fos.close()
                    }
            }
        }
        def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    static void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider) {
        if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
            //重名名输出文件,因为可能同名,会覆盖
            def jarName = jarInput.name
            def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries()
            File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_temp.jar")
            //避免上次的缓存被重复插入
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))
            //用于保存
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                //插桩class
                if (entryName.endsWith(".class") && !entryName.startsWith("R\$")
                        && !"R.class".equals(entryName) && !"BuildConfig.class".equals(entryName)
                        && "MainActivity.class".equals(entryName)) {
                    //class文件处理
                    println '----------- deal with "jar" class file <' + entryName + '> -----------'
                    jarOutputStream.putNextEntry(zipEntry)
                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new MyClassVisitor(classWriter)
                    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
                    byte[] code = classWriter.toByteArray()
                    jarOutputStream.write(code)
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }
            //结束
            jarOutputStream.close()
            jarFile.close()
            def dest = outputProvider.getContentLocation(jarName + md5Name,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR)
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }


    static boolean checkClassFile(String name) {
        return (name.endsWith(".class") && !name.startsWith("R\$") && "R.class" != name && "BuildConfig.class" != name)
    }
}
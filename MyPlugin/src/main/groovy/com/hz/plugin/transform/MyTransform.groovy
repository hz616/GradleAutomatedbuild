package com.hz.plugin.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.hz.plugin.AutoModify
import com.hz.plugin.util.AutoMatchUtil
import com.hz.plugin.util.AutoTextUtil
import com.hz.plugin.util.Logger
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import java.util.concurrent.Callable
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
        printlnJarAndDir(inputs)
        TransformOutputProvider outPutProvider = transformInvocation.outputProvider
        def context = transformInvocation.context

        if(!incremental && outPutProvider != null){
            outPutProvider.deleteAll()
        }

        def waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()//并发编译

        inputs.each { TransformInput input ->

            input.directoryInputs.each {
                DirectoryInput directoryInput ->
                    waitableExecutor.execute(new Callable<Object>() {
                        @Override
                        Object call() throws Exception {
                            handleDirectInput(directoryInput, outPutProvider, context)
                            return null
                        }
                    })

            }

            //遍历jarInputs
            input.jarInputs.each { JarInput jarInput ->
                waitableExecutor.execute(new Callable<Object>() {
                    @Override
                    Object call() throws Exception {
                        //处理jarInputs
                        handleJarInputs(jarInput, outPutProvider,context)
                        return null
                    }
                })


            }
        }
        waitableExecutor.waitForTasksWithQuickFail(true)

        def cost = (System.currentTimeMillis() - startTime) / 1000
        println("----------MyTransform visit end----------")
        println("myTransform cost: $cost s")
    }

    static void handleDirectInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider, Context context) {
        //directoryInput.changedFiles  changeFile可以获取增量模式下的修改过的文件
        def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
        Logger.info("||-->开始遍历特定目录  ${dest.absolutePath}")
        File dir = directoryInput.file
        if (dir) {
            HashMap<String, File> modifyMap = new HashMap<>()
            dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                File classFile ->
                    Logger.info("||-->file name is $classFile.name")
                    File modified = modifyClassFile(dir, classFile, context.getTemporaryDir())
                    if (modified != null) {
                        modifyMap.put(classFile.absolutePath.replace(dir.absolutePath, ""), modified)
                    }
            }
            FileUtils.copyDirectory(directoryInput.file, dest)
            modifyMap.entrySet().each {
                Map.Entry<String, File> en ->
                    File target = new File(dest.absolutePath + en.getKey())
                    if (target.exists()) {
                        target.delete()
                    }
                    FileUtils.copyFile(en.getValue(), target)
                    en.getValue().delete()
            }
        }
        Logger.info("||-->结束遍历特定目录  ${dest.absolutePath}")
    }

    /**
     * jar的处理逻辑必须有，即使不做任何处理
     * @param jarInput
     * @param outputProvider
     */
    static void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider,Context context) {

        String destName = jarInput.file.name
        /** 截取文件路径的md5值重命名输出文件,因为可能同名,会覆盖*/
        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4)
        }
        /** 获得输出文件*/
        File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        Logger.info("||-->开始遍历特定jar ${dest.absolutePath}")
//        modifyJarFile(jarInput.file, context.getTemporaryDir())
        def modifiedJar = null
        Logger.info("||-->结束遍历特定jar ${dest.absolutePath}")
        if (modifiedJar == null) {
            modifiedJar = jarInput.file
        }
        FileUtils.copyFile(modifiedJar, dest)
    }

    /**
     * Jar文件中修改对应字节码
     */
    private static File modifyJarFile(File jarFile, File tempDir) {
        if (jarFile) {
            return modifyJar(jarFile, tempDir, true)

        }
        return null
    }

    private static File modifyJar(File jarFile, File tempDir, boolean nameHex) {
        /**
         * 读取原jar
         */
        def file = new JarFile(jarFile)
        /** 设置输出到的jar */
        def hexName = ""
        if (nameHex) {
            hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        }
        def outputJar = new File(tempDir, hexName + jarFile.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream = file.getInputStream(jarEntry)

            String entryName = jarEntry.getName()
            String className

            ZipEntry zipEntry = new ZipEntry(entryName)

            jarOutputStream.putNextEntry(zipEntry)

            byte[] modifiedClassBytes = null
            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)
            if (entryName.endsWith(".class")) {
                className = entryName.replace("/", ".").replace(".class", "")
//                Logger.info("Jar:className:" + className)
                if (AutoMatchUtil.isShouldModifyClass(className)) {
                    modifiedClassBytes = AutoModify.modifyClasses(className, sourceClassBytes)
                }
            }
            if (modifiedClassBytes == null) {
                jarOutputStream.write(sourceClassBytes)
            } else {
                jarOutputStream.write(modifiedClassBytes)
            }
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        file.close()
        return outputJar
    }


    /**
     * 目录文件中修改对应字节码
     */
    private static File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        FileOutputStream outputStream = null
        try {
            String className = AutoTextUtil.path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
            if (AutoMatchUtil.isShouldModifyClass(className)) {
                byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
                byte[] modifiedClassBytes = AutoModify.modifyClasses(className, sourceClassBytes)
                if (modifiedClassBytes) {
                    modified = new File(tempDir, className.replace('.', '') + '.class')
                    if (modified.exists()) {
                        modified.delete()
                    }
                    modified.createNewFile()
                    outputStream = new FileOutputStream(modified)
                    outputStream.write(modifiedClassBytes)
                }
            } else {
                return classFile
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close()
                }
            } catch (Exception e) {
            }
        }
        return modified

    }

    /**
     * 包括两种数据:jar包和class目录，打印出来用于调试
     */
    private static void printlnJarAndDir(Collection<TransformInput> inputs) {

        def classPaths = []
        String buildTypes
        String productFlavors
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                classPaths.add(directoryInput.file.absolutePath)
                buildTypes = directoryInput.file.name
                productFlavors = directoryInput.file.parentFile.name
                Logger.info("||项目class目录：${directoryInput.file.absolutePath}")
            }
            input.jarInputs.each { JarInput jarInput ->
                classPaths.add(jarInput.file.absolutePath)
                Logger.info("||项目jar包：${jarInput.file.absolutePath}")
            }
        }
    }

}
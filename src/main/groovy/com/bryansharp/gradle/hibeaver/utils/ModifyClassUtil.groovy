package com.bryansharp.gradle.hibeaver.utils

import org.objectweb.asm.*

/**
 * Created by bryansharp(bsp0911932@163.com) on 2016/5/10.
 *
 * @author bryansharp
 * Project: FirstGradle
 * introduction:
 */
public class ModifyClassUtil {

    public
    static byte[] modifyClasses(String className, byte[] srcByteCode, List<Map<String, Object>> methodMatchMaps) {
        byte[] classBytesCode = null;
        try {
            Log.info("====start modifying ${className}====");
            classBytesCode = modifyClass(srcByteCode, methodMatchMaps);
            Log.info("====revisit modified ${className}====");
            onlyVisitClassMethod(classBytesCode, methodMatchMaps);
            Log.info("====finish modifying ${className}====");
            return classBytesCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (classBytesCode == null) {
            classBytesCode = srcByteCode;
        }
        return classBytesCode;
    }


    private
    static byte[] modifyClass(byte[] srcClass, List<Map<String, Object>> modifyMatchMaps) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassAdapter adapter = new MethodFilterClassAdapter(classWriter, modifyMatchMaps);
        ClassReader cr = new ClassReader(srcClass);
        cr.accept(adapter, ClassReader.SKIP_DEBUG);
        return classWriter.toByteArray();
    }

    private
    static void onlyVisitClassMethod(byte[] srcClass, List<Map<String, Object>> modifyMatchMaps) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        MethodFilterClassAdapter adapter = new MethodFilterClassAdapter(classWriter, modifyMatchMaps);
        adapter.onlyVisit = true;
        ClassReader cr = new ClassReader(srcClass);
        cr.accept(adapter, ClassReader.SKIP_DEBUG);
    }

    static class MethodFilterClassAdapter extends ClassAdapter implements Opcodes {
//        private String className;
        private List<Map<String, Object>> methodMatchMaps;
        public boolean onlyVisit = false;

        public MethodFilterClassAdapter(
                final ClassVisitor cv, List<Map<String, Object>> methodMatchMaps) {
            super(cv);
//            this.className = className;
            this.methodMatchMaps = methodMatchMaps;
        }

        @Override
        void visitEnd() {
            Log.logEach('* visitEnd *');
            super.visitEnd()
        }

        @Override
        void visitAttribute(Attribute attribute) {
            Log.logEach('* visitAttribute *', attribute, attribute.type, attribute.metaClass, attribute.metaPropertyValues, attribute.properties);
            super.visitAttribute(attribute)
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            Log.logEach('* visitAnnotation *', desc, visible);
            return super.visitAnnotation(desc, visible)
        }

        @Override
        void visitInnerClass(String name, String outerName,
                             String innerName, int access) {
            Log.logEach('* visitInnerClass *', name, outerName, innerName, Log.accCode2String(access));
            super.visitInnerClass(name, outerName, innerName, access)
        }

        @Override
        void visitOuterClass(String owner, String name, String desc) {
            Log.logEach('* visitOuterClass *', owner, name, desc);
            super.visitOuterClass(owner, name, desc)
        }

        @Override
        void visitSource(String source, String debug) {
            Log.logEach('* visitSource *', source, debug);
            super.visitSource(source, debug)
        }

        @Override
        FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            Log.logEach('* visitField *', Log.accCode2String(access), name, desc, signature, value);
            return super.visitField(access, name, desc, signature, value)
        }

        @Override
        public void visit(int version, int access, String name,
                          String signature, String superName, String[] interfaces) {
            Log.logEach('* visit *', Log.accCode2String(access), name, signature, superName, interfaces);
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name,
                                         String desc, String signature, String[] exceptions) {
            MethodVisitor myMv = null;
            if (!onlyVisit) {
                Log.logEach("* visitMethod *", Log.accCode2String(access), name, desc, signature, exceptions);
            }
            methodMatchMaps.each {
                Map<String, Object> map ->
                    String metName = map.get('methodName');
                    String methodDesc = map.get('methodDesc');
                    if (name.equals(metName)) {
                        Closure visit = map.get('adapter');
                        if (visit != null) {
                            if (methodDesc != null) {
                                if (methodDesc.equals(desc)) {
                                    if (onlyVisit) {
                                        myMv = new MethodLogAdapter(cv.visitMethod(access, name, desc, signature, exceptions));
                                    } else {
                                        try {
                                            myMv = visit(cv, access, name, desc, signature, exceptions);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            myMv = null
                                        }
                                    }
                                }
                            } else {
                                try {
                                    myMv = visit(cv, access, name, desc, signature, exceptions);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    myMv = null
                                }
                            }
                        }
                    }
            }
            if (myMv != null) {
                if (onlyVisit) {
                    Log.logEach("* revisitMethod *", Log.accCode2String(access), name, desc, signature);
                }
                return myMv;
            } else {
                return cv.visitMethod(access, name, desc, signature, exceptions);
            }
        }

    }

}
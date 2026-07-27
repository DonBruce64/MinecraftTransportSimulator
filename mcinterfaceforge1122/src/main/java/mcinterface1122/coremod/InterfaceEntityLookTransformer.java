package mcinterface1122.coremod;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Routes EntityLivingBase view vectors through the mouse-flight camera hook.
 * The hook returns the original vector for every entity and camera mode that
 * should retain vanilla behavior.
 */
public final class InterfaceEntityLookTransformer implements IClassTransformer {
    private static final String TARGET_CLASS = "net.minecraft.entity.EntityLivingBase";
    private static final String LOOK_DESCRIPTOR = "(F)Lnet/minecraft/util/math/Vec3d;";
    private static final String HOOK_OWNER = "mcinterface1122/WrapperEntity";
    private static final String HOOK_METHOD = "getMouseFlightViewVector";
    private static final String HOOK_DESCRIPTOR = "(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/Entity;F)Lnet/minecraft/util/math/Vec3d;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        final boolean[] transformed = {false};
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
                if (LOOK_DESCRIPTOR.equals(descriptor) && ("getLook".equals(methodName) || "func_70676_i".equals(methodName))) {
                    transformed[0] = true;
                    return new MethodVisitor(Opcodes.ASM5, visitor) {
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.ARETURN) {
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitVarInsn(Opcodes.FLOAD, 1);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, HOOK_METHOD, HOOK_DESCRIPTOR, false);
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                return visitor;
            }
        }, 0);

        if (!transformed[0]) {
            throw new IllegalStateException("Unable to patch EntityLivingBase#getLook for mouse-flight interaction.");
        }
        return writer.toByteArray();
    }
}

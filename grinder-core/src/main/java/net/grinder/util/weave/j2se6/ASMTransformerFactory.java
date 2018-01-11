// // Copyright (C) 2009 - 2012 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.util.weave.j2se6;

import static java.util.Collections.nCopies;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import net.grinder.util.Pair;
import net.grinder.util.weave.ClassSource;
import net.grinder.util.weave.ParameterSource;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.Weaver.TargetSource;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


/**
 * {@link ClassFileTransformerFactory} implementation that uses ASM to
 * advise methods.
 *
 * @author Philip Aston
 */
public final class ASMTransformerFactory
  implements ClassFileTransformerFactory {

  private final String m_adviceClass;

  /**
   * Constructor.
   *
   * <p>
   * We can't add fields to the class due to DCR limitations, so we have to wire
   * in the advice class using static methods.{@code adviceClass} should
   * implement methods with the following names and signatures.
   * </p>
   *
   * <pre>
   * public static void enter(Object reference,
   *                          String location);
   *
   * public static void exit(Object reference,
   *                         String location,
   *                         boolean success);
   *
   * public static void enter(Object reference,
   *                          Object reference2,
   *                          String location);
   *
   * public static void exit(Object reference,
   *                         Object reference2,
   *                         String location,
   *                         boolean success);
   * </pre>
   *
   * @param adviceClass
   *          Class that provides the advice.
   * @throws WeavingException
   *           If {@code adviceClass} does not implement {@code enter} and
   *           {@code exit} static methods.
   */
  public ASMTransformerFactory(final Class<?> adviceClass)
      throws WeavingException {

    try {
      final Method[] methods = {
        adviceClass.getMethod("enter",
                              Object.class,
                              String.class),
        adviceClass.getMethod("exit",
                              Object.class,
                              String.class,
                              Boolean.TYPE),
        adviceClass.getMethod("enter",
                              Object.class,
                              Object.class,
                              String.class),
        adviceClass.getMethod("exit",
                              Object.class,
                              Object.class,
                              String.class,
                              Boolean.TYPE),
      };

      for (final Method m : methods) {
        if (!Modifier.isStatic(m.getModifiers())) {
          throw new WeavingException(m + " is not static");
        }
      }
    }
    catch (final Exception e) {
      throw new WeavingException(
        adviceClass.getName() +
        " does not have expected enter and exit methods",
        e);
    }

    m_adviceClass = Type.getInternalName(adviceClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ClassFileTransformer create(final PointCutRegistry pointCutRegistry) {
    return new ASMTransformer(pointCutRegistry);
  }

  /**
   * {@link ClassFileTransformer} that advise methods using ASM.
   *
   * @author Philip Aston
   */
  private class ASMTransformer implements ClassFileTransformer {

    private final PointCutRegistry m_pointCutRegistry;

    /**
     * Constructor.
     *
     * <p>
     * Each method has at most one advice. If the class is re-transformed,
     * perhaps with additional advised methods, we will be passed the original
     * class byte code . We rely on a {@link PointCutRegistry} to remember which
     * methods to advise.
     * </p>
     *
     * @param pointCutRegistry
     *          Remembers the methods to advice, and the location strings.
     */
    public ASMTransformer(final PointCutRegistry pointCutRegistry) {
      m_pointCutRegistry = pointCutRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override public byte[] transform(final ClassLoader loader,
                                      final String internalClassName,
                                      final Class<?> classBeingRedefined,
                                      final ProtectionDomain protectionDomain,
                                      final byte[] originalBytes)
      throws IllegalClassFormatException {

      // The PointCutRegistry provides us the constructors and methods to advise
      // organised by class. This allows us quickly to find the right methods,
      // and ignore classes that aren't to be advised. (Important, since we're
      // called for every class that is loaded).
      final Map<Constructor<?>, List<WeavingDetails>>
        constructorToWeavingDetails =
          m_pointCutRegistry.getConstructorPointCutsForClass(internalClassName);

      final Map<Method, List<WeavingDetails>> methodToWeavingDetails =
        m_pointCutRegistry.getMethodPointCutsForClass(internalClassName);

      final int size =
        (constructorToWeavingDetails != null ?
         constructorToWeavingDetails.size() : 0) +
        (methodToWeavingDetails != null ? methodToWeavingDetails.size() : 0);

      if (size == 0) {
        return null;
      }

      // Having found the right set of constructors methods, we transform the
      // key to a form that is easier for our ASM visitor to use.
      final Map<Pair<String, String>, List<WeavingDetails>>
        nameAndDescriptionToWeavingDetails =
          new HashMap<Pair<String, String>, List<WeavingDetails>>(size);

      if (constructorToWeavingDetails != null) {
        for (final Entry<Constructor<?>, List<WeavingDetails>> entry :
             constructorToWeavingDetails.entrySet()) {

          final Constructor<?> c = entry.getKey();

          // The key will be unique, so we can set the value directly.
          nameAndDescriptionToWeavingDetails.put(
                  Pair.of("<init>", Type.getConstructorDescriptor(c)),
                  entry.getValue());
        }
      }

      if (methodToWeavingDetails != null) {
        for (final Entry<Method, List<WeavingDetails>> entry :
             methodToWeavingDetails.entrySet()) {

          final Method m = entry.getKey();

          // The key will be unique, so we can set the value directly.
          nameAndDescriptionToWeavingDetails.put(
                  Pair.of(m.getName(), Type.getMethodDescriptor(m)),
                  entry.getValue());
        }
      }

      final ClassReader classReader = new ClassReader(originalBytes);

      final ClassWriter classWriter =
        new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

      ClassVisitor visitorChain = classWriter;

      // Uncomment to see the generated code:
//      visitorChain =
//        new TraceClassVisitor(visitorChain, new PrintWriter(System.err));

      visitorChain = new AddAdviceClassAdapter(
                           visitorChain,
                           Type.getType("L" + internalClassName + ";"),
                           nameAndDescriptionToWeavingDetails);

      // Uncomment to see the original code:
//      visitorChain =
//        new TraceClassVisitor(visitorChain, new PrintWriter(System.out));

      classReader.accept(visitorChain, 0);

      return classWriter.toByteArray();
    }
  }

  private final class AddAdviceClassAdapter extends ClassAdapter {

    private final Type m_internalClassType;
    private final Map<Pair<String, String>,
                      List<WeavingDetails>> m_weavingDetails;

    private AddAdviceClassAdapter(
      final ClassVisitor classVisitor,
      final Type internalClassType,
      final Map<Pair<String, String>, List<WeavingDetails>> weavingDetails) {

      super(classVisitor);
      m_internalClassType = internalClassType;
      m_weavingDetails = weavingDetails;
    }

    @Override
    public void visit(final int originalVersion,
                      final int access,
                      final String name,
                      final String signature,
                      final String superName,
                      final String[] interfaces) {

      cv.visit(Math.max(originalVersion & 0xFFFF, Opcodes.V1_5),
               access,
               name,
               signature,
               superName,
               interfaces);
    }

    @Override
    public MethodVisitor visitMethod(final int access,
                                     final String name,
                                     final String desc,
                                     final String signature,
                                     final String[] exceptions) {

      final MethodVisitor defaultVisitor =
        cv.visitMethod(access, name, desc, signature, exceptions);

      final List<WeavingDetails> weavingDetails =
        m_weavingDetails.get(Pair.of(name, desc));

      if (weavingDetails != null) {
        assert defaultVisitor != null;

        return new AdviceMethodVisitor(defaultVisitor,
                                       m_internalClassType,
                                       access,
                                       name,
                                       weavingDetails);
      }

      return defaultVisitor;
    }
  }

  private final Map<Weaver.TargetSource, TargetExtractor> m_extractors =
    new HashMap<Weaver.TargetSource, TargetExtractor>() { {
      put(ClassSource.CLASS, new ClassTargetExtractor());
      put(ParameterSource.FIRST_PARAMETER,
          new LocalVariableTargetExtractor(0));
      put(ParameterSource.SECOND_PARAMETER,
          new LocalVariableTargetExtractor(1));
      put(ParameterSource.THIRD_PARAMETER,
          new LocalVariableTargetExtractor(2));
    } };

  private TargetExtractor getExtractor(final Weaver.TargetSource source) {
    return m_extractors.get(source);
  }

  private interface TargetExtractor {
    void extract(ContextMethodVisitor methodVisitor);
  }

  private static class LocalVariableTargetExtractor implements TargetExtractor {
    private final int m_variableNumber;

    public LocalVariableTargetExtractor(final int variableNumber) {
      m_variableNumber = variableNumber;
    }

    @Override
    public void extract(final ContextMethodVisitor methodVisitor) {
      methodVisitor.visitVarInsn(Opcodes.ALOAD, m_variableNumber);
    }
  }

  private static class ClassTargetExtractor implements TargetExtractor {
    @Override
    public void extract(final ContextMethodVisitor methodVisitor) {
      methodVisitor.visitLdcInsn(methodVisitor.getInternalClassName());
    }
  }

  private interface ContextMethodVisitor extends MethodVisitor {
    Type getInternalClassName();
  }

  /**
   * <p>
   * Generate our advice.
   * </p>
   * <p>
   * Originally this was based on
   * {@link org.objectweb.asm.commons.AdviceAdapter}. This had the following
   * problems:
   * </p>
   * <ul>
   * <li>
   * 95% of {@code AdviceAdapter} code exists to call
   * {@link org.objectweb.asm.commons.AdviceAdapter#onMethodEnter()} for a
   * constructor after it has called {@code this()} or {@code super()}. This
   * seems unnatural for our purposes, we really want to wrap our {@code
   * TRYCATCHBLOCK} around the whole constructor.</li>
   * <li>
   * {@code AdviceAdapter} doesn't handle exceptions that propagate through the
   * method, so we must add our own {@code TRYCATCHBLOCK} handling. We need to
   * ignore add code before other adapters in the chain {@see
   * #visitTryCatchBlock}), which the {@code onMethodEnter} callback doesn't let
   * us do.</li>
   * <li>
   * The {@code AdviceAdapter} class Javadoc doesn't match the implementation -
   * a smell.</li>
   * <li>
   * {@code AdviceAdapter} was the only reason we required the ASM {@code
   * commons} jar.</li>
   *
   * </ul>
   *
   * @author Philip Aston
   */
  private final class AdviceMethodVisitor
    extends MethodAdapter implements ContextMethodVisitor, Opcodes {

    private final Type m_internalClassType;
    private final List<WeavingDetails> m_weavingDetails;

    private final Label m_entryLabel = new Label();
    private final Label m_exceptionExitLabel = new Label();
    private boolean m_tryCatchBlockNeeded = true;
    private boolean m_entryCallNeeded = true;

    private AdviceMethodVisitor(final MethodVisitor mv,
                                final Type internalClassType,
                                final int access,
                                final String name,
                                final List<WeavingDetails> weavingDetails) {
      super(mv);

      m_internalClassType = internalClassType;
      m_weavingDetails = weavingDetails;
    }

    private void generateTryCatchBlock() {
      if (m_tryCatchBlockNeeded) {
        super.visitTryCatchBlock(m_entryLabel,
                                 m_exceptionExitLabel,
                                 m_exceptionExitLabel,
                                 null);

        m_tryCatchBlockNeeded = false;
      }
    }

    @Override
    public Type getInternalClassName() {
      return m_internalClassType;
    }

    private void generateEntryCall() {
      if (m_entryCallNeeded) {
        m_entryCallNeeded = false;

        super.visitLabel(m_entryLabel);

        for (final WeavingDetails weavingDetails : m_weavingDetails) {
          final List<TargetSource> targetSources =
              weavingDetails.getTargetSources();

          for (final TargetSource targetSource : targetSources) {
            getExtractor(targetSource).extract(this);
          }

          super.visitLdcInsn(weavingDetails.getLocation());

          super.visitMethodInsn(INVOKESTATIC,
                                m_adviceClass,
                                "enter",
                                entryMethodDescriptor(targetSources.size()));
        }
      }
    }

    private void generateEntryBlocks() {
      generateTryCatchBlock();
      generateEntryCall();
    }

    private void generateExitCall(final boolean success) {
      // Iterate in reverse.
      final ListIterator<WeavingDetails> i =
        m_weavingDetails.listIterator(m_weavingDetails.size());

      while (i.hasPrevious()) {
        final WeavingDetails weavingDetails = i.previous();
        final List<TargetSource> targetSources =
            weavingDetails.getTargetSources();

        for (final TargetSource targetSource : targetSources) {
          getExtractor(targetSource).extract(this);
        }

        super.visitLdcInsn(weavingDetails.getLocation());

        super.visitInsn(success ? ICONST_1 : ICONST_0);

        super.visitMethodInsn(INVOKESTATIC,
                              m_adviceClass,
                              "exit",
                              exitMethodDescriptor(targetSources.size()));
      }
    }

    /**
     * To nest well if another similar transformation has been done. we must
     * ensure that any existing top level TryCatchBlock comes first. Otherwise
     * our TryCatchBlock would have higher precedence, and other catch blocks
     * would be skipped.
     *
     * <p>
     * This is the reason for the delayed generateEntryBlock*() calls.
     * Unfortunately, this considerably adds to the complexity of this adapter.
     * </p>
     */
    @Override public void visitTryCatchBlock(final Label start,
                                             final Label end,
                                             final Label handler,
                                             final String type) {

      super.visitTryCatchBlock(start, end, handler, type);
      generateTryCatchBlock();
    }

    @Override public void visitLabel(final Label label) {
      generateEntryBlocks();
      super.visitLabel(label);
    }

    @Override public void visitFrame(final int type,
                                     final int nLocal,
                                     final Object[] local,
                                     final int nStack,
                                     final Object[] stack) {
      generateEntryBlocks();
      super.visitFrame(type, nLocal, local, nStack, stack);
    }

    @Override
    public void visitInsn(final int opcode) {
      generateEntryBlocks();

      switch (opcode) {
        case RETURN:
        case IRETURN:
        case FRETURN:
        case ARETURN:
        case LRETURN:
        case DRETURN:
          generateExitCall(true);
          break;

        default:
          break;
      }

      super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
      generateEntryBlocks();
      super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
      generateEntryBlocks();
      super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
      generateEntryBlocks();
      super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(final int opcode,
                               final String owner,
                               final String name,
                               final String desc) {
      generateEntryBlocks();
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(final int opcode,
                                final String owner,
                                final String name,
                                final String desc) {
      generateEntryBlocks();
      super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
      generateEntryBlocks();
      super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(final Object cst) {
      generateEntryBlocks();
      super.visitLdcInsn(cst);
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
      generateEntryBlocks();
      super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(final int min,
                                     final int max,
                                     final Label dflt,
                                     final Label[] labels) {
      generateEntryBlocks();
      super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt,
                                      final int[] keys,
                                      final Label[] labels) {
      generateEntryBlocks();
      super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
      generateEntryBlocks();
      super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override public void visitMaxs(final int maxStack, final int maxLocals) {
      super.visitLabel(m_exceptionExitLabel);
      generateExitCall(false);
      super.visitInsn(ATHROW);       // Re-throw.
      super.visitMaxs(maxStack, maxLocals);
    }
  }

  private static final Type STRING_TYPE =
      Type.getObjectType(Type.getInternalName(String.class));
  private static final Type OBJECT_TYPE =
      Type.getObjectType(Type.getInternalName(Object.class));

  private static List<Type> parameterSignature(final int numberOfTargets) {
    return new ArrayList<Type>(nCopies(numberOfTargets, OBJECT_TYPE));
  }

  private static String entryMethodDescriptor(final int numberOfTargets) {
    final List<Type> parameters = parameterSignature(numberOfTargets);
    parameters.add(STRING_TYPE);

    return Type.getMethodDescriptor(Type.VOID_TYPE,
                                    parameters.toArray(
                                      new Type[parameters.size()]));
  }

  private static String exitMethodDescriptor(final int numberOfTargets) {
    final List<Type> parameters = parameterSignature(numberOfTargets);
    parameters.add(STRING_TYPE);
    parameters.add(Type.BOOLEAN_TYPE);

    return Type.getMethodDescriptor(Type.VOID_TYPE,
                                    parameters.toArray(
                                      new Type[parameters.size()]));
  }
}

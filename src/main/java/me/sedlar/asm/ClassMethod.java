package me.sedlar.asm;

import me.sedlar.asm.pattern.nano.AdvancedNanoPattern;
import me.sedlar.asm.pattern.nano.SimpleNanoPattern;
import me.sedlar.asm.pattern.nano.calling.*;
import me.sedlar.asm.pattern.nano.flow.control.Exceptions;
import me.sedlar.asm.pattern.nano.flow.control.Looping;
import me.sedlar.asm.pattern.nano.flow.control.StraightLine;
import me.sedlar.asm.pattern.nano.flow.data.*;
import me.sedlar.asm.pattern.nano.oop.FieldReader;
import me.sedlar.asm.pattern.nano.oop.FieldWriter;
import me.sedlar.asm.pattern.nano.oop.ObjectCreator;
import me.sedlar.asm.pattern.nano.oop.TypeManipulator;
import me.sedlar.asm.util.Assembly;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

/**
 * @author Tyler Sedlar
 * @since 3/8/15
 */
public class ClassMethod {

    private static final SimpleNanoPattern[] SIMPLE_NANO_PATTERNS = {
            new NoParams(), new NoReturn(), new Chained(), new Recursive(), new SameName(), new Leaf(), // Calling
            new StraightLine(), new Looping(), new Exceptions(), // Control Flow
    };

    private static final AdvancedNanoPattern[] ADVANCED_NANO_PATTERNS = {
            new ObjectCreator(), new FieldReader(), new FieldWriter(), new TypeManipulator(), // Object-Oriented
            new LocalReader(), new LocalWriter(), new ArrayCreator(), new ArrayReader(), new ArrayWriter() // Data Flow
    };

    private static final Map<String, ClassMethod> cached = new HashMap<>();

    public final ClassFactory owner;
    public final MethodNode method;
    public final Handle handle;

    private List<String> simpleNanoPatterns, advancedNanoPatterns;

    public ClassMethod(ClassFactory owner, MethodNode method) {
        this.owner = owner;
        this.method = method;
        this.handle = new Handle(0, owner.node.name, method.name, method.desc);
        cached.put(key(), this);
    }

    /**
     * Calls MethodNode#accept with the given visitor.
     *
     * @param cmv The visitor to call.
     */
    public void accept(ClassMethodVisitor cmv) {
        cmv.method = this;
        method.accept(cmv);
    }

    /**
     * Gets this method's name.
     *
     * @return The name of this method.
     */
    public String name() {
        return method.name;
    }

    /**
     * Sets the name of this method.
     *
     * @param name The name to set this method's name to.
     */
    public void setName(String name) {
        method.name = name;
    }

    /**
     * Gets this method's desc.
     *
     * @return The desc of this method.
     */
    public String desc() {
        return method.desc;
    }

    /**
     * Sets the desc of this method.
     *
     * @param desc The desc to set this method's desc to.
     */
    public void setDescriptor(String desc) {
        method.desc = desc;
    }

    /**
     * Gets this method's access flags.
     *
     * @return The access flags of this method.
     */
    public int access() {
        return method.access;
    }

    /**
     * Sets this method's access flags.
     *
     * @param access The access flags to set this method's access to.
     */
    public void setAccess(int access) {
        method.access = access;
    }

    /**
     * Checks whether this method is non-static.
     *
     * @return <t>true</t> if this method is non-static, otherwise <t>false</t>.
     */
    public boolean local() {
        return (access() & ACC_STATIC) == 0;
    }

    /**
     * Gets the InsnList for this method.
     *
     * @return The InsnList for this method.
     */
    public InsnList instructions() {
        return method.instructions;
    }

    /**
     * Gets this method's key label (class.name + "." + method.name + method.desc)
     *
     * @return This method's key label (class.name + "." + method.name + method.desc)
     */
    public String key() {
        return owner.node.name + "." + method.name + method.desc;
    }

    /**
     * Removes this method from its class.
     */
    public void remove() {
        owner.remove(this);
    }

    /**
     * Gets the amount of instructions matching the given opcode.
     *
     * @param opcode The opcode to match.
     * @return The amount of instructions matching the given opcode.
     */
    public int count(int opcode) {
        return Assembly.count(instructions(), insn -> insn.getOpcode() == opcode);
    }

    /**
     * Gets the amount of instructions matching the given predicate.
     *
     * @param predicate The predicate to match.
     * @return The amount of instructions matching the given predicate.
     */
    public int count(Predicate<AbstractInsnNode> predicate) {
        return Assembly.count(instructions(), predicate);
    }

    /**
     * Gets the methods that call this method.
     *
     * @param classes The ClassFactory map to search.
     * @return The methods that call this method.
     */
    public List<MethodInsnNode> callers(Map<String, ClassFactory> classes) {
        List<MethodInsnNode> callers = new ArrayList<>();
        for (ClassFactory factory : classes.values()) {
            for (ClassMethod method : factory.methods) {
                for (AbstractInsnNode ain : method.instructions().toArray()) {
                    if (ain instanceof MethodInsnNode) {
                        MethodInsnNode min = (MethodInsnNode) ain;
                        if ((min.owner + "." + min.name + min.desc).equals(key())) {
                            callers.add(min);
                        }
                    }
                }
            }
        }
        return callers;
    }

    /**
     * Checks whether this method returns a desc of the class it's in.
     *
     * @return <t>true</t> if this method returns a desc of the class it's in, otherwise <t>false</t>.
     */
    public boolean chained() {
        return local() && desc().endsWith(")L" + owner.name() + ";");
    }

    /**
     * Checks whether this method calls a method matching the given predicate or not.
     *
     * @param predicate The predicate to match.
     * @return <t>true</t> if this method calls a method matching the given predicate, otherwise <t>false</t>.
     */
    public boolean calls(Predicate<MethodInsnNode> predicate) {
        return count(insn -> insn instanceof MethodInsnNode && predicate.test((MethodInsnNode) insn)) > 0;
    }

    /**
     * Gets a list of simple nano-patterns that are used within this method.
     *
     * @param cached Whether to used the cached list from prior lookups or not.
     * @return A list of simple nano-patterns that are used within this method.
     */
    public List<String> findSimpleNanoPatterns(boolean cached) {
        if (cached && simpleNanoPatterns != null) {
            return simpleNanoPatterns;
        }
        List<String> matching = new ArrayList<>();
        for (SimpleNanoPattern pattern : SIMPLE_NANO_PATTERNS) {
            if (pattern.matches(this)) {
                matching.add(pattern.info().name());
            }
        }
        return (simpleNanoPatterns = matching);
    }

    /**
     * Gets a list of simple nano-patterns that are used within this method.
     *
     * @return A list of simple nano-patterns that are used within this method.
     */
    public List<String> findSimpleNanoPatterns() {
        return findSimpleNanoPatterns(true);
    }

    /**
     * Checks whether all the given simple nano-patterns are used in this method.
     *
     * @param patterns The patterns to match.
     * @return <t>true</t> if all the given simple nano-patterns are used in this method, otherwise <t>false</t>.
     */
    public boolean hasSimpleNanoPatterns(String... patterns) {
        List<String> patternList = findSimpleNanoPatterns();
        for (String pattern : patterns) {
            if (!patternList.contains(pattern)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets a list of advanced nano-patterns that are used within this method.
     *
     * @param cached Whether to used the cached list from prior lookups or not.
     * @return A list of advanced nano-patterns that are used within this method.
     */
    public List<String> findAdvancedNanoPatterns(boolean cached) {
        if (cached && advancedNanoPatterns != null) {
            return advancedNanoPatterns;
        }
        List<String> matching = new ArrayList<>();
        AbstractInsnNode[] instructions = instructions().toArray();
        for (AbstractInsnNode insn : instructions) {
            for (AdvancedNanoPattern pattern : ADVANCED_NANO_PATTERNS) {
                if (pattern.matches(insn)) {
                    matching.add(pattern.info().name());
                }
            }
        }
        return (advancedNanoPatterns = matching);
    }

    /**
     * Gets a list of simple nano-patterns that are used within this method.
     *
     * @return A list of simple nano-patterns that are used within this method.
     */
    public List<String> findAdvancedNanoPatterns() {
        return findAdvancedNanoPatterns(true);
    }

    /**
     * Checks whether all the given advanced nano-patterns are used in this method.
     *
     * @param patterns The patterns to match.
     * @return <t>true</t> if all the given advanced nano-patterns are used in this method, otherwise <t>false</t>.
     */
    public boolean hasAdvancedNanoPatterns(String... patterns) {
        List<String> patternList = findAdvancedNanoPatterns();
        for (String pattern : patterns) {
            if (!patternList.contains(pattern)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }

    /**
     * Gets the amount of parameters in this method's desc.
     *
     * @return The amount of parameters in this method's desc.
     */
    public int parameters() {
        return Type.getArgumentTypes(desc()).length;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ClassMethod && ((ClassMethod) o).key().equals(key())) ||
                (o instanceof MethodNode && method.equals(o));
    }

    /**
     * Gets the ClassMethod matching the given key, if it is within the cache.
     *
     * @param key The key to match.
     * @return The ClassMethod matching the given key, if it is within the cache.
     */
    public static ClassMethod resolve(String key) {
        return cached.get(key);
    }
}
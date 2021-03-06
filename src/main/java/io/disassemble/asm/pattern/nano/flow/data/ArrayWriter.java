package io.disassemble.asm.pattern.nano.flow.data;

import io.disassemble.asm.pattern.nano.AdvancedNanoPattern;
import io.disassemble.asm.pattern.nano.PatternInfo;
import org.objectweb.asm.tree.AbstractInsnNode;

import static org.objectweb.asm.Opcodes.IASTORE;
import static org.objectweb.asm.Opcodes.SASTORE;

/**
 * @author Tyler Sedlar
 * @since 2/1/16
 */
@PatternInfo(category = "Data Flow", name = "ArrayWriter", simple = false, description = "writes values to an array")
public class ArrayWriter extends AdvancedNanoPattern {

    @Override
    public boolean matches(AbstractInsnNode insn) {
        return insn.getOpcode() >= IASTORE && insn.getOpcode() <= SASTORE;
    }
}

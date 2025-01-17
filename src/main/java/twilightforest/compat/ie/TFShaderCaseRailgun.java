package twilightforest.compat.ie;

import blusunrize.immersiveengineering.api.shader.ShaderLayer;
import blusunrize.immersiveengineering.api.shader.impl.ShaderCaseRailgun;

import java.util.Collection;

// Fix for the uncolored top layer not rendering
public class TFShaderCaseRailgun extends ShaderCaseRailgun {
    private final int STACK_BREAK;

    public TFShaderCaseRailgun(int stackBreak, Collection<ShaderLayer> layers) {
        super(layers);
        this.STACK_BREAK = stackBreak;
    }

    @Override
    public boolean shouldRenderGroupForPass(String part, int pass) {
        if("sled".equals(part) || "wires".equals(part) || "tubes".equals(part))
            return pass >= STACK_BREAK - 1;

        if("grip".equals(part))
            return pass == 0;

        return pass != 0;
    }
}
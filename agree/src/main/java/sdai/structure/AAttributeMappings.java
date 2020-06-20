package sdai.structure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AAttributeMappings {
    public static final Map<String, String> logical = new HashMap<String, String>();
    public static final Map<String, String> bool = new HashMap<String, String>();
	public static final Set<Class> labelClasses = new HashSet<Class>();
    static {
        logical.put("1", ".F.");  logical.put("2", ".T."); logical.put("3", ".U.");
        bool.put("true", ".T.");  bool.put("false", ".F.");
    }
}

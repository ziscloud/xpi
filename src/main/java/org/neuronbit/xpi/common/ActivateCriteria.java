package org.neuronbit.xpi.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ActivateCriteria {
    private Map<String, String> params;

    public ActivateCriteria() {
    }

    public ActivateCriteria(Map<String, String> params) {
        this.params = params;
    }

    public ActivateCriteria(String key, String value) {
        final HashMap<String, String> params = new HashMap<>(1);
        params.put(key, value);
        this.params = params;
    }

    public String getParameter(String key) {
        if (null == params) {
            return null;
        }
        return params.get(key);
    }

    public String getParameter(String key, String defaultValue) {
        if (null == params) {
            return defaultValue;
        }

        final String value = params.get(key);
        if (null != value) {
            return value;
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        String paramsStr = "";
        if (null == params) {
            paramsStr = "[]";
        } else {
            StringBuilder sb = new StringBuilder("[");
            final Set<String> keySet = params.keySet();
            final int size = keySet.size();
            int count = 0;
            for (String key : keySet) {
                sb.append(key);
                sb.append("=");
                sb.append(params.get(key));
                if (count < size - 1) {
                    sb.append(",");
                }
                count++;
            }
            sb.append("]");
            paramsStr = sb.toString();
        }
        return "ActivateCriteria" + paramsStr;
    }
}

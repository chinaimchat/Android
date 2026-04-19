package com.chat.base.endpoint;

import com.chat.base.utils.WKReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 2020-09-01 18:17
 */
public class EndpointManager {
    private EndpointManager() {
    }

    private static class EndpointManagerBinder {
        final static EndpointManager manager = new EndpointManager();
    }

    public static EndpointManager getInstance() {
        return EndpointManagerBinder.manager;
    }

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Endpoint>> endpointList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Endpoint> endpointBySid = new ConcurrentHashMap<>();

    private void register(String sid, String category, int sort, EndpointHandler iHandler) {
        CopyOnWriteArrayList<Endpoint> endpoints = endpointList.computeIfAbsent(category, key -> new CopyOnWriteArrayList<>());
        Endpoint endpoint = new Endpoint(sid, category, sort, iHandler);
        endpoints.add(endpoint);
        endpointBySid.put(sid, endpoint);
    }

    public void setMethod(String sid, EndpointHandler EndpointHandler) {
        register(sid, "", 0, EndpointHandler);
    }

    public void setMethod(String sid, String category, EndpointHandler EndpointHandler) {
        register(sid, category, 0, EndpointHandler);
    }

    public void setMethod(String sid, String category, int sort, EndpointHandler EndpointHandler) {
        register(sid, category, sort, EndpointHandler);
    }

    public void remove(String sid) {
        if (endpointList.isEmpty()) {
            return;
        }
        endpointBySid.remove(sid);
        for (Map.Entry<String, CopyOnWriteArrayList<Endpoint>> entry : endpointList.entrySet()) {
            CopyOnWriteArrayList<Endpoint> list = entry.getValue();
            if (WKReader.isNotEmpty(list)) {
                list.removeIf(endpoint -> endpoint != null && sid.equals(endpoint.sid));
                if (list.isEmpty()) {
                    endpointList.remove(entry.getKey(), list);
                }
            }
        }
    }

    public Object invoke(String sid, Object param) {
        if (endpointList.isEmpty()) {
            return null;
        }
        Endpoint endpoint = endpointBySid.get(sid);
        if (endpoint == null) {
            endpoint = findEndpointBySid(sid);
            if (endpoint != null) {
                endpointBySid.put(sid, endpoint);
            }
        }
        if (endpoint != null && endpoint.iHandler != null) {
            return endpoint.iHandler.invoke(param);
        }
        return endpoint;
    }

    private Endpoint findEndpointBySid(String sid) {
        Endpoint endpoint = null;
        for (CopyOnWriteArrayList<Endpoint> list : endpointList.values()) {
            if (WKReader.isNotEmpty(list)) {
                int max = list.size() - 1;
                for (int i = max; i >= 0; i--) {
                    Endpoint temp = list.get(i);
                    if (temp != null && sid.equals(temp.sid)) {
                        endpoint = temp;
                        break;
                    }
                }
            }
            if (endpoint != null) {
                break;
            }
        }
        return endpoint;
    }

    @SuppressWarnings("unchecked")
    public <K> List<K> invokes(String category, Object object) {
        List<K> list = new ArrayList<>();
        if (endpointList.isEmpty()) {
            return list;
        }
        CopyOnWriteArrayList<Endpoint> tempList = endpointList.get(category);
        if (tempList == null || tempList.isEmpty()) {
            return list;
        }
        List<Endpoint> sortedList = new ArrayList<>(tempList);
        Collections.sort(sortedList);
        for (Endpoint endpoint : sortedList) {
            if (endpoint != null && endpoint.iHandler != null) {
                K result = (K) endpoint.iHandler.invoke(object);
                if (result != null) {
                    list.add(result);
                }
            }
        }
        return list;
    }
}
